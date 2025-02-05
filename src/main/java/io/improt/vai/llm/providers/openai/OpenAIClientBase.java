package io.improt.vai.llm.providers.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.*;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.utils.Messages;
import io.improt.vai.testing.ISnippetAction;
import io.improt.vai.testing.SnippetHandler;

import java.io.File;
import java.util.*;

public abstract class OpenAIClientBase implements IModelProvider {

    private final String modelName;
    private final String baseUrl;
    private final String apiKey;

    public OpenAIClientBase(String baseUrl, String modelName, String apiKey) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.apiKey = apiKey;
    }

    public OpenAIClientBase(String modelName) {
        this.baseUrl = null; // Null baseUrl = Assume SDK API.
        this.modelName = modelName;
        this.apiKey = App.getOpenAIKey();
    }

    @Override
    public void init() {}

    @Override
    public String request(String systemMessage, String userRequest, List<File> files) {
        if (!this.supportsVision()) {
            return this.blockingCompletion(this.systemUserConfig(systemMessage, userRequest));
        }

        List<ChatCompletionContentPart> parts = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                ChatCompletionContentPart img = Messages.imagePart(file);
                if (img == null) continue;
                parts.add(img);
            }
        }

        parts.add(Messages.textPart(userRequest));

        ChatCompletionUserMessageParam usrParam = ChatCompletionUserMessageParam
                .builder().contentOfArrayOfContentParts(parts).build();

        ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder();

        if (this.supportsDeveloperRole()) {
            paramsBuilder.addMessage(Messages.developer(systemMessage));
        } else {
            paramsBuilder.addMessage(Messages.system(systemMessage));
        }

        paramsBuilder.addMessage(usrParam).model(this.getModelName());

        if (this.supportsReasoningEffort()) {
            paramsBuilder.reasoningEffort(App.getInstance().getConfiguredReasoningEffort());
        }

        return this.blockingCompletion(paramsBuilder.build());
    }

    @Override
    public String getModelName() {
        return this.modelName;
    }

    @Override
    public String chatRequest(List<ChatMessage> messages) throws Exception {
        ChatCompletionCreateParams.Builder builder = Messages
                .buildChat(this, messages)
                .model(this.getModelName());

        if (this.supportsReasoningEffort()) {
            builder.reasoningEffort(App.getInstance().getConfiguredReasoningEffort());
        }

        return this.blockingCompletion(builder.build());
    }

    public void stream(ChatCompletionCreateParams params, ISnippetAction streamReceived, int bufferSize) {
        try (StreamResponse<ChatCompletionChunk> streaming = getOrCreateClient()
                .chat()
                .completions()
                .createStreaming(params)) {

            Iterator<ChatCompletionChunk> iterator = streaming.stream().iterator();
            SnippetHandler snippetHandler = new SnippetHandler(streamReceived);

            var ref = new Object() {
                String buffer = "";
                int len = 0;
            };

            while (iterator.hasNext()) {
                ChatCompletionChunk chunk = iterator.next();
                chunk.choices().forEach(choice ->
                        choice.delta().content().ifPresent(content -> {
                            ref.buffer += content;
                            int len = ref.buffer.length();
                            if (len - ref.len > bufferSize) {
                                String substr = ref.buffer.substring(ref.len);
                                ref.len = len;
                                snippetHandler.addSnippet(substr);
                            }
                        })
                );
            }

            snippetHandler.addSnippet(ref.buffer.substring(ref.len));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChatCompletionCreateParams systemUserConfig(String systemMessage, String userMessage) {
        return this.simpleSystemUserRequest(systemMessage, userMessage, null);
    }

    public ChatCompletionCreateParams simpleSystemUserRequest(String systemMessage, String userMessage, ChatCompletionReasoningEffort effort) {
        ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder();

        if (this.supportsDeveloperRole()) {
            paramsBuilder.addMessage(Messages.developer(systemMessage));
        } else {
            paramsBuilder.addMessage(Messages.system(systemMessage));
        }

        paramsBuilder.addMessage(ChatCompletionUserMessageParam.builder().content(userMessage).build());
        if (this.supportsReasoningEffort()) {
            // Fallback to client configured.
            if (effort == null) effort = App.getInstance().getConfiguredReasoningEffort();
            paramsBuilder.reasoningEffort(effort);
        }
        paramsBuilder.model(this.getModelName());
        return paramsBuilder.build();
    }

    public String blockingCompletion(ChatCompletionCreateParams params) {
        System.out.println("[OpenAIClientBase] Beginning completion...");
        long start = System.currentTimeMillis();
        try {
            ChatCompletion completion = this.getOrCreateClient().chat().completions().create(params);
            long end = System.currentTimeMillis();
            System.out.println("[OpenAIClientBase] Took " + (end - start) + " ms.");

            ChatCompletion validate = completion.validate();
            List<ChatCompletion.Choice> choices = validate.choices();
            if (choices.isEmpty()) {
                return null;
            }
            Optional<String> content = choices.get(0).message().content();
            return content.orElse(null);
        } catch (Exception e) {
            throw new RuntimeException("[OpenAIClientBase] Unable to complete request: " + e.getMessage());
        }
    }

    public boolean supportsDeveloperRole() {
        return false;
    }

    public boolean supportsReasoningEffort() {
        return false;
    }

    // Defaults to false.
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

    public OpenAIClient getOrCreateClient() throws Exception {
        OpenAIClient client = OpenAIClientBase.clients.get(this.baseUrl);
        if (client == null) {
            client = this.createClient();
            OpenAIClientBase.clients.put(this.baseUrl, client);
        }

        return client;
    }

    private OpenAIClient createClient() throws Exception {
        if (this.apiKey == null) {
            throw new Exception("API key was not found, cannot create OpenAI Client.");
        }

        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder().apiKey(apiKey.trim());
        if (this.baseUrl != null) {
            builder.baseUrl(this.baseUrl);
        }
        return builder.build();
    }

    protected static Map<String, OpenAIClient> clients = new HashMap<>();
}
