package io.improt.vai.llm.providers;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.*;
import com.openai.services.blocking.ChatService;
import io.improt.vai.backend.App;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class OpenAICommons implements IModelProvider {

    protected static OpenAIClient client;

    @Override
    public void init() {
        String apiKey = App.GetOpenAIKey();

        if (apiKey == null) {
            System.out.println("No API key found");
            return;
        }

        // Trim newlines and whitespace from the API key
        apiKey = apiKey.trim();
        if (OpenAICommons.client == null) {
            OpenAIClient client = OpenAIOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();

            System.out.println("[OpenAICommonsProvider] OpenAI client initialized");
            OpenAICommons.client = client;
        }
    }

    @Nullable
    protected static String simpleCompletion(String prompt, String request, long start, ChatModel modelEnum, ChatService chat) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build())
                .addMessage(ChatCompletionUserMessageParam.builder()
                        .content("REQUEST: " + request)
                        .build())
                .model(modelEnum)
                .build();
        // TODO: Unsure if this supports the developer message. In API it says modern o1 models do, but o1-mini and o1-preview aren't modern.

        ChatCompletion completion = chat.completions().create(params);
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
