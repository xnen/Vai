package io.improt.vai.frame;

import com.formdev.flatlaf.FlatLightLaf;
import io.improt.vai.backend.App;
import org.jetbrains.annotations.NotNull;
import io.improt.vai.llm.chat.ChatLLMHandler;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.chat.ChatWindow;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.llm.chat.content.AudioContent;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HelpOverlayFrame is a separate window dedicated to help content.
 * Its text field supports pasting images (and text) via clipboard.
 * Audio recording functionality has been added – recording is done in PCM, then saved as a .wav file, 
 * and later converted to MP3 using an external command.
 * For Linux, the required command is: "ffmpeg -y -i <input.wav> <output.mp3>"
 * For Windows, assuming ffmpeg is installed, the same command applies: "ffmpeg -y -i <input.wav> <output.mp3>"
 */
public class HelpOverlayFrame extends JFrame {

    // Fixed dimensions for width and layout margins.
    private final int overlayWidth = 780;
    private final int horizontalMargin = 30;     // left/right margin
    private final int topOffset = 56;              // space below prompt and model selection dropdown
    private final int bottomMargin = 20;           // additional space below the content area
    private final int maxTextAreaHeight = 325;     // maximum height for the multi-line text field;
    
    // Constants for the resource grid panel and image previews.
    private final int resourcePanelMargin = 10;    // gap between text area and resource grid
    private final int previewSize = 100;           // each preview is 100x100
    private final int previewGap = 10;             // gap between previews

    // UI Components.
    private JComboBox<ModelOption> modelCombo;
    private JPanel resourcePanel;                // Panel for image previews (resource grid)
    private JButton audioRecordButton;
    private JScrollPane scrollPane;              // Scroll pane for text area

    private final List<File> imageFiles = new ArrayList<>();

    // Audio recording related fields
    private boolean audioRecordingStarted = false; // UI flag for audio mode
    private boolean isAudioRecording = false;      // true when actual recording is active
    private TargetDataLine audioLine;
    private ByteArrayOutputStream audioOutputStream;
    private Thread audioRecordingThread;
    private File audioTempFile; // temporary MP3 file

    public HelpOverlayFrame() {
        super("Help Overlay");

        // Remove window decorations and set the background to be completely transparent.
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        // Center horizontally on screen and position 200px from the top.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - overlayWidth) / 2;
        // Initial components height determination:
        int initialTextAreaHeight = 40;
        int initialFrameHeight = topOffset + initialTextAreaHeight + bottomMargin;
        int y = 200;
        setBounds(x, y, overlayWidth, initialFrameHeight);

        // Optionally set the window shape to a rounded rectangle if the platform supports it.
        try {
            setShape(new RoundRectangle2D.Double(0, 0, overlayWidth, initialFrameHeight, 30, 30));
        } catch (UnsupportedOperationException ex) {
            // Window shaping not supported.
        }

        // Create the overlay panel that draws our simplified semi-transparent rounded rectangle.
        OverlayPanel overlayPanel = new OverlayPanel();
        overlayPanel.setLayout(null);
        overlayPanel.setOpaque(false);
        overlayPanel.setDoubleBuffered(true);
        setContentPane(overlayPanel);

        // The main prompt label.
        JLabel promptLabel = new JLabel("What's up?");
        JLabel vaiLabel = new JLabel("vai");
        vaiLabel.setFont(new Font("Clash Grotesk Variable Normal", Font.PLAIN, 20));
        vaiLabel.setForeground(new Color(20, 20, 20));
        vaiLabel.setBounds(30, 24, 80, 30);

        // Use a modern sans-serif font to improve readability.
        promptLabel.setFont(new Font("Supply Medium", Font.PLAIN, 24));
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setBounds(68, 20, 500, 30);
        overlayPanel.add(vaiLabel);
        overlayPanel.add(promptLabel);

        // New: Model selection dropdown at top-right.
        ModelOption[] modelOptions = new ModelOption[] {
                new ModelOption("o3-mini", false, false),
                new ModelOption("chatgpt-4o-latest", false, true),
                new ModelOption("gpt-4o-audio", true, false),
                new ModelOption("gpt-4o-audio-mini", true, false),
                new ModelOption("o1", false, true),
        };
        modelCombo = new JComboBox<>(modelOptions);
        modelCombo.setRenderer(new ModelOptionRenderer());
        // Set a modern dark style for the combo box.
        modelCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        modelCombo.setBackground(new Color(60, 60, 60));
        modelCombo.setForeground(Color.black);
        modelCombo.putClientProperty("JComboBox.buttonType", "roundRect");
        int dropdownWidth = 150;
        int dropdownHeight = 25;
        int dropdownX = overlayWidth - dropdownWidth - 30;
        int dropdownY = 20;
        modelCombo.setBounds(dropdownX, dropdownY, dropdownWidth, dropdownHeight);
        overlayPanel.add(modelCombo);

        // Multi-line text area + scroll pane.
        JTextArea inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setOpaque(true);
        // Use a slightly lighter dark gray for better contrast.
        inputArea.setBackground(new Color(45, 45, 45));
        inputArea.setForeground(Color.WHITE);
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        // Add a small margin for padding.
        inputArea.setMargin(new Insets(5, 5, 5, 5));
        inputArea.setBorder(BorderFactory.createEmptyBorder());
        inputArea.getCaret().setBlinkRate(500);

        scrollPane = new JScrollPane(inputArea);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(new Color(45, 45, 45));
        // Use a subtle matte border for a refined look.
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(85, 85, 85)));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        int textFieldWidth = overlayWidth - 2 * horizontalMargin;
        scrollPane.setBounds(horizontalMargin, topOffset, textFieldWidth, initialTextAreaHeight);
        overlayPanel.add(scrollPane);

        // Add key binding for CTRL+ENTER to trigger transition to chat.
        inputArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "ctrlEnterAction");
        inputArea.getActionMap().put("ctrlEnterAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                transitionToChat();
            }
        });

        // Create resource panel for image previews (initially invisible).
        resourcePanel = new ResourceGridPanel();
        resourcePanel.setLayout(new FlowLayout(FlowLayout.LEFT, previewGap, previewGap));
        resourcePanel.setVisible(false);
        overlayPanel.add(resourcePanel);

        // Use an array to store previous text area height to avoid unnecessary resizing updates.
        final int[] prevTextAreaHeight = new int[]{initialTextAreaHeight};

        // Dynamically adjust text area (and window) size.
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            private void updateSize() {
                SwingUtilities.invokeLater(() -> {
                    inputArea.setSize(textFieldWidth, Short.MAX_VALUE);
                    Dimension preferred = inputArea.getPreferredSize();
                    int newTextAreaHeight = Math.min(preferred.height + 22, maxTextAreaHeight);
                    if(newTextAreaHeight == prevTextAreaHeight[0]) {
                        return;
                    }
                    prevTextAreaHeight[0] = newTextAreaHeight;
                    scrollPane.setBounds(horizontalMargin, topOffset, textFieldWidth, newTextAreaHeight);
                    updateFrameLayout();
                });
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSize();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSize();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSize();
            }
        });

        // Add key binding for paste action (supports images and text).
        inputArea.getInputMap().put(KeyStroke.getKeyStroke("ctrl V"), "pasteAction");
        inputArea.getActionMap().put("pasteAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable contents = clipboard.getContents(null);
                try {
                    if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                        int width = image.getWidth(null);
                        int height = image.getHeight(null);
                        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2d = bufferedImage.createGraphics();
                        g2d.drawImage(image, 0, 0, null);
                        g2d.dispose();
                        // Save image as temporary file.
                        File tempFile = File.createTempFile("helpOverlayPastedImage", ".png");
                        ImageIO.write(bufferedImage, "png", tempFile);
                        // Create image preview and add to resource panel.
                        BufferedImage previewImage = cropAndResizeImage(bufferedImage);
                        JLabel previewLabel = getImagePreviewLabel(previewImage, tempFile);
                        resourcePanel.add(previewLabel);
                        imageFiles.add(tempFile);
                        resourcePanel.setVisible(true);
                        resourcePanel.revalidate();
                        resourcePanel.repaint();
                        updateFrameLayout();
                        updateModelComboState();
                    } else if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String pasteText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                        int pos = inputArea.getCaretPosition();
                        inputArea.insert(pasteText, pos);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Add a mouse listener for a context menu with a Paste option.
        inputArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem pasteItem = new JMenuItem("Paste");
                    pasteItem.addActionListener(ae -> {
                        Action pasteAction = inputArea.getActionMap().get("pasteAction");
                        pasteAction.actionPerformed(null);
                    });
                    popup.add(pasteItem);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // Always on top.
        setAlwaysOnTop(true);

        // Ensure the text area gets default focus when window opens.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                inputArea.requestFocusInWindow();
            }
        });

        // Add ESC key binding to close the dialog.
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Stop audio recording without saving if active.
                if (isAudioRecording) {
                    stopAudioRecording(false);
                }
                dispose();
            }
        });
    }

    /**
     * Handles CTRL+ENTER key press to transition from HelpOverlayFrame to ChatWindow.
     * It transfers the user input text and all pasted image files into the chat history,
     * and if audio recording is active, stops recording and appends the audio file and a note.
     * It creates a ChatLLMHandler instance using the selected model, opens the ChatWindow,
     * and disposes of the HelpOverlayFrame.
     */
    private void transitionToChat() {
        // If audio recording is in progress, stop and save the recording as MP3.
        if (isAudioRecording) {
            stopAudioRecording(true);
        }
        JTextArea inputArea = (JTextArea) scrollPane.getViewport().getView();
        String userText = inputArea.getText().trim();

        // Get selected model from modelCombo.
        ModelOption selectedModel = (ModelOption) modelCombo.getSelectedItem();
        String modelLabel = selectedModel != null ? selectedModel.label : "o3-mini";

        // Create new ChatLLMHandler and add the initial messages.
        ChatLLMHandler chatHandler = new ChatLLMHandler(modelLabel);

        chatHandler.addMessage(new ChatMessage(ChatMessageUserType.SYSTEM, new TextContent("You're \"Ada,\" a legendary engineer in a challenge chat booth. Respond briefly, ensuring correctness and completeness. Prioritize readability but compute where vital. Grade criteria: brevity, correctness, thoroughness. You excel in: SW Dev, PM, consulting, full-stack production. Answer all user requests, providing explanations where applicable. Use HTML formatting, not Markdown in your responses. Do not write full HTML files as your response, only <b> <i> etc tags are allowed. No CSS or JavaScript. Provide each code block in split unique messages. You can split up your message by using the <end_message> delimiter. No images.")));

        for (File imageFile : imageFiles) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new ImageContent(imageFile)));
        }

        // If an audio recording exists, add it as AudioContent along with a textual note.
        if (audioTempFile != null) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new AudioContent(audioTempFile)));
        }

        if (!userText.isEmpty()) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new TextContent(userText)));
        }

        // Create ChatWindow, populate with initial chat history, and show with fade-in.
        ChatWindow chatWindow = new ChatWindow(chatHandler);
        chatWindow.populateInitialChatHistory();
        chatWindow.setLocationRelativeTo(null);
        chatWindow.setVisible(true);

        // Close the HelpOverlayFrame.
        this.dispose();
    }

    @NotNull
    private JLabel getImagePreviewLabel(BufferedImage previewImage, File tempFile) {
        JLabel previewLabel = new JLabel(new ImageIcon(previewImage));
        previewLabel.setPreferredSize(new Dimension(previewSize, previewSize));
        // Use a refined rounded border style.
        previewLabel.setBorder(new LineBorder(Color.GREEN, 1));
        previewLabel.putClientProperty("file", tempFile);
        // Add tooltip to instruct removal.
        previewLabel.setToolTipText("Double-click to remove this image preview.");
        previewLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    resourcePanel.remove(previewLabel);
                    imageFiles.remove(tempFile);
                    resourcePanel.revalidate();
                    resourcePanel.repaint();
                    updateFrameLayout();
                    updateModelComboState();
                }
            }
        });
        return previewLabel;
    }

    /**
     * Starts audio recording mode in the HelpOverlayFrame.
     * It updates the UI and begins capturing audio, saving it as a temporary WAV file first,
     * then converting it to MP3 using an external command.
     */
    public void startAudioRecording() {
        audioRecordingStarted = true;
        updateModelComboState();
        if (audioRecordButton == null) {
            audioRecordButton = new JButton("Stop Recording");
            // Position the audio record button at the top-right corner.
            audioRecordButton.setBounds(overlayWidth - 110, 55, 100, 25);
            // Style the button with a modern rounded look.
            audioRecordButton.putClientProperty("JButton.buttonType", "roundRect");
            audioRecordButton.setBackground(new Color(220, 53, 69));
            audioRecordButton.setForeground(Color.WHITE);
            audioRecordButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            audioRecordButton.setFocusPainted(false);
            audioRecordButton.setToolTipText("Click to stop recording");
            audioRecordButton.addActionListener(e -> stopAudioRecording(true));
            getContentPane().add(audioRecordButton);
            getContentPane().revalidate();
            getContentPane().repaint();
        }
        audioRecordButton.setVisible(true);

        // Begin actual audio recording.
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioOutputStream = new ByteArrayOutputStream();
            audioLine.start();
            isAudioRecording = true;
            audioRecordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isAudioRecording) {
                    int bytesRead = audioLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            });
            audioRecordingThread.start();
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Stops audio recording. If saveFile is true, the captured audio is first saved as a WAV temporary file,
     * and then converted to an MP3 using an external command.
     *
     * For Linux: the conversion command used is: "ffmpeg -y -i <input.wav> <output.mp3>"
     * For Windows: the same command is used, assuming ffmpeg is installed.
     *
     * @param saveFile whether to save the recorded audio to an MP3 file.
     */
    public void stopAudioRecording(boolean saveFile) {
        if (!isAudioRecording) {
            return;
        }
        isAudioRecording = false;
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
        if (audioRecordingThread != null) {
            try {
                audioRecordingThread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ex.printStackTrace();
            }
            audioRecordingThread = null;
        }
        if (saveFile) {
            byte[] audioData = audioOutputStream.toByteArray();
            AudioFormat format = getAudioFormat();
            try {
                // Save the recorded audio as a WAV file first.
                File tempWavFile = File.createTempFile("helpOverlayAudio", ".wav");
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                long frameLength = audioData.length / format.getFrameSize();
                try (AudioInputStream ais = new AudioInputStream(bais, format, frameLength)) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempWavFile);
                }
                // Create the final MP3 file.
                audioTempFile = File.createTempFile("helpOverlayAudio", ".mp3");
                // Convert the WAV file to MP3.
                convertWavToMp3(tempWavFile, audioTempFile);
                // Delete the temporary WAV file.
                tempWavFile.delete();
                System.out.println("Audio recording saved to " + audioTempFile.getAbsolutePath());
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
        audioRecordingStarted = false;
        if (audioRecordButton != null) {
            audioRecordButton.setVisible(false);
        }
        updateModelComboState();
    }

    /**
     * Stops audio recording without saving the file.
     */
    public void stopAudioRecording() {
        stopAudioRecording(false);
    }

    /**
     * Converts a WAV file to an MP3 file using an external conversion command.
     *
     * For Linux systems, the command used is: "ffmpeg -y -i <input.wav> <output.mp3>"
     * For Windows systems (assuming ffmpeg is installed), the same command is used.
     *
     * @param inputWav  the source WAV file.
     * @param outputMp3 the destination MP3 file.
     * @throws IOException if an I/O error occurs during conversion.
     */
    private void convertWavToMp3(File inputWav, File outputMp3) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        // Using ffmpeg for conversion.
        // Command for Linux and Windows (assuming ffmpeg is in the system PATH).
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

    /**
     * Dynamically updates the model selection dropdown items based on pasted content and recording status.
     * If audio is recording, only models that support audio will be enabled.
     * If an image is present, only models that support images will be enabled.
     * Otherwise, all models are enabled. In addition, if the current selection becomes invalid,
     * the first available valid model is automatically selected.
     */
    private void updateModelComboState() {
        boolean hasImage = resourcePanel.getComponentCount() > 0;
        if (audioRecordingStarted) {
            // Only models that support audio should be enabled.
            for (int i = 0; i < modelCombo.getItemCount(); i++) {
                ModelOption option = modelCombo.getItemAt(i);
                option.enabled = option.supportsAudio;
            }
        } else if (hasImage) {
            // Only models that support images should be enabled.
            for (int i = 0; i < modelCombo.getItemCount(); i++) {
                ModelOption option = modelCombo.getItemAt(i);
                option.enabled = option.supportsImage;
            }
        } else {
            // No media present: all models enabled.
            for (int i = 0; i < modelCombo.getItemCount(); i++) {
                ModelOption option = modelCombo.getItemAt(i);
                option.enabled = true;
            }
        }
        
        // Ensure the currently selected model is valid.
        ModelOption currentSelection = (ModelOption) modelCombo.getSelectedItem();
        if (currentSelection == null || !currentSelection.enabled) {
            for (int i = 0; i < modelCombo.getItemCount(); i++) {
                ModelOption option = modelCombo.getItemAt(i);
                if (option.enabled) {
                    modelCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        modelCombo.repaint();
    }

    /**
     * Helper method to crop an image to a square (centered crop) and resize to the given target size.
     */
    private BufferedImage cropAndResizeImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int minDim = Math.min(width, height);
        int x = (width - minDim) / 2;
        int y = (height - minDim) / 2;
        BufferedImage cropped = source.getSubimage(x, y, minDim, minDim);
        BufferedImage resized = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(cropped, 0, 0, 100, 100, null);
        g2.dispose();
        return resized;
    }

    /**
     * Recalculates and updates the layout of the frame, adjusting positions and sizes of components.
     */
    private void updateFrameLayout() {
        int textFieldWidth = overlayWidth - 2 * horizontalMargin;
        int textAreaHeight = scrollPane.getHeight();
        int newFrameHeight = topOffset + textAreaHeight + bottomMargin;
        
        if (resourcePanel.getComponentCount() > 0) {
            Dimension rpPref = getResourcePanelPreferredSize();
            int resourceY = topOffset + textAreaHeight + resourcePanelMargin;
            resourcePanel.setBounds(horizontalMargin, resourceY, textFieldWidth, rpPref.height);
            newFrameHeight = resourceY + rpPref.height + bottomMargin;
        } else {
            resourcePanel.setVisible(false);
        }
        
        int x = (Toolkit.getDefaultToolkit().getScreenSize().width - overlayWidth) / 2;
        setBounds(x, 200, overlayWidth, newFrameHeight);
        try {
            setShape(new RoundRectangle2D.Double(0, 0, overlayWidth, newFrameHeight, 30, 30));
        } catch (UnsupportedOperationException ex) {
            // Not supported.
        }
        revalidate();
        repaint();
    }
    
    /**
     * Computes the preferred size of the resource panel based on the number of previews.
     */
    private Dimension getResourcePanelPreferredSize() {
        int textFieldWidth = overlayWidth - 2 * horizontalMargin;
        int count = resourcePanel.getComponentCount();
        if(count == 0) {
            return new Dimension(textFieldWidth, 0);
        }
        int nPerRow = Math.max(1, (textFieldWidth + previewGap) / (previewSize + previewGap));
        int rows = (int) Math.ceil((double) count / nPerRow);
        int height = rows * previewSize + (rows + 1) * previewGap;
        return new Dimension(textFieldWidth, height);
    }

    /**
     * Helper method to get the desired audio format for recording.
     *
     * @return The AudioFormat to be used.
     */
    private AudioFormat getAudioFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    /**
     * ModelOption represents an item in the model selection dropdown.
     * It now includes capability flags to indicate support for audio and image media.
     */
    private static class ModelOption {
        String label;
        boolean enabled;
        final boolean supportsAudio;
        final boolean supportsImage;

        ModelOption(String label, boolean supportsAudio, boolean supportsImage) {
            this.label = label;
            this.supportsAudio = supportsAudio;
            this.supportsImage = supportsImage;
            this.enabled = true;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Custom renderer to display disabled items in a grayed-out style.
     */
    private static class ModelOptionRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ModelOption) {
                ModelOption option = (ModelOption) value;
                setText(option.label);
                if (!option.enabled) {
                    setForeground(new Color(150, 150, 150));
                } else {
                    setForeground(Color.black);
                }
            }
            return c;
        }
    }

    /**
     * OverlayPanel paints a semi-transparent rounded rectangle as its background with a subtle drop shadow.
     */
    private static class OverlayPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            // Draw background with drop shadow.
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int shadowGap = 4;
            int arc = 30;
            // Draw drop shadow.
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(shadowGap, shadowGap, getWidth() - shadowGap, getHeight() - shadowGap, arc, arc);
            // Draw main background.
            g2.setColor(new Color(40, 40, 40, 220));
            g2.fillRoundRect(0, 0, getWidth() - shadowGap, getHeight() - shadowGap, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    /**
     * ResourceGridPanel paints a semi-transparent rounded rectangle as its background with a refined drop shadow.
     */
    private static class ResourceGridPanel extends JPanel {
        public ResourceGridPanel() {
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int shadowGap = 2;
            int arc = 20;
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillRoundRect(shadowGap, shadowGap, getWidth() - shadowGap, getHeight() - shadowGap, arc, arc);
            g2.setColor(new Color(50, 50, 50, 220));
            g2.fillRoundRect(0, 0, getWidth() - shadowGap, getHeight() - shadowGap, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        UIManager.setLookAndFeel(new FlatLightLaf());
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Menu.font", new Font("Segoe UI", Font.PLAIN, 13));
        EventQueue.invokeLater(() -> {
            HelpOverlayFrame frame = new HelpOverlayFrame();
            frame.setVisible(true);
        });
    }
}
