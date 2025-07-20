package com.hzx.ai.controller;

import com.hzx.ai.model.vo.MsgVO;
import com.hzx.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对话历史记录管理
 * @author hzx
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * 查询会话历史列表
     * @param type 业务类型，如：chat,service,pdf
     * @return chatId列表
     */
    @GetMapping("/{type}")
    public List<String> getChatIds(
            @PathVariable("type") String type
    ) {
        return chatHistoryRepository.getChatIds(type);
    }

    private final ChatMemory chatMemory;

    /**
     * 查询会话历史详情
     * @param type   业务类型，如：chat,service,pdf
     * @param chatId 会话id
     * @return 指定会话的历史消息
     */
    @GetMapping("/{type}/{chatId}")
    public List<MsgVO> getChatHistory(
            @PathVariable("type") String type,
            @PathVariable("chatId") String chatId
    ) {
        List<Message> messages = chatMemory.get(chatId, Integer.MAX_VALUE);
        if (messages == null) {
            return List.of();
        }
        return messages.stream().map(MsgVO::new).toList();
    }
}
