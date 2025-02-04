package io.improt.vai.llm.chat;

import io.improt.vai.llm.chat.content.ChatMessageContent;
import io.improt.vai.llm.chat.content.ChatMessageUserType;

import java.util.Date;

/**
 * ChatMessage represents an individual message in the chat conversation.
 * It encapsulates the sender type, the content, and a timestamp.
 */
public class ChatMessage {
    private final ChatMessageUserType messageType;
    private final ChatMessageContent content;
    private final Date timestamp;

    public ChatMessage(ChatMessageUserType messageType, ChatMessageContent content) {
        this.messageType = messageType;
        this.content = content;
        this.timestamp = new Date();
    }

    public ChatMessageUserType getMessageType() {
        return messageType;
    }

    public ChatMessageContent getContent() {
        return content;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
