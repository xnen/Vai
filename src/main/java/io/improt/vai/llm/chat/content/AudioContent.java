package io.improt.vai.llm.chat.content;

import java.io.File;

/**
 * AudioContent represents a chat message containing an audio file.
 * Expected to be in MP3 format as required.
 */
public class AudioContent implements ChatMessageContent {
    private final File audioFile;

    public AudioContent(File audioFile) {
        this.audioFile = audioFile;
    }

    public File getAudioFile() {
        return audioFile;
    }

    @Override
    public String getBrief() {
        return "[Audio: " + audioFile.getName() + "]";
    }

    @Override
    public String toString() {
        return "[Audio File: " + audioFile.getAbsolutePath() + "]";
    }
}
