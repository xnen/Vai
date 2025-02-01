package io.improt.vai.llm.providers;

import com.openai.models.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class O3MiniLowProvider extends OpenAICommons implements IModelProvider {

    public O3MiniLowProvider() {
        super();
    }

    @Override
    public String request(String model, String prompt, String userRequest, List<File> files) {
        if (files != null && !files.isEmpty()) {
            System.err.println("[O3Mini] Warning: OpenAI does not support sending files. Ignoring " + files.size() + " files.");
        }

        System.out.println("o3 mini low called");


        long start = System.currentTimeMillis();
        System.out.println("[O3-Mini] Beginning request of ");
        System.out.println(prompt);


        ChatCompletionDeveloperMessageParam developerMessage = ChatCompletionDeveloperMessageParam.builder()
                .content(prompt).build();

        ChatCompletionContentPartText text = ChatCompletionContentPartText.builder().text(userRequest).build();

        List<ChatCompletionContentPart> parts = new ArrayList<>();

        parts.add(ChatCompletionContentPart.ofChatCompletionContentPartText(text));

        ChatCompletionUserMessageParam build = ChatCompletionUserMessageParam
                .builder().contentOfArrayOfContentParts(parts).build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(developerMessage)
                .addMessage(build)
                .model("o3-mini")
                .reasoningEffort(ChatCompletionReasoningEffort.LOW).build();


        ChatCompletion completion = client.chat().completions().create(params);
        ChatCompletion validate = completion.validate();
        List<ChatCompletion.Choice> choices = validate.choices();

        if (choices.isEmpty()) {
            return null;
        }

        Optional<String> content = choices.get(0).message().content();
        long end = System.currentTimeMillis();
        System.out.println("Request took " + (end - start) + " milliseconds");
        return content.orElse(null);
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
