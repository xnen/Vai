package io.improt.vai.llm.chat.content;

import com.openai.models.ChatCompletionContentPart;
import com.openai.models.ChatCompletionContentPartText;
import io.improt.vai.llm.chat.content.impl.IChatContent;

/**
 * TextContent represents a chat message containing plain text.
 */
public class TextContent implements IChatContent {
    private final String text;

    public TextContent(String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }

    @Override
    public ChatCompletionContentPart getPart() {
        ChatCompletionContentPartText text = ChatCompletionContentPartText.builder()
                .text(this.text)
                .build();
        return ChatCompletionContentPart.ofText(text);
    }
}
