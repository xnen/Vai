package io.improt.vai.frame;

import com.formdev.flatlaf.FlatLightLaf;
import org.jetbrains.annotations.NotNull;
import io.improt.vai.llm.chat.ChatLLMHandler;
import io.improt.vai.llm.chat.ChatMessage;
import io.improt.vai.llm.chat.ChatWindow;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.chat.content.ImageContent;

import javax.imageio.ImageIO;
import javax.swing.*;
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * HelpOverlayFrame is a separate window dedicated to help content.
 * Its text field supports pasting images (and text) via clipboard.
 */
public class HelpOverlayFrame extends JFrame {

    // Fixed dimensions for width and layout margins.
    private final int overlayWidth = 780;
    private final int horizontalMargin = 30;     // left/right margin
    private final int topOffset = 90;              // space below prompt and model selection dropdown
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

    private boolean audioRecordingStarted = false;

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
        JLabel promptLabel = new JLabel("What do you need help with?");
        promptLabel.setFont(new Font("Roboto Slab", Font.PLAIN, 20));
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setBounds(30, 20, 500, 30);
        overlayPanel.add(promptLabel);

        // New: Model selection dropdown at top-right.
        ModelOption[] modelOptions = new ModelOption[] {
                new ModelOption("o3-mini", false, false),
                new ModelOption("gpt-4o", false, true),
                new ModelOption("gpt-4o-mini", false, true),
                new ModelOption("gpt-4o-audio", true, false),
                new ModelOption("gpt-4o-audio-mini", true, false),
                new ModelOption("o1", false, true),
        };
        modelCombo = new JComboBox<>(modelOptions);
        modelCombo.setRenderer(new ModelOptionRenderer());
        int dropdownWidth = 150;
        int dropdownHeight = 25;
        int dropdownX = overlayWidth - dropdownWidth - 30;
        int dropdownY = 20;
        modelCombo.setBounds(dropdownX, dropdownY, dropdownWidth, dropdownHeight);
        modelCombo.setBackground(Color.BLACK);
        modelCombo.setForeground(Color.WHITE);
        overlayPanel.add(modelCombo);

        // Multi-line text area + scroll pane.
        JTextArea inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setOpaque(true);
        inputArea.setBackground(Color.BLACK);
        inputArea.setForeground(Color.WHITE);
        inputArea.setFont(new Font("Roboto Slab", Font.PLAIN, 20));
        inputArea.setBorder(BorderFactory.createEmptyBorder());
        inputArea.getCaret().setBlinkRate(500);
        scrollPane = new JScrollPane(inputArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(Color.BLACK);
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.WHITE));
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
                    int newTextAreaHeight = Math.min(preferred.height, maxTextAreaHeight);
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
    }

    /**
     * Handles CTRL+ENTER key press to transition from HelpOverlayFrame to ChatWindow.
     * It transfers the user input text and all pasted image files into the chat history,
     * creates a ChatLLMHandler instance using the selected model, and then opens the ChatWindow
     * with a fade-in effect. Finally, it disposes of the HelpOverlayFrame.
     */
    private void transitionToChat() {
        JTextArea inputArea = (JTextArea) scrollPane.getViewport().getView();
        String userText = inputArea.getText().trim();
        
        // Get selected model from modelCombo
        ModelOption selectedModel = (ModelOption) modelCombo.getSelectedItem();
        String modelLabel = selectedModel != null ? selectedModel.label : "o3-mini";
        
        // Create new ChatLLMHandler and add the initial messages.
        ChatLLMHandler chatHandler = new ChatLLMHandler(modelLabel);
        
        if (!userText.isEmpty()) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new TextContent(userText)));
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.ASSISTANT, new TextContent("EZ")));
        }
        
        for (File imageFile : imageFiles) {
            chatHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new ImageContent(imageFile)));
        }
        
        // Create ChatWindow, populate with initial chat bubbles, and show with fade-in.
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
        previewLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
        previewLabel.putClientProperty("file", tempFile);
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
     * Initiates audio recording mode in the HelpOverlayFrame.
     * This method sets a flag and displays a cancel button.
     */
    public void startAudioRecording() {
        audioRecordingStarted = true;
        updateModelComboState();
        if (audioRecordButton == null) {
            audioRecordButton = new JButton("Stop Recording");
            // Position the audio record button at the top-right corner.
            audioRecordButton.setBounds(overlayWidth - 110, 55, 80, 25);
            audioRecordButton.addActionListener(e -> stopAudioRecording());
            getContentPane().add(audioRecordButton);
            getContentPane().revalidate();
            getContentPane().repaint();
        }
        audioRecordButton.setVisible(true);
    }

    /**
     * Stops audio recording mode and hides the cancel button.
     */
    public void stopAudioRecording() {
        audioRecordingStarted = false;
        if (audioRecordButton != null) {
            audioRecordButton.setVisible(false);
        }
        updateModelComboState();
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
                    setForeground(Color.GRAY);
                } else {
                    setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }

    /**
     * OverlayPanel paints a semi-transparent rounded rectangle as its background.
     */
    private static class OverlayPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            g2.dispose();
        }
    }
    
    /**
     * ResourceGridPanel paints a semi-transparent rounded rectangle as its background,
     * similar to the main overlay.
     */
    private static class ResourceGridPanel extends JPanel {
        public ResourceGridPanel() {
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
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
