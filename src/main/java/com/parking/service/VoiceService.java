package com.parking.service;

import com.parking.model.vo.VoiceCommandResult;

/**
 * 语音交互服务接口
 * 负责处理用户语音指令识别和响应
 */
public interface VoiceService {
    
    /**
     * 处理用户语音指令
     * @param voiceCommand 语音指令文本
     * @param userId 用户ID
     * @param conversationHistory 对话历史（最近5次对话），可以为null
     * @return 处理结果
     */
    VoiceCommandResult processVoiceCommand(String voiceCommand, Long userId, java.util.List<java.util.Map<String, Object>> conversationHistory);
    
    /**
     * 识别语音指令类型
     * @param command 指令文本
     * @return 指令类型
     */
    String recognizeCommandType(String command);
    
    /**
     * 获取附近空车位
     * @param userId 用户ID
     * @return 附近空车位信息
     */
    VoiceCommandResult getNearbyEmptySpaces(Long userId);
    
    /**
     * 导航到指定车位
     * @param spaceId 车位ID
     * @param userId 用户ID
     * @return 导航信息
     */
    VoiceCommandResult navigateToSpace(Long spaceId, Long userId);
}