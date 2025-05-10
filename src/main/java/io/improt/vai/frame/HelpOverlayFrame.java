package io.improt.vai.frame;

import io.improt.vai.backend.App;
import io.improt.vai.llm.LLMRegistry;
import io.improt.vai.llm.chat.ChatLLMHandler;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.chat.ChatWindow;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.llm.chat.content.AudioContent;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.util.AudioRecorder;
import io.improt.vai.util.ImageUtils;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * HelpOverlayFrame is a separate window dedicated to help content.
 * Its text field supports pasting images (and text) via clipboard.
 * Audio recording functionality has been added – recording is done in PCM, then saved as a .wav file, 
 * and later converted to MP3 using an external command.
 * For Linux, the required command is: "ffmpeg -y -i <input.wav> <output.mp3>"
 * For Windows, assuming ffmpeg is installed, the same command applies.
 */
public class HelpOverlayFrame extends JFrame {

    // Fixed dimensions for width and layout margins.
    private final int overlayWidth = 780;
    private final int horizontalMargin = 30;     // left/right margin
    private final int topOffset = 56;              // space below prompt and model selection dropdown
    private final int bottomMargin = 20;           // additional space below the content area
    private final int maxTextAreaHeight = 325;     // maximum height for the multi-line text field;

    private final int previewSize = 100;           // each preview is 100x100
    private final int previewGap = 10;             // gap between previews

    // UI Components.
    private final JComboBox<ModelOption> modelCombo;
    private final JPanel resourcePanel;                // Panel for image previews (resource grid)
    private JButton audioRecordButton;
    private final JScrollPane scrollPane;              // Scroll pane for text area
    // New UI component: Keyboard mode checkbox.
    private JLabel promptLabel;

    private final List<File> imageFiles = new ArrayList<>();

    // Audio recording utility.
    private AudioRecorder audioRecorder;
    private File audioTempFile;

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
        this.promptLabel = promptLabel;
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

        // Auto-populate model selection dropdown from the LLM registry.
        LLMRegistry llmRegistry = App.getInstance().getLLMRegistry();
        List<String> registeredModels = llmRegistry.getRegisteredModelNames();
        List<ModelOption> modelOptionsList = new ArrayList<>();
        ModelOption o3mini = null;
        for (String modelName : registeredModels) {
            IModelProvider provider = llmRegistry.getModel(modelName);
            ModelOption o = new ModelOption(modelName, provider.supportsAudio(), provider.supportsVision());
            modelOptionsList.add(o);
            String currentModel = "gpt-4o-mini-search-preview"; // chatgpt-4o-latest
            if (Objects.equals(modelName, currentModel)) {
                o3mini = o;
            }
        }
        ModelOption[] modelOptions = modelOptionsList.toArray(new ModelOption[0]);
        modelCombo = new JComboBox<>(modelOptions);
        modelCombo.setRenderer(new ModelOptionRenderer());
        modelCombo.setSelectedItem(o3mini);

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
        JTextArea inputArea = getTextArea();
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
        inputArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "ctrlEnterAction");
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
                        BufferedImage previewImage = ImageUtils.cropAndResizeImage(bufferedImage, previewSize);
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
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Stop audio recording without saving if active.
                if (audioRecorder != null && audioRecorder.isRecording()) {
                    stopAudioRecording(false);
                }

                if (HelpOverlayFrame.this.llmHandler != null) {
                    HelpOverlayFrame.this.llmHandler.killStream();
                }
                if (HelpOverlayFrame.this.keyboardThread != null) {
                    HelpOverlayFrame.this.keyboardThread.interrupt();
                }
                dispose();
            }
        });
    }

    @NotNull
    private static JTextArea getTextArea() {
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
        return inputArea;
    }

    /**
     * Handles CTRL+ENTER key press to transition from HelpOverlayFrame to ChatWindow.
     */
    private void transitionToChat() {
        // If audio recording is in progress, stop and save the recording as MP3.
        if (audioRecorder != null && audioRecorder.isRecording()) {
            stopAudioRecording(true);
        }
        JTextArea inputArea = (JTextArea) scrollPane.getViewport().getView();
        String userText = inputArea.getText().trim();

        if (userText.contains("!vai")) {
            userText = userText.replace("!vai", "");
            ClientFrame client = App.getInstance().getClient();
            client.setLLMPrompt(userText);
            client.submit(HelpOverlayFrame.this::dispose);

            this.promptLabel.setText("Thinking...");
            inputArea.setEnabled(false);
            inputArea.setEditable(false);
            this.setAlwaysOnTop(false);
            return;
        }

        if (userText.contains("!ddg")) {
            userText = userText.replace("!ddg", "").trim();
            try {
                String query = URLEncoder.encode(userText, StandardCharsets.UTF_8);
                URI uri = new URI("https://duckduckgo.com/?q=" + query);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(uri);
                } else {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", uri.toString()});
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            this.dispose();
            return;
        }

        // Get selected model from modelCombo.
        ModelOption selectedModel = (ModelOption) modelCombo.getSelectedItem();
        String modelLabel = selectedModel != null ? selectedModel.label : "o3-mini";

        // Create new ChatLLMHandler and add the initial messages.
        ChatLLMHandler chatHandler = new ChatLLMHandler(modelLabel);
        this.llmHandler = chatHandler;

        boolean kbMode = userText.toLowerCase().startsWith("kb:");

        if (kbMode) {
            userText = userText.substring(3);
        }

        if (kbMode) {
            System.out.println("KEYBOARD MODE ACTIVE");
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.SYSTEM, new TextContent(
                    "You are currently in control over the user's keyboard, to type anything on their device.\n" +
                    "Any messages or characters that you type **will** be mirrored on the user's computer.\n" +
                    "Keep all commentary to an absolute minimum. If you're writing code, assume you are writing into an IDE (write comments, etc). No ``` code blocks in IDEs!\n" +
                    "If the user is requesting commentary, assume you're typing into a Markdown editor.\n" +
                    "Assume you're in a predefined class, don't write new classes when asked to write a method."
            )));
        } else {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.SYSTEM, new TextContent(
                    "You're \"Ada,\" a legendary engineer in a challenge chat booth. Respond briefly, ensuring correctness and completeness. " +
                            "Prioritize readability but compute where vital. Grade criteria: brevity, correctness, thoroughness. You excel in: SW Dev, PM, consulting, full-stack production. " +
                            "Answer all user requests, providing explanations where applicable. Use HTML formatting, not Markdown in your responses. " +
                            "Do not write full HTML files as your response, only <b> <i> etc tags are allowed. No CSS or JavaScript. " +
                            "Provide each code block in split unique messages. You can split up your message by using the <end_message> delimiter. No images."
            )));
        }

        for (File imageFile : imageFiles) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new ImageContent(imageFile)));
        }

        // If an audio recording exists, add it as AudioContent.
        if (audioTempFile != null) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new AudioContent(audioTempFile)));
        }

        if (!userText.isEmpty()) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new TextContent(userText)));
        }

        if (!kbMode) {
            // Create ChatWindow, populate with initial chat history, and show with fade-in.
            ChatWindow chatWindow = new ChatWindow(chatHandler);
            chatWindow.populateInitialChatHistory();
            chatWindow.setLocationRelativeTo(null);
            chatWindow.setVisible(true);
            this.dispose();
        } else {
            this.promptLabel.setText("Thinking...");
            inputArea.setEnabled(false);
            inputArea.setEditable(false);
            this.setAlwaysOnTop(false);

            this.keyboardThread = new Thread(() -> {
                chatHandler.doKeyboardStreaming();
                // Dispose only after it finishes.
                HelpOverlayFrame.this.dispose();
            });

            this.keyboardThread.start();
        }
    }

    private ChatLLMHandler llmHandler;
    private Thread keyboardThread;

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
     */
    public void startAudioRecording() {
        try {
            audioRecorder = new AudioRecorder();
            audioRecorder.startRecording();
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return;
        }
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
    }

    /**
     * Stops audio recording. If saveFile is true, the captured audio is saved and converted to MP3.
     *
     * @param saveFile whether to save the recorded audio.
     */
    public void stopAudioRecording(boolean saveFile) {
        if (audioRecorder == null || !audioRecorder.isRecording()) {
            return;
        }
        File recorded = audioRecorder.stopRecording(saveFile);
        if (saveFile) {
            audioTempFile = recorded;
            System.out.println("Audio recording saved to " + (audioTempFile != null ? audioTempFile.getAbsolutePath() : "null"));
        }
        audioRecorder = null;
        if (audioRecordButton != null) {
            audioRecordButton.setVisible(false);
        }
        updateModelComboState();
    }

    /**
     * Dynamically updates the model selection dropdown items based on pasted content and recording status.
     */
    private void updateModelComboState() {
        boolean hasImage = resourcePanel.getComponentCount() > 0;
        if (audioRecorder != null && audioRecorder.isRecording()) {
            for (int i = 0; i < modelCombo.getItemCount(); i++) {
                ModelOption option = modelCombo.getItemAt(i);
                option.enabled = option.supportsAudio;
            }
        } else if (hasImage) {
            for (int i = 0; i < modelCombo.getItemCount(); i++) {
                ModelOption option = modelCombo.getItemAt(i);
                option.enabled = option.supportsImage;
            }
        } else {
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
     * Recalculates and updates the layout of the frame, adjusting positions and sizes of components.
     */
    private void updateFrameLayout() {
        int textFieldWidth = overlayWidth - 2 * horizontalMargin;
        int textAreaHeight = scrollPane.getHeight();
        int newFrameHeight = topOffset + textAreaHeight + bottomMargin;
        
        if (resourcePanel.getComponentCount() > 0) {
            Dimension rpPref = getResourcePanelPreferredSize();
            // Constants for the resource grid panel and image previews.
            // gap between text area and resource grid
            int resourcePanelMargin = 10;
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
}

