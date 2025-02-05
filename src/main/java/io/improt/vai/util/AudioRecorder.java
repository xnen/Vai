package io.improt.vai.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;

public class AudioRecorder {
    private TargetDataLine audioLine;
    private ByteArrayOutputStream audioOutputStream;
    private Thread recordingThread;
    private boolean isRecording = false;
    private File audioFile; // Final MP3 file

    public void startRecording() throws LineUnavailableException {
        AudioFormat format = AudioUtils.getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        audioLine = (TargetDataLine) AudioSystem.getLine(info);
        audioLine.open(format);
        audioOutputStream = new ByteArrayOutputStream();
        audioLine.start();
        isRecording = true;
        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (isRecording) {
                int bytesRead = audioLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    audioOutputStream.write(buffer, 0, bytesRead);
                }
            }
        });
        recordingThread.start();
    }

    public File stopRecording(boolean saveFile) {
        if (!isRecording) {
            return null;
        }
        isRecording = false;
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        if (saveFile) {
            byte[] audioData = audioOutputStream.toByteArray();
            AudioFormat format = AudioUtils.getAudioFormat();
            try {
                // Save the recording as a WAV file first.
                File tempWavFile = File.createTempFile("helpOverlayAudio", ".wav");
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                long frameLength = audioData.length / format.getFrameSize();
                try (AudioInputStream ais = new AudioInputStream(bais, format, frameLength)) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempWavFile);
                }
                // Create the final MP3 file.
                audioFile = File.createTempFile("helpOverlayAudio", ".mp3");
                AudioUtils.convertWavToMp3(tempWavFile, audioFile);
                // Delete the temporary WAV file.
                tempWavFile.delete();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            audioOutputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        audioOutputStream = null;
        return audioFile;
    }

    public boolean isRecording() {
        return isRecording;
    }
}
