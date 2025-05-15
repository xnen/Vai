package io.improt.vai.llm.chat;

import io.improt.vai.backend.App;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.TextContent;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.llm.providers.impl.IModelProvider;
import io.improt.vai.util.UICommons;
import io.improt.vai.util.stream.ISnippetAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.io.File;
import java.util.concurrent.Executors;

/**
 * The main chat window frame. Supports streaming responses. Modernized UI.
 */
public class ChatWindow extends JFrame {
    // --- Constants ---
    private static final Color BG_DARK = new Color(43, 43, 43); // Darker background
    private static final Color BG_MEDIUM = new Color(55, 55, 55); // Slightly lighter for elements
    private static final Color BG_INPUT = new Color(60, 60, 60); // Input field background
    private static final Color TEXT_PRIMARY = new Color(220, 220, 220); // Primary text
    private static final Color TEXT_SECONDARY = new Color(160, 160, 160); // Secondary text (header)
    private static final Color BORDER_COLOR = new Color(75, 75, 75); // Subtle borders
    private static final Color SCROLLBAR_COLOR = new Color(85, 85, 85);
    private static final Color SCROLLBAR_THUMB_COLOR = new Color(120, 120, 120);
    private static final Font PRIMARY_FONT = new Font("Inter Regular", Font.PLAIN, 14); // Modern font
    private static final Font BOLD_FONT = new Font("Inter Regular", Font.BOLD, 12); // Bold variant

    private final ChatLLMHandler llmHandler;
    private ChatPanel chatPanel;
    private JScrollPane scrollPane;
    private JTextArea inputArea;
    private volatile boolean isModelRunning = false;
    private Point initialClick;
    private JPanel headerPanel;

    public ChatWindow(ChatLLMHandler handler) {
        super("Chat Window");
        this.llmHandler = handler;
        try {
            // Attempt to load the Inter font if available
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Inter-Regular.ttf"))); // Adjust path if needed
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Inter-Bold.ttf"))); // Adjust path if needed
        } catch (Exception e) {
            System.err.println("Inter font not found, using default sans-serif.");
            // Font constants will use the fallback if "Inter" isn't registered
        }

        initComponents();
        setAlwaysOnTop(true);
        this.setUndecorated(true);
        UICommons.applyRoundedCorners(this, 20, 20); // Slightly more rounded

        SwingUtilities.invokeLater(() -> inputArea.requestFocusInWindow());
    }

    private void initComponents() {
        setSize(680, 720); // Slightly larger
        setLocationRelativeTo(null);

        // Main panel using the dark background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_DARK);
        setContentPane(mainPanel);

        // --- Draggable Header ---
        headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8)); // Added padding
        headerPanel.setBackground(BG_MEDIUM); // Use medium background
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR)); // Subtle bottom border
        JLabel titleLabel = new JLabel(llmHandler.getSelectedModel());
        titleLabel.setForeground(TEXT_SECONDARY); // Use secondary text color
        titleLabel.setFont(BOLD_FONT); // Use bold font
        headerPanel.add(titleLabel);
        addWindowDragListeners(headerPanel); // Add drag listeners
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- Chat Area ---
        chatPanel = new ChatPanel(); // ChatPanel background is set internally
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.getViewport().setBackground(BG_DARK); // Match main background
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18); // Slightly adjusted scroll speed
        // Customize Scrollbar UI
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // Hide horizontal bar
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Input Panel ---
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5)); // Add small gap
        inputPanel.setOpaque(true);
        inputPanel.setBackground(BG_MEDIUM); // Match header background
        // Add padding and a top border
        inputPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 15, 10, 15) // Generous padding
        ));
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // --- Input Text Area ---
        inputArea = new JTextArea(3, 30); // Start with 3 rows
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(BG_INPUT); // Specific input background
        inputArea.setForeground(TEXT_PRIMARY); // Primary text color
        inputArea.setCaretColor(TEXT_PRIMARY); // White caret
        inputArea.setFont(PRIMARY_FONT); // Use primary font
        // Padding inside the text area and rounded border
        inputArea.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true), // Rounded line border
                BorderFactory.createEmptyBorder(8, 10, 8, 10) // Internal padding
        ));
        // Make border rounded (Requires custom painting or look-and-feel)
        // For simplicity, using standard rounded line border above.

        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        inputScrollPane.setBorder(null);
        inputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI()); // Style scrollbar here too
        inputScrollPane.setOpaque(false);
        inputScrollPane.getViewport().setOpaque(false);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        // --- Key Bindings ---
        // CTRL+ENTER to send
        inputArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "sendMessage");
        inputArea.getActionMap().put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // CTRL+V to paste (handle images if model supports vision)
        inputArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl V"), "pasteAction");
        inputArea.getActionMap().put("pasteAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handlePaste();
            }
        });

        // ESC to close
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Add listener to dynamically adjust input area height
        inputArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { adjustInputAreaHeight(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { adjustInputAreaHeight(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { adjustInputAreaHeight(); }
        });
        adjustInputAreaHeight(); // Initial adjustment
    }

    /** Adjusts the input JTextArea's preferred height based on content, up to a max. */
    private void adjustInputAreaHeight() {
        SwingUtilities.invokeLater(() -> {
            int maxRows = 8; // Max height equivalent to 8 rows
            int minRows = 2; // Min height equivalent to 2 rows
            int fontHeight = inputArea.getFontMetrics(inputArea.getFont()).getHeight();
            int desiredRows = Math.max(minRows, Math.min(maxRows, inputArea.getLineCount()));
            int bordersAndPadding = inputArea.getInsets().top + inputArea.getInsets().bottom + 10; // Estimate padding

            // Calculate preferred height based on desired rows
            int preferredHeight = (desiredRows * fontHeight) + bordersAndPadding;

            // Update scroll pane preferred size
            Dimension currentSize = inputArea.getPreferredSize();
            if (currentSize.height != preferredHeight) {
                 JScrollPane inputScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, inputArea);
                 if (inputScrollPane != null) {
                    inputScrollPane.setPreferredSize(new Dimension(currentSize.width, preferredHeight));
                    inputScrollPane.revalidate();
                    // Revalidate the parent container holding the scroll pane
                    Container parent = inputScrollPane.getParent();
                     if (parent != null) {
                         parent.revalidate();
                     }
                 }
            }
        });
    }

    /** Handles the paste action, checking for images if the model supports vision. */
    private void handlePaste() {
        String modelName = llmHandler.getSelectedModel();
        IModelProvider llmProvider = App.getInstance().getLLMProvider(modelName);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        try {
            // Handle image paste only if provider supports vision
            if (llmProvider != null && llmProvider.supportsVision() &&
                contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor))
            {
                Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                BufferedImage bufferedImage = toBufferedImage(image);
                File tempFile = File.createTempFile("chatWindowPastedImage", ".png");
                ImageIO.write(bufferedImage, "png", tempFile);

                // Add image message to history and UI
                ChatMessage imageMessage = new ChatMessage(ChatMessageUserType.USER, new ImageContent(tempFile));
                llmHandler.addMessage(imageMessage);
                ChatBubble imageBubble = new ChatBubble(imageMessage, true); // true for user
                chatPanel.addChatBubble(imageBubble, true);
                scrollToBottom();
            } else if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                // Standard text paste
                inputArea.paste();
            }
        } catch (Exception ex) {
            System.err.println("Paste error: " + ex.getMessage());
            ex.printStackTrace();
            // Fallback to standard text paste if image handling fails unexpectedly
            inputArea.paste();
        }
    }

     /** Converts an Image to a BufferedImage. */
    private BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }

    /** Adds mouse listeners for dragging the window using a specific component. */
    private void addWindowDragListeners(Component dragComponent) {
        MouseAdapter dragAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                SwingUtilities.convertPointToScreen(initialClick, dragComponent);
                initialClick.x -= getLocation().x;
                initialClick.y -= getLocation().y;
            }
        };
        MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentScreenPoint = e.getLocationOnScreen();
                setLocation(currentScreenPoint.x - initialClick.x, currentScreenPoint.y - initialClick.y);
            }
        };
        dragComponent.addMouseListener(dragAdapter);
        dragComponent.addMouseMotionListener(motionAdapter);
    }

    /** Sends the message from the input area. */
    public void sendMessage() {
        if (isModelRunning) {
            triggerErrorAnimation();
            return;
        }
        String text = inputArea.getText().trim();
        inputArea.setText(""); // Clear input immediately
        adjustInputAreaHeight(); // Adjust height after clearing

        if (!text.isEmpty()) {
            ChatMessage userMessage = new ChatMessage(ChatMessageUserType.USER, new TextContent(text));
            llmHandler.addMessage(userMessage);
            ChatBubble userBubble = new ChatBubble(userMessage, true);
            chatPanel.addChatBubble(userBubble, true);
            scrollToBottom();
        }
        // Always call the model, even if text is empty (might have pasted images)
        this.callModelStreaming();
    }

    /** Flashes the input area border to indicate an error (e.g., sending while busy). */
    private void triggerErrorAnimation() {
        Color errorColor = new Color(180, 50, 50);
        Border originalBorder = inputArea.getBorder();
        Border errorBorder = new CompoundBorder(
                BorderFactory.createLineBorder(errorColor, 2, true), // Thicker error border
                ((CompoundBorder)originalBorder).getInsideBorder() // Keep padding
        );

        inputArea.setBorder(errorBorder);
        Timer timer = new Timer(500, e -> inputArea.setBorder(originalBorder));
        timer.setRepeats(false);
        timer.start();
    }

    /** Initiates the streaming model request and updates the UI. */
    private void callModelStreaming() {
        isModelRunning = true;
        SwingUtilities.invokeLater(() -> inputArea.setEnabled(false));

        // Create placeholder message and bubble for the assistant
        final ChatMessage assistantMessage = new ChatMessage(ChatMessageUserType.ASSISTANT, new TextContent(""));
        // Add the placeholder message to history immediately
        // llmHandler.addMessage(assistantMessage); // Add message to history only *after* stream completes

        // Create the visual bubble first
        final ChatBubble assistantBubble = new ChatBubble(assistantMessage, false);
        SwingUtilities.invokeLater(() -> {
            chatPanel.addChatBubble(assistantBubble, false);
            scrollToBottom();
        });

        final StringBuilder fullResponseBuilder = new StringBuilder();

        // Action for each received snippet
        ISnippetAction snippetAction = snippet -> {
            fullResponseBuilder.append(snippet);
            SwingUtilities.invokeLater(() -> {
                assistantBubble.appendText(snippet); // Update bubble content live
                revalidateScrollPane();
                scrollToBottom();
            });
        };

        // Action on stream completion
        Runnable onCompleteAction = () -> SwingUtilities.invokeLater(() -> {
            // Update the *actual* ChatMessage content now that we have the full response
            ((TextContent) assistantMessage.getContent()).setText(fullResponseBuilder.toString());
            // Now add the completed message to the handler's history
            llmHandler.addMessage(assistantMessage);

            // Re-enable input and clean up
            inputArea.setEnabled(true);
            inputArea.requestFocusInWindow();
            isModelRunning = false;
            revalidateScrollPane();
            scrollToBottom();
        });

        // Start streaming in background
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                llmHandler.streamModelResponse(snippetAction, onCompleteAction);
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    String errorText = "Error during streaming: " + ex.getMessage();
                    // Update the bubble with the error
                    assistantBubble.appendText("\n\n**Error:**\n```\n" + errorText + "\n```");
                    // Update the message content in history as well
                    ((TextContent) assistantMessage.getContent()).setText(fullResponseBuilder.toString() + "\n\n**Error:**\n```\n" + errorText + "\n```");
                    llmHandler.addMessage(assistantMessage); // Add error message to history

                    inputArea.setEnabled(true);
                    inputArea.requestFocusInWindow();
                    isModelRunning = false;
                    revalidateScrollPane();
                    scrollToBottom();
                });
            }
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
            // Small delay and re-scroll can sometimes help ensure it reaches the absolute bottom
            Timer timer = new Timer(50, e -> verticalBar.setValue(verticalBar.getMaximum()));
            timer.setRepeats(false);
            timer.start();
        });
    }

    public void revalidateScrollPane() {
         SwingUtilities.invokeLater(() -> {
             if (scrollPane != null) {
                 scrollPane.revalidate();
                 scrollPane.repaint();
             }
             // Sometimes revalidating the chatPanel itself helps BoxLayout
             if (chatPanel != null) {
                 chatPanel.revalidate();
                 chatPanel.repaint();
             }
         });
    }

    public void populateInitialChatHistory() {
        chatPanel.clearBubbles(); // Clear existing bubbles first
        for (ChatMessage msg : llmHandler.getConversationHistory()) {
            if (msg.getMessageType() == ChatMessageUserType.SYSTEM) continue;
            boolean isUser = msg.getMessageType() == ChatMessageUserType.USER;
            ChatBubble bubble = new ChatBubble(msg, isUser);
            chatPanel.addChatBubble(bubble, isUser);
        }
        revalidateScrollPane();
        scrollToBottom();
    }

    /** Custom UI for modern scrollbars. */
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        private final Dimension d = new Dimension();

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return new JButton() { // Invisible button
                @Override public Dimension getPreferredSize() { return d; }
            };
        }
        @Override
        protected JButton createIncreaseButton(int orientation) {
            return new JButton() { // Invisible button
                 @Override public Dimension getPreferredSize() { return d; }
            };
        }
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(SCROLLBAR_COLOR); // Track color
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SCROLLBAR_THUMB_COLOR); // Thumb color
            // Draw rounded thumb
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 5, 5);
            g2.dispose();
        }
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = SCROLLBAR_THUMB_COLOR;
            this.trackColor = SCROLLBAR_COLOR;
        }
    }

    /** Removes a bubble and its associated message from history. */
    public void removeChatBubble(ChatBubble bubble) {
        chatPanel.removeBubble(bubble); // Use dedicated remove method in ChatPanel
        if (bubble.getAssociatedMessage() != null) {
            llmHandler.removeMessage(bubble.getAssociatedMessage());
        }
        revalidateScrollPane();
        // No need to scroll to bottom after removal usually
    }

    public JTextArea getInputArea() {
        return inputArea;
    }
}
