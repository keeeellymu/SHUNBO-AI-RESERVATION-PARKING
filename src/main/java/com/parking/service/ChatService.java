package com.parking.service;

/**
 * 讯飞星火大模型服务接口
 * 负责自然语言理解和对话生成
 */
public interface ChatService {
    
    /**
     * 获取 NLU 模式 (指令1) 的回复 (返回JSON)
     * @param text 用户的输入文本
     * @return AI模型的NLU JSON回复
     */
    String getNluResponse(String text);
    
    /**
     * 获取通用的聊天回复 (指令2)
     * @param text 用户的输入文本
     * @param conversationHistory 对话历史（最近5次对话），可以为null
     * @return AI模型的聊天回复
     */
    String getChatResponse(String text, java.util.List<java.util.Map<String, Object>> conversationHistory);
}

