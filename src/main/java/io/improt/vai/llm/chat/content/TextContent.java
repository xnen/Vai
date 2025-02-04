package io.improt.vai.llm.chat.content;

/**
 * TextContent represents a chat message containing plain text.
 */
public class TextContent implements ChatMessageContent {
    private final String text;

    public TextContent(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String getBrief() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
