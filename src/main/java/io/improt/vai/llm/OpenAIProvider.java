package io.improt.vai.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.*;
import io.improt.vai.backend.App;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class OpenAIProvider implements LLMProvider {

    private OpenAIClient client;

    public OpenAIProvider() {

    }

    @Override
    public void init() {
        String apiKey = App.getApiKey();

        if (apiKey == null) {
            System.out.println("No API key found");
            return;
        }

        // Trim newlines and whitespace from the API key
        apiKey = apiKey.trim();

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        System.out.println("[OpenAIProvider] OpenAI client initialized");
        this.client = client;
    }

    @Override
    public String request(String model, String prompt, List<File> files) {
        if (files != null && !files.isEmpty()) {
            System.err.println("[OpenAIProvider] Warning: OpenAI does not support sending files. Ignoring " + files.size() + " files.");
        }

        long start = System.currentTimeMillis();
        System.out.println("Beginning request of ");
        System.out.println(prompt);
        ChatModel modelEnum;

        if (model.equals("o1-preview")) {
            modelEnum = ChatModel.O1_PREVIEW;
        } else if (model.equals("o1-mini")) {
            modelEnum = ChatModel.O1_MINI;
        } else {
            System.out.println("Invalid model: " + model);
            // fall back to o1-mini
            modelEnum = ChatModel.O1_MINI;
        }

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .messages(List.of(ChatCompletionMessageParam.ofChatCompletionUserMessageParam(ChatCompletionUserMessageParam.builder()
                        .role(ChatCompletionUserMessageParam.Role.USER)
                        .content(ChatCompletionUserMessageParam.Content.ofTextContent(prompt))
                        .build())))
                .model(modelEnum)
                .build();

        ChatCompletion completion = this.client.chat().completions().create(params);
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
}
