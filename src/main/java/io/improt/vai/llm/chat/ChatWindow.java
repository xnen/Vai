package io.improt.vai.llm.chat;

import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.util.UICommons;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Executors;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/**
 * The main chat window frame.
 */
public class ChatWindow extends JFrame {
    private final ChatLLMHandler llmHandler;
    private ChatPanel chatPanel;
    private JScrollPane scrollPane;
    private JTextArea inputArea;

    public ChatWindow(ChatLLMHandler handler) {
        super("Chat Window");
        this.llmHandler = handler;
        initComponents();
        // Start transparent, fade in for effect.
        setOpacity(0f);
        fadeIn();
    }

    private void initComponents() {
        // Basic frame properties
        setSize(660, 700);
        setLocationRelativeTo(null);
        setUndecorated(true);

        // Main panel with a slightly transparent, rounded rectangle background.
        // We'll keep it somewhat dark, so the chat panel can be even darker.
        RoundedPanel mainPanel = new RoundedPanel(30, new Color(50, 50, 50, 230));
        mainPanel.setLayout(new BorderLayout());
        setContentPane(mainPanel);

        // The chat area, inside a scroll pane
        chatPanel = new ChatPanel();
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setBorder(null);
        // Increase scroll speed to make it about 3x faster than default
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Input panel at the bottom
        JPanel inputPanel = new JPanel(new BorderLayout());
        // Make it opaque and lightly different color from the chat panel.
        inputPanel.setOpaque(true);
        inputPanel.setBackground(new Color(70, 70, 70));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // A multi-line text area for input
        inputArea = new JTextArea(3, 30);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        // Dark gray background, light text
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
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputArea.setText("");

        // Add user text bubble
        ChatBubble userBubble = new ChatBubble(text, true);
        chatPanel.addChatBubble(userBubble, true);
        scrollToBottom();

        // Add conversation history content
        llmHandler.addMessage(new ChatMessage(ChatMessageUserType.USER, new TextContent(text)));

        // Call model in background
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                llmHandler.runModelWithCurrentHistory();
                ChatMessage lastMessage = llmHandler.getConversationHistory()
                        .get(llmHandler.getConversationHistory().size() - 1);
                SwingUtilities.invokeLater(() -> {
                    ChatBubble assistantBubble;
                    // Check if the response is an image message.
                    assistantBubble = new ChatBubble(lastMessage.getContent().getBrief(), false);
                    chatPanel.addChatBubble(assistantBubble, false);
                    scrollToBottom();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
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
        Timer timer = new Timer(30, null);
        timer.addActionListener(new ActionListener() {
            float opacity = 0f;
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += 0.1f;
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
}
