package io.improt.vai.llm.chat.content.impl;

import com.openai.models.chat.completions.ChatCompletionContentPart;

import java.io.IOException;

public interface IChatContent {
    ChatCompletionContentPart getPart() throws IOException;
}
