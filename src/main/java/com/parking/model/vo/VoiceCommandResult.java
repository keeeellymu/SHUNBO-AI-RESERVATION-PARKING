package com.parking.model.vo;

import lombok.Data;
import java.util.Map;

/**
 * 语音指令处理结果
 */
@Data
public class VoiceCommandResult {
    
    /**
     * 处理状态：success/fail
     */
    private String status;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 指令类型
     */
    private String commandType;
    
    /**
     * 相关数据
     */
    private Object data;
    
    /**
     * 后续动作指令（例如 "NAV_TO_RESERVATION_FORM"）
     */
    private String followUpAction;
    
    /**
     * 用于后续动作的预填数据
     */
    private Map<String, Object> prefillData;
    
    /**
     * 创建成功响应
     */
    public static VoiceCommandResult success(String message, String commandType, Object data) {
        VoiceCommandResult result = new VoiceCommandResult();
        result.setStatus("success");
        result.setMessage(message);
        result.setCommandType(commandType);
        result.setData(data);
        return result;
    }
    
    /**
     * 创建带后续动作的成功响应
     */
    public static VoiceCommandResult successWithFollowUp(String message, String commandType, 
                                                         String followUpAction, Map<String, Object> prefillData) {
        VoiceCommandResult result = new VoiceCommandResult();
        result.setStatus("success");
        result.setMessage(message);
        result.setCommandType(commandType);
        result.setFollowUpAction(followUpAction);
        result.setPrefillData(prefillData);
        return result;
    }
    
    /**
     * 创建聊天响应（用于聊天模式）
     */
    public static VoiceCommandResult successChat(String message, Object data) {
        VoiceCommandResult result = new VoiceCommandResult();
        result.setStatus("success");
        result.setMessage(message);
        result.setCommandType("chat");
        result.setData(data);
        return result;
    }
    
    /**
     * 创建失败响应
     */
    public static VoiceCommandResult fail(String message) {
        VoiceCommandResult result = new VoiceCommandResult();
        result.setStatus("fail");
        result.setMessage(message);
        return result;
    }
}