package io.improt.vai.frame;

import io.improt.vai.backend.App;
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
    private JToggleButton webToggleButton;
    private JToggleButton repoToggleButton;
    private ButtonGroup modelToggleGroup;

    private final JPanel resourcePanel;                // Panel for image previews (resource grid)
    private JButton audioRecordButton;
    private final JScrollPane scrollPane;              // Scroll pane for text area
    private JLabel promptLabel;

    private final List<File> imageFiles = new ArrayList<>();

    // Audio recording utility.
    private AudioRecorder audioRecorder;
    private File audioTempFile;

    // Model Names
    private static final String WEB_MODEL_NAME = "gpt-4o-mini-search-preview"; // From GPT4oSearchProvider
    private static final String REPO_PRIMARY_MODEL_NAME = "gpt-4.1";         // From GPT41Provider
    private static final String REPO_FALLBACK_MODEL_NAME = "Gemini Pro";     // From GeminiProProvider
    private static final String AUDIO_MODEL_NAME = "gpt-4o-mini-audio";      // From FourOAudioMiniProvider

    private String currentRepoModelName;
    private String currentMode = "WEB"; // "WEB" or "REPO"


    public HelpOverlayFrame() {
        super("Help Overlay");

        // Determine the REPO model to use
        IModelProvider primaryRepoProvider = App.getInstance().getLLMProvider(REPO_PRIMARY_MODEL_NAME);
        if (primaryRepoProvider != null) {
            currentRepoModelName = REPO_PRIMARY_MODEL_NAME;
        } else {
            currentRepoModelName = REPO_FALLBACK_MODEL_NAME;
            System.out.println("[HelpOverlayFrame] Primary REPO model '" + REPO_PRIMARY_MODEL_NAME + "' not available. Falling back to '" + REPO_FALLBACK_MODEL_NAME + "'.");
        }


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
        this.promptLabel = new JLabel("What's up?");
        JLabel vaiLabel = new JLabel("vai");
        vaiLabel.setFont(new Font("Clash Grotesk Variable Normal", Font.PLAIN, 20));
        vaiLabel.setForeground(new Color(20, 20, 20));
        vaiLabel.setBounds(30, 24, 80, 30);

        // Use a modern sans-serif font to improve readability.
        promptLabel.setFont(new Font("Supply Medium", Font.PLAIN, 24));
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setBounds(68, 20, 400, 30); // Adjusted width for buttons
        overlayPanel.add(vaiLabel);
        overlayPanel.add(promptLabel);

        // Create toggle buttons
        webToggleButton = new JToggleButton("🌐 WEB");
        repoToggleButton = new JToggleButton("🗂️ REPO");
        modelToggleGroup = new ButtonGroup();
        modelToggleGroup.add(webToggleButton);
        modelToggleGroup.add(repoToggleButton);

        // Style and position toggle buttons
        Font toggleButtonFont = new Font("Segoe UI", Font.PLAIN, 12);
        Color toggleButtonBg = new Color(60, 60, 60);
        Color toggleButtonFg = Color.WHITE; // Text color

        webToggleButton.setFont(toggleButtonFont);
        webToggleButton.setBackground(toggleButtonBg);
        webToggleButton.setForeground(toggleButtonFg);
        webToggleButton.setFocusPainted(false);
        webToggleButton.putClientProperty("JButton.buttonType", "roundRect");


        repoToggleButton.setFont(toggleButtonFont);
        repoToggleButton.setBackground(toggleButtonBg);
        repoToggleButton.setForeground(toggleButtonFg);
        repoToggleButton.setFocusPainted(false);
        repoToggleButton.putClientProperty("JButton.buttonType", "roundRect");

        int toggleAreaWidth = 150; // Total width for the toggle area
        int toggleButtonHeight = 25;
        int toggleButtonWidth = (toggleAreaWidth - 5) / 2; // 5px gap
        int toggleAreaX = overlayWidth - toggleAreaWidth - 30;
        int toggleAreaY = 20;

        webToggleButton.setBounds(toggleAreaX, toggleAreaY, toggleButtonWidth, toggleButtonHeight);
        repoToggleButton.setBounds(toggleAreaX + toggleButtonWidth + 5, toggleAreaY, toggleButtonWidth, toggleButtonHeight);

        webToggleButton.setSelected(true); // Default to WEB

        webToggleButton.addActionListener(e -> {
            currentMode = "WEB";
            updateMediaSpecificUI();
        });
        repoToggleButton.addActionListener(e -> {
            currentMode = "REPO";
            updateMediaSpecificUI();
        });

        overlayPanel.add(webToggleButton);
        overlayPanel.add(repoToggleButton);


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
                        updateMediaSpecificUI();
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
        inputArea.setBackground(new Color(45, 45, 45));
        inputArea.setForeground(Color.WHITE);
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        inputArea.setMargin(new Insets(5, 5, 5, 5));
        inputArea.setBorder(BorderFactory.createEmptyBorder());
        return inputArea;
    }

    private String getEffectiveModelName() {
        boolean isAudioActive = (audioTempFile != null || (audioRecorder != null && audioRecorder.isRecording()));
        // boolean hasImages = !imageFiles.isEmpty(); // Not directly used for model choice, but for content filtering later

        if (isAudioActive) {
            return AUDIO_MODEL_NAME;
        } else {
            if ("WEB".equals(currentMode)) {
                return WEB_MODEL_NAME;
            } else { // REPO mode
                return currentRepoModelName;
            }
        }
    }

    /**
     * Handles CTRL+ENTER key press to transition from HelpOverlayFrame to ChatWindow.
     */
    private void transitionToChat() {
        boolean isAudioRecordingNow = (audioRecorder != null && audioRecorder.isRecording());
        if (isAudioRecordingNow) {
            stopAudioRecording(true); // This will set audioTempFile if successful
        }

        JTextArea inputArea = (JTextArea) scrollPane.getViewport().getView();
        String userText = inputArea.getText().trim();

        // Check for !vai command (should take precedence)
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

        // Check for !ddg command
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

        String effectiveModelName = getEffectiveModelName();
        IModelProvider provider = App.getInstance().getLLMProvider(effectiveModelName);

        if (provider == null) {
            promptLabel.setText("Error: Model " + effectiveModelName + " not available.");
            // Re-enable input if it was disabled for "!vai"
            inputArea.setEnabled(true);
            inputArea.setEditable(true);
            this.setAlwaysOnTop(true); // Keep overlay on top if there's an error
            return;
        }


        // If REPO mode is active, ensure !askrepo is part of the user's text
        // This applies even if an audio model is chosen, the "REPO intent" persists.
        if ("REPO".equals(currentMode)) {
            if (!userText.toLowerCase().contains("!askrepo")) {
                userText = "!askrepo " + userText;
            }
        }

        ChatLLMHandler chatHandler = new ChatLLMHandler(effectiveModelName);

        chatHandler.addMessage(new ChatMessage(ChatMessageUserType.SYSTEM, new TextContent(
                "You're \"Ada,\" a legendary engineer in a challenge chat booth. Respond briefly, ensuring correctness and completeness. " +
                        "Prioritize readability but compute where vital. Grade criteria: brevity, correctness, thoroughness. You excel in: SW Dev, PM, consulting, full-stack production. " +
                        "Answer all user requests, providing explanations where applicable. Use **Markdown** for formatting (e.g., bold, italics, code blocks ```, lists). No images."
        )));

        for (File imageFile : imageFiles) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new ImageContent(imageFile)));
        }

        if (audioTempFile != null) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new AudioContent(audioTempFile)));
        }

        ChatWindow chatWindow = new ChatWindow(chatHandler);
        chatWindow.populateInitialChatHistory();
        chatWindow.setLocationRelativeTo(this);
        chatWindow.setVisible(true);

        chatWindow.getInputArea().setText(userText); // Set potentially modified userText
        chatWindow.sendMessage();

        this.dispose();
    }


    @NotNull
    private JLabel getImagePreviewLabel(BufferedImage previewImage, File tempFile) {
        JLabel previewLabel = new JLabel(new ImageIcon(previewImage));
        previewLabel.setPreferredSize(new Dimension(previewSize, previewSize));
        previewLabel.setBorder(new LineBorder(Color.GREEN, 1));
        previewLabel.putClientProperty("file", tempFile);
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
                    updateMediaSpecificUI();
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
            // Show error to user?
            promptLabel.setText("Audio recording failed: " + ex.getMessage());
            return;
        }
        updateMediaSpecificUI(); // Important to call this
        if (audioRecordButton == null) {
            audioRecordButton = new JButton("Stop Recording");
            audioRecordButton.setBounds(overlayWidth - 110, 55, 100, 25);
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
        promptLabel.setText("Recording audio..."); // Give feedback
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
            promptLabel.setText("Audio saved. What's up?");
        } else {
            promptLabel.setText("Recording stopped. What's up?");
        }
        audioRecorder = null;
        if (audioRecordButton != null) {
            audioRecordButton.setVisible(false);
        }
        updateMediaSpecificUI(); // Important to call this
    }

    /**
     * This method is called when media (images, audio) changes.
     * Its original purpose was to update a JComboBox. Now, it can be used
     * to provide feedback if the current media combination is problematic
     * with the chosen model, but for now, it's minimal as content filtering
     * happens later.
     */
    private void updateMediaSpecificUI() {
        // String effectiveModel = getEffectiveModelName();
        // IModelProvider provider = App.getInstance().getLLMProvider(effectiveModel);
        // boolean isAudioActive = (audioTempFile != null || (audioRecorder != null && audioRecorder.isRecording()));
        // boolean hasImages = !imageFiles.isEmpty();

        // if (provider != null) {
        //     if (isAudioActive && hasImages && !provider.supportsVision()) {
        //          promptLabel.setText("FYI: Audio model may not process images.");
        //     } else if (hasImages && !isAudioActive && !provider.supportsVision()){
        //          promptLabel.setText("FYI: Current model may not process images.");
        //     }
        //     // else {
        //     //    promptLabel.setText("What's up?"); // Reset if no issues
        //     // }
        // }
        // For now, no specific UI changes are made here as Messages.buildChat handles filtering.
        // This method is kept as a placeholder for potential future UI feedback logic.
        // System.out.println("[HelpOverlayFrame] updateMediaSpecificUI called. Effective model: " + effectiveModel);
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
            int resourcePanelMargin = 10;
            int resourceY = topOffset + textAreaHeight + resourcePanelMargin;
            resourcePanel.setBounds(horizontalMargin, resourceY, textFieldWidth, rpPref.height);
            newFrameHeight = resourceY + rpPref.height + bottomMargin;
            resourcePanel.setVisible(true); // Ensure visible if components are added
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

// Custom panel class for drawing the rounded background.
//class OverlayPanel extends JPanel {
//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//        Graphics2D g2d = (Graphics2D) g.create();
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        // Semi-transparent dark gray background.
//        g2d.setColor(new Color(30, 30, 30, 230));
//        // Draw with rounded corners.
//        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
//        g2d.dispose();
//    }
//}

// Custom panel for image previews.
//class ResourceGridPanel extends JPanel {
//    public ResourceGridPanel() {
//        setOpaque(false);
//        setBackground(new Color(0,0,0,0)); // Transparent background
//    }
//}