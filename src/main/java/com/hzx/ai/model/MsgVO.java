package com.hzx.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

/**
 * 对话历史记录VO
 * @author hzx
 */
@NoArgsConstructor
@Data
public class MsgVO {
    private String role;
    private String content;

    public MsgVO(Message message) {
        this.role = switch (message.getMessageType()) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            default -> "";
        };
        this.content = message.getText();
    }
}