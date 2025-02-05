package io.improt.vai.util;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;

public class AudioUtils {
    public static AudioFormat getAudioFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public static void convertWavToMp3(File inputWav, File outputMp3) throws IOException {
        // Using ffmpeg for conversion.
        String command = "ffmpeg";
        ProcessBuilder pb = new ProcessBuilder(command, "-y", "-i", inputWav.getAbsolutePath(), outputMp3.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Error converting WAV to MP3. Process exited with code " + exitCode);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("MP3 conversion interrupted", ex);
        }
    }
}
