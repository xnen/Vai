package io.improt.vai.llm.chat;

import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.llm.providers.IModelProvider;
import io.improt.vai.util.UICommons;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.border.LineBorder;
import java.io.File;
import java.util.concurrent.Executors;

/**
 * The main chat window frame.
 */
public class ChatWindow extends JFrame {
    private final ChatLLMHandler llmHandler;
    private ChatPanel chatPanel;
    private JScrollPane scrollPane;
    private JTextArea inputArea;
    // Flag to track if the model is currently running.
    private volatile boolean isModelRunning = false;
    // Remember initial click for dragging the window.
    private Point initialClick;
    // New field for the typing bubble indicator.
    private TypingBubble typingBubble;

    public ChatWindow(ChatLLMHandler handler) {
        super("Chat Window");
        this.llmHandler = handler;
        initComponents();
        // Start transparent, fade in for effect.
        setOpacity(0f);
        fadeIn();
        
        // Set input area to be the default focused field.
        SwingUtilities.invokeLater(() -> inputArea.requestFocusInWindow());
        
        // Always on top (Feature #3)
        setAlwaysOnTop(true);
        
        this.callModel();
    }

    private void initComponents() {
        // Basic frame properties
        setSize(660, 700);
        setLocationRelativeTo(null);
        setUndecorated(true);

        // Main panel with a slightly transparent, rounded rectangle background.
        RoundedPanel mainPanel = new RoundedPanel(30, new Color(50, 50, 50, 230));
        mainPanel.setLayout(new BorderLayout());
        setContentPane(mainPanel);

        // Add drag functionality to move the window by clicking and dragging the main panel.
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });
        mainPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int thisX = getLocation().x;
                int thisY = getLocation().y;

                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                setLocation(thisX + xMoved, thisY + yMoved);
            }
        });

        // The chat area, inside a scroll pane
        chatPanel = new ChatPanel();
        // Set the overlay to display the current selected model in small bold text.
        chatPanel.setModelName(llmHandler.getSelectedModel());
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setBorder(null);
        // Increase scroll speed to make it about 3x faster than default
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Input panel at the bottom
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setOpaque(true);
        inputPanel.setBackground(new Color(70, 70, 70));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // A multi-line text area for input
        inputArea = new JTextArea(3, 30);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(new Color(60, 60, 60));
        inputArea.setForeground(Color.WHITE);
        inputArea.setCaretColor(Color.WHITE);
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        inputScrollPane.setBorder(null);
        inputScrollPane.setOpaque(false);
        inputScrollPane.getViewport().setOpaque(false);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        // Removed send and record buttons per requirements;
        // user now sends message on Ctrl+Enter.

        // Key binding for sending the message via Ctrl+Enter
        inputArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "sendMessage");
        inputArea.getActionMap().put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        // Key binding for paste action in ChatWindow's text field (Feature #2)
        inputArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl V"), "pasteAction");
        inputArea.getActionMap().put("pasteAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String modelName = llmHandler.getSelectedModel();
                IModelProvider llmProvider = App.getInstance().getLLMProvider(modelName);
                if (llmProvider == null || !llmProvider.supportsVision()) {
                    return;
                }

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
                        File tempFile = File.createTempFile("chatWindowPastedImage", ".png");
                        ImageIO.write(bufferedImage, "png", tempFile);
                        
                        // Create a new ChatMessage for the pasted image (as a user message) and add it to history.
                        ChatMessage imageMessage = new ChatMessage(ChatMessageUserType.USER, new ImageContent(tempFile));
                        llmHandler.addMessage(imageMessage);
                        ChatBubble imageBubble = new ChatBubble(imageMessage, true);
                        chatPanel.addChatBubble(imageBubble, true);
                        scrollToBottom();
                    } else if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        inputArea.paste();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Add ESC key listener to dispose the chat window
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Keep corners rounded whenever window is resized.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                UICommons.applyRoundedCorners(ChatWindow.this, 30, 30);
            }
        });
    }

    /**
     * Sends user text to the chat panel and triggers the LLM response in a background thread.
     */
    private void sendMessage() {
        if (isModelRunning) {
            triggerErrorAnimation();
            return;
        }
        
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputArea.setText("");
        
        // Create ChatMessage for the user text and add it to the conversation history.
        ChatMessage userMessage = new ChatMessage(ChatMessageUserType.USER, new TextContent(text));
        llmHandler.addMessage(userMessage);
        ChatBubble userBubble = new ChatBubble(userMessage, true);
        chatPanel.addChatBubble(userBubble, true);
        scrollToBottom();
        
        // Call model in background
        this.callModel();
    }

    /**
     * Triggers an error-like animation by flashing a red border on the input text area.
     */
    private void triggerErrorAnimation() {
        Color originalBorderColor = ((LineBorder)inputArea.getBorder()).getLineColor();
        inputArea.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        Timer timer = new Timer(500, e -> {
            inputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Initiates the model request.
     * Displays a typing bubble (animated dots) while the request is running,
     * then removes it before adding the actual assistant response.
     */
    private void callModel() {
        isModelRunning = true;
        SwingUtilities.invokeLater(() -> inputArea.setEnabled(false));
        
        // Show the typing bubble before starting the background model call.
        SwingUtilities.invokeLater(() -> {
            typingBubble = new TypingBubble();
            chatPanel.addBubble(typingBubble, false);
            scrollToBottom();
        });
        
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                int previous = llmHandler.getConversationHistory().size();
                llmHandler.runModelWithCurrentHistory();
                int newSize = llmHandler.getConversationHistory().size();
                SwingUtilities.invokeLater(() -> {
                    if (typingBubble != null) {
                        typingBubble.stopAnimation();
                        chatPanel.remove(typingBubble);
                        chatPanel.recalcLayout();
                        typingBubble = null;
                    }
                });

                for (int i = previous; i < newSize; i++) {
                    ChatMessage newMessage = llmHandler.getConversationHistory().get(i);
                    ChatBubble assistantBubble = new ChatBubble(newMessage, false);
                    chatPanel.addChatBubble(assistantBubble, false);
                    scrollToBottom();
                }


            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    // Ensure the typing bubble is removed in case of an error.
                    if (typingBubble != null) {
                        typingBubble.stopAnimation();
                        chatPanel.remove(typingBubble);
                        chatPanel.recalcLayout();
                        typingBubble = null;
                    }
                    inputArea.setEnabled(true);
                    inputArea.requestFocusInWindow();
                });
                isModelRunning = false;
            }
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }

    /**
     * Populate the chat panel with any existing conversation history from the LLM handler.
     */
    public void populateInitialChatHistory() {
        for (ChatMessage msg : llmHandler.getConversationHistory()) {
            if (msg.getMessageType() == ChatMessageUserType.SYSTEM) continue;

            boolean isUser = msg.getMessageType() == ChatMessageUserType.USER;
            ChatBubble bubble;
            if (msg.getContent() instanceof ImageContent) {
                File imageFile = ((ImageContent) msg.getContent()).getImageFile();
                try {
                    BufferedImage img = ImageIO.read(imageFile);
                    BufferedImage preview = cropAndResizeImage(img);
                    ImageIcon icon = new ImageIcon(preview);
                    bubble = new ChatBubble(icon, isUser);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    bubble = new ChatBubble("Image could not be loaded.", isUser);
                }
            } else {
                bubble = new ChatBubble(msg.getContent().getBrief(), isUser);
            }
            chatPanel.addChatBubble(bubble, isUser);
        }
        scrollToBottom();
    }

    /**
     * Fades the window in over ~0.3 seconds.
     */
    private void fadeIn() {
        Timer timer = new Timer(5, null);
        timer.addActionListener(new ActionListener() {
            float opacity = 0f;
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += 0.03f;
                if (opacity >= 1.0f) {
                    setOpacity(1.0f);
                    timer.stop();
                } else {
                    setOpacity(opacity);
                }
            }
        });
        timer.start();
    }

    /**
     * A simple helper to crop an image to a square and then resize to 100x100 for display.
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
     * A simple rounded panel for the main window background.
     */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color backgroundColor;

        public RoundedPanel(int radius, Color bgColor) {
            super();
            this.radius = radius;
            this.backgroundColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    /**
     * Removes the given chat bubble from the UI and also from the conversation history.
     * (Feature #1: Double-click removal)
     */
    public void removeChatBubble(ChatBubble bubble) {
        chatPanel.remove(bubble);
        if (bubble.getAssociatedMessage() != null) {
            llmHandler.removeMessage(bubble.getAssociatedMessage());
        }
        chatPanel.recalcLayout();
        scrollToBottom();
    }
    
}
