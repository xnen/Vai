package io.improt.vai.llm.chat.content;

import java.io.File;

/**
 * ImageContent represents a chat message containing an image file.
 */
public class ImageContent implements ChatMessageContent {
    private final File imageFile;

    public ImageContent(File imageFile) {
        this.imageFile = imageFile;
    }

    public File getImageFile() {
        return imageFile;
    }

    @Override
    public String getBrief() {
        return "[Image: " + imageFile.getName() + "]";
    }

    @Override
    public String toString() {
        return "[Image File: " + imageFile.getAbsolutePath() + "]";
    }
}
