package com.parking.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.parking.service.ChatService;
import com.parking.dao.ParkingSpaceMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 讯飞星火大模型服务实现类
 * 使用 WebSocket 连接讯飞星火 API v3.5
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Value("${iflytek.spark.appid}")
    private String appId;

    @Value("${iflytek.spark.api-key}")
    private String apiKey;

    @Value("${iflytek.spark.api-secret}")
    private String apiSecret;

    @Value("${iflytek.spark.chat-url}")
    private String chatUrl;
    
    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;

    private final Gson gson = new Gson();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // NLU 指令提示词模板（返回JSON格式）- 支持时间提取
    // 关键改进：注入当前时间，要求AI提取时间信息
    private static final String NLU_PROMPT_TEMPLATE = 
            "你是一个智能停车场助手。当前系统时间是：【%s】。\n\n" + // 关键：注入当前时间
            "请分析用户的语音指令，判断用户意图并提取关键信息。\n\n" +
            "【重要】关键词识别规则：\n" +
            "1. 停车场相关关键词：停车场、停车位、车位、停车、泊车、停车库、停车楼、停车区、停车点等\n" +
            "2. 地址相关关键词：\n" +
            "   - 位置修饰词：附近、旁边、周围、一带、周边、邻近等\n" +
            "   - 行政区划：路、街、道、区、县、市、广场、大厦、商场、中心、公园、桥、站等\n" +
            "3. 指令关键词：预约、预订、查找、找、搜索、查询、查、导航、去、到、取消、距离等\n" +
            "4. 时间相关关键词：今天、明天、后天、上午、下午、晚上、早上、中午、X点、X点半、X点X分、X小时后、X分钟后等\n\n" +
            "【意图类型】（注意：取消预约的优先级最高）：\n" +
            "1. CANCEL_RESERVATION - 用户想取消预约（如\"取消预约\"、\"取消预订\"、\"不预约\"、\"删除预约\"、\"撤销预约\"等）。如果同时包含\"取消\"和\"预约\"关键词，优先识别为取消预约\n" +
            "2. RESERVE_NEARBY - 用户想预约附近某个地点的停车场（如\"预约北京路附近的停车场\"、\"预约下午四点的停车场\"）\n" +
            "3. RESERVE_SPACE - 用户想预约特定车位（如\"预约A101车位\"）\n" +
            "4. FIND_NEARBY - 用户想查找附近停车场（如\"附近有什么停车场\"、\"查找天河区的停车场\"、\"北京路附近有什么停车场\"）\n" +
            "5. NAVIGATE - 用户想导航到某个地点（如\"导航到北京路\"、\"去万菱汇停车场\"）\n" +
            "6. QUERY_RESERVATION - 用户想查询预约状态（如\"查询我的预约\"、\"查看预约订单\"）\n" +
            "7. QUERY_UNPAID - 用户想查询未支付订单（如\"查询未支付订单\"、\"待支付订单\"）\n" +
            "8. QUERY_DISTANCE - 用户想查询距离（如\"距离停车场多远\"、\"到停车场有多远\"）\n" +
            "9. UNKNOWN - 无法识别或普通聊天\n\n" +
            "【提取规则】：\n" +
            "- destination（目的地/地址）：提取地点名称，如\"北京路\"、\"天河区\"、\"万菱汇\"等。如果指令是\"预约XX附近的停车场\"，则destination为\"XX\"\n" +
            "- parkingLotName（停车场名称）：提取完整的停车场名称，如\"万菱汇停车场\"、\"天河城停车位\"等。如果只是提到\"停车场\"而没有具体名称，则留空\n" +
            "- targetTime（目标时间）：如果用户提到了具体时间，请提取并转换为标准格式（yyyy-MM-dd HH:mm:ss）。\n" +
            "  时间转换规则：\n" +
            "  * \"今天下午四点\" -> 今天的16:00:00（如果当前时间已过16:00，则顺延到明天）\n" +
            "  * \"明天上午九点\" -> 明天的09:00:00\n" +
            "  * \"后天下午两点半\" -> 后天的14:30:00\n" +
            "  * \"下午四点半\" -> 今天的16:30:00（如果当前时间已过16:30，则顺延到明天）\n" +
            "  * \"2小时后\" -> 当前时间+2小时\n" +
            "  * \"30分钟后\" -> 当前时间+30分钟\n" +
            "  * 如果用户没有提到时间，则targetTime留空\n\n" +
            "请以JSON格式返回结果，格式如下：\n" +
            "{\n" +
            "  \"intent\": \"意图类型（如RESERVE_NEARBY）\",\n" +
            "  \"entities\": {\n" +
            "    \"destination\": \"地点名称（如北京路，如果适用）\",\n" +
            "    \"parkingLotName\": \"停车场名称（如果适用）\",\n" +
            "    \"targetTime\": \"用户提到的具体时间点（格式：yyyy-MM-dd HH:mm:ss），如果未提及时间则留空\"\n" +
            "  }\n" +
            "}\n\n" +
            "【示例】：\n" +
            "假设当前时间是2025-11-23 15:30:00\n" +
            "用户说\"预约下午四点的停车场\" -> {\"intent\":\"RESERVE_NEARBY\",\"entities\":{\"targetTime\":\"2025-11-23 16:00:00\"}}\n" +
            "用户说\"预约明天上午九点天河城停车场\" -> {\"intent\":\"RESERVE_NEARBY\",\"entities\":{\"parkingLotName\":\"天河城停车场\",\"targetTime\":\"2025-11-24 09:00:00\"}}\n" +
            "用户说\"预约后天下午两点半的停车位\" -> {\"intent\":\"RESERVE_NEARBY\",\"entities\":{\"targetTime\":\"2025-11-25 14:30:00\"}}\n" +
            "用户说\"预约北京路附近的停车场\" -> {\"intent\":\"RESERVE_NEARBY\",\"entities\":{\"destination\":\"北京路\"}}\n" +
            "用户说\"取消预约\" -> {\"intent\":\"CANCEL_RESERVATION\",\"entities\":{}}\n" +
            "用户说\"取消天河城停车场的预约\" -> {\"intent\":\"CANCEL_RESERVATION\",\"entities\":{\"parkingLotName\":\"天河城停车场\"}}\n" +
            "用户说\"附近有什么停车场\" -> {\"intent\":\"FIND_NEARBY\",\"entities\":{}}\n" +
            "用户说\"你好\" -> {\"intent\":\"UNKNOWN\",\"entities\":{}}";

    // 聊天模式提示词
    private static final String CHAT_PROMPT = "你是一个智能停车场助手，名字叫波波。你友好、专业，可以帮助用户预约停车位、查询附近停车场、导航等功能。请用简洁、友好的方式回复用户。";

    @Override
    public String getNluResponse(String text) {
        try {
            // 1. 动态构建提示词，填入当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTime = sdf.format(new Date());
            String finalPrompt = String.format(NLU_PROMPT_TEMPLATE, currentTime);
            
            System.out.println("========== NLU请求 ==========");
            System.out.println("当前系统时间: " + currentTime);
            System.out.println("用户输入: " + text);
            System.out.println("=============================");
            
            // 2. 调用大模型
            String response = callSparkAPI(text, finalPrompt, null);
            
            if (response == null || response.trim().isEmpty()) {
                System.err.println("NLU响应为空，返回默认UNKNOWN");
                return "{\"intent\":\"UNKNOWN\",\"entities\":{}}";
            }
            
            // 3. 清理响应中的 Markdown 代码块标记（```json ... ```）
            // 大模型有时会画蛇添足加这些标记，导致 JSON 解析失败
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```")) {
                System.out.println("检测到Markdown代码块标记，正在清理...");
                cleanResponse = cleanResponse.replaceAll("```json", "").replaceAll("```", "").trim();
                System.out.println("清理后的响应: " + cleanResponse);
            }
            
            // 4. 尝试解析JSON验证格式
            try {
                JsonObject json = gson.fromJson(cleanResponse, JsonObject.class);
                if (json.has("intent")) {
                    System.out.println("========== NLU解析成功 ==========");
                    System.out.println("意图: " + json.get("intent").getAsString());
                    if (json.has("entities")) {
                        JsonObject entities = json.getAsJsonObject("entities");
                        if (entities.has("targetTime") && !entities.get("targetTime").getAsString().isEmpty()) {
                            System.out.println("提取到时间: " + entities.get("targetTime").getAsString());
                        }
                        if (entities.has("destination")) {
                            System.out.println("提取到目的地: " + entities.get("destination").getAsString());
                        }
                        if (entities.has("parkingLotName")) {
                            System.out.println("提取到停车场名称: " + entities.get("parkingLotName").getAsString());
                        }
                    }
                    System.out.println("==================================");
                    return cleanResponse; // 返回清理后的JSON
                }
            } catch (Exception jsonEx) {
                System.err.println("========== JSON解析失败 ==========");
                System.err.println("原始响应: " + response);
                System.err.println("清理后响应: " + cleanResponse);
                System.err.println("错误信息: " + jsonEx.getMessage());
                System.err.println("==================================");
                
                // 如果不是JSON格式，尝试解析为旧格式（向后兼容）
                System.out.println("尝试解析为旧格式...");
                try {
                    String[] parts = response.split("\\|");
                    String intent = parts[0].trim().toUpperCase();
                    String destination = parts.length > 1 ? parts[1].trim() : "";
                    
                    // 转换为JSON格式
                    JsonObject json = new JsonObject();
                    json.addProperty("intent", intent);
                    JsonObject entities = new JsonObject();
                    if (!destination.isEmpty()) {
                        entities.addProperty("destination", destination);
                    }
                    json.add("entities", entities);
                    return gson.toJson(json);
                } catch (Exception e) {
                    System.err.println("旧格式解析也失败: " + e.getMessage());
                }
            }
            
            // 5. 如果都失败了，返回默认UNKNOWN
            System.err.println("所有解析方式都失败，返回默认UNKNOWN");
            return "{\"intent\":\"UNKNOWN\",\"entities\":{}}";
            
        } catch (Exception e) {
            System.err.println("========== NLU处理异常 ==========");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常消息: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==================================");
            return "{\"intent\":\"UNKNOWN\",\"entities\":{}}";
        }
    }

    @Override
    public String getChatResponse(String text, java.util.List<java.util.Map<String, Object>> conversationHistory) {
        try {
            System.out.println("开始调用讯飞星火API，用户输入: " + text);
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                System.out.println("收到对话历史，共 " + conversationHistory.size() + " 条");
            }
            System.out.println("配置信息 - APPID: " + appId + ", API-Key: " + (apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "null"));
            System.out.println("配置信息 - Chat URL: " + chatUrl);
            
            // 1. 查询实时可用车位数量（如果查询失败，不影响整体聊天）
            int emptyCount = -1;
            try {
                emptyCount = parkingSpaceMapper.countAvailableSpaces();
                System.out.println("实时空车位数量: " + emptyCount);
            } catch (Exception e) {
                System.err.println("统计空车位数量失败，不影响聊天功能: " + e.getMessage());
            }
            
            // 2. 动态拼接带有实时数据的提示词
            String dynamicPrompt = CHAT_PROMPT;
            if (emptyCount >= 0) {
                dynamicPrompt = CHAT_PROMPT + "\n\n当前系统实时数据：全系统剩余空车位约为 " + emptyCount + " 个。"
                        + "当用户询问\"现在还有多少车位\"\"还有位吗\"等类似问题时，请结合这个实时数字进行回答。";
            }
            
            // 3. 将动态提示词和对话历史传给大模型
            String response = callSparkAPI(text, dynamicPrompt, conversationHistory);
            
            if (response == null || response.trim().isEmpty()) {
                System.err.println("警告: 讯飞API返回空响应");
                return "抱歉，我现在有点忙，请稍后再试。";
            }
            
            System.out.println("讯飞API调用成功，响应长度: " + response.length());
            return response;
        } catch (Exception e) {
            System.err.println("========== 聊天处理异常 ==========");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常消息: " + e.getMessage());
            System.err.println("异常原因: " + (e.getCause() != null ? e.getCause().getMessage() : "无"));
            
            // 输出更详细的错误信息
            if (e.getMessage() != null) {
                String errorMsg = e.getMessage().toLowerCase();
                if (errorMsg.contains("401") || errorMsg.contains("unauthorized") || errorMsg.contains("鉴权")) {
                    System.err.println("错误类型: 鉴权失败 - 请检查 appid、api-key 和 api-secret 是否正确");
                } else if (errorMsg.contains("403") || errorMsg.contains("forbidden")) {
                    System.err.println("错误类型: 权限不足 - 请确认 APPID 是否开通了 v3.5 版本权限");
                } else if (errorMsg.contains("timeout") || errorMsg.contains("超时")) {
                    System.err.println("错误类型: 连接超时 - 请检查网络连接或防火墙设置");
                } else if (errorMsg.contains("connection") || errorMsg.contains("连接")) {
                    System.err.println("错误类型: 连接失败 - 无法连接到讯飞服务器");
                } else if (errorMsg.contains("signature") || errorMsg.contains("签名")) {
                    System.err.println("错误类型: 签名错误 - 请检查系统时间是否准确");
                }
            }
            
            System.err.println("完整堆栈跟踪:");
            e.printStackTrace();
            System.err.println("==================================");
            
            return "抱歉，我现在有点忙，请稍后再试。";
        }
    }

    /**
     * 调用讯飞星火API
     * @param userText 用户当前输入
     * @param systemPrompt 系统提示词
     * @param conversationHistory 对话历史（最近5次对话），可以为null
     */
    private String callSparkAPI(String userText, String systemPrompt, java.util.List<java.util.Map<String, Object>> conversationHistory) throws Exception {
        // 验证配置
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalArgumentException("讯飞 APPID 未配置");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("讯飞 API-Key 未配置");
        }
        if (apiSecret == null || apiSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("讯飞 API-Secret 未配置");
        }
        if (chatUrl == null || chatUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("讯飞 Chat URL 未配置");
        }
        
        // 生成鉴权URL
        System.out.println("正在生成鉴权URL...");
        String authUrl = generateAuthUrl();
        System.out.println("鉴权URL生成成功（已隐藏敏感信息）");
        
        // 构建消息数组（用于payload.message.text）
        // 注意：v3.5 API可能不支持system角色，需要将系统提示词合并到用户消息中
        JsonArray textMessages = new JsonArray();
        
        // 如果有对话历史，先添加历史对话
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            System.out.println("添加对话历史，共 " + conversationHistory.size() + " 条");
            for (java.util.Map<String, Object> historyItem : conversationHistory) {
                // 添加用户消息
                if (historyItem.containsKey("user")) {
                    JsonObject userMsg = new JsonObject();
                    userMsg.addProperty("role", "user");
                    userMsg.addProperty("content", String.valueOf(historyItem.get("user")));
                    textMessages.add(userMsg);
                }
                // 添加助手回复
                if (historyItem.containsKey("assistant")) {
                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", String.valueOf(historyItem.get("assistant")));
                    textMessages.add(assistantMsg);
                }
            }
        }
        
        // 构建完整的用户消息（包含系统提示词和用户输入）
        String fullUserMessage = userText;
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            // 如果有对话历史，系统提示词只在第一条消息中包含
            if (conversationHistory == null || conversationHistory.isEmpty()) {
                // 将系统提示词作为上下文加入到用户消息中
                // 格式：系统提示词 + \n\n用户输入
                fullUserMessage = systemPrompt + "\n\n用户说：" + userText;
            } else {
                // 有对话历史时，系统提示词已经在第一条消息中，当前消息只包含用户输入
                fullUserMessage = userText;
            }
        }
        
        // 添加当前用户消息
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", fullUserMessage);
        textMessages.add(userMessage);
        
        System.out.println("构建的消息数量: " + textMessages.size());
        System.out.println("用户消息长度: " + fullUserMessage.length());
        
        // 构建请求体 - 按照讯飞星火 v3.5 API 的正确格式
        // 1. header 字段
        JsonObject header = new JsonObject();
        header.addProperty("app_id", appId);
        header.addProperty("uid", "user_" + System.currentTimeMillis()); // 用户标识，可以使用时间戳
        
        // 2. parameter 字段
        JsonObject parameter = new JsonObject();
        JsonObject chatParam = new JsonObject();
        // v3.5 版本：使用 domain 参数，正确值应该是 "generalv3.5"
        chatParam.addProperty("domain", "generalv3.5");
        chatParam.addProperty("temperature", 0.5);
        chatParam.addProperty("max_tokens", 4096);
        parameter.add("chat", chatParam);
        
        // 3. payload 字段
        JsonObject payload = new JsonObject();
        JsonObject messageObj = new JsonObject();
        messageObj.add("text", textMessages); // 消息数组放在 text 字段中
        payload.add("message", messageObj);
        
        // 4. 构建完整的请求体
        JsonObject requestData = new JsonObject();
        requestData.add("header", header);
        requestData.add("parameter", parameter);
        requestData.add("payload", payload);
        
        String requestBody = gson.toJson(requestData);
        System.out.println("========== 请求体详情 ==========");
        System.out.println("请求体JSON（已隐藏敏感信息）: " + requestBody.replace(appId, "***").replace(apiKey, "***"));
        System.out.println("请求体大小: " + requestBody.length() + " 字节");
        System.out.println("==================================");
        
        // 建立WebSocket连接
        Request request = new Request.Builder()
                .url(authUrl)
                .build();
        
        final StringBuilder responseBuilder = new StringBuilder();
        final Object lock = new Object();
        final boolean[] completed = {false};
        final Exception[] connectionError = {null}; // 用于存储连接错误
        
        System.out.println("========== 开始建立WebSocket连接 ==========");
        System.out.println("鉴权URL（前100字符）: " + (authUrl.length() > 100 ? authUrl.substring(0, 100) + "..." : authUrl));
        
        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("========== WebSocket连接已建立 ==========");
                System.out.println("HTTP状态码: " + response.code());
                System.out.println("响应消息: " + response.message());
                
                if (response.code() != 101) {
                    System.err.println("警告: WebSocket连接状态码异常！期望101，实际: " + response.code());
                    System.err.println("这可能导致连接失败");
                    connectionError[0] = new RuntimeException("WebSocket握手失败，状态码: " + response.code());
                } else {
                    System.out.println("WebSocket握手成功（状态码101）");
                }
                
                // 发送消息
                System.out.println("正在发送请求消息...");
                System.out.println("请求体长度: " + requestBody.length());
                try {
                    webSocket.send(requestBody);
                    System.out.println("请求消息已发送");
                } catch (Exception e) {
                    System.err.println("发送请求消息失败: " + e.getMessage());
                    connectionError[0] = new RuntimeException("发送请求消息失败", e);
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notify();
                    }
                }
                System.out.println("======================================");
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("========== 收到WebSocket消息 ==========");
                System.out.println("消息长度: " + text.length());
                System.out.println("消息内容（前500字符）: " + (text.length() > 500 ? text.substring(0, 500) + "..." : text));
                System.out.println("======================================");
                try {
                    JsonObject json = gson.fromJson(text, JsonObject.class);
                    
                    // 检查是否有错误
                    if (json.has("header")) {
                        JsonObject header = json.getAsJsonObject("header");
                        if (header.has("code") && header.get("code").getAsInt() != 0) {
                            int code = header.get("code").getAsInt();
                            String message = header.has("message") ? header.get("message").getAsString() : "未知错误";
                            System.err.println("讯飞API返回错误 - 错误码: " + code + ", 错误消息: " + message);
                            
                            // 根据错误码提供更详细的说明
                            switch (code) {
                                case 10013:
                                    System.err.println("错误说明: APPID不存在或未开通服务");
                                    break;
                                case 10014:
                                    System.err.println("错误说明: 签名校验失败，请检查api-key和api-secret");
                                    break;
                                case 10015:
                                    System.err.println("错误说明: 参数校验失败");
                                    break;
                                case 10019:
                                    System.err.println("错误说明: 请求过于频繁，请稍后再试");
                                    break;
                                default:
                                    System.err.println("请查看讯飞星火API文档了解错误码含义");
                            }
                            
                            // 保存错误信息
                            connectionError[0] = new RuntimeException("讯飞API返回错误: " + code + " - " + message);
                            
                            synchronized (lock) {
                                completed[0] = true;
                                lock.notify();
                            }
                            webSocket.close(1000, "错误关闭");
                            return;
                        }
                    }
                    
                    JsonObject payload = json.getAsJsonObject("payload");
                    
                    if (payload != null) {
                        // v3.5 API 响应格式：payload.choices.text 数组
                        JsonObject choices = payload.getAsJsonObject("choices");
                        if (choices != null) {
                            JsonArray textArray = choices.getAsJsonArray("text");
                            if (textArray != null && textArray.size() > 0) {
                                JsonObject textObj = textArray.get(0).getAsJsonObject();
                                if (textObj.has("content")) {
                                    String content = textObj.get("content").getAsString();
                                    responseBuilder.append(content);
                                    System.out.println("已追加内容片段，当前总长度: " + responseBuilder.length());
                                } else {
                                    System.err.println("警告: text对象中未找到content字段");
                                    System.err.println("text对象内容: " + textObj.toString());
                                }
                            } else {
                                System.err.println("警告: text数组为空或不存在");
                                System.err.println("choices对象: " + choices.toString());
                            }
                        } else {
                            System.err.println("警告: payload中未找到choices字段");
                            System.err.println("payload对象: " + payload.toString());
                        }
                        
                        // 检查是否完成 - v3.5 API使用payload.status.status字段
                        JsonObject status = payload.getAsJsonObject("status");
                        if (status != null && status.has("status")) {
                            int statusCode = status.get("status").getAsInt();
                            System.out.println("收到状态码: " + statusCode);
                            if (statusCode == 2) {
                                System.out.println("收到完成信号，准备关闭连接");
                                synchronized (lock) {
                                    completed[0] = true;
                                    lock.notify();
                                }
                                webSocket.close(1000, "正常关闭");
                            }
                        } else {
                            // 也可能在header中检查
                            if (json.has("header")) {
                                JsonObject header = json.getAsJsonObject("header");
                                if (header.has("status")) {
                                    int headerStatus = header.get("status").getAsInt();
                                    System.out.println("header中的状态码: " + headerStatus);
                                    if (headerStatus == 2) {
                                        System.out.println("收到完成信号（来自header），准备关闭连接");
                                        synchronized (lock) {
                                            completed[0] = true;
                                            lock.notify();
                                        }
                                        webSocket.close(1000, "正常关闭");
                                    }
                                }
                            }
                        }
                    } else {
                        System.err.println("警告: 消息中未找到payload字段");
                        System.err.println("完整消息内容: " + text);
                        // 如果多次收到无payload的消息，可能是格式问题
                        // 但不立即失败，等待其他消息
                    }
                } catch (Exception e) {
                    System.err.println("解析WebSocket消息异常: " + e.getMessage());
                    System.err.println("原始消息: " + text);
                    e.printStackTrace();
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notify();
                    }
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("========== WebSocket连接失败 ==========");
                System.err.println("错误消息: " + t.getMessage());
                System.err.println("错误类型: " + t.getClass().getName());
                if (response != null) {
                    System.err.println("HTTP状态码: " + response.code());
                    System.err.println("响应消息: " + response.message());
                    try {
                        if (response.body() != null) {
                            String body = response.body().string();
                            System.err.println("响应体: " + body);
                        }
                    } catch (Exception e) {
                        System.err.println("无法读取响应体: " + e.getMessage());
                    }
                } else {
                    System.err.println("响应对象为null，可能是网络连接问题");
                }
                t.printStackTrace();
                System.err.println("=====================================");
                
                // 保存错误信息，以便后续抛出
                connectionError[0] = new RuntimeException("WebSocket连接失败: " + t.getMessage(), t);
                
                synchronized (lock) {
                    completed[0] = true;
                    lock.notify();
                }
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
            }
        });
        
        // 等待响应完成（最多50秒，因为大模型生成可能需要较长时间）
        synchronized (lock) {
            if (!completed[0]) {
                try {
                    System.out.println("等待WebSocket响应（最多50秒）...");
                    lock.wait(50000);
                    
                    if (!completed[0]) {
                        System.err.println("========== WebSocket响应超时 ==========");
                        System.err.println("在50秒内未收到完成信号");
                        System.err.println("当前responseBuilder长度: " + responseBuilder.length());
                        webSocket.close(1000, "超时关闭");
                    } else {
                        System.out.println("WebSocket响应完成，等待结束");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("等待响应被中断");
                }
            } else {
                System.out.println("WebSocket响应已完成，无需等待");
            }
        }
        
        // 检查是否有连接错误
        if (connectionError[0] != null) {
            System.err.println("检测到连接错误，抛出异常");
            throw connectionError[0];
        }
        
        String result = responseBuilder.length() > 0 ? responseBuilder.toString() : null;
        if (result == null || result.trim().isEmpty()) {
            System.err.println("========== 错误: 未收到任何响应内容 ==========");
            System.err.println("responseBuilder长度: " + responseBuilder.length());
            System.err.println("completed状态: " + completed[0]);
            System.err.println("连接错误: " + (connectionError[0] != null ? connectionError[0].getMessage() : "无"));
            System.err.println("=============================================");
            throw new RuntimeException("讯飞API未返回任何内容。可能原因：1) WebSocket连接失败 2) 未收到响应消息 3) 响应格式错误。请检查网络连接、API配置和后端日志。");
        }
        
        System.out.println("========== 成功获取响应 ==========");
        System.out.println("响应内容长度: " + result.length());
        System.out.println("响应内容预览: " + (result.length() > 100 ? result.substring(0, 100) + "..." : result));
        System.out.println("==================================");
        return result;
    }

    /**
     * 生成鉴权URL（带HMAC-SHA256签名）
     */
    private String generateAuthUrl() throws Exception {
        // >>>>> 添加下面这两行调试日志 <<<<<
        System.out.println("【调试】正在生成鉴权URL，原始配置: " + chatUrl);
        String httpUrl = chatUrl.replace("wss://", "https://").replace("ws://", "http://");
        System.out.println("【调试】替换后的HTTP URL: " + httpUrl);
        // >>>>> 调试日志结束 <<<<<

        URL url = new URL(httpUrl); 
        // ...
        String host = url.getHost();
        String path = url.getPath();
        
        // 检查系统时间（如果时间偏差过大，会导致鉴权失败）
        long currentTime = System.currentTimeMillis();
        System.out.println("当前系统时间: " + new Date(currentTime));
        
        // 生成时间戳
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());
        System.out.println("GMT时间戳: " + date);
        
        // 构建签名字符串
        String signatureOrigin = String.format("host: %s\ndate: %s\nGET %s HTTP/1.1", host, date, path);
        
        // HMAC-SHA256签名
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8)));
        
        // 构建authorization
        String authorizationOrigin = String.format("api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"", 
                apiKey, signature);
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));
        
        // 构建最终URL
        String authUrl = String.format("%s?authorization=%s&date=%s&host=%s",
                chatUrl,
                java.net.URLEncoder.encode(authorization, "UTF-8"),
                java.net.URLEncoder.encode(date, "UTF-8"),
                java.net.URLEncoder.encode(host, "UTF-8"));
        
        return authUrl;
    }
}


