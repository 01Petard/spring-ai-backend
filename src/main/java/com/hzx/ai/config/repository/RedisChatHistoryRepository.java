package com.hzx.ai.config.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 对话历史存到Redis中
 * @author hzx
 */
@RequiredArgsConstructor
@Component
@Primary
public class RedisChatHistoryRepository implements com.hzx.ai.config.repository.ChatHistoryRepository {

    private final static String CHAT_HISTORY_KEY_PREFIX = "chat:history:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String type, String chatId) {
        redisTemplate.opsForSet().add(CHAT_HISTORY_KEY_PREFIX + type, chatId);
    }

    @Override
    public List<String> getChatIds(String type) {
        Set<String> chatIds = redisTemplate.opsForSet().members(CHAT_HISTORY_KEY_PREFIX + type);
        if (chatIds == null || chatIds.isEmpty()) {
            return Collections.emptyList();
        }
        return chatIds.stream().sorted(String::compareTo).toList();
    }
}
