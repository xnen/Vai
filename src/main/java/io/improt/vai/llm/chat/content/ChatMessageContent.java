package io.improt.vai.llm.chat.content;

/**
 * Interface for chat message content.
 * Implementations include text, audio, and image content.
 */
public interface ChatMessageContent {
    /**
     * Provides a brief summary of the content.
     *
     * @return String summary.
     */
    String getBrief();
}
