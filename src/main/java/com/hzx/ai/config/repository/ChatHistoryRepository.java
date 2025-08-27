package com.hzx.ai.config.repository;

import java.util.List;

/**
 * 对话历史持久化操作
 * @author hzx
 */
public interface ChatHistoryRepository {

    /**
     * 保存会话记录
     * @param type   业务类型，如：chat、service、pdf
     * @param chatId 会话ID
     */
    void save(String type, String chatId);

    /**
     * 获取会话ID列表
     * @param type 业务类型，如：chat、service、pdf
     * @return 会话ID列表
     */
    List<String> getChatIds(String type);
}
