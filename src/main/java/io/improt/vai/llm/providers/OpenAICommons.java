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

    // Shared among all OpenAI model providers.
    // Lazy initialization ensures we create the client only on first request.
    protected static OpenAIClient client;

    @Override
    public void init() {
        // Do nothing here; we initialize lazily in getClient().
    }

    protected OpenAIClient getClient() {
        if (client == null) {
            String apiKey = App.GetOpenAIKey();
            if (apiKey == null) {
                System.out.println("No API key found");
                throw new RuntimeException("No API key found");
            }
            apiKey = apiKey.trim();
            client = OpenAIOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();
            System.out.println("[OpenAICommons] OpenAI client lazily initialized");
        }
        return client;
    }

    @Nullable
    protected String simpleCompletion(String prompt, String request, long start, ChatModel modelEnum, ChatService chat) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build())
                .addMessage(ChatCompletionUserMessageParam.builder()
                        .content("REQUEST: " + request)
                        .build())
                .model(modelEnum)
                .build();
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
