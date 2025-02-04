package io.improt.vai.llm.providers;

import com.openai.models.*;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.util.OpenAIUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class O1Provider extends OpenAICommons implements IModelProvider {

    public O1Provider() {
        super();
    }

    @Override
    public String request(String prompt, String userRequest, List<File> files) {
        long start = System.currentTimeMillis();
        System.out.println("[O1] Beginning request of ");
        System.out.println(prompt);

        ChatCompletionDeveloperMessageParam developerMessage = ChatCompletionDeveloperMessageParam.builder()
                .content(prompt)
                .build();

        ChatCompletionContentPartText text = ChatCompletionContentPartText.builder()
                .text(userRequest)
                .build();

        List<ChatCompletionContentPart> parts = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                ChatCompletionContentPartImage imagePart = OpenAIUtil.createImagePart(file);
                if (imagePart != null) {
                    parts.add(ChatCompletionContentPart.ofImageUrl(imagePart));
                }
            }
        }
        parts.add(ChatCompletionContentPart.ofText(text));

        ChatCompletionUserMessageParam userMessage = ChatCompletionUserMessageParam
                .builder().contentOfArrayOfContentParts(parts).build();

        ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder()
                .addMessage(developerMessage)
                .addMessage(userMessage)
                .model(ChatModel.O1);

        ChatCompletionReasoningEffort reasoningEffort = App.getInstance().getConfiguredReasoningEffort();

        if (supportsReasoningEffort() && reasoningEffort != null) {
            System.out.println("USING EFFORT = " + reasoningEffort);
            paramsBuilder.reasoningEffort(reasoningEffort);
        }

        ChatCompletionCreateParams params = paramsBuilder.build();

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
        return "o1";
    }

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
        return true; // O1 supports vision.
    }
}
