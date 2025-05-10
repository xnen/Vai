package io.improt.vai.llm.chat.content;

import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartInputAudio;
import io.improt.vai.llm.chat.content.impl.IChatContent;
import io.improt.vai.util.EncodingUtils;

import java.io.File;
import java.io.IOException;

/**
 * AudioContent represents a chat message containing an audio file.
 * Expected to be in MP3 format as required.
 */
public class AudioContent implements IChatContent {
    private final File audioFile;

    public AudioContent(File audioFile) {
        this.audioFile = audioFile;
    }

    public File getAudioFile() {
        return audioFile;
    }

    public String toString() {
        return "[Audio File: " + audioFile.getAbsolutePath() + "]";
    }

    @Override
    public ChatCompletionContentPart getPart() throws IOException {
        String s = EncodingUtils.encodeMp3ToBase64(this.audioFile.getAbsolutePath());

        ChatCompletionContentPartInputAudio audio = ChatCompletionContentPartInputAudio
                .builder()
                .inputAudio(ChatCompletionContentPartInputAudio.InputAudio
                        .builder()
                        .data(s)
                        .format(ChatCompletionContentPartInputAudio.InputAudio.Format.MP3)
                        .build()
                ).build();

        return ChatCompletionContentPart.ofInputAudio(audio);
    }
}
