package io.improt.vai.llm.providers;

import com.openai.models.*;
import io.improt.vai.llm.chat.ChatMessage;

import java.io.File;
import java.util.List;

public class O1MiniProvider extends OpenAICommons implements IModelProvider {

    public O1MiniProvider() {
        super();
    }

    @Override
    public String request(String prompt, String userRequest, List<File> files) {
        if (files != null && !files.isEmpty()) {
            System.err.println("[O1MiniProvider] Warning: OpenAI does not support sending files. Ignoring " + files.size() + " files.");
        }

        long start = System.currentTimeMillis();
        System.out.println("[O1Mini] Beginning request of ");
        System.out.println(prompt);
        ChatModel modelEnum = ChatModel.O1_MINI;

        return simpleCompletion(prompt, userRequest, start, modelEnum, getClient().chat());
    }

    @Override
    public String chatRequest(List<ChatMessage> messages) throws Exception {
        throw new Exception("Unsupported model for chat.");
    }

    @Override
    protected boolean supportsDeveloperRole() {
        return false;
    }

    @Override
    public String getModelName() {
        return "o1-mini";
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
