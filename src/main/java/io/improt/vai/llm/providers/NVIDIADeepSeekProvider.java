package io.improt.vai.llm.providers;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.*;
import com.openai.services.blocking.ChatService;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.ChatMessage;
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
    public String request(String prompt, String userRequest, List<File> files) {
        if (files != null && !files.isEmpty()) {
            System.err.println("[NVIDIADeepSeekProvider] Warning: Nvidia does not support sending files. Ignoring " + files.size() + " files.");
        }
        if (client == null) {
            init();
        }
        long start = System.currentTimeMillis();
        return simpleCompletion(prompt, start, client.chat(), userRequest);
    }

    @Override
    public String chatRequest(List<ChatMessage> messages) throws Exception {
        throw new Exception("Unsupported model for chat.");
    }

    @Override
    public String getModelName() {
        return "deepseek-ai/deepseek-r1";
    }

    @Nullable
    protected String simpleCompletion(String prompt, long start, ChatService chat, String userRequest) {
        prompt += "\n\n REQUEST: " + userRequest;
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build())
                .model(this.getModelName())
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
