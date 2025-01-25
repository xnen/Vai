package io.improt.vai.llm.providers;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionUserMessageParam;
import com.openai.models.ChatModel;
import com.openai.services.blocking.ChatService;
import io.improt.vai.backend.App;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class DeepSeekProvider implements IModelProvider {
    private OpenAIClient client;

    public DeepSeekProvider() {
        super();
    }

    @Override
    public void init() {
        String apiKey = "LocalLLM";

        // TODO: ENDPOINT CUSTOMIZATION.
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl("http://192.168.1.195:11434/v1")
                .build();

        System.out.println("[OpenAICommonsProvider] OpenAI client initialized");
        this.client = client;
    }

    @Override
    public String request(String model, String prompt, String userRequest, List<File> files) {
        if (files != null && !files.isEmpty()) {
            System.err.println("[DeepSeekProvider] Warning: OpenAI does not support sending files. Ignoring " + files.size() + " files.");
        }
        long start = System.currentTimeMillis();
        ChatModel modelEnum = ChatModel.O1_MINI;

        return simpleCompletion(prompt, start, modelEnum, this.client.chat(), userRequest);
    }

    @Nullable
    protected static String simpleCompletion(String prompt, long start, ChatModel modelEnum, ChatService chat, String userRequest) {
        prompt += "\n\n REQUEST: " + userRequest;

        System.out.println("Full Prompt:");
        System.out.println(prompt);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build())
//                .addMessage(ChatCompletionUserMessageParam.builder()
//                        .content("REQUEST: " + userRequest)
//                        .build())
                .model("hf.co/unsloth/DeepSeek-R1-Distill-Qwen-32B-GGUF:DeepSeek-R1-Distill-Qwen-32B-Q4_K_M.gguf")
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
}
