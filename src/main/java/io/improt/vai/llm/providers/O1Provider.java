package io.improt.vai.llm.providers;

import com.openai.models.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class O1Provider extends OpenAICommons implements IModelProvider {

    public O1Provider() {
        super();
    }

    @Override
    public String request(String model, String prompt, String userRequest, List<File> files) {
        long start = System.currentTimeMillis();
        System.out.println("Beginning request of ");
        System.out.println(prompt);


        ChatCompletionDeveloperMessageParam developerMessage = ChatCompletionDeveloperMessageParam.builder()
                .content(prompt).build();

        ChatCompletionContentPartText text = ChatCompletionContentPartText.builder().text(userRequest).build();

        List<com.openai.models.ChatCompletionContentPart> parts = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                ChatCompletionContentPartImage imagePart = createImagePart(file);
                if (imagePart != null) {
                    parts.add(ChatCompletionContentPart.ofChatCompletionContentPartImage(imagePart));
                }
            }
        }
        parts.add(ChatCompletionContentPart.ofChatCompletionContentPartText(text));


        ChatCompletionUserMessageParam build = ChatCompletionUserMessageParam
                .builder().contentOfArrayOfContentParts(parts).build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addMessage(developerMessage)
                .addMessage(build)
                .model(ChatModel.O1).build();


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

    private ChatCompletionContentPartImage createImagePart(File file) {
        String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
        if (!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg")) {
            System.err.println("[O1Provider] Warning: Unsupported image extension: " + extension + ". Skipping file: " + file.getAbsolutePath());
            return null;
        }

        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);
            String mimeType = "image/" + extension;
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            com.openai.models.ChatCompletionContentPartImage.ImageUrl imageUrl = com.openai.models.
                    ChatCompletionContentPartImage.ImageUrl.builder().url(dataUrl).build();
            return ChatCompletionContentPartImage.builder().imageUrl(imageUrl).build();

        } catch (IOException e) {
            System.err.println("[O1Provider] Error reading file: " + file.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean supportsAudio() {
        return false; // tbh idk
    }

    @Override
    public boolean supportsVideo() {
        return false;
    }

    @Override
    public boolean supportsVision() {
        return true; // O1 supports vision
    }
}
