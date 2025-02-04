package io.improt.vai.llm.providers;

import com.openai.models.*;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.ChatMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class O3MiniProvider extends OpenAICommons implements IModelProvider {

    public O3MiniProvider() {
        super();
    }

    @Override
    public String request(String prompt, String userRequest, List<File> files) {
        if (files != null && !files.isEmpty()) {
            System.err.println("[O3MiniProvider] Warning: OpenAI does not support sending files. Ignoring " + files.size() + " files.");
        }

        System.out.println("o3 mini called");

        long start = System.currentTimeMillis();
        System.out.println("[O3-Mini] Beginning request of ");
        System.out.println(prompt);

        ChatCompletionDeveloperMessageParam developerMessage = ChatCompletionDeveloperMessageParam.builder()
                .content(prompt)
                .build();

        ChatCompletionContentPartText text = ChatCompletionContentPartText.builder()
                .text(userRequest)
                .build();

        List<ChatCompletionContentPart> parts = new ArrayList<>();
        ChatCompletionContentPart chatCompletionContentPart = ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder().text(userRequest).build());
        parts.add(chatCompletionContentPart);

        ChatCompletionUserMessageParam userMessage = ChatCompletionUserMessageParam.builder()
                .contentOfArrayOfContentParts(parts)
                .build();

        // Use the provided reasoningEffort if not null; default to MEDIUM otherwise.
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(developerMessage)
                .addMessage(userMessage)
                .model(ChatModel.O3_MINI)
                .reasoningEffort(App.getInstance().getConfiguredReasoningEffort())
                .build();

        String s = this.submitToModel(params);
        long end = System.currentTimeMillis();
        System.out.println("Request took " + (end - start) + " milliseconds");
        return s;
    }

    @Override
    public String chatRequest(List<ChatMessage> messages) throws Exception {
        ChatCompletionCreateParams build = this.buildChat(messages, false)
                .model(this.getModelName())
                .reasoningEffort(App.getInstance().getConfiguredReasoningEffort())
                .build();

        return this.submitToModel(build);
    }

    @Override
    protected boolean supportsDeveloperRole() {
        return true;
    }

    @Override
    public String getModelName() {
        return "o3-mini";
    }

    // This provider supports a dynamic reasoning effort provided by the UI slider.
    @Override
    public boolean supportsReasoningEffort() {
        return true;
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
}
