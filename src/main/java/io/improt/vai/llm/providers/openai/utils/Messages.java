package io.improt.vai.llm.providers.openai.utils;

import com.openai.models.*;
import com.openai.models.chat.completions.*;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.llm.providers.openai.OpenAIClientBase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Messages {
    public static ChatCompletionSystemMessageParam system(String message) {
        return ChatCompletionSystemMessageParam.builder()
                .content(message)
                .build();
    }

    public static ChatCompletionDeveloperMessageParam developer(String message) {
        return ChatCompletionDeveloperMessageParam.builder()
                .content(message)
                .build();
    }

    public static ChatCompletionAssistantMessageParam assistant(String message) {
        return ChatCompletionAssistantMessageParam.builder()
                .content(message)
                .build();
    }

    public static ChatCompletionContentPart textPart(String msg) {
        return ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder()
                .text(msg)
                .build());
    }

    public static ChatCompletionContentPart imagePart(File image) {
        ImageContent imageContent = new ImageContent(image);
        return imageContent.getPart();
    }


    public static ChatCompletionCreateParams.Builder buildChat(OpenAIClientBase clientBase, List<ChatMessage> conversationHistory) throws Exception {
        ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder();

        for (ChatMessage message : conversationHistory) {
            switch (message.getMessageType()) {
                case USER:
                    paramsBuilder.addMessage(Messages.userFromChat(message));
                    break;
                case SYSTEM:
                    if (!clientBase.supportsDeveloperRole()) {
                        paramsBuilder.addMessage(Messages.system(message.getContent().toString()));
                    } else {
                        paramsBuilder.addMessage(Messages.developer(message.getContent().toString()));
                    }
                    break;
                case ASSISTANT:
                    paramsBuilder.addMessage(Messages.assistant(message.getContent().toString()));
                    break;
            }
        }
        return paramsBuilder;
    }

    // TODO: Not ideal, some messages need image + text parts, etc.
    private static ChatCompletionUserMessageParam userFromChat(ChatMessage message) throws IOException {
        List<ChatCompletionContentPart> parts = new ArrayList<>();
        parts.add(message.getContent().getPart());
        return ChatCompletionUserMessageParam.builder().contentOfArrayOfContentParts(parts).build();
    }
}
