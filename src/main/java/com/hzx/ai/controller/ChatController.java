package com.hzx.ai.controller;

import com.hzx.ai.config.repository.ChatHistoryRepository;
import com.hzx.ai.service.ChatService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 对话管理
 * @author hzx
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatService chatService;

    private final ChatHistoryRepository chatHistoryRepository;

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(
            @RequestParam("prompt") @Schema(description = "对话内容") String prompt,
            @RequestParam("chatId") @Schema(description = "对话id") String chatId,
            @RequestParam(value = "files", required = false) @Schema(description = "附件") List<MultipartFile> files
    ) {
        // 1.保存会话id
        chatHistoryRepository.save("chat", chatId);
        // 2.请求模型
        if (files == null || files.isEmpty()) {
            // 没有附件，纯文本聊天
            return chatService.textChat(prompt, chatId);
        } else {
            // 有附件，多模态聊天
            return chatService.multiModalChat(prompt, chatId, files);
        }
    }


}
