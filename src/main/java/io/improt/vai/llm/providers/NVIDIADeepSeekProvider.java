package io.improt.vai.llm.providers;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.*;
import com.openai.services.blocking.ChatService;
import io.improt.vai.backend.App;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class NVIDIADeepSeekProvider implements IModelProvider {
    private OpenAIClient client;

    public NVIDIADeepSeekProvider() {
        super();
    }

    @Override
    public void init() {
        String apiKey = App.GetNvidiaKey();
        if (apiKey == null) {
            System.out.println("[NVIDIA-DeepSeek] Nvidia API key was not found. NVIDIA will be disabled.");
            return;
        }
        apiKey = apiKey.trim();
        // TODO: ENDPOINT CUSTOMIZATION.
        client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl("https://integrate.api.nvidia.com/v1")
                .build();
        System.out.println("[NVIDIA-DeepSeek] OpenAI client lazily initialized");
    }

    @Override
    public String request(String model, String prompt, String userRequest, List<File> files, ChatCompletionReasoningEffort reasoningEffort) {
        if (files != null && !files.isEmpty()) {
            System.err.println("[NVIDIADeepSeekProvider] Warning: Nvidia does not support sending files. Ignoring " + files.size() + " files.");
        }
        if (client == null) {
            init();
        }
        long start = System.currentTimeMillis();
        ChatModel modelEnum = ChatModel.O1_MINI;

        return simpleCompletion(prompt, start, modelEnum, client.chat(), userRequest);
    }

    @Nullable
    protected static String simpleCompletion(String prompt, long start, ChatModel modelEnum, ChatService chat, String userRequest) {
        prompt += "\n\n REQUEST: " + userRequest;
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build())
                .model("deepseek-ai/deepseek-r1")
                .temperature(0.6f)
                .topP(0.7)
                .maxCompletionTokens(4096L)
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
