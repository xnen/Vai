package io.improt.vai.llm.providers;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.*;
import io.improt.vai.llm.Cost;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.providers.impl.IModelProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClaudeProvider implements IModelProvider {
    private String apiKey;
    private AnthropicClient client;

    private List<String> messages = new ArrayList<>();

    @Override
    public String request(String prompt, String userRequest, List<File> files) {
        if (client == null) init();
        messages.clear();

        MessageCreateParams params = MessageCreateParams.builder()
                .maxTokens(64000L)
                .addUserMessage(prompt)
                .addUserMessage(userRequest)
                .model(Model.CLAUDE_3_7_SONNET_LATEST)
                .build();
//        Message message = client.messages().create(params);

        final boolean[] hadStop = {false};
        try (StreamResponse<RawMessageStreamEvent> streamResponse = client.messages().createStreaming(params)) {
            streamResponse.stream().forEach(chunk -> {
                if (chunk.isContentBlockDelta()) {
                    RawContentBlockDeltaEvent contentBlockDelta = chunk.asContentBlockDelta();
                    TextDelta text = contentBlockDelta.delta().asText();
                    TextDelta validate = text.validate();
                    String text1 = validate.text();
                    this.messages.add(text1);
                } else if (chunk.isStop()) {
                    System.out.println("No more chunks!");
                    hadStop[0] = true;
                }
            });
        }

        if (!hadStop[0]) {
            System.out.println("No stop!?");
        }

        List<String> result = this.messages;
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : result) {
            stringBuilder.append(s);
        }
        System.out.println(stringBuilder);
        return stringBuilder.toString();
    }

    public void test(String msg) {
        this.messages.add(msg);
    }


    @Override
    public String chatRequest(List<ChatMessage> messages) throws Exception {
        throw new Exception("Unsupported");
    }

    @Override
    public String getModelName() {
        return "Claude";
    }

    @Override
    public Cost getCost() {
        return Cost.MEDIUM;
    }

    @Override
    public void init() {
        this.apiKey = System.getenv("CLAUDE_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("[ClaudeProvider] API key not found in environment variable GOOGLE_API_KEY. Gemini will be disabled.");
        } else {
            System.out.println("[ClaudeProvider] Gemini API key found.");
        }

        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
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
