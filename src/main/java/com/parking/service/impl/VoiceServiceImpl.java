package com.parking.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.parking.model.dto.ParkingSpaceDTO;
import com.parking.model.dto.ResultDTO;
import com.parking.model.vo.VoiceCommandResult;
import com.parking.service.*;
import com.parking.util.KeywordExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语音服务实现类 (已升级为智能调度中心)
 */
@Service
public class VoiceServiceImpl implements VoiceService {

    // 业务服务
    @Autowired
    private ParkingSpaceService parkingSpaceService;
    @Autowired
    private ParkingService parkingService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private UserService userService;
    
    // AI 服务
    @Autowired
    private AmapService amapService;
    @Autowired
    private ChatService chatService;
    
    // 关键词提取工具
    @Autowired
    private KeywordExtractor keywordExtractor;
    
    // 停车场名称服务
    @Autowired
    private com.parking.service.ParkingNameService parkingNameService;

    // JSON 解析器
    private final Gson gson = new Gson();

    // 指令类型常量
    private static final String COMMAND_TYPE_CANCEL_RESERVATION = "CANCEL_RESERVATION";
    private static final String COMMAND_TYPE_RESERVE_NEARBY = "RESERVE_NEARBY";
    private static final String COMMAND_TYPE_FIND_NEARBY = "FIND_NEARBY";
    private static final String COMMAND_TYPE_NAVIGATE = "NAVIGATE";
    private static final String COMMAND_TYPE_QUERY_RESERVATION = "QUERY_RESERVATION";
    private static final String COMMAND_TYPE_QUERY_UNPAID = "QUERY_UNPAID";
    private static final String COMMAND_TYPE_QUERY_DISTANCE = "QUERY_DISTANCE";
    private static final String COMMAND_TYPE_UNKNOWN = "UNKNOWN";

    @Override
    public VoiceCommandResult processVoiceCommand(String voiceCommand, Long userId, java.util.List<java.util.Map<String, Object>> conversationHistory) {
        
        // 步骤 0: 预处理 - 提取时间信息
        TimeInfo timeInfo = extractTimeInfo(voiceCommand);
        String cleanedCommand = timeInfo.getCleanedCommand();
        
        // 步骤 0.5: 使用关键词提取工具提取关键词
        KeywordExtractor.ExtractionResult keywordResult = keywordExtractor.extract(cleanedCommand);
        System.out.println("========== 关键词提取结果 ==========");
        System.out.println("指令关键词: " + keywordResult.getCommandKeywords());
        System.out.println("停车场关键词: " + keywordResult.getParkingKeywords());
        System.out.println("地址关键词: " + keywordResult.getAddressKeywords());
        System.out.println("提取的目的地: " + keywordResult.getDestination());
        System.out.println("提取的停车场名称: " + keywordResult.getParkingLotName());
        System.out.println("识别的指令类型: " + keywordResult.getCommand());
        System.out.println("==================================");
        
        // 步骤 1: 调用 NLU 模式 (指令1)，获取JSON回复
        String nluJsonResponse = chatService.getNluResponse(cleanedCommand);
        
        String intent = COMMAND_TYPE_UNKNOWN;
        String destination = null;
        String parkingLotName = null;
        String aiExtractedTime = null; // AI提取的时间

        try {
            // 解析 NLU JSON
            JsonObject root = gson.fromJson(nluJsonResponse, JsonObject.class);
            if (root != null && root.has("intent")) {
                intent = root.get("intent").getAsString();
            }
            JsonObject entities = (root != null && root.has("entities")) ? root.getAsJsonObject("entities") : null;
            
            if (entities != null) {
                if (entities.has("destination") && !entities.get("destination").isJsonNull()) {
                    destination = entities.get("destination").getAsString();
                }
                if (entities.has("parkingLotName") && !entities.get("parkingLotName").isJsonNull()) {
                    parkingLotName = entities.get("parkingLotName").getAsString();
                }
                // 新增：提取AI识别的时间
                if (entities.has("targetTime") && !entities.get("targetTime").isJsonNull()) {
                    String timeStr = entities.get("targetTime").getAsString();
                    if (timeStr != null && !timeStr.trim().isEmpty()) {
                        aiExtractedTime = timeStr.trim();
                        System.out.println("========== AI提取到时间 ==========");
                        System.out.println("AI提取的时间: " + aiExtractedTime);
                        System.out.println("==================================");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("NLU JSON 解析失败: " + nluJsonResponse);
            e.printStackTrace();
            // 保持 intent 为 UNKNOWN，进入聊天模式
        }
        
        // 步骤 1.5: 使用关键词提取结果补充或修正NLU结果
        // 如果NLU没有提取到目的地，使用关键词提取的结果
        if ((destination == null || destination.isEmpty()) && keywordResult.getDestination() != null && !keywordResult.getDestination().isEmpty()) {
            destination = keywordResult.getDestination();
            System.out.println("使用关键词提取的目的地: " + destination);
        }
        
        // 如果NLU没有提取到停车场名称，使用关键词提取的结果
        if ((parkingLotName == null || parkingLotName.isEmpty()) && keywordResult.getParkingLotName() != null && !keywordResult.getParkingLotName().isEmpty()) {
            parkingLotName = keywordResult.getParkingLotName();
            System.out.println("使用关键词提取的停车场名称: " + parkingLotName);
        }
        
        // 如果NLU识别为UNKNOWN，但关键词提取识别到了指令，尝试映射指令类型
        // 注意：取消指令优先级最高，即使NLU识别为其他意图也要检查
        if (keywordResult.getCommand() != null) {
            String keywordCommand = keywordResult.getCommand();
            // 优先检查取消指令（即使NLU识别为其他意图）
            if ("CANCEL_RESERVATION".equals(keywordCommand)) {
                intent = COMMAND_TYPE_CANCEL_RESERVATION;
                System.out.println("根据关键词提取结果，识别为取消预约指令（优先级最高）");
            } else if (COMMAND_TYPE_UNKNOWN.equals(intent)) {
                // 只有在NLU识别为UNKNOWN时才尝试其他指令类型
                if ("RESERVE".equals(keywordCommand) && keywordResult.getParkingKeywords().size() > 0) {
                    intent = COMMAND_TYPE_RESERVE_NEARBY;
                    System.out.println("根据关键词提取结果，识别为预约指令");
                } else if ("FIND".equals(keywordCommand) && keywordResult.getParkingKeywords().size() > 0) {
                    intent = COMMAND_TYPE_FIND_NEARBY;
                    System.out.println("根据关键词提取结果，识别为查找指令");
                } else if ("QUERY".equals(keywordCommand) && (cleanedCommand.contains("预约") || cleanedCommand.contains("订单"))) {
                    intent = COMMAND_TYPE_QUERY_RESERVATION;
                    System.out.println("根据关键词提取结果，识别为查询预约指令");
                } else if ("NAVIGATE".equals(keywordCommand)) {
                    intent = COMMAND_TYPE_NAVIGATE;
                    System.out.println("根据关键词提取结果，识别为导航指令");
                } else if ("DISTANCE".equals(keywordCommand)) {
                    intent = COMMAND_TYPE_QUERY_DISTANCE;
                    System.out.println("根据关键词提取结果，识别为查询距离指令");
                }
            }
        }

        // 兜底规则：如果NLU没有识别成我们关心的意图，则尝试简单规则匹配
        // 注意：取消指令优先级最高，优先检查
        if (cleanedCommand != null) {
            String cmd = cleanedCommand.trim();
            
            // 规则-1: 识别"取消预约"指令（最高优先级）
            if ((cmd.contains("取消") || cmd.contains("删除") || cmd.contains("撤销") || 
                 cmd.contains("作废") || cmd.contains("不要") || cmd.contains("不用") ||
                 cmd.contains("不预约") || cmd.contains("不预订") || cmd.contains("不预定")) &&
                (cmd.contains("预约") || cmd.contains("预订") || cmd.contains("预定") || 
                 cmd.contains("订单") || cmd.contains("停车"))) {
                intent = COMMAND_TYPE_CANCEL_RESERVATION;
                System.out.println("兜底规则识别为取消预约指令");
            }
        }
        
        if (!COMMAND_TYPE_CANCEL_RESERVATION.equals(intent)
                && !COMMAND_TYPE_RESERVE_NEARBY.equals(intent) 
                && !COMMAND_TYPE_FIND_NEARBY.equals(intent) 
                && !COMMAND_TYPE_NAVIGATE.equals(intent)
                && !COMMAND_TYPE_QUERY_RESERVATION.equals(intent)
                && !COMMAND_TYPE_QUERY_UNPAID.equals(intent)
                && !COMMAND_TYPE_QUERY_DISTANCE.equals(intent)
                && cleanedCommand != null) {
            
            String cmd = cleanedCommand.trim();
            
            // 规则0: 识别"查询未支付订单"指令
            if (cmd.contains("未支付") || cmd.contains("待支付") || (cmd.contains("订单") && (cmd.contains("未支付") || cmd.contains("待支付")))) {
                intent = COMMAND_TYPE_QUERY_UNPAID;
            }
            // 规则0.3: 识别"查询距离"指令
            else if (cmd.contains("距离") && (cmd.contains("多远") || cmd.contains("有多远") || cmd.contains("停车场"))) {
                intent = COMMAND_TYPE_QUERY_DISTANCE;
                // 尝试提取停车场名称
                if (parkingLotName == null) {
                    int idxDistance = cmd.indexOf("距离");
                    int idxParking = cmd.indexOf("停车场");
                    if (idxParking == -1) {
                        idxParking = cmd.length();
                    }
                    if (idxDistance != -1 && idxParking > idxDistance) {
                        String between = cmd.substring(idxDistance + 2, idxParking).trim();
                        between = between.replaceAll("(有多远|多远|到)", "").trim();
                        if (!between.isEmpty()) {
                            parkingLotName = between;
                        }
                    }
                }
            }
            // 规则0.5: 识别"查询预约"指令
            else if (cmd.contains("预约") && (cmd.contains("查询") || cmd.contains("查看") || cmd.contains("有什么") 
                    || cmd.contains("我的预约") || cmd.contains("当前预约") || cmd.contains("现在预约")
                    || cmd.contains("待使用") || cmd.contains("订单"))) {
                intent = COMMAND_TYPE_QUERY_RESERVATION;
            }
            // 规则1: 识别"预约"指令（优先级高于"查找附近"，因为预约是更明确的意图）
            else if (cmd.contains("预约") && (cmd.contains("停车场") || cmd.contains("车位"))) {
                intent = COMMAND_TYPE_RESERVE_NEARBY;
                
                // 如果NLU没有解析出destination，则尝试简单抽取地点词
                if (destination == null) {
                    // 使用当前命令（已经处理过时间信息）
                    int idxReserve = cmd.indexOf("预约");
                    int idxParking = cmd.indexOf("停车场");
                    if (idxParking == -1) {
                        idxParking = cmd.indexOf("车位");
                    }
                    if (idxReserve != -1 && idxParking != -1 && idxParking > idxReserve) {
                        // 提取"预约"和"停车场"之间的内容
                        String between = cmd.substring(idxReserve + 2, idxParking).trim();
                        // 移除常见的时间关键词
                        between = between.replaceAll("(一个小时后|一小时后|小时后|明天|今天|后天|上午|下午|晚上|早上|中午)", "").trim();
                        // 移除"的"字和"附近"等修饰词（注意：要保留地点名称）
                        between = between.replaceAll("(的$|附近|旁边|周围)", "").trim();
                        // 移除"帮我"、"请"等助词
                        between = between.replaceAll("^(帮我|请|给我)", "").trim();
                        if (!between.isEmpty()) {
                            destination = between;
                            System.out.println("从预约指令中提取地点: " + destination);
                        }
                    }
                    // 如果上述方法没有提取到地点，尝试从"预约"后面提取到"附近"之前的内容
                    if (destination == null || destination.isEmpty()) {
                        int idxReserve2 = cmd.indexOf("预约");
                        int idxNearby = cmd.indexOf("附近");
                        if (idxReserve2 != -1 && idxNearby != -1 && idxNearby > idxReserve2) {
                            String beforeNearby = cmd.substring(idxReserve2 + 2, idxNearby).trim();
                            // 移除"的"字
                            beforeNearby = beforeNearby.replaceAll("的$", "").trim();
                            // 移除"帮我"、"请"等助词
                            beforeNearby = beforeNearby.replaceAll("^(帮我|请|给我)", "").trim();
                            if (!beforeNearby.isEmpty()) {
                                destination = beforeNearby;
                                System.out.println("从预约指令中提取地点（备用方法）: " + destination);
                            }
                        }
                    }
                }
            }
            // 规则0.6: 识别"查找附近停车场"指令（如"北京路附近有什么停车场"）
            // 注意：这个规则放在"预约"规则之后，避免"预约XX附近的停车场"被误识别为查找
            else if ((cmd.contains("附近") || cmd.contains("旁边") || cmd.contains("周围")) 
                    && (cmd.contains("停车场") || cmd.contains("车位") || cmd.contains("有什么"))) {
                intent = COMMAND_TYPE_FIND_NEARBY;
                
                // 提取地点名称
                if (destination == null) {
                    // 匹配"XX附近有什么停车场"或"XX附近的停车场"
                    int idxNearby = cmd.indexOf("附近");
                    if (idxNearby == -1) {
                        idxNearby = cmd.indexOf("旁边");
                    }
                    if (idxNearby == -1) {
                        idxNearby = cmd.indexOf("周围");
                    }
                    
                    if (idxNearby != -1) {
                        String beforeNearby = cmd.substring(0, idxNearby).trim();
                        // 移除"查找"、"查询"、"有什么"等关键词
                        beforeNearby = beforeNearby.replaceAll("(查找|查询|有什么|看看|看看|找找)", "").trim();
                        // 移除"帮我"、"请"等助词
                        beforeNearby = beforeNearby.replaceAll("^(帮我|请|给我)", "").trim();
                        if (!beforeNearby.isEmpty()) {
                            destination = beforeNearby;
                        }
                    }
                }
            }
            // 规则2: 识别"导航"指令
            else if (cmd.contains("导航") || cmd.contains("导航到") || cmd.contains("去")) {
                intent = COMMAND_TYPE_NAVIGATE;
                
                // 尝试抽取目的地
                if (destination == null) {
                    // 匹配"导航到XXX"或"去XXX"
                    int idxNavigate = cmd.indexOf("导航到");
                    if (idxNavigate == -1) {
                        idxNavigate = cmd.indexOf("导航");
                    }
                    if (idxNavigate == -1) {
                        idxNavigate = cmd.indexOf("去");
                    }
                    
                    if (idxNavigate != -1) {
                        String afterNavigate = cmd.substring(idxNavigate).trim();
                        // 移除"导航到"、"导航"、"去"等关键词
                        afterNavigate = afterNavigate.replaceFirst("^(导航到|导航|去)", "").trim();
                        // 移除可能的标点符号
                        afterNavigate = afterNavigate.replaceAll("[。，,！!？?]", "").trim();
                        if (!afterNavigate.isEmpty()) {
                            destination = afterNavigate;
                        }
                    }
                }
            }
        }

        // 步骤 1.6: 如果识别到停车场名称，优先使用停车场名称进行联想匹配
        if (parkingLotName != null && !parkingLotName.isEmpty()) {
            // 使用停车场名称服务进行模糊匹配
            List<String> matchedNames = parkingNameService.fuzzyMatch(parkingLotName);
            if (!matchedNames.isEmpty()) {
                String matchedName = matchedNames.get(0);
                System.out.println("停车场名称联想匹配: " + parkingLotName + " -> " + matchedName);
                parkingLotName = matchedName;
            }
        }
        
        // 步骤 1.7: 如果destination可能是停车场名称，也进行联想匹配
        if (destination != null && !destination.isEmpty() && parkingLotName == null) {
            if (parkingNameService.isPossibleParkingName(destination)) {
                List<String> matchedNames = parkingNameService.fuzzyMatch(destination);
                if (!matchedNames.isEmpty()) {
                    String matchedName = matchedNames.get(0);
                    System.out.println("目的地识别为停车场名称，联想匹配: " + destination + " -> " + matchedName);
                    parkingLotName = matchedName;
                    destination = null; // 清空destination，因为已经识别为停车场名称
                }
            }
        }

        // 步骤 2: 智能调度
        switch (intent) {
            case COMMAND_TYPE_CANCEL_RESERVATION:
                // 执行"取消预约"（传递停车场名称用于识别特定预约）
                return handleCancelReservationCommand(parkingLotName, destination, userId);
                
            case COMMAND_TYPE_RESERVE_NEARBY:
                // 执行"一句话预约"（传递时间信息和停车场名称）
                return handleReserveNearbyCommand(destination, parkingLotName, userId, timeInfo, aiExtractedTime);
                
            case COMMAND_TYPE_FIND_NEARBY:
                // 执行"查找附近" (可以是基于地理位置或停车场名称)
                System.out.println("识别为FIND_NEARBY意图 - parkingLotName: " + parkingLotName + ", destination: " + destination);
                if (parkingLotName != null || destination != null) {
                    // 按名称或地点搜索停车场
                    String searchKeyword = parkingLotName != null ? parkingLotName : destination;
                    System.out.println("查找停车场 - 关键词: " + searchKeyword);
                    return handleFindParkingCommand(searchKeyword, userId);
                } else {
                    System.out.println("没有指定地点，使用用户当前位置");
                    return handleNearbyCommand(userId); // 复用旧的"附近车位"
                }
                
            case COMMAND_TYPE_QUERY_DISTANCE:
                // 执行"查询距离"
                return handleQueryDistanceCommand(parkingLotName, userId);
                
            case COMMAND_TYPE_QUERY_RESERVATION:
                // 执行"查询预约"
                return handleQueryReservationCommand(userId);
                
            case COMMAND_TYPE_QUERY_UNPAID:
                // 执行"查询未支付订单"
                return handleQueryUnpaidCommand(userId);
                
            case COMMAND_TYPE_NAVIGATE:
                // 执行"导航到XXX"
                return handleNavigateCommand(destination);

            case COMMAND_TYPE_UNKNOWN:
            default:
                // 步骤 3: 转入聊天模式 (指令2)
                try {
                    String chatResponse = chatService.getChatResponse(voiceCommand, conversationHistory);
                    return VoiceCommandResult.successChat(chatResponse, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return VoiceCommandResult.fail("抱歉，我现在有点忙，您可以换个问题吗？");
                }
        }
    }

    /**
     * 提取时间信息
     * 支持多种时间表达方式：
     * 1. 相对时间：X小时后、X分钟后、明天、后天
     * 2. 具体日期：今天、明天、后天、X月X日
     * 3. 具体时间：X点、X点X分、上午X点、下午X点、晚上X点
     * 4. 组合时间：明天上午9点、后天下午2点30分
     */
    private TimeInfo extractTimeInfo(String command) {
        java.util.Date startTime = null;
        String cleanedCommand = command;
        
        if (command == null || command.trim().isEmpty()) {
            return new TimeInfo(null, command);
        }
        
        String cmd = command.trim();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.util.Date now = new java.util.Date();
        cal.setTime(now);
        
        boolean timeFound = false;
        int dayOffset = 0; // 日期偏移（0=今天，1=明天，2=后天）
        int hour = -1; // 小时（-1表示未指定）
        int minute = 0; // 分钟（默认0）
        String timePeriod = ""; // 时间段（上午、下午、晚上）
        
        // 1. 提取日期信息
        if (cmd.contains("后天")) {
            dayOffset = 2;
            timeFound = true;
            cmd = cmd.replace("后天", "").trim();
        } else if (cmd.contains("明天")) {
            dayOffset = 1;
            timeFound = true;
            cmd = cmd.replace("明天", "").trim();
        } else if (cmd.contains("今天")) {
            dayOffset = 0;
            timeFound = true;
            cmd = cmd.replace("今天", "").trim();
        }
        
        // 2. 提取时间段（上午、下午、晚上）
        if (cmd.contains("上午")) {
            timePeriod = "上午";
            cmd = cmd.replace("上午", "").trim();
            timeFound = true;
        } else if (cmd.contains("下午")) {
            timePeriod = "下午";
            cmd = cmd.replace("下午", "").trim();
            timeFound = true;
        } else if (cmd.contains("晚上")) {
            timePeriod = "晚上";
            cmd = cmd.replace("晚上", "").trim();
            timeFound = true;
        } else if (cmd.contains("早上")) {
            timePeriod = "上午";
            cmd = cmd.replace("早上", "").trim();
            timeFound = true;
        } else if (cmd.contains("中午")) {
            timePeriod = "中午";
            cmd = cmd.replace("中午", "").trim();
            timeFound = true;
        }
        
        // 3. 提取具体时间（X点X分、X点半、X点、X时X分、X时）
        // 优先匹配"X点半"（如"4点半"、"10点半"）
        java.util.regex.Pattern timePatternHalf = java.util.regex.Pattern.compile("(\\d{1,2})[点时]半");
        java.util.regex.Matcher timeMatcherHalf = timePatternHalf.matcher(cmd);
        if (timeMatcherHalf.find()) {
            hour = Integer.parseInt(timeMatcherHalf.group(1));
            minute = 30; // 半 = 30分
            timeFound = true;
            String matched = timeMatcherHalf.group(0);
            cmd = cmd.replaceFirst(java.util.regex.Pattern.quote(matched), "").trim();
            System.out.println("识别到'点半'表达: " + hour + "点30分");
        }
        // 匹配"X点X分"或"X时X分"（如"4点30分"、"10点15分"）
        if (hour < 0) {
            java.util.regex.Pattern timePattern1 = java.util.regex.Pattern.compile("(\\d{1,2})[点时](\\d{1,2})分");
            java.util.regex.Matcher timeMatcher1 = timePattern1.matcher(cmd);
            if (timeMatcher1.find()) {
                hour = Integer.parseInt(timeMatcher1.group(1));
                minute = Integer.parseInt(timeMatcher1.group(2));
                timeFound = true;
                // 只替换第一个匹配，避免误替换
                String matched = timeMatcher1.group(0);
                cmd = cmd.replaceFirst(java.util.regex.Pattern.quote(matched), "").trim();
                System.out.println("识别到'点X分'表达: " + hour + "点" + minute + "分");
            }
        }
        // 匹配"X点整"或"X点"或"X时"（但排除"X点X分"和"X点半"的情况）
        if (hour < 0) {
            java.util.regex.Pattern timePattern2 = java.util.regex.Pattern.compile("(\\d{1,2})[点时](?!分|半)");
            java.util.regex.Matcher timeMatcher2 = timePattern2.matcher(cmd);
            if (timeMatcher2.find()) {
                hour = Integer.parseInt(timeMatcher2.group(1));
                minute = 0;
                timeFound = true;
                // 只替换第一个匹配
                String matched = timeMatcher2.group(0);
                cmd = cmd.replaceFirst(java.util.regex.Pattern.quote(matched), "").trim();
                System.out.println("识别到'点'表达: " + hour + "点整");
            }
        }
        
        // 4. 提取相对时间（X小时后、X分钟后）
        if (!timeFound) {
            // 匹配"X小时后"、"X个小时后"
            java.util.regex.Pattern hourPattern = java.util.regex.Pattern.compile("(\\d+)(个)?小时后");
            java.util.regex.Matcher hourMatcher = hourPattern.matcher(cmd);
            if (hourMatcher.find()) {
                int hours = Integer.parseInt(hourMatcher.group(1));
                cal.add(java.util.Calendar.HOUR_OF_DAY, hours);
                startTime = cal.getTime();
                timeFound = true;
                cmd = cmd.replaceAll("(\\d+)(个)?小时后", "").trim();
            }
            // 匹配"X分钟后"、"X分钟后"
            else {
                java.util.regex.Pattern minutePattern = java.util.regex.Pattern.compile("(\\d+)(个)?分钟后");
                java.util.regex.Matcher minuteMatcher = minutePattern.matcher(cmd);
                if (minuteMatcher.find()) {
                    int minutes = Integer.parseInt(minuteMatcher.group(1));
                    cal.add(java.util.Calendar.MINUTE, minutes);
                    startTime = cal.getTime();
                    timeFound = true;
                    cmd = cmd.replaceAll("(\\d+)(个)?分钟后", "").trim();
                }
            }
        }
        
        // 5. 如果提取到了日期和时间，组合计算
        if (timeFound && startTime == null && hour >= 0) {
            // 重置Calendar为当前时间（重要：确保从当前时间开始计算）
            cal = java.util.Calendar.getInstance();
            cal.setTime(now);
            
            // 设置日期
            if (dayOffset > 0) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, dayOffset);
            }
            // 如果dayOffset=0（今天），保持当前日期
            
            // 处理时间段（重要：必须在设置Calendar之前处理，因为会影响小时数）
            if (!timePeriod.isEmpty()) {
                if ("下午".equals(timePeriod)) {
                    // 下午：1-12点转换为13-24点（24点=0点），但通常下午是1-11点
                    if (hour >= 1 && hour <= 11) {
                        hour += 12; // 下午1点 = 13点，下午4点 = 16点，下午11点 = 23点
                    } else if (hour == 12) {
                        // 下午12点就是12点（中午），但通常说"下午12点"是指中午
                        hour = 12;
                    }
                    System.out.println("处理下午时间段: 原始小时=" + (hour - 12 >= 1 ? hour - 12 : hour) + ", 转换后=" + hour);
                } else if ("晚上".equals(timePeriod)) {
                    // 晚上：1-11点转换为13-23点，12点保持12点
                    if (hour >= 1 && hour <= 11) {
                        hour += 12; // 晚上7点 = 19点
                    } else if (hour == 12) {
                        hour = 12;
                    }
                    System.out.println("处理晚上时间段: 原始小时=" + (hour - 12 >= 1 ? hour - 12 : hour) + ", 转换后=" + hour);
                } else if ("中午".equals(timePeriod)) {
                    hour = 12; // 中午 = 12点
                    System.out.println("处理中午时间段: 设置为12点");
                }
                // 上午保持原样（0-11点），但通常上午是1-11点
                if ("上午".equals(timePeriod)) {
                    System.out.println("处理上午时间段: 保持原样=" + hour);
                }
            }
            
            // 验证小时范围
            if (hour < 0 || hour > 23) {
                System.err.println("无效的小时数: " + hour);
                hour = 9; // 默认9点
            }
            if (minute < 0 || minute > 59) {
                System.err.println("无效的分钟数: " + minute);
                minute = 0;
            }
            
            // 设置时间（重要：先设置日期，再设置时间）
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
            cal.set(java.util.Calendar.MINUTE, minute);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            
            startTime = cal.getTime();
            
            System.out.println("========== 时间计算详情 ==========");
            System.out.println("日期偏移: " + dayOffset + " (0=今天, 1=明天, 2=后天)");
            System.out.println("时间段: " + (timePeriod.isEmpty() ? "无" : timePeriod));
            int originalHour = hour;
            if (!timePeriod.isEmpty() && ("下午".equals(timePeriod) || "晚上".equals(timePeriod))) {
                if (hour >= 13 && hour <= 23) {
                    originalHour = hour - 12; // 还原原始小时
                }
            }
            System.out.println("原始小时: " + (originalHour >= 0 ? originalHour : "未指定"));
            System.out.println("处理后小时: " + hour);
            System.out.println("分钟: " + minute);
            java.text.SimpleDateFormat debugSdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("计算后的时间: " + debugSdf.format(startTime));
            System.out.println("当前时间: " + debugSdf.format(now));
            System.out.println("==================================");
        } else if (timeFound && startTime == null && dayOffset > 0) {
            // 只有日期，没有具体时间，默认设置为当天的9点
            cal = java.util.Calendar.getInstance();
            cal.setTime(now);
            cal.add(java.util.Calendar.DAY_OF_YEAR, dayOffset);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 9);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            startTime = cal.getTime();
            System.out.println("只有日期，设置默认时间（9点）: " + startTime);
        }
        
        // 6. 验证时间必须在当前时间之后
        if (startTime != null) {
            long timeDiffMs = startTime.getTime() - now.getTime();
            long timeDiffMinutes = timeDiffMs / 60000;
            
            if (startTime.before(now) || startTime.equals(now)) {
                System.err.println("========== 时间验证失败 ==========");
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.err.println("提取的时间: " + sdf.format(startTime));
                System.err.println("当前时间: " + sdf.format(now));
                System.err.println("时间差（分钟）: " + timeDiffMinutes);
                System.err.println("原因: 时间早于或等于当前时间");
                System.err.println("处理: 将使用默认立即预约");
                System.err.println("==================================");
                // 如果时间无效，设置为null，使用默认立即预约
                startTime = null;
            } else {
                System.out.println("========== 时间提取成功 ==========");
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println("提取的时间: " + sdf.format(startTime));
                System.out.println("当前时间: " + sdf.format(now));
                System.out.println("时间差（分钟）: " + timeDiffMinutes);
                System.out.println("时间差（小时）: " + String.format("%.2f", timeDiffMinutes / 60.0));
                System.out.println("==================================");
            }
        } else if (timeFound) {
            System.out.println("========== 时间提取警告 ==========");
            System.out.println("提取到时间关键词，但时间计算失败");
            System.out.println("dayOffset: " + dayOffset);
            System.out.println("timePeriod: " + timePeriod);
            System.out.println("hour: " + hour);
            System.out.println("minute: " + minute);
            System.out.println("==================================");
        }
        
        // 7. 清理多余的标点和空格
        cleanedCommand = cmd.replaceAll("[。，,！!？?]", "").trim();
        
        return new TimeInfo(startTime, cleanedCommand);
    }
    
    /**
     * 时间信息内部类
     */
    private static class TimeInfo {
        private final java.util.Date startTime;
        private final String cleanedCommand;
        
        public TimeInfo(java.util.Date startTime, String cleanedCommand) {
            this.startTime = startTime;
            this.cleanedCommand = cleanedCommand;
        }
        
        public java.util.Date getStartTime() {
            return startTime;
        }
        
        public String getCleanedCommand() {
            return cleanedCommand;
        }
    }

    /**
     * 处理 "预约附近" 指令 - 直接创建预约
     * @param destination 目的地/地址（如"北京路"）
     * @param parkingLotName 停车场名称（如"天河城停车场"）
     * @param userId 用户ID
     * @param timeInfo 时间信息
     */
    private VoiceCommandResult handleReserveNearbyCommand(String destination, String parkingLotName, Long userId, TimeInfo timeInfo, String aiExtractedTime) {
        try {
            // 1. 获取用户信息（车牌号和电话）
            com.parking.model.dto.UserDTO userDTO = userService.getUserById(userId);
            if (userDTO == null) {
                return VoiceCommandResult.fail("无法获取用户信息，请先登录。");
            }
            
            System.out.println("========== 语音预约 - 获取用户信息 ==========");
            System.out.println("用户ID: " + userId);
            System.out.println("用户车牌号: " + userDTO.getLicensePlate());
            System.out.println("用户电话: " + userDTO.getPhone());
            System.out.println("用户信息完整对象: " + userDTO);
            
            String defaultPlateNumber = userDTO.getLicensePlate();
            String defaultPhone = userDTO.getPhone();
            
            if (defaultPlateNumber == null || defaultPlateNumber.trim().isEmpty()) {
                System.err.println("错误：用户车牌号为空，userId=" + userId);
                return VoiceCommandResult.fail("您还没有设置默认车牌号，请先在\"我的车牌\"页面设置默认车牌。");
            }
            if (defaultPhone == null || defaultPhone.trim().isEmpty()) {
                return VoiceCommandResult.fail("您还没有设置联系电话，请先在个人中心设置电话。");
            }
            
            // 2. 优先使用停车场名称搜索，如果没有则使用地址
            ResultDTO nearbyResult = null;
            String searchKeyword = null;
            
            // 2.1 如果识别到停车场名称，直接按名称搜索
            if (parkingLotName != null && !parkingLotName.isEmpty()) {
                System.out.println("========== 使用停车场名称搜索 ==========");
                System.out.println("停车场名称: " + parkingLotName);
                searchKeyword = parkingLotName;
                nearbyResult = parkingService.searchParkings(parkingLotName);
            }
            // 2.2 如果没有停车场名称，但有目的地，尝试处理
            else if (destination != null && !destination.isEmpty()) {
                // 清理地点名称：移除"的"、"附近"等修饰词
                String cleanedDestination = destination.trim()
                    .replaceAll("的$", "")  // 移除末尾的"的"
                    .replaceAll("(附近|旁边|周围|一带)", "")  // 移除位置修饰词
                    .trim();
                
                System.out.println("========== 使用地址搜索 ==========");
                System.out.println("原始地点: " + destination + ", 清理后: " + cleanedDestination);
                
                // 先检查是否可能是停车场名称（支持联想匹配）
                if (parkingNameService.isPossibleParkingName(cleanedDestination)) {
                    List<String> matchedNames = parkingNameService.fuzzyMatch(cleanedDestination);
                    if (!matchedNames.isEmpty()) {
                        String matchedName = matchedNames.get(0);
                        System.out.println("地址识别为停车场名称，联想匹配: " + cleanedDestination + " -> " + matchedName);
                        searchKeyword = matchedName;
                        nearbyResult = parkingService.searchParkings(matchedName);
                    }
                }
                
                // 如果不是停车场名称，进行地理编码
                if (nearbyResult == null || !nearbyResult.isSuccess()) {
                    Map<String, Double> coords = amapService.geocode(cleanedDestination);
                    
                    if (coords != null) {
                        // 有坐标：按坐标附近查找停车场
                        System.out.println("地理编码成功，坐标: " + coords.get("longitude") + ", " + coords.get("latitude"));
                        searchKeyword = cleanedDestination;
                        nearbyResult = parkingService.getNearbyParkings(coords.get("longitude"), coords.get("latitude"), 5000, null);
                    } else {
                        // 无坐标：退化为按名称搜索停车场
                        System.out.println("地理编码失败，改用停车场名称搜索，关键词: " + cleanedDestination);
                        searchKeyword = cleanedDestination;
                        nearbyResult = parkingService.searchParkings(cleanedDestination);
                    }
                } else {
                    searchKeyword = cleanedDestination;
                }
            } else {
                return VoiceCommandResult.fail("请告诉我您的目的地或停车场名称，例如'北京路'或'天河城停车场'。");
            }
            
            // 检查搜索结果
            if (nearbyResult == null || !nearbyResult.isSuccess() || !(nearbyResult.getData() instanceof List)) {
                String errorMsg = searchKeyword != null ? 
                    ("未找到与 \"" + searchKeyword + "\" 相关的停车场。") : 
                    "未找到停车场。";
                return VoiceCommandResult.fail(errorMsg);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parkingLots = (List<Map<String, Object>>) nearbyResult.getData();
            if (parkingLots.isEmpty()) {
                String errorMsg = searchKeyword != null ? 
                    ("未找到与 \"" + searchKeyword + "\" 相关的停车场。") : 
                    "未找到停车场。";
                return VoiceCommandResult.fail(errorMsg);
            }
            
            // 3. 遍历停车场，查找第一个有空车位的
            for (Map<String, Object> lot : parkingLots) {
                Long parkingId = ((Number) lot.get("id")).longValue();
                String parkingName = (String) lot.get("name");
                
                // 4. 查找空车位
                List<ParkingSpaceDTO> availableSpaces = parkingSpaceService.getAvailableSpaces(parkingId);
                
                if (availableSpaces != null && !availableSpaces.isEmpty()) {
                    // 5. 随机选择一个空车位
                    java.util.Collections.shuffle(availableSpaces);
                    ParkingSpaceDTO selectedSpace = availableSpaces.get(0);
                    
                    // 6. 创建预约（支持指定开始时间）
                    java.util.Date now = new java.util.Date();
                    java.util.Date startTime = null;
                    
                    // 优先级1: 优先使用AI提取的时间（如果AI成功提取了时间）
                    if (aiExtractedTime != null && !aiExtractedTime.trim().isEmpty()) {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            startTime = sdf.parse(aiExtractedTime);
                            System.out.println("========== 使用AI提取的时间 ==========");
                            System.out.println("AI提取的时间字符串: " + aiExtractedTime);
                            System.out.println("解析后的时间: " + sdf.format(startTime));
                            System.out.println("当前时间: " + sdf.format(now));
                            
                            // 验证时间必须在当前时间之后
                            if (startTime.before(now) || startTime.equals(now)) {
                                System.err.println("警告：AI提取的时间早于或等于当前时间，尝试使用本地时间提取");
                                startTime = null; // 设置为null，继续尝试其他方式
                            } else {
                                System.out.println("AI提取的时间有效，将使用此时间");
                                System.out.println("==================================");
                            }
                        } catch (Exception e) {
                            System.err.println("========== AI时间解析失败 ==========");
                            System.err.println("AI提取的时间字符串: " + aiExtractedTime);
                            System.err.println("错误信息: " + e.getMessage());
                            System.err.println("将尝试使用本地时间提取");
                            System.err.println("==================================");
                            startTime = null; // 解析失败，继续尝试其他方式
                        }
                    }
                    
                    // 优先级2: 如果AI没有提取到时间或解析失败，使用本地时间提取结果
                    if (startTime == null && timeInfo != null && timeInfo.getStartTime() != null) {
                        startTime = timeInfo.getStartTime();
                        // 验证时间必须在当前时间之后（extractTimeInfo已经验证过，这里再次确认）
                        if (startTime.before(now) || startTime.equals(now)) {
                            System.err.println("警告：本地提取的时间无效，使用当前时间（立即预约）");
                            System.err.println("提取的时间: " + startTime);
                            System.err.println("当前时间: " + now);
                            startTime = now;
                        } else {
                            System.out.println("使用本地时间提取的预约时间: " + startTime);
                        }
                    }
                    
                    // 优先级3: 如果都没有，默认立即预约
                    if (startTime == null) {
                        startTime = now;
                        System.out.println("未指定时间，使用当前时间（立即预约）: " + startTime);
                    }
                    
                    // 结束时间 = 开始时间 + 2小时
                    java.util.Calendar calendar = java.util.Calendar.getInstance();
                    calendar.setTime(startTime);
                    calendar.add(java.util.Calendar.HOUR_OF_DAY, 2);
                    java.util.Date endTime = calendar.getTime();
                    
                    com.parking.model.dto.ReservationCreateRequestDTO requestDTO = new com.parking.model.dto.ReservationCreateRequestDTO();
                    requestDTO.setParkingSpaceId(selectedSpace.getId());
                    requestDTO.setStartTime(startTime);
                    requestDTO.setEndTime(endTime);
                    requestDTO.setPlateNumber(defaultPlateNumber);
                    requestDTO.setContactPhone(defaultPhone);
                    requestDTO.setRemark("语音预约：" + destination);
                    
                    // 7. 调用预约服务创建预约
                    System.out.println("========== 创建预约请求 ==========");
                    System.out.println("车位ID: " + selectedSpace.getId());
                    System.out.println("开始时间: " + startTime);
                    System.out.println("结束时间: " + endTime);
                    java.text.SimpleDateFormat debugSdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    System.out.println("开始时间（格式化）: " + debugSdf.format(startTime));
                    System.out.println("结束时间（格式化）: " + debugSdf.format(endTime));
                    System.out.println("车牌号: " + defaultPlateNumber);
                    System.out.println("联系电话: " + defaultPhone);
                    System.out.println("==================================");
                    
                    com.parking.model.dto.ReservationDTO reservationDTO;
                    try {
                        reservationDTO = reservationService.createReservation(requestDTO, userId);
                        
                        // 打印创建后的预约信息，验证时间是否正确
                        if (reservationDTO != null) {
                            System.out.println("========== 预约创建成功 ==========");
                            System.out.println("预约ID: " + reservationDTO.getId());
                            if (reservationDTO.getStartTime() != null) {
                                System.out.println("预约开始时间: " + debugSdf.format(reservationDTO.getStartTime()));
                                System.out.println("请求的开始时间: " + debugSdf.format(startTime));
                                // 验证时间是否一致
                                long timeDiff = Math.abs(reservationDTO.getStartTime().getTime() - startTime.getTime());
                                if (timeDiff > 1000) { // 超过1秒的差异
                                    System.err.println("警告：预约创建后的时间与请求时间不一致，差异: " + (timeDiff / 1000) + "秒");
                                }
                            }
                            if (reservationDTO.getEndTime() != null) {
                                System.out.println("预约结束时间: " + debugSdf.format(reservationDTO.getEndTime()));
                                System.out.println("请求的结束时间: " + debugSdf.format(endTime));
                            }
                            System.out.println("==================================");
                        }
                    } catch (com.parking.exception.ParkingException e) {
                        // 处理未支付订单异常
                        if (e.getMessage() != null && e.getMessage().startsWith("UNPAID_ORDER:")) {
                            String unpaidOrderId = e.getMessage().substring("UNPAID_ORDER:".length());
                            String errorMessage = "您有未支付的订单（订单号：" + unpaidOrderId + "），请先完成支付后再预约新的停车位。您可以前往\"预约\"页面查看并支付。";
                            return VoiceCommandResult.fail(errorMessage);
                        }
                        // 其他业务异常
                        throw e;
                    }
                    
                    if (reservationDTO != null && reservationDTO.getId() != null) {
                        // 8. 返回预约成功结果，包含预约ID
                        Map<String, Object> reservationData = new HashMap<>();
                        reservationData.put("reservationId", reservationDTO.getId());
                        reservationData.put("parkingName", parkingName);
                        reservationData.put("spaceNumber", selectedSpace.getSpaceNumber());
                        reservationData.put("floorName", selectedSpace.getFloor());
                        
                        // 构建消息，使用预约服务返回的实际时间
                        String timeInfoStr = "";
                        // 优先使用预约服务返回的实际时间，确保显示的时间与数据库中的一致
                        java.util.Date actualStartTime = reservationDTO.getStartTime() != null ? reservationDTO.getStartTime() : startTime;
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                        
                        if (timeInfo != null && timeInfo.getStartTime() != null && !timeInfo.getStartTime().equals(now)) {
                            timeInfoStr = "，预约时间：" + sdf.format(actualStartTime);
                            System.out.println("预约成功，指定时间: " + timeInfoStr);
                        } else {
                            timeInfoStr = "，预约时间：" + sdf.format(actualStartTime) + "（立即生效）";
                            System.out.println("预约成功，立即生效: " + timeInfoStr);
                        }
                        String message = "已为您找到停车场【" + parkingName + "】，并成功预约车位【" + selectedSpace.getFloor() + "-" + selectedSpace.getSpaceNumber() + "】" + timeInfoStr + "。";
                        
                        return VoiceCommandResult.successWithFollowUp(
                            message, 
                            COMMAND_TYPE_RESERVE_NEARBY, 
                            "NAV_TO_RESERVATION_DETAIL", // 告诉前端跳转到预约详情页
                            reservationData
                        );
                    } else {
                        return VoiceCommandResult.fail("预约创建失败，请稍后重试。");
                    }
                }
            }
            
            // 循环结束，没有找到任何空车位
            String locationInfo = searchKeyword != null ? searchKeyword : (destination != null ? destination : "附近");
            return VoiceCommandResult.fail("抱歉，" + locationInfo + " 的所有停车场均已满位。");

        } catch (com.parking.exception.ParkingException e) {
            // 处理业务异常（包括未支付订单）
            if (e.getMessage() != null && e.getMessage().startsWith("UNPAID_ORDER:")) {
                String unpaidOrderId = e.getMessage().substring("UNPAID_ORDER:".length());
                String errorMessage = "您有未支付的订单（订单号：" + unpaidOrderId + "），请先完成支付后再预约新的停车位。您可以前往\"预约\"页面查看并支付。";
                System.err.println("预约失败：用户有未支付订单，订单ID: " + unpaidOrderId);
                return VoiceCommandResult.fail(errorMessage);
            }
            // 其他业务异常
            System.err.println("预约失败（业务异常）: " + e.getMessage());
            e.printStackTrace();
            return VoiceCommandResult.fail("预约失败：" + e.getMessage());
        } catch (Exception e) {
            System.err.println("预约失败（系统异常）: " + e.getMessage());
            e.printStackTrace();
            return VoiceCommandResult.fail("处理预约时发生内部错误，请稍后重试。");
        }
    }

    /**
     * 处理 "附近车位" 指令
     * (这是旧的实现，复用于 FIND_NEARBY 意图)
     */
    @Override
    public VoiceCommandResult getNearbyEmptySpaces(Long userId) {
        return handleNearbyCommand(userId);
    }
    
    private VoiceCommandResult handleNearbyCommand(Long userId) {
        try {
            Map<String, Double> location = locationService.getUserLocation(userId);
            if (location == null || location.get("longitude") == null || location.get("latitude") == null) {
                return VoiceCommandResult.fail("无法获取您的位置信息，请授权位置权限。");
            }
            
            ResultDTO nearbyResult = parkingService.getNearbyParkings(location.get("longitude"), location.get("latitude"), 5000, null);
            
            if (!nearbyResult.isSuccess() || !(nearbyResult.getData() instanceof List)) {
                return VoiceCommandResult.fail("附近暂无可用车位。");
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parkingLots = (List<Map<String, Object>>) nearbyResult.getData();
            if (parkingLots.isEmpty()) {
                return VoiceCommandResult.fail("附近暂无可用车位。");
            }

            // 只返回第一个
            Map<String, Object> firstLot = parkingLots.get(0);
            String message = "已为您找到附近停车场：" + firstLot.get("name") + "，剩余车位 " + firstLot.get("availableSpaces") + " 个。";
            
            return VoiceCommandResult.success(message, COMMAND_TYPE_FIND_NEARBY, firstLot);
            
        } catch (Exception e) {
            e.printStackTrace();
            return VoiceCommandResult.fail("获取附近车位失败：" + e.getMessage());
        }
    }

    // (旧的 recognizeCommandType 可以删掉，或保留用于兼容旧代码)
    @Override
    public String recognizeCommandType(String command) {
        // 这个方法现在被 processVoiceCommand 内部的 NLU 调用取代了
        // 我们可以保留它，但它不再是主要逻辑
        if (command.contains("附近") && (command.contains("车位") || command.contains("停车位"))) {
            return COMMAND_TYPE_FIND_NEARBY;
        }
        return COMMAND_TYPE_UNKNOWN;
    }

    /**
     * 处理 "查找停车场" 指令（按名称或地点）
     */
    private VoiceCommandResult handleFindParkingCommand(String keyword, Long userId) {
        try {
            System.out.println("========== 查找停车场 ==========");
            System.out.println("关键词: " + keyword);
            System.out.println("用户ID: " + userId);
            
            if (keyword == null || keyword.trim().isEmpty()) {
                return VoiceCommandResult.fail("请告诉我您要查找的停车场名称或地点。");
            }
            
            String cleanedKeyword = keyword.trim();
            // 移除"附近"、"旁边"等修饰词
            cleanedKeyword = cleanedKeyword.replaceAll("(附近|旁边|周围|一带)", "").trim();
            
            ResultDTO searchResult;
            List<Map<String, Object>> parkings;
            
            // 判断是地点还是停车场名称
            // 先尝试地理编码，如果能编码成功，说明是地点，使用附近搜索
            Map<String, Double> coords = amapService.geocode(cleanedKeyword);
            if (coords != null && coords.get("longitude") != null && coords.get("latitude") != null) {
                System.out.println("识别为地点，使用附近搜索 - 坐标: " + coords.get("longitude") + ", " + coords.get("latitude"));
                // 使用附近搜索
                searchResult = parkingService.getNearbyParkings(
                    coords.get("longitude"), 
                    coords.get("latitude"), 
                    5000, // 5公里范围内
                    null
                );
            } else {
                System.out.println("识别为停车场名称，使用名称搜索");
                // 使用名称搜索
                searchResult = parkingService.searchParkings(cleanedKeyword);
            }
            
            if (!searchResult.isSuccess() || !(searchResult.getData() instanceof List)) {
                System.out.println("搜索失败: " + searchResult.getMessage());
                return VoiceCommandResult.fail("未找到与 \"" + keyword + "\" 相关的停车场。");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parkingsList = (List<Map<String, Object>>) searchResult.getData();
            parkings = parkingsList;
            
            System.out.println("找到 " + parkings.size() + " 个停车场");
            if (!parkings.isEmpty()) {
                System.out.println("第一个停车场: " + parkings.get(0).get("name") + ", 剩余车位: " + parkings.get(0).get("availableSpaces"));
            }
            
            if (parkings.isEmpty()) {
                return VoiceCommandResult.fail("未找到与 \"" + keyword + "\" 相关的停车场。");
            }
            
            // 构建回复消息
            StringBuilder message = new StringBuilder("为您找到以下停车场：\n\n");
            for (int i = 0; i < Math.min(parkings.size(), 5); i++) {
                Map<String, Object> parking = parkings.get(i);
                message.append(i + 1).append(". ");
                message.append(parking.get("name"));
                
                Object address = parking.get("address");
                if (address != null) {
                    message.append("（").append(address).append("）");
                }
                
                // 确保获取真实的剩余车位数据
                Object availableSpaces = parking.get("availableSpaces");
                if (availableSpaces == null) {
                    // 如果availableSpaces为null，尝试从available_spaces获取
                    availableSpaces = parking.get("available_spaces");
                }
                if (availableSpaces != null) {
                    message.append("，剩余车位：").append(availableSpaces).append("个");
                } else {
                    message.append("，剩余车位：查询中");
                }
                
                message.append("\n");
            }
            
            if (parkings.size() > 5) {
                message.append("\n...还有 ").append(parkings.size() - 5).append(" 个停车场");
            }
            
            message.append("\n需要我帮您预约或导航吗？");
            
            return VoiceCommandResult.success(message.toString(), COMMAND_TYPE_FIND_NEARBY, null);
            
        } catch (Exception e) {
            System.err.println("查找停车场失败: " + e.getMessage());
            e.printStackTrace();
            return VoiceCommandResult.fail("查找停车场时发生错误，请稍后重试。");
        }
    }

    /**
     * 处理 "查询距离" 指令
     */
    private VoiceCommandResult handleQueryDistanceCommand(String parkingLotName, Long userId) {
        try {
            if (parkingLotName == null || parkingLotName.trim().isEmpty()) {
                return VoiceCommandResult.fail("请告诉我您要查询的停车场名称。");
            }
            
            // 1. 获取用户当前位置
            Map<String, Double> userLocation = locationService.getUserLocation(userId);
            if (userLocation == null || userLocation.get("latitude") == null || userLocation.get("longitude") == null) {
                return VoiceCommandResult.fail("无法获取您的位置信息，请授权位置权限。");
            }
            
            // 2. 搜索停车场
            ResultDTO searchResult = parkingService.searchParkings(parkingLotName.trim());
            if (!searchResult.isSuccess() || !(searchResult.getData() instanceof List)) {
                return VoiceCommandResult.fail("未找到名为 \"" + parkingLotName + "\" 的停车场。");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parkings = (List<Map<String, Object>>) searchResult.getData();
            if (parkings.isEmpty()) {
                return VoiceCommandResult.fail("未找到名为 \"" + parkingLotName + "\" 的停车场。");
            }
            
            // 3. 计算距离（使用第一个匹配的停车场）
            Map<String, Object> parking = parkings.get(0);
            Object latObj = parking.get("latitude");
            Object lngObj = parking.get("longitude");
            
            if (latObj == null || lngObj == null) {
                return VoiceCommandResult.fail("该停车场没有位置信息。");
            }
            
            double parkingLat = Double.parseDouble(String.valueOf(latObj));
            double parkingLng = Double.parseDouble(String.valueOf(lngObj));
            double userLat = userLocation.get("latitude");
            double userLng = userLocation.get("longitude");
            
            // 计算距离（使用Haversine公式）
            double distance = calculateDistance(userLat, userLng, parkingLat, parkingLng);
            String distanceStr;
            if (distance < 1) {
                distanceStr = String.format("%.0f", distance * 1000) + "米";
            } else {
                distanceStr = String.format("%.1f", distance) + "公里";
            }
            
            // 4. 获取剩余车位信息
            Object availableSpaces = parking.get("availableSpaces");
            String spacesInfo = "";
            if (availableSpaces != null) {
                spacesInfo = "，当前剩余空车位约" + availableSpaces + "个";
            }
            
            String message = "距离" + parking.get("name") + "大约还有" + distanceStr + spacesInfo + "，需要为您导航或预约车位吗？";
            
            return VoiceCommandResult.success(message, COMMAND_TYPE_QUERY_DISTANCE, null);
            
        } catch (Exception e) {
            System.err.println("查询距离失败: " + e.getMessage());
            e.printStackTrace();
            return VoiceCommandResult.fail("查询距离时发生错误，请稍后重试。");
        }
    }
    
    /**
     * 计算两点之间的距离（Haversine公式，单位：公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 处理 "查询未支付订单" 指令
     */
    private VoiceCommandResult handleQueryUnpaidCommand(Long userId) {
        try {
            // 查询用户的预约列表
            List<com.parking.model.dto.ReservationDTO> reservations = reservationService.getUserReservations(userId, 1, 50);
            
            if (reservations == null || reservations.isEmpty()) {
                return VoiceCommandResult.success("您目前没有未支付的订单。", COMMAND_TYPE_QUERY_UNPAID, null);
            }
            
            // 过滤出未支付的订单（status=1已使用 且 paymentStatus=0未支付）
            List<com.parking.model.dto.ReservationDTO> unpaidOrders = new java.util.ArrayList<>();
            for (com.parking.model.dto.ReservationDTO res : reservations) {
                if (res.getStatus() != null && res.getStatus() == 1) { // 已使用
                    Integer paymentStatus = res.getPaymentStatus();
                    if (paymentStatus == null || paymentStatus == 0) { // 未支付
                        unpaidOrders.add(res);
                    }
                }
            }
            
            if (unpaidOrders.isEmpty()) {
                return VoiceCommandResult.success("您目前没有未支付的订单。", COMMAND_TYPE_QUERY_UNPAID, null);
            }
            
            // 构建回复消息
            StringBuilder message = new StringBuilder("您有 " + unpaidOrders.size() + " 个未支付的订单：\n");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM月dd日 HH:mm");
            
            for (int i = 0; i < Math.min(unpaidOrders.size(), 3); i++) {
                com.parking.model.dto.ReservationDTO res = unpaidOrders.get(i);
                message.append("\n");
                message.append(i + 1).append(". ");
                
                if (res.getParkingLotName() != null) {
                    message.append(res.getParkingLotName());
                } else {
                    message.append("停车场");
                }
                
                if (res.getStartTime() != null) {
                    message.append("，预约时间：").append(sdf.format(res.getStartTime()));
                }
                
                // 金额信息可能在其他字段中，暂时不显示
                // if (res.getAmount() != null) {
                //     message.append("，金额：").append(String.format("%.2f", res.getAmount())).append("元");
                // }
                
                if (res.getId() != null) {
                    message.append("（订单号：").append(res.getId()).append("）");
                }
            }
            
            if (unpaidOrders.size() > 3) {
                message.append("\n...还有 ").append(unpaidOrders.size() - 3).append(" 个未支付订单");
            }
            
            message.append("\n\n请前往\"预约\"页面完成支付。");
            
            return VoiceCommandResult.success(message.toString(), COMMAND_TYPE_QUERY_UNPAID, null);
            
        } catch (Exception e) {
            System.err.println("查询未支付订单失败: " + e.getMessage());
            e.printStackTrace();
            return VoiceCommandResult.fail("查询未支付订单时发生错误，请稍后重试。");
        }
    }

    /**
     * 处理 "查询预约" 指令
     */
    private VoiceCommandResult handleQueryReservationCommand(Long userId) {
        try {
            System.out.println("========== 查询预约订单 ==========");
            System.out.println("用户ID: " + userId);
            
            // 查询用户的预约列表（当前和未来的预约）
            java.util.Date now = new java.util.Date();
            System.out.println("当前时间: " + now);
            
            // 调用预约服务查询用户的预约
            List<com.parking.model.dto.ReservationDTO> reservations = reservationService.getUserReservations(userId, 1, 20);
            System.out.println("查询到的预约总数: " + (reservations != null ? reservations.size() : 0));
            
            if (reservations == null || reservations.isEmpty()) {
                System.out.println("没有预约记录");
                return VoiceCommandResult.success("目前您没有预约记录。需要我帮您预约吗？", COMMAND_TYPE_QUERY_RESERVATION, null);
            }
            
            // 过滤出当前和未来的预约（排除已取消和已完成的）
            List<com.parking.model.dto.ReservationDTO> activeReservations = new java.util.ArrayList<>();
            for (com.parking.model.dto.ReservationDTO res : reservations) {
                System.out.println("检查预约 ID=" + res.getId() + ", status=" + res.getStatus() + ", startTime=" + res.getStartTime() + ", endTime=" + res.getEndTime());
                
                if (res.getStatus() != null) {
                    // 状态：0=待使用, 1=使用中, 2=已完成, 3=已取消
                    int status = res.getStatus();
                    if (status == 0 || status == 1) {
                        // 待使用或使用中的预约
                        // 只要结束时间在未来，或者没有结束时间但开始时间在未来，都算有效预约
                        boolean isValid = false;
                        if (res.getEndTime() != null) {
                            isValid = res.getEndTime().after(now);
                            System.out.println("  结束时间检查: " + res.getEndTime() + " after " + now + " = " + isValid);
                        } else if (res.getStartTime() != null) {
                            // 如果没有结束时间，但开始时间在未来，也算有效
                            isValid = res.getStartTime().after(now) || res.getStartTime().equals(now);
                            System.out.println("  开始时间检查: " + res.getStartTime() + " after/equal " + now + " = " + isValid);
                        } else {
                            // 如果都没有时间信息，但状态是待使用或使用中，也算有效
                            isValid = true;
                            System.out.println("  无时间信息，但状态有效，算作有效");
                        }
                        if (isValid) {
                            activeReservations.add(res);
                            System.out.println("  -> 添加到有效预约列表");
                        }
                    } else {
                        System.out.println("  状态不符合（status=" + status + "），跳过");
                    }
                } else {
                    System.out.println("  状态为null，跳过");
                }
            }
            
            System.out.println("有效预约数量: " + activeReservations.size());
            
            if (activeReservations.isEmpty()) {
                return VoiceCommandResult.success("目前您没有有效的预约记录。需要我帮您预约吗？", COMMAND_TYPE_QUERY_RESERVATION, null);
            }
            
            // 构建回复消息
            StringBuilder message = new StringBuilder("您当前有 " + activeReservations.size() + " 个预约：\n");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM月dd日 HH:mm");
            
            for (int i = 0; i < Math.min(activeReservations.size(), 5); i++) {
                com.parking.model.dto.ReservationDTO res = activeReservations.get(i);
                message.append("\n");
                message.append(i + 1).append(". ");
                
                if (res.getParkingLotName() != null) {
                    message.append(res.getParkingLotName());
                } else {
                    message.append("停车场");
                }
                
                if (res.getStartTime() != null) {
                    message.append("，时间：").append(sdf.format(res.getStartTime()));
                }
                
                if (res.getStatus() != null) {
                    if (res.getStatus() == 0) {
                        message.append("（待使用）");
                    } else if (res.getStatus() == 1) {
                        message.append("（使用中）");
                    }
                }
            }
            
            if (activeReservations.size() > 5) {
                message.append("\n...还有 ").append(activeReservations.size() - 5).append(" 个预约");
            }
            
            message.append("\n\n您可以前往\"预约\"页面查看详情。");
            
            return VoiceCommandResult.success(message.toString(), COMMAND_TYPE_QUERY_RESERVATION, null);
            
        } catch (Exception e) {
            System.err.println("查询预约失败: " + e.getMessage());
            e.printStackTrace();
            return VoiceCommandResult.fail("查询预约时发生错误，请稍后重试。");
        }
    }

    /**
     * 处理 "取消预约" 指令
     * @param parkingLotName 停车场名称（如果指定了特定停车场）
     * @param destination 目的地（备用，可能包含停车场名称）
     * @param userId 用户ID
     */
    private VoiceCommandResult handleCancelReservationCommand(String parkingLotName, String destination, Long userId) {
        try {
            System.out.println("========== 取消预约 ==========");
            System.out.println("停车场名称: " + parkingLotName);
            System.out.println("目的地: " + destination);
            System.out.println("用户ID: " + userId);
            
            // 1. 获取用户的所有预约
            List<com.parking.model.dto.ReservationDTO> reservations = reservationService.getUserReservations(userId, 1, 50);
            
            if (reservations == null || reservations.isEmpty()) {
                return VoiceCommandResult.success("您目前没有预约记录，无需取消。", COMMAND_TYPE_CANCEL_RESERVATION, null);
            }
            
            // 2. 过滤出可取消的预约（状态为0=待使用）
            List<com.parking.model.dto.ReservationDTO> cancellableReservations = new java.util.ArrayList<>();
            for (com.parking.model.dto.ReservationDTO res : reservations) {
                if (res.getStatus() != null && res.getStatus() == 0) { // 0=待使用
                    cancellableReservations.add(res);
                }
            }
            
            if (cancellableReservations.isEmpty()) {
                return VoiceCommandResult.success("您目前没有可取消的预约（所有预约都已使用或已取消）。", COMMAND_TYPE_CANCEL_RESERVATION, null);
            }
            
            // 3. 如果指定了停车场名称，尝试匹配特定预约
            List<com.parking.model.dto.ReservationDTO> targetReservations = new java.util.ArrayList<>();
            
            if (parkingLotName != null && !parkingLotName.isEmpty()) {
                // 使用停车场名称服务进行模糊匹配
                List<String> matchedNames = parkingNameService.fuzzyMatch(parkingLotName);
                System.out.println("停车场名称联想匹配结果: " + matchedNames);
                
                // 查找匹配的预约
                for (com.parking.model.dto.ReservationDTO res : cancellableReservations) {
                    String resParkingName = res.getParkingLotName();
                    if (resParkingName != null) {
                        // 检查是否完全匹配或包含匹配
                        if (resParkingName.contains(parkingLotName) || parkingLotName.contains(resParkingName)) {
                            targetReservations.add(res);
                        } else {
                            // 检查是否匹配到联想结果
                            for (String matchedName : matchedNames) {
                                if (resParkingName.contains(matchedName) || matchedName.contains(resParkingName)) {
                                    targetReservations.add(res);
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (destination != null && !destination.isEmpty()) {
                // 如果destination可能是停车场名称，也尝试匹配
                if (parkingNameService.isPossibleParkingName(destination)) {
                    List<String> matchedNames = parkingNameService.fuzzyMatch(destination);
                    System.out.println("目的地识别为停车场名称，联想匹配结果: " + matchedNames);
                    
                    for (com.parking.model.dto.ReservationDTO res : cancellableReservations) {
                        String resParkingName = res.getParkingLotName();
                        if (resParkingName != null) {
                            for (String matchedName : matchedNames) {
                                if (resParkingName.contains(matchedName) || matchedName.contains(resParkingName)) {
                                    targetReservations.add(res);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            // 4. 执行取消操作
            int successCount = 0;
            int failCount = 0;
            List<String> successMessages = new java.util.ArrayList<>();
            List<String> failMessages = new java.util.ArrayList<>();
            
            // 如果指定了停车场，只取消匹配的预约；否则取消所有可取消的预约
            List<com.parking.model.dto.ReservationDTO> toCancel = targetReservations.isEmpty() ? 
                cancellableReservations : targetReservations;
            
            for (com.parking.model.dto.ReservationDTO res : toCancel) {
                try {
                    boolean result = reservationService.cancelReservation(res.getId(), userId);
                    if (result) {
                        successCount++;
                        String parkingName = res.getParkingLotName() != null ? res.getParkingLotName() : "停车场";
                        successMessages.add(parkingName + "（订单号：" + res.getId() + "）");
                    } else {
                        failCount++;
                        failMessages.add("订单号：" + res.getId());
                    }
                } catch (Exception e) {
                    failCount++;
                    failMessages.add("订单号：" + res.getId() + "（" + e.getMessage() + "）");
                    System.err.println("取消预约失败，订单ID: " + res.getId() + ", 错误: " + e.getMessage());
                }
            }
            
            // 5. 构建返回消息
            StringBuilder message = new StringBuilder();
            if (successCount > 0) {
                message.append("已成功取消 ").append(successCount).append(" 个预约：\n");
                for (int i = 0; i < Math.min(successMessages.size(), 3); i++) {
                    message.append("  - ").append(successMessages.get(i)).append("\n");
                }
                if (successMessages.size() > 3) {
                    message.append("  ...还有 ").append(successMessages.size() - 3).append(" 个预约已取消\n");
                }
            }
            
            if (failCount > 0) {
                if (message.length() > 0) {
                    message.append("\n");
                }
                message.append("有 ").append(failCount).append(" 个预约取消失败，请稍后重试。");
            }
            
            if (successCount == 0 && failCount == 0) {
                if (parkingLotName != null && !parkingLotName.isEmpty()) {
                    return VoiceCommandResult.fail("未找到与 \"" + parkingLotName + "\" 相关的可取消预约。");
                } else {
                    return VoiceCommandResult.fail("未找到可取消的预约。");
                }
            }
            
            return VoiceCommandResult.success(message.toString(), COMMAND_TYPE_CANCEL_RESERVATION, null);
            
        } catch (Exception e) {
            System.err.println("取消预约失败: " + e.getMessage());
            e.printStackTrace();
            return VoiceCommandResult.fail("取消预约时发生错误，请稍后重试。");
        }
    }
    
    /**
     * 处理 "导航到XXX" 指令
     */
    private VoiceCommandResult handleNavigateCommand(String destination) {
        try {
            if (destination == null || destination.trim().isEmpty()) {
                return VoiceCommandResult.fail("请告诉我您要去的目的地，例如'北京路'或'万菱汇停车场'。");
            }
            
            // 1. 先尝试按停车场名称搜索
            ResultDTO searchResult = parkingService.searchParkings(destination);
            if (searchResult.isSuccess() && searchResult.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parkings = (List<Map<String, Object>>) searchResult.getData();
                if (!parkings.isEmpty()) {
                    // 找到停车场，使用停车场的坐标
                    Map<String, Object> parking = parkings.get(0);
                    Object latObj = parking.get("latitude");
                    Object lngObj = parking.get("longitude");
                    
                    if (latObj != null && lngObj != null) {
                        double latitude = Double.parseDouble(String.valueOf(latObj));
                        double longitude = Double.parseDouble(String.valueOf(lngObj));
                        String parkingName = (String) parking.get("name");
                        String address = (String) parking.getOrDefault("address", "");
                        
                        Map<String, Object> navData = new HashMap<>();
                        navData.put("latitude", latitude);
                        navData.put("longitude", longitude);
                        navData.put("name", parkingName);
                        navData.put("address", address);
                        navData.put("destination", destination);
                        
                        return VoiceCommandResult.successWithFollowUp(
                            "已为您找到 " + parkingName + "，正在打开导航",
                            COMMAND_TYPE_NAVIGATE,
                            "OPEN_NAVIGATION",
                            navData
                        );
                    }
                }
            }
            
            // 2. 如果没找到停车场，使用地理编码获取坐标
            Map<String, Double> coords = amapService.geocode(destination);
            if (coords != null && coords.containsKey("latitude") && coords.containsKey("longitude")) {
                Map<String, Object> navData = new HashMap<>();
                navData.put("latitude", coords.get("latitude"));
                navData.put("longitude", coords.get("longitude"));
                navData.put("name", destination);
                navData.put("address", destination);
                navData.put("destination", destination);
                
                return VoiceCommandResult.successWithFollowUp(
                    "正在为您打开导航到 " + destination,
                    COMMAND_TYPE_NAVIGATE,
                    "OPEN_NAVIGATION",
                    navData
                );
            }
            
            return VoiceCommandResult.fail("抱歉，我找不到 '" + destination + "' 的位置，请检查地点名称是否正确。");
            
        } catch (Exception e) {
            e.printStackTrace();
            return VoiceCommandResult.fail("导航处理失败：" + e.getMessage());
        }
    }

    @Override
    public VoiceCommandResult navigateToSpace(Long spaceId, Long userId) {
        // (此功能保持不变)
        try {
            Map<String, Object> navigationData = new HashMap<>();
            navigationData.put("spaceId", spaceId);
            navigationData.put("distance", "约200米");
            navigationData.put("estimatedTime", "约3分钟");
            
            return VoiceCommandResult.success(
                "导航已生成，正在为您规划路线",
                "navigate",
                navigationData
            );
        } catch (Exception e) {
            return VoiceCommandResult.fail("导航生成失败，请稍后再试");
        }
    }
}
