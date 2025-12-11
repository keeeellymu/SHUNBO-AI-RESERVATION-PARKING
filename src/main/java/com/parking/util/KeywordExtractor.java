package com.parking.util;

import com.parking.service.ParkingNameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 关键词提取工具类
 * 用于从语音指令中提取停车场相关、地址相关和指令相关的关键词
 * 支持停车场名称的模糊匹配和联想功能
 */
@Component
public class KeywordExtractor {
    
    @Autowired
    private ParkingNameService parkingNameService;
    
    // 停车场名称列表（从数据库加载）
    private Set<String> parkingNames = new HashSet<>();
    
    // 停车场相关关键词
    private static final Set<String> PARKING_KEYWORDS = new HashSet<>(Arrays.asList(
        "停车场", "停车位", "车位", "停车", "泊车", "停车库", "停车楼",
        "停车区", "停车点", "停车处", "停车场所", "停车设施"
    ));
    
    // 地址相关关键词（位置修饰词）
    private static final Set<String> LOCATION_MODIFIERS = new HashSet<>(Arrays.asList(
        "附近", "旁边", "周围", "一带", "周边", "邻近", "临近", "边上",
        "左右", "前后", "附近有", "附近找", "附近查"
    ));
    
    // 地址相关关键词（行政区划）
    private static final Set<String> ADMINISTRATIVE_KEYWORDS = new HashSet<>(Arrays.asList(
        "区", "县", "市", "省", "街道", "路", "街", "道", "巷", "弄",
        "广场", "大厦", "商场", "中心", "广场", "公园", "桥", "站"
    ));
    
    // 指令相关关键词（注意：取消相关的关键词优先级更高，放在前面）
    private static final Map<String, String> COMMAND_KEYWORDS = new HashMap<String, String>() {{
        // 取消相关关键词（优先级最高）
        put("取消预约", "CANCEL_RESERVATION");
        put("取消预订", "CANCEL_RESERVATION");
        put("取消预定", "CANCEL_RESERVATION");
        put("不预约", "CANCEL_RESERVATION");
        put("不预订", "CANCEL_RESERVATION");
        put("不预定", "CANCEL_RESERVATION");
        put("取消", "CANCEL_RESERVATION");
        put("删除", "CANCEL_RESERVATION");
        put("撤销", "CANCEL_RESERVATION");
        put("作废", "CANCEL_RESERVATION");
        put("不要", "CANCEL_RESERVATION");
        put("不用", "CANCEL_RESERVATION");
        // 预约相关关键词
        put("预约", "RESERVE");
        put("预订", "RESERVE");
        put("预定", "RESERVE");
        put("订", "RESERVE");
        // 查找相关关键词
        put("查找", "FIND");
        put("找", "FIND");
        put("搜索", "FIND");
        // 查询相关关键词
        put("查询", "QUERY");
        put("查", "QUERY");
        put("查看", "QUERY");
        // 导航相关关键词
        put("导航", "NAVIGATE");
        put("去", "NAVIGATE");
        put("到", "NAVIGATE");
        // 距离相关关键词
        put("距离", "DISTANCE");
        put("多远", "DISTANCE");
        put("有多远", "DISTANCE");
    }};
    
    // 时间相关关键词
    private static final Set<String> TIME_KEYWORDS = new HashSet<>(Arrays.asList(
        "今天", "明天", "后天", "上午", "下午", "晚上", "早上", "中午",
        "小时后", "分钟后", "小时", "分钟", "点", "时", "分"
    ));
    
    /**
     * 提取结果类
     */
    public static class ExtractionResult {
        private String command;              // 识别的指令类型
        private String destination;          // 提取的目的地/地址
        private String parkingLotName;       // 提取的停车场名称
        private List<String> parkingKeywords; // 停车场相关关键词
        private List<String> addressKeywords; // 地址相关关键词
        private List<String> commandKeywords; // 指令相关关键词
        private String cleanedText;          // 清理后的文本
        
        public ExtractionResult() {
            this.parkingKeywords = new ArrayList<>();
            this.addressKeywords = new ArrayList<>();
            this.commandKeywords = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        
        public String getParkingLotName() { return parkingLotName; }
        public void setParkingLotName(String parkingLotName) { this.parkingLotName = parkingLotName; }
        
        public List<String> getParkingKeywords() { return parkingKeywords; }
        public void setParkingKeywords(List<String> parkingKeywords) { this.parkingKeywords = parkingKeywords; }
        
        public List<String> getAddressKeywords() { return addressKeywords; }
        public void setAddressKeywords(List<String> addressKeywords) { this.addressKeywords = addressKeywords; }
        
        public List<String> getCommandKeywords() { return commandKeywords; }
        public void setCommandKeywords(List<String> commandKeywords) { this.commandKeywords = commandKeywords; }
        
        public String getCleanedText() { return cleanedText; }
        public void setCleanedText(String cleanedText) { this.cleanedText = cleanedText; }
    }
    
    @PostConstruct
    public void init() {
        // 初始化时加载停车场名称
        refreshParkingNames();
    }
    
    /**
     * 刷新停车场名称列表
     */
    public void refreshParkingNames() {
        try {
            parkingNames = parkingNameService.getAllParkingNames();
            System.out.println("KeywordExtractor: 已加载 " + parkingNames.size() + " 个停车场名称");
        } catch (Exception e) {
            System.err.println("KeywordExtractor: 加载停车场名称失败: " + e.getMessage());
            parkingNames = new HashSet<>();
        }
    }
    
    /**
     * 从文本中提取所有关键词
     * @param text 输入文本
     * @return 提取结果
     */
    public ExtractionResult extract(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ExtractionResult();
        }
        
        ExtractionResult result = new ExtractionResult();
        String cleanedText = text.trim();
        
        // 1. 提取指令关键词
        String detectedCommand = extractCommand(cleanedText, result);
        result.setCommand(detectedCommand);
        
        // 2. 提取停车场相关关键词
        extractParkingKeywords(cleanedText, result);
        
        // 3. 提取地址相关关键词
        extractAddressKeywords(cleanedText, result);
        
        // 4. 提取目的地（地址名称）
        String destination = extractDestination(cleanedText, result);
        result.setDestination(destination);
        
        // 5. 提取停车场名称
        String parkingLotName = extractParkingLotName(cleanedText, result);
        result.setParkingLotName(parkingLotName);
        
        // 6. 清理文本（移除已识别的关键词，保留核心内容）
        result.setCleanedText(cleanText(cleanedText, result));
        
        return result;
    }
    
    /**
     * 提取指令关键词
     */
    private String extractCommand(String text, ExtractionResult result) {
        String detectedCommand = null;
        int maxLength = 0;
        
        for (Map.Entry<String, String> entry : COMMAND_KEYWORDS.entrySet()) {
            String keyword = entry.getKey();
            if (text.contains(keyword)) {
                result.getCommandKeywords().add(keyword);
                // 选择最长的匹配关键词（更精确）
                if (keyword.length() > maxLength) {
                    maxLength = keyword.length();
                    detectedCommand = entry.getValue();
                }
            }
        }
        
        return detectedCommand;
    }
    
    /**
     * 提取停车场相关关键词
     */
    private void extractParkingKeywords(String text, ExtractionResult result) {
        for (String keyword : PARKING_KEYWORDS) {
            if (text.contains(keyword)) {
                result.getParkingKeywords().add(keyword);
            }
        }
    }
    
    /**
     * 提取地址相关关键词
     */
    private void extractAddressKeywords(String text, ExtractionResult result) {
        // 提取位置修饰词
        for (String modifier : LOCATION_MODIFIERS) {
            if (text.contains(modifier)) {
                result.getAddressKeywords().add(modifier);
            }
        }
        
        // 提取行政区划关键词
        for (String admin : ADMINISTRATIVE_KEYWORDS) {
            if (text.contains(admin)) {
                result.getAddressKeywords().add(admin);
            }
        }
    }
    
    /**
     * 提取目的地（地址名称）
     * 优先提取"附近"、"旁边"等位置修饰词前面的内容
     * 同时检查是否可能是停车场名称
     */
    private String extractDestination(String text, ExtractionResult result) {
        String destination = null;
        
        // 方法1: 提取"XX附近"中的XX（最优先，因为这是最常见的表达方式）
        for (String modifier : LOCATION_MODIFIERS) {
            int idx = text.indexOf(modifier);
            if (idx > 0) {
                String before = text.substring(0, idx).trim();
                // 移除指令关键词（但保留地址名称）
                before = removeCommandKeywords(before);
                // 移除停车场关键词
                before = removeParkingKeywords(before);
                // 移除时间关键词
                before = removeTimeKeywords(before);
                // 移除助词
                before = before.replaceAll("^(帮我|请|给我|要|想)", "").trim();
                before = before.replaceAll("的$", "").trim();
                
                // 如果提取到的内容不为空且看起来像地址名称（包含中文字符或数字）
                if (!before.isEmpty() && before.length() <= 20 && 
                    (before.matches(".*[\\u4e00-\\u9fa5].*") || before.matches(".*\\d+.*"))) {
                    // 检查是否可能是停车场名称（支持联想匹配）
                    if (parkingNameService.isPossibleParkingName(before)) {
                        List<String> matches = parkingNameService.fuzzyMatch(before);
                        if (!matches.isEmpty()) {
                            // 如果匹配到停车场名称，将其作为停车场名称而不是目的地
                            // 但这里我们仍然将其作为目的地，因为用户说的是"XX附近的停车场"
                            destination = before;
                            System.out.println("方法1提取到目的地（可能是停车场名称）: " + destination + " (从位置修饰词 '" + modifier + "' 前提取)");
                            System.out.println("  联想匹配结果: " + matches);
                        } else {
                            destination = before;
                            System.out.println("方法1提取到目的地: " + destination + " (从位置修饰词 '" + modifier + "' 前提取)");
                        }
                    } else {
                        destination = before;
                        System.out.println("方法1提取到目的地: " + destination + " (从位置修饰词 '" + modifier + "' 前提取)");
                    }
                    break;
                }
            }
        }
        
        // 方法2: 如果方法1没找到，尝试提取包含行政区划关键词的短语
        if (destination == null || destination.isEmpty()) {
            // 匹配"XX路"、"XX区"、"XX广场"等模式
            Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]{1,10}(?:路|街|道|区|县|市|广场|大厦|商场|中心|公园|桥|站))");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                destination = matcher.group(1);
                System.out.println("方法2提取到目的地: " + destination + " (从行政区划关键词提取)");
            }
        }
        
        // 方法3: 如果是指令"预约XX"或"查找XX"，提取指令后的内容
        if (destination == null || destination.isEmpty()) {
            for (String cmd : Arrays.asList("预约", "查找", "找", "搜索", "查询", "查", "导航", "去", "到")) {
                int idx = text.indexOf(cmd);
                if (idx >= 0) {
                    String after = text.substring(idx + cmd.length()).trim();
                    // 移除位置修饰词
                    for (String modifier : LOCATION_MODIFIERS) {
                        after = after.replace(modifier, "").trim();
                    }
                    // 移除停车场关键词
                    after = removeParkingKeywords(after);
                    // 移除时间关键词
                    after = removeTimeKeywords(after);
                    // 移除助词和标点
                    after = after.replaceAll("^(帮我|请|给我|要|想)", "").trim();
                    after = after.replaceAll("的$", "").trim();
                    after = after.replaceAll("[。，,！!？?]", "").trim();
                    
                    // 如果剩余内容不为空且不太长，作为目的地
                    if (!after.isEmpty() && after.length() <= 20) {
                        // 检查是否包含停车场关键词，如果包含则可能是停车场名称而不是地址
                        boolean hasParkingKeyword = false;
                        for (String pk : PARKING_KEYWORDS) {
                            if (after.contains(pk)) {
                                hasParkingKeyword = true;
                                break;
                            }
                        }
                        if (!hasParkingKeyword && (after.matches(".*[\\u4e00-\\u9fa5].*") || after.matches(".*\\d+.*"))) {
                            destination = after;
                            System.out.println("方法3提取到目的地: " + destination + " (从指令 '" + cmd + "' 后提取)");
                            break;
                        }
                    }
                }
            }
        }
        
        return destination;
    }
    
    /**
     * 提取停车场名称
     * 支持从数据库加载的停车场名称列表进行模糊匹配和联想
     */
    private String extractParkingLotName(String text, ExtractionResult result) {
        String parkingLotName = null;
        
        // 方法1: 使用停车场名称服务进行模糊匹配
        // 提取文本中可能包含停车场名称的部分
        String possibleName = extractPossibleParkingName(text);
        if (possibleName != null && !possibleName.isEmpty()) {
            List<String> matches = parkingNameService.fuzzyMatch(possibleName);
            if (!matches.isEmpty()) {
                parkingLotName = matches.get(0); // 使用最匹配的结果
                System.out.println("通过模糊匹配找到停车场名称: " + parkingLotName + " (输入: " + possibleName + ")");
                return parkingLotName;
            }
        }
        
        // 方法2: 提取"XX停车场"中的XX，然后尝试匹配
        for (String pk : PARKING_KEYWORDS) {
            int idx = text.indexOf(pk);
            if (idx > 0) {
                String before = text.substring(0, idx).trim();
                // 移除指令关键词
                before = removeCommandKeywords(before);
                // 移除位置修饰词
                before = removeLocationModifiers(before);
                // 移除时间关键词
                before = removeTimeKeywords(before);
                // 移除助词
                before = before.replaceAll("^(帮我|请|给我|要|想)", "").trim();
                before = before.replaceAll("的$", "").trim();
                
                if (!before.isEmpty() && before.length() <= 30) {
                    // 尝试匹配完整的停车场名称
                    List<String> matches = parkingNameService.fuzzyMatch(before);
                    if (!matches.isEmpty()) {
                        parkingLotName = matches.get(0);
                        System.out.println("通过关键词匹配找到停车场名称: " + parkingLotName + " (输入: " + before + ")");
                        return parkingLotName;
                    } else {
                        // 如果匹配不到，使用原始格式
                        parkingLotName = before + pk;
                    }
                    break;
                }
            }
        }
        
        // 方法3: 如果方法1和2没找到，尝试提取已知的停车场名称模式
        Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,15}(?:停车场|停车位|停车库|停车楼))");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String found = matcher.group(1);
            // 尝试匹配到数据库中的名称
            List<String> matches = parkingNameService.fuzzyMatch(found);
            if (!matches.isEmpty()) {
                parkingLotName = matches.get(0);
            } else {
                parkingLotName = found;
            }
        }
        
        return parkingLotName;
    }
    
    /**
     * 从文本中提取可能的停车场名称部分
     */
    private String extractPossibleParkingName(String text) {
        // 移除指令关键词
        String cleaned = removeCommandKeywords(text);
        // 移除位置修饰词
        cleaned = removeLocationModifiers(cleaned);
        // 移除时间关键词
        cleaned = removeTimeKeywords(cleaned);
        // 移除停车场通用关键词（保留具体名称）
        for (String pk : PARKING_KEYWORDS) {
            cleaned = cleaned.replace(pk, "").trim();
        }
        // 移除助词和标点
        cleaned = cleaned.replaceAll("^(帮我|请|给我|要|想)", "").trim();
        cleaned = cleaned.replaceAll("的$", "").trim();
        cleaned = cleaned.replaceAll("[。，,！!？?]", "").trim();
        
        // 如果剩余内容看起来像停车场名称（2-20个字符，主要是中文）
        if (!cleaned.isEmpty() && cleaned.length() >= 2 && cleaned.length() <= 20 
            && cleaned.matches(".*[\\u4e00-\\u9fa5].*")) {
            return cleaned;
        }
        
        return null;
    }
    
    /**
     * 清理文本
     */
    private String cleanText(String text, ExtractionResult result) {
        String cleaned = text;
        
        // 移除时间关键词（但保留时间信息）
        cleaned = removeTimeKeywords(cleaned);
        
        // 移除多余的标点
        cleaned = cleaned.replaceAll("[。，,！!？?]", "").trim();
        
        return cleaned;
    }
    
    /**
     * 移除指令关键词
     */
    private String removeCommandKeywords(String text) {
        String result = text;
        for (String keyword : COMMAND_KEYWORDS.keySet()) {
            result = result.replace(keyword, "").trim();
        }
        return result;
    }
    
    /**
     * 移除停车场关键词
     */
    private String removeParkingKeywords(String text) {
        String result = text;
        for (String keyword : PARKING_KEYWORDS) {
            result = result.replace(keyword, "").trim();
        }
        return result;
    }
    
    /**
     * 移除位置修饰词
     */
    private String removeLocationModifiers(String text) {
        String result = text;
        for (String modifier : LOCATION_MODIFIERS) {
            result = result.replace(modifier, "").trim();
        }
        return result;
    }
    
    /**
     * 移除时间关键词（但保留数字，因为可能包含时间信息）
     */
    private String removeTimeKeywords(String text) {
        String result = text;
        for (String keyword : TIME_KEYWORDS) {
            result = result.replace(keyword, "").trim();
        }
        return result;
    }
    
    /**
     * 检查文本是否包含停车场相关关键词
     */
    public boolean containsParkingKeyword(String text) {
        if (text == null) return false;
        for (String keyword : PARKING_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查文本是否包含地址相关关键词
     */
    public boolean containsAddressKeyword(String text) {
        if (text == null) return false;
        for (String keyword : LOCATION_MODIFIERS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        for (String keyword : ADMINISTRATIVE_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查文本是否包含指令关键词
     */
    public boolean containsCommandKeyword(String text) {
        if (text == null) return false;
        for (String keyword : COMMAND_KEYWORDS.keySet()) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

