package com.parking.controller;

import com.parking.model.vo.VoiceCommandResult;
import com.parking.service.VoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 语音交互控制器
 * 处理用户语音指令的API请求
 */
@RestController
@RequestMapping("/api/v1/voice")
public class VoiceController {
    
    @Autowired
    private VoiceService voiceService;
    
    @Autowired
    private com.parking.service.ChatService chatService;
    
    /**
     * 处理语音指令
     * @param command 语音指令文本
     * @return 语音处理结果
     */
    @PostMapping("/process")
    public ResponseEntity<VoiceCommandResult> processVoiceCommand(
            @RequestBody Map<String, Object> requestBody) {
        
        String command = (String) requestBody.get("command");
        if (command == null || command.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                VoiceCommandResult.fail("语音指令不能为空")
            );
        }
        
        // 使用默认userId（1L），或者从请求中获取（如果前端传递了）
        Long userId = 1L; // 默认用户ID
        if (requestBody.containsKey("userId")) {
            try {
                Object userIdObj = requestBody.get("userId");
                if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                } else if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                }
            } catch (NumberFormatException e) {
                // 如果解析失败，使用默认值
            }
        }
        
        // 获取对话历史
        java.util.List<Map<String, Object>> conversationHistory = null;
        if (requestBody.containsKey("conversationHistory")) {
            Object historyObj = requestBody.get("conversationHistory");
            if (historyObj instanceof java.util.List) {
                conversationHistory = (java.util.List<Map<String, Object>>) historyObj;
                System.out.println("收到对话历史，共 " + conversationHistory.size() + " 条");
            }
        }
        
        System.out.println("收到语音指令: " + command + ", userId: " + userId);
        
        try {
            VoiceCommandResult result = voiceService.processVoiceCommand(command, userId, conversationHistory);
            System.out.println("语音指令处理成功: " + result.getMessage());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("处理语音指令时发生异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(VoiceCommandResult.fail("抱歉，我现在有点忙，请稍后再试。"));
        }
    }
    
    /**
     * 获取附近空车位
     * @param userId 用户ID
     * @return 附近空车位信息
     */
    @GetMapping("/nearby-spaces")
    public ResponseEntity<VoiceCommandResult> getNearbyEmptySpaces(
            @RequestAttribute("userId") Long userId) {
        
        VoiceCommandResult result = voiceService.getNearbyEmptySpaces(userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 导航到指定车位
     * @param spaceId 车位ID
     * @param userId 用户ID
     * @return 导航信息
     */
    @GetMapping("/navigate/{spaceId}")
    public ResponseEntity<VoiceCommandResult> navigateToSpace(
            @PathVariable Long spaceId,
            @RequestAttribute("userId") Long userId) {
        
        VoiceCommandResult result = voiceService.navigateToSpace(spaceId, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 识别语音指令类型
     * @param requestBody 包含指令文本的请求体
     * @return 指令类型
     */
    @PostMapping("/recognize")
    public ResponseEntity<Map<String, String>> recognizeCommandType(
            @RequestBody Map<String, String> requestBody) {
        
        String command = requestBody.get("command");
        if (command == null || command.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "语音指令不能为空");
            return ResponseEntity.badRequest().body(error);
        }
        
        String commandType = voiceService.recognizeCommandType(command);
        Map<String, String> result = new HashMap<>();
        result.put("commandType", commandType);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 测试讯飞API连接（用于调试）
     */
    @GetMapping("/test-spark")
    public ResponseEntity<Map<String, Object>> testSparkConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("========== 开始测试讯飞API连接 ==========");
            String testResponse = chatService.getChatResponse("你好", null);
            result.put("success", true);
            result.put("message", "讯飞API连接成功");
            result.put("response", testResponse);
            System.out.println("========== 讯飞API测试成功 ==========");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "讯飞API连接失败: " + e.getMessage());
            result.put("error", e.getClass().getName());
            result.put("errorDetail", e.toString());
            result.put("stackTrace", getStackTrace(e));
            System.err.println("========== 讯飞API测试失败 ==========");
            e.printStackTrace();
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 测试NLU解析（用于调试）
     */
    @PostMapping("/test-nlu")
    public ResponseEntity<Map<String, Object>> testNlu(@RequestBody Map<String, String> requestBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            String command = requestBody.get("command");
            if (command == null || command.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "command参数不能为空");
                return ResponseEntity.ok(result);
            }
            
            System.out.println("========== 开始测试NLU解析 ==========");
            System.out.println("输入指令: " + command);
            
            String nluResponse = chatService.getNluResponse(command);
            result.put("success", true);
            result.put("message", "NLU解析成功");
            result.put("nluResponse", nluResponse);
            
            System.out.println("NLU响应: " + nluResponse);
            System.out.println("========== NLU测试完成 ==========");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "NLU解析失败: " + e.getMessage());
            result.put("error", e.getClass().getName());
            result.put("errorDetail", e.toString());
            result.put("stackTrace", getStackTrace(e));
            System.err.println("========== NLU测试失败 ==========");
            e.printStackTrace();
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 上传音频文件进行语音识别（用于原生录音API）
     * 注意：当前实现仅返回提示信息，因为需要集成第三方语音识别服务
     * 建议使用微信同声传译插件，它可以直接返回识别文本
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAudio(
            @RequestParam("audio") org.springframework.web.multipart.MultipartFile audioFile,
            @RequestParam(value = "format", defaultValue = "mp3") String format) {
        
        Map<String, Object> result = new HashMap<>();
        
        System.out.println("========== 收到音频上传请求 ==========");
        System.out.println("文件名: " + (audioFile != null ? audioFile.getOriginalFilename() : "null"));
        System.out.println("文件大小: " + (audioFile != null ? audioFile.getSize() : 0) + " 字节");
        System.out.println("格式: " + format);
        
        try {
            if (audioFile == null || audioFile.isEmpty()) {
                result.put("status", "error");
                result.put("message", "音频文件不能为空");
                System.err.println("错误: 音频文件为空");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 注意：当前后端没有集成语音识别服务（如讯飞语音识别API）
            // 建议使用微信同声传译插件，它可以直接返回识别文本
            // 如果需要使用原生录音API，需要：
            // 1. 集成讯飞语音识别API或其他语音识别服务
            // 2. 将音频文件转换为识别服务需要的格式
            // 3. 调用识别服务获取文本
            
            result.put("status", "error");
            result.put("message", "当前后端未集成语音识别服务。请使用微信同声传译插件，或联系开发人员集成语音识别API。");
            
            System.err.println("警告: 音频上传接口被调用，但后端未集成语音识别服务");
            System.err.println("建议: 使用微信同声传译插件（WechatSI），它可以直接返回识别文本");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("处理音频上传时发生异常: " + e.getMessage());
            e.printStackTrace();
            result.put("status", "error");
            result.put("message", "处理音频文件失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}