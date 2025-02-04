package io.improt.vai.llm.providers;

import com.openai.models.*;
import io.improt.vai.llm.chat.ChatMessage;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class O1PreviewProvider extends OpenAICommons implements IModelProvider {

    public O1PreviewProvider() {
        super();
    }

    @Override
    public String request(String prompt, String userRequest, List<File> files) {
        if (files != null && !files.isEmpty()) {
            System.err.println("[O1PreviewProvider] Warning: o1-preview does not support sending files. Ignoring " + files.size() + " files.");
        }

        long start = System.currentTimeMillis();
        System.out.println("[O1Preview] Beginning request of ");
        System.out.println(prompt);
        ChatModel modelEnum = ChatModel.O1_PREVIEW;

        return simpleCompletion(prompt, userRequest, start, modelEnum, getClient().chat());
    }

    @Override
    public String getModelName() {
        return ChatModel.O1_PREVIEW.asString();
    }

    @Override
    public boolean supportsAudio() {
        return false;
    }

    @Override
    public boolean supportsVideo() {
        return false;
    }

    @Override
    public boolean supportsVision() {
        return false;
    }

    @Override
    public boolean supportsReasoningEffort() {
        return false;
    }
}
