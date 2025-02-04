package io.improt.vai.llm.providers;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.*;
import com.openai.services.blocking.ChatService;
import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.chat.content.AudioContent;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.util.OpenAIUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
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

    @Override
    public String chatRequest(List<ChatMessage> messages) throws Exception {
        ChatCompletionCreateParams build = this.buildChat(messages)
                .model(this.getModelName())
                .build();

        return this.submitToModel(build);
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

    public ChatCompletionCreateParams.Builder buildChat(List<ChatMessage> conversationHistory) throws Exception {
        ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder();

        for (ChatMessage message : conversationHistory) {
            switch (message.getMessageType()) {
                case SYSTEM:
                    paramsBuilder.addMessage(buildSystemMessage(message));
                    break;
                case USER:
                    paramsBuilder.addMessage(buildUserMessage(message));
                    break;
                case ASSISTANT:
                    paramsBuilder.addMessage(buildAssistantMessage(message));
                    break;
            }
        }

        return paramsBuilder;
    }

    private ChatCompletionAssistantMessageParam buildAssistantMessage(ChatMessage message) throws Exception {
        if (!(message.getContent() instanceof TextContent)) {
            throw new Exception("Tried building assistant message with non-text!?");
        }

        TextContent content = (TextContent) message.getContent();
        return ChatCompletionAssistantMessageParam.builder()
                .content(content.getText())
                .build();
    }

    private ChatCompletionUserMessageParam buildUserMessage(ChatMessage message) throws IOException {
        List<ChatCompletionContentPart> parts = new ArrayList<>();

        if (message.getContent() instanceof ImageContent) {
            ImageContent imgContent = (ImageContent) message.getContent();
            File file = imgContent.getImageFile();
            ChatCompletionContentPartImage imagePart = OpenAIUtil.createImagePart(file);
            if (imagePart != null) {
                parts.add(ChatCompletionContentPart.ofImageUrl(imagePart));
            }
        } else if (message.getContent() instanceof TextContent) {
            TextContent txtContent = (TextContent) message.getContent();
            ChatCompletionContentPartText text = ChatCompletionContentPartText.builder()
                    .text(txtContent.getText())
                    .build();
            parts.add(ChatCompletionContentPart.ofText(text));
        } else if (message.getContent() instanceof AudioContent) {
            AudioContent audioContent = (AudioContent) message.getContent();
            String s = encodeMp3ToBase64(audioContent.getAudioFile().getAbsolutePath());

            ChatCompletionContentPartInputAudio audio = ChatCompletionContentPartInputAudio.builder()
                    .inputAudio(ChatCompletionContentPartInputAudio.InputAudio.builder().data(s).build()).build();

            parts.add(ChatCompletionContentPart.ofInputAudio(audio));
        }

        return ChatCompletionUserMessageParam
                .builder().contentOfArrayOfContentParts(parts).build();
    }

    public static String encodeMp3ToBase64(String filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Path.of(filePath));
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    private ChatCompletionSystemMessageParam buildSystemMessage(ChatMessage message) throws Exception {
        if (!(message.getContent() instanceof TextContent)) {
            throw new Exception("Tried building system message with non-text!?");
        }

        TextContent content = (TextContent) message.getContent();
        return ChatCompletionSystemMessageParam.builder()
                .content(content.getText())
                .build();

    }

    protected String submitToModel(ChatCompletionCreateParams params) {
        ChatCompletion completion = this.getClient().chat().completions().create(params);
        ChatCompletion validate = completion.validate();
        List<ChatCompletion.Choice> choices = validate.choices();
        if (choices.isEmpty()) {
            return null;
        }
        Optional<String> content = choices.get(0).message().content();
        return content.orElse(null);
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
