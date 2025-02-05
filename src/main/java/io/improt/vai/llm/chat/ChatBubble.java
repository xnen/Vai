package io.improt.vai.llm.chat;

import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.util.UICommons;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/**
 * A chat bubble component that can display either text or an image, with a rounded, painted background.
 */
public class ChatBubble extends JPanel {
    private final boolean isImage;
    private final boolean isUser;
    // For text bubbles: use a JEditorPane for selectable text.
    private JEditorPane textPane;
    // Copy button is only applicable for non-user messages.
    private JButton copyButton;
    private String rawMessage; // unformatted text for clipboard copy
    // Associated ChatMessage for history (used for double-click removal)
    private ChatMessage associatedMessage;

    /**
     * Constructs a text-based chat bubble from a plain message string.
     * This constructor is preserved for legacy use; associatedMessage remains null.
     *
     * @param message the message content.
     * @param isUser  true if this bubble represents a user message.
     */
    public ChatBubble(String message, boolean isUser) {
        this.isImage = false;
        this.isUser = isUser;
        this.associatedMessage = null;
        initTextBubble(message, isUser);
        addDoubleClickListener();
    }

    /**
     * Constructs an image chat bubble (displays a 100x100 preview) from an ImageIcon.
     * This constructor is preserved for legacy use; associatedMessage remains null.
     *
     * @param imageIcon the image icon to display.
     * @param isUser    true if this bubble represents a user message.
     */
    public ChatBubble(ImageIcon imageIcon, boolean isUser) {
        this.isImage = true;
        this.isUser = isUser;
        this.associatedMessage = null;
        initImageBubble(imageIcon, isUser);
        addDoubleClickListener();
    }

    /**
     * Constructs a chat bubble associated with a ChatMessage.
     * Determines type based on the embedded content in the message.
     *
     * @param message the ChatMessage to display.
     * @param isUser  true if this bubble represents a user message.
     */
    public ChatBubble(ChatMessage message, boolean isUser) {
        this.associatedMessage = message;
        this.isUser = isUser;
        if (message.getContent() instanceof ImageContent) {
            this.isImage = true;
            try {
                File imageFile = ((ImageContent) message.getContent()).getImageFile();
                BufferedImage img = ImageIO.read(imageFile);
                BufferedImage preview = cropAndResizeImage(img);
                ImageIcon icon = new ImageIcon(preview);
                initImageBubble(icon, isUser);
            } catch (Exception ex) {
                ex.printStackTrace();
                initTextBubble("Image could not be loaded.", isUser);
            }
        } else {
            this.isImage = false;
            initTextBubble(message.getContent().toString(), isUser);
        }
        addDoubleClickListener();
    }

    private void initTextBubble(String message, boolean isUser) {
        this.rawMessage = message;
        setLayout(null);
        setOpaque(false);
        String htmlMessage = "<html><div style='max-width:400px;'>" 
                + message.replaceAll("\n", "<br>") + "</div></html>";
        textPane = new JEditorPane();
        textPane.setContentType("text/html");
        textPane.setText(htmlMessage);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textPane.setForeground(Color.WHITE);
        add(textPane);

        if (!isUser) {
            copyButton = new JButton(new ImageIcon("images/copy.png"));
            copyButton.setBorderPainted(false);
            copyButton.setFocusPainted(false);
            copyButton.setContentAreaFilled(false);
            copyButton.setOpaque(false);
            copyButton.setPreferredSize(new Dimension(8, 8));

            copyButton.addActionListener(e -> {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(rawMessage),
                        null
                );
                final String originalToolTip = copyButton.getToolTipText();
                copyButton.setToolTipText("Copied!");
                Timer timer = new Timer(1500, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        copyButton.setToolTipText(originalToolTip);
                    }
                });
                timer.setRepeats(false);
                timer.start();
            });
            copyButton.setVisible(false);
            add(copyButton);

            MouseAdapter hoverAdapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    copyButton.setVisible(true);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    copyButton.setVisible(false);
                }
            };

            this.addMouseListener(hoverAdapter);
            textPane.addMouseListener(hoverAdapter);
            copyButton.addMouseListener(hoverAdapter);
        }
    }

    private void initImageBubble(ImageIcon imageIcon, boolean isUser) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel imageLabel = new JLabel(imageIcon);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setOpaque(false);

        JPanel labelContainer = new JPanel(new BorderLayout());
        labelContainer.setOpaque(false);
        labelContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        labelContainer.add(imageLabel, BorderLayout.CENTER);

        add(labelContainer, BorderLayout.CENTER);
        setPreferredSize(new Dimension(100, 100));
    }
    
    /**
     * Helper method to crop an image to a square and resize to 100x100.
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
     * Adds a double-click listener to this chat bubble.
     * On double-click, the bubble asks its top-level ChatWindow to remove it.
     */
    private void addDoubleClickListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    Window window = SwingUtilities.getWindowAncestor(ChatBubble.this);
                    if(window instanceof ChatWindow) {
                        ((ChatWindow) window).removeChatBubble(ChatBubble.this);
                    }
                }
            }
        });
    }
    
    /**
     * Returns the associated ChatMessage.
     */
    public ChatMessage getAssociatedMessage() {
        return associatedMessage;
    }
    
    /**
     * Indicates whether this bubble represents a user message.
     */
    public boolean isUserMessage() {
        return isUser;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isImage) {
            return super.getPreferredSize();
        } else {
            int leftPadding = 12;
            int topPadding = 10;
            int bottomPadding = 10;
            int rightPadding = !isUser ? (12 + 10) : 12;
            int maxBubbleWidth = !isUser ? 410 : 400;

            int availableWidth = maxBubbleWidth - leftPadding - rightPadding;
            textPane.setSize(new Dimension(availableWidth, Integer.MAX_VALUE));
            Dimension textPref = textPane.getPreferredSize();

            int width = Math.min(textPref.width + leftPadding + rightPadding, maxBubbleWidth);
            int height = textPref.height + topPadding + bottomPadding;
            return new Dimension(width, height);
        }
    }

    @Override
    public void doLayout() {
        if (isImage) {
            super.doLayout();
        } else {
            int leftPadding = 12;
            int topPadding = 10;
            int bottomPadding = 10;
            int rightPadding = !isUser ? (12 + 10) : 12;

            int textWidth = getWidth() - leftPadding - rightPadding;
            int textHeight = getHeight() - topPadding - bottomPadding;
            textPane.setBounds(leftPadding, topPadding, textWidth, textHeight);

            if (!isUser && copyButton != null) {
                int buttonSize = 16;
                int buttonX = getWidth() - 12 - buttonSize;
                int buttonY = topPadding;
                copyButton.setBounds(buttonX, buttonY, buttonSize, buttonSize);
            }
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bubbleColor = isUser ? new Color(70, 70, 90) : new Color(80, 80, 80);
        g2.setColor(bubbleColor);

        int arc = 15;
        int width = getWidth();
        int height = getHeight();
        g2.fillRoundRect(0, 0, width, height, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
