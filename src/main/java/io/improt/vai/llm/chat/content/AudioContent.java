package io.improt.vai.llm.chat.content;

import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartInputAudio;
import io.improt.vai.llm.chat.content.impl.IChatContent;
import io.improt.vai.util.EncodingUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * AudioContent represents a chat message containing an audio file.
 * Supports various audio formats by detecting the file extension.
 */
public class AudioContent implements IChatContent {
    private final File audioFile;

    public AudioContent(File audioFile) {
        if (audioFile == null || !audioFile.exists() || !audioFile.isFile()) {
            throw new IllegalArgumentException("Invalid audio file provided: " + audioFile);
        }
        this.audioFile = audioFile;
    }

    public File getAudioFile() {
        return audioFile;
    }

    public String toString() {
        return "[Audio File: " + audioFile.getAbsolutePath() + "]";
    }

    private Optional<String> getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return Optional.empty(); // No extension found
        }
        return Optional.of(name.substring(lastIndexOf + 1).toLowerCase(Locale.ROOT));
    }

    @Override
    public ChatCompletionContentPart getPart() throws IOException {
        String s = EncodingUtils.encodeFileToBase64(this.audioFile.getAbsolutePath());

        Optional<String> extensionOpt = getFileExtension(this.audioFile);
        ChatCompletionContentPartInputAudio.InputAudio.Format format = ChatCompletionContentPartInputAudio.InputAudio.Format.WAV; // Default

        if (extensionOpt.isPresent()) {
            String extension = extensionOpt.get();
            switch (extension) {
                case "wav":
                    format = ChatCompletionContentPartInputAudio.InputAudio.Format.WAV;
                    break;
                case "mp3":
                    format = ChatCompletionContentPartInputAudio.InputAudio.Format.MP3;
                    break;
                // Add other supported formats here if needed (e.g., opus, aac, flac)
                // case "opus":
                //    format = ChatCompletionContentPartInputAudio.InputAudio.Format.OPUS;
                //    break;
                // case "aac":
                //    format = ChatCompletionContentPartInputAudio.InputAudio.Format.AAC;
                //    break;
                // case "flac":
                //    format = ChatCompletionContentPartInputAudio.InputAudio.Format.FLAC;
                //    break;
                default:
                    System.out.printf("WARN: Unsupported audio file extension '%s', defaulting to WAV.%n", extension);
                    // Defaulting to WAV as already set
                    break;
            }
        } else {
            System.out.printf("WARN: No file extension found for '%s', defaulting to WAV.%n", this.audioFile.getName());
            // Defaulting to WAV as already set
        }

        System.out.printf("DEBUG: Using audio format: %s for file: %s%n", format, this.audioFile.getName());

        ChatCompletionContentPartInputAudio audio = ChatCompletionContentPartInputAudio
                .builder()
                .inputAudio(ChatCompletionContentPartInputAudio.InputAudio
                        .builder()
                        .data(s)
                        .format(format) // Use the determined format
                        .build()
                ).build();

        return ChatCompletionContentPart.ofInputAudio(audio);
    }
}
