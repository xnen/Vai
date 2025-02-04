package io.improt.vai.llm.providers;

import com.openai.models.*;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.util.OpenAIUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FourOProvider extends OpenAICommons implements IModelProvider {

    public FourOProvider() {
        super();
    }

    @Override
    public String request(String prompt, String userRequest, List<File> files) {
        long start = System.currentTimeMillis();
        System.out.println("[4o] Beginning request of ");
        System.out.println(prompt);

        ChatCompletionSystemMessageParam systemMessage = ChatCompletionSystemMessageParam.builder()
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
                .addMessage(systemMessage)
                .addMessage(userMessage)
                .model(ChatModel.CHATGPT_4O_LATEST);


        ChatCompletionCreateParams params = paramsBuilder.build();

        String s = this.submitToModel(params);
        long end = System.currentTimeMillis();
        System.out.println("Request took " + (end - start) + " milliseconds");
        return s;
    }

    @Override
    public String getModelName() {
        return "chatgpt-4o-latest";
    }

    @Override
    public boolean supportsReasoningEffort() {
        return false;
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

    @Override
    protected boolean supportsDeveloperRole() {
        return false;
    }
}
