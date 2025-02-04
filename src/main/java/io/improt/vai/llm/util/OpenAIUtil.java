package io.improt.vai.llm.util;

import com.openai.models.ChatCompletionContentPartImage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class OpenAIUtil {

    public static ChatCompletionContentPartImage createImagePart(File file) {
        String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
        if (!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg")) {
            System.err.println("[OpenAIUtil] Warning: Unsupported image extension: " + extension + ". Skipping file: " + file.getAbsolutePath());
            return null;
        }

        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);
            String mimeType = "image/" + extension;
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            ChatCompletionContentPartImage.ImageUrl imageUrl = ChatCompletionContentPartImage.ImageUrl.builder()
                    .url(dataUrl)
                    .build();
            return ChatCompletionContentPartImage.builder().imageUrl(imageUrl).build();

        } catch (IOException e) {
            System.err.println("[OpenAIUtil] Error reading file: " + file.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }
}
