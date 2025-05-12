package io.improt.vai.llm.providers.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.*;
import com.openai.models.chat.completions.*;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.llm.providers.openai.utils.Messages;
import io.improt.vai.testing.ISnippetAction;
import io.improt.vai.testing.SnippetHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public abstract class OpenAIClientBase implements IModelProvider {

    private final String modelName;
    private final String baseUrl;
    private final String apiKey;

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "ogg", "flac", "m4a", "aac", "opus");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");


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
        List<ChatCompletionContentPart> parts = new ArrayList<>();

        // Always add the text part first
        parts.add(Messages.textPart(userRequest));

        boolean visionSupported = this.supportsVision();
        boolean audioSupported = this.supportsAudio();

        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                String extension = getFileExtension(file);
                if (extension == null) continue; // Skip files without extensions

                ChatCompletionContentPart filePart = null;

                if (visionSupported && IMAGE_EXTENSIONS.contains(extension)) {
                    System.out.println("[OpenAIClientBase] Adding image part: " + file.getName());
                    filePart = Messages.imagePart(file);
                } else if (audioSupported && AUDIO_EXTENSIONS.contains(extension)) {
                    System.out.println("[OpenAIClientBase] Adding audio part: " + file.getName());
                    // Assuming Messages.audioPart exists and functions similarly to imagePart
                    filePart = Messages.audioPart(file);
                } else {
                     System.out.println("[OpenAIClientBase] Skipping unsupported file type or feature: " + file.getName());
                }

                if (filePart != null) {
                    parts.add(filePart);
                } else {
                    System.err.println("[OpenAIClientBase] Failed to create content part for file: " + file.getName());
                }
            }
        }

        // If only text was added (or no files/supported files were provided),
        // and the model doesn't support multi-modal input in this way,
        // potentially fall back to simple text request?
        // For now, we build the potentially multi-modal request regardless.
        ChatCompletionUserMessageParam usrParam = ChatCompletionUserMessageParam
                .builder().contentOfArrayOfContentParts(parts).build();

        ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder();

        if (this.supportsDeveloperRole()) {
            paramsBuilder.addMessage(Messages.developer(systemMessage));
        } else {
            // Add system message only if it's not empty or null
            if (systemMessage != null && !systemMessage.trim().isEmpty()) {
                 paramsBuilder.addMessage(Messages.system(systemMessage));
            }
        }

        paramsBuilder.addMessage(usrParam).model(this.getModelName());

        if (this.supportsReasoningEffort()) {
            paramsBuilder.reasoningEffort(App.getInstance().getConfiguredReasoningEffort());
        }

        return this.blockingCompletion(paramsBuilder.build());
    }

    @Nullable
    private String getFileExtension(@NotNull File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return null; // No extension found
        }
        return name.substring(lastIndexOf + 1).toLowerCase();
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

        boolean debug = false;
        if (debug) {
            System.out.println("================ CHAT REQUEST START ================");
            for (ChatMessage msg : messages) {
                System.out.println("[" + msg.getMessageType() + "]");
                System.out.println(msg.getContent().toString());
                System.out.println();
            }
            System.out.println("Params: " + builder.build().toString()); // Log params
            System.out.println("================ CHAT REQUEST END ================");
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
                            // Check buffer size and also handle potential empty content strings
                            if (!content.isEmpty() && len - ref.len >= bufferSize) {
                                String substr = ref.buffer.substring(ref.len);
                                ref.len = len;
                                snippetHandler.addSnippet(substr);
                            }
                        })
                );
            }
            // Send any remaining buffered content after the loop finishes
            if (ref.buffer.length() > ref.len) {
               snippetHandler.addSnippet(ref.buffer.substring(ref.len));
            }


        } catch (Exception e) {
             // Log the error more informatively
            System.err.println("[OpenAIClientBase] Error during streaming request: " + e.getMessage());
            e.printStackTrace();
            // Optionally rethrow or handle more gracefully
             throw new RuntimeException("Streaming failed", e);
        }
    }

    public ChatCompletionCreateParams systemUserConfig(String systemMessage, String userMessage) {
        return this.simpleSystemUserRequest(systemMessage, userMessage, null);
    }

    public ChatCompletionCreateParams simpleSystemUserRequest(String systemMessage, String userMessage, ReasoningEffort effort) {
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
        System.out.println("[OpenAIClientBase] Beginning completion for model: " + params.model());
        // System.out.println("[OpenAIClientBase] Request Params: " + params.toString()); // Be cautious logging potentially sensitive data
        long start = System.currentTimeMillis();
        try {
            ChatCompletion completion = this.getOrCreateClient().chat().completions().create(params);
            long end = System.currentTimeMillis();
            System.out.println("[OpenAIClientBase] Completion took " + (end - start) + " ms.");

            List<ChatCompletion.Choice> choices = completion.choices();
            if (choices == null || choices.isEmpty()) {
                System.err.println("[OpenAIClientBase] No choices returned from API.");
                // Log usage data if available
                 completion.usage().ifPresent(usage -> System.out.println("[OpenAIClientBase] Usage: " + usage));
                return null;
            }
            // Log finish reason
            String finishReason = choices.get(0).finishReason().asString();
            System.out.println("[OpenAIClientBase] Finish Reason: " + finishReason);

            Optional<String> content = choices.get(0).message().content();
            // Log usage data if available
            completion.usage().ifPresent(usage -> System.out.println("[OpenAIClientBase] Usage: " + usage));

            if (content.isEmpty()) {
                 System.err.println("[OpenAIClientBase] Choice message content is empty.");
                 return null;
            }

            return content.get();
        } catch (Exception e) {
            // Log the parameters that caused the error (be careful with sensitive data)
            System.err.println("[OpenAIClientBase] Error during blocking completion for model " + params.model());
             // System.err.println("[OpenAIClientBase] Failing Params: " + params.toString()); // Be cautious logging potentially sensitive data
            e.printStackTrace();
            throw new RuntimeException("[OpenAIClientBase] Unable to complete request: " + e.getMessage(), e);
        }
    }

    public boolean supportsDeveloperRole() {
        return false;
    }

    public boolean supportsReasoningEffort() {
        return false;
    }

    // Defaults to false. Override in specific model implementations.
    @Override
    public boolean supportsAudio() {
        return false;
    }

    // Defaults to false. Override in specific model implementations.
    @Override
    public boolean supportsVideo() {
        // Video support not implemented in this pass.
        return false;
    }

    // Defaults to false. Override in specific model implementations.
    @Override
    public boolean supportsVision() {
        return false;
    }

    public OpenAIClient getOrCreateClient() throws Exception {
        // Use baseUrl as key, fallback to "DEFAULT" if null (for official SDK endpoint)
        String clientKey = this.baseUrl != null ? this.baseUrl : "DEFAULT_OPENAI";
        OpenAIClient client = OpenAIClientBase.clients.get(clientKey);
        if (client == null) {
            synchronized (clients) { // Synchronize client creation
                 // Double-check locking pattern
                 client = OpenAIClientBase.clients.get(clientKey);
                 if (client == null) {
                    System.out.println("[OpenAIClientBase] Creating new OpenAIClient for key: " + clientKey);
                    client = this.createClient();
                    OpenAIClientBase.clients.put(clientKey, client);
                 }
            }
        }
        return client;
    }

    private OpenAIClient createClient() throws Exception {
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new Exception("API key was not found or is empty, cannot create OpenAI Client.");
        }

        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder().apiKey(apiKey.trim());
        if (this.baseUrl != null) {
            System.out.println("[OpenAIClientBase] Configuring client with Base URL: " + this.baseUrl);
            builder.baseUrl(this.baseUrl);
        } else {
             System.out.println("[OpenAIClientBase] Configuring client for default OpenAI API URL.");
        }
        // Potentially add timeouts, interceptors etc. here if needed
        // builder.connectTimeout(Duration.ofSeconds(20));
        // builder.readTimeout(Duration.ofSeconds(120));

        return builder.build();
    }

    // Use ConcurrentHashMap for thread safety if clients can be accessed concurrently
    // private static Map<String, OpenAIClient> clients = new ConcurrentHashMap<>();
    // Using HashMap for now, assuming client creation/access is controlled or infrequent enough
    // Added synchronization in getOrCreateClient for safety.
    protected static final Map<String, OpenAIClient> clients = new HashMap<>();
}
