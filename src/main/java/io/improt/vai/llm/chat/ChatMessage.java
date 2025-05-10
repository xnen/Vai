package io.improt.vai.llm.chat;

import io.improt.vai.llm.chat.content.impl.IChatContent;
import io.improt.vai.llm.chat.content.ChatMessageUserType;

import java.util.Date;

/**
 * ChatMessage represents an individual message in the chat conversation.
 * It encapsulates the sender type, the content, and a timestamp.
 */
public class ChatMessage {
    private ChatMessageUserType messageType;
    private IChatContent content;
    private final Date timestamp;

    public ChatMessage(ChatMessageUserType messageType, IChatContent content) {
        this.messageType = messageType;
        this.content = content;
        this.timestamp = new Date();
    }

    public ChatMessageUserType getMessageType() {
        return messageType;
    }

    public IChatContent getContent() {
        return content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void replaceContents(ChatMessage msg) {
        this.messageType = msg.messageType;
        this.content = msg.content;
    }
}
