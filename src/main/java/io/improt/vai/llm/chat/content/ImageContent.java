package io.improt.vai.llm.chat.content;

import com.openai.models.ChatCompletionContentPart;
import com.openai.models.ChatCompletionContentPartImage;
import io.improt.vai.llm.chat.content.impl.IChatContent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 * ImageContent represents a chat message containing an image file.
 */
public class ImageContent implements IChatContent {
    private final File imageFile;

    public ImageContent(File imageFile) {
        this.imageFile = imageFile;
    }

    public File getImageFile() {
        return imageFile;
    }

    public String toString() {
        return "[Image File: " + this.imageFile.getAbsolutePath() + "]";
    }

    @Override
    public ChatCompletionContentPart getPart() {
        String fileName = this.imageFile.getName();

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg")) {
            System.err.println("[OpenAIUtil] Warning: Unsupported image extension: " + extension + ". Skipping file: " + this.imageFile.getAbsolutePath());
            return null;
        }

        try {
            byte[] fileContent = Files.readAllBytes(this.imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);
            String mimeType = "image/" + extension;
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            ChatCompletionContentPartImage.ImageUrl imageUrl = ChatCompletionContentPartImage.ImageUrl.builder()
                    .url(dataUrl)
                    .build();
            return ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder().imageUrl(imageUrl).build());

        } catch (IOException e) {
            System.err.println("[ImageContent] Error reading file: " + this.imageFile.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }
}
