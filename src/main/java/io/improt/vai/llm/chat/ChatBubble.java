package io.improt.vai.llm.chat;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import io.improt.vai.llm.chat.content.ChatMessageUserType;
import io.improt.vai.llm.chat.content.ImageContent;
import io.improt.vai.llm.chat.content.TextContent;
// import io.improt.vai.util.UICommons; // Not strictly needed here anymore
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * A chat bubble component displaying text (Markdown rendered) or images.
 * Features distinct styling for user/assistant messages and improved visuals.
 * Enforces a maximum width for text content via CSS.
 */
public class ChatBubble extends JPanel {
    // --- Constants ---
    private static final Color USER_BUBBLE_COLOR = new Color(74, 105, 189); // Blueish for user
    private static final Color ASSISTANT_BUBBLE_COLOR = new Color(74, 74, 74); // Dark grey for assistant
    // --- Updated Text Colors for Better Contrast ---
    private static final Color TEXT_COLOR = new Color(235, 235, 235); // Lighter grey/off-white text
    private static final Color CODE_BG_COLOR = new Color(45, 45, 45);   // Slightly lighter dark background for code
    private static final Color CODE_TEXT_COLOR = new Color(220, 220, 220); // Light text for code
    private static final Color CODE_BORDER_COLOR = new Color(90, 90, 90); // Keep border distinct
    private static final Color LINK_COLOR = new Color(138, 180, 248); // Keep light blue for links, contrasts well
    private static final Color BLOCKQUOTE_TEXT_COLOR = new Color(190, 190, 190); // Lighter blockquote text
    private static final Color BUTTON_TEXT_COLOR = new Color(180, 180, 180); // Specific color for copy button idle state
    private static final Color BUTTON_TEXT_HOVER_COLOR = new Color(235, 235, 235); // Match main text color on hover

    private static final Font BUBBLE_FONT = new Font("Inter Regular", Font.PLAIN, 14); // Consistent font
    private static final int BUBBLE_ARC = 25; // Roundness
    private static final int MAX_BUBBLE_WIDTH_PX = 450; // Max width in pixels before wrapping aggressively
    private static final Insets PADDING = new Insets(10, 14, 10, 14); // Generous padding

    private final boolean isImage;
    private final boolean isUser;
    private JEditorPane textPane; // For text/markdown content
    private JLabel imageLabel;    // For image content
    private JButton copyButton;
    private String rawMessage = ""; // Store raw markdown/text
    private ChatMessage associatedMessage;

    // Flexmark (Markdown to HTML)
    private static final Parser parser;
    private static final HtmlRenderer renderer;

    // Swing HTML Rendering
    private static final HTMLEditorKit kit;
    private static final StyleSheet styleSheet;

    static {
        // Configure Flexmark
        MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        // Configure HTMLEditorKit and StyleSheet for JEditorPane
        kit = new HTMLEditorKit();
        styleSheet = kit.getStyleSheet();
        // Define base styles - using points for font size often works better in JEditorPane
        // Use updated color constants
        styleSheet.addRule("body { color: " + toHex(TEXT_COLOR) + "; font-family: '" + BUBBLE_FONT.getFamily() + "', sans-serif; font-size: " + BUBBLE_FONT.getSize() + "pt; margin: 0; padding: 0; max-width: " + MAX_BUBBLE_WIDTH_PX + "px; word-wrap: break-word; }");
        styleSheet.addRule("p { margin: 4px 0; word-wrap: break-word; color: white; }");
        styleSheet.addRule("pre { background-color: " + toHex(CODE_BG_COLOR) + "; border: 1px solid " + toHex(CODE_BORDER_COLOR) + "; padding: 10px; border-radius: 6px; font-family: Consolas, Menlo, monospace; font-size: 11pt; color: " + toHex(CODE_TEXT_COLOR) + "; overflow-x: auto; white-space: pre-wrap; word-wrap: break-word; }");
        styleSheet.addRule("code { background-color: " + toHex(CODE_BG_COLOR) + "; padding: 2px 5px; border-radius: 4px; font-family: Consolas, Menlo, monospace; font-size: 11pt; color: " + toHex(CODE_TEXT_COLOR) + "; word-wrap: break-word; }");
        styleSheet.addRule("a { color: " + toHex(LINK_COLOR) + "; text-decoration: none; word-wrap: break-word; }");
        styleSheet.addRule("a:hover { text-decoration: underline; }");
        styleSheet.addRule("h1, h2, h3, h4, h5, h6 { margin-top: 10px; margin-bottom: 5px; color: " + toHex(TEXT_COLOR) + "; font-weight: bold; word-wrap: break-word; }");
        styleSheet.addRule("ul, ol { margin-left: 25px; padding-left: 0; margin-top: 5px; margin-bottom: 5px; word-wrap: break-word; }");
        styleSheet.addRule("li { margin-bottom: 4px; word-wrap: break-word; }");
        styleSheet.addRule("blockquote { border-left: 3px solid " + toHex(USER_BUBBLE_COLOR) + "; padding-left: 10px; margin-left: 5px; color: " + toHex(BLOCKQUOTE_TEXT_COLOR) + "; word-wrap: break-word; }");
    }

    /** Helper to convert Color to CSS hex format */
    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Constructs a chat bubble associated with a ChatMessage.
     * Determines type (text/image) based on the message content.
     */
    public ChatBubble(ChatMessage message, boolean isUser) {
        this.associatedMessage = message;
        this.isUser = isUser;
        this.setOpaque(false); // Important: Panel itself is transparent, painted in paintComponent
        this.setLayout(new BorderLayout()); // Use BorderLayout

        if (message.getContent() instanceof ImageContent) {
            this.isImage = true;
            initImageBubble(((ImageContent) message.getContent()).getImageFile(), isUser);
        } else {
            // Assume text content otherwise
            this.isImage = false;
            initTextBubble(message.getContent().toString(), isUser);
        }
        addDoubleClickListener();
    }

    private void initTextBubble(String markdownMessage, boolean isUser) {
        this.rawMessage = markdownMessage;

        textPane = new JEditorPane();
        textPane.setEditorKit(kit);
        textPane.setEditable(false);
        textPane.setOpaque(false); // EditorPane transparent to show bubble background
        // Set padding via EmptyBorder on the *textPane* itself
        textPane.setBorder(BorderFactory.createEmptyBorder(PADDING.top, PADDING.left, PADDING.bottom, PADDING.right));
        textPane.setContentType("text/html"); // Crucial for rendering
        renderMarkdown(markdownMessage); // Render initial content

        // Handle hyperlink clicks
        textPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } else {
                        // Fallback for environments where Desktop API is not supported
                        System.err.println("Desktop browsing not supported. Link: " + e.getURL());
                        JOptionPane.showMessageDialog(this, "Cannot open link: " + e.getURL(), "Browse Error", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Could not open link: " + e.getURL(), "Link Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Use a wrapper panel to potentially hold the copy button above the text
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(textPane, BorderLayout.CENTER);

        // Add Copy button for Assistant messages
        if (!isUser) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2)); // Top-right, minimal padding
            buttonPanel.setOpaque(false);

            copyButton = new JButton("Copy"); // Simple text button
            styleMinimalButton(copyButton);
            copyButton.setToolTipText("Copy Markdown");
            copyButton.addActionListener(e -> copyRawMessageToClipboard());

            buttonPanel.add(copyButton);
            contentWrapper.add(buttonPanel, BorderLayout.NORTH); // Button above text

            // Hide button initially, show on hover
            copyButton.setVisible(false);
            MouseAdapter hoverAdapter = new MouseAdapter() {
                 @Override public void mouseEntered(MouseEvent e) { if (!isUser) copyButton.setVisible(true); }
                 @Override public void mouseExited(MouseEvent e) {
                     // Only hide if mouse exits the *entire bubble*
                     Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), ChatBubble.this);
                     if (!contains(p)) {
                          if (!isUser) copyButton.setVisible(false);
                     }
                 }
            };
            this.addMouseListener(hoverAdapter);
            textPane.addMouseListener(hoverAdapter); // Also listen on textPane
            // Add to wrapper too
             contentWrapper.addMouseListener(hoverAdapter);
             buttonPanel.addMouseListener(hoverAdapter);
             copyButton.addMouseListener(hoverAdapter);
        }

        this.add(contentWrapper, BorderLayout.CENTER);
    }

    private void initImageBubble(File imageFile, boolean isUser) {
        try {
            BufferedImage img = ImageIO.read(imageFile);
            // Keep aspect ratio, scale down if needed, max size 200x200
            int previewSize = 200;
            Image scaledImg = img.getScaledInstance(
                img.getWidth() > img.getHeight() ? previewSize : -1,
                img.getHeight() > img.getWidth() ? previewSize : -1,
                Image.SCALE_SMOOTH
            );
             if (scaledImg == null) throw new IOException("Image scaling failed");

            ImageIcon icon = new ImageIcon(scaledImg);
            imageLabel = new JLabel(icon);
            imageLabel.setOpaque(false);
            // Add padding around the image label using a border
            imageLabel.setBorder(BorderFactory.createEmptyBorder(PADDING.top, PADDING.left, PADDING.bottom, PADDING.right));

            this.add(imageLabel, BorderLayout.CENTER);
            // Enforce max width for image bubbles too, using the label's max size
            imageLabel.setMaximumSize(new Dimension(MAX_BUBBLE_WIDTH_PX + PADDING.left + PADDING.right, Short.MAX_VALUE));


        } catch (Exception ex) {
            ex.printStackTrace();
            // Fallback to text if image loading fails
            initTextBubble("Error loading image: " + imageFile.getName(), isUser);
        }
    }

    private void styleMinimalButton(JButton button) {
        button.setFont(new Font("Inter", Font.PLAIN, 10));
        button.setForeground(BUTTON_TEXT_COLOR); // Use specific button text color
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Simple hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setForeground(BUTTON_TEXT_HOVER_COLOR); } // Use specific hover color
            @Override public void mouseExited(MouseEvent e) { button.setForeground(BUTTON_TEXT_COLOR); } // Revert to idle color
        });
    }

    private void copyRawMessageToClipboard() {
        if (rawMessage == null) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(rawMessage),
                null
        );
        // Visual feedback
        if (copyButton != null) {
             String originalText = copyButton.getText();
             Color originalColor = copyButton.getForeground(); // Store original idle color
             copyButton.setText("Copied!");
             copyButton.setForeground(new Color(0, 190, 0)); // Brighter Green feedback
             Timer timer = new Timer(1500, evt -> {
                  copyButton.setText(originalText);
                  copyButton.setForeground(originalColor); // Restore original idle color
             });
             timer.setRepeats(false);
             timer.start();
        }
    }


    /** Renders Markdown to HTML and updates the JEditorPane. Must run on EDT. */
    private void renderMarkdown(String markdown) {
        if (textPane == null) return;
        this.rawMessage = markdown; // Update raw message whenever rendering

        Runnable renderTask = () -> {
            try {
                com.vladsch.flexmark.util.ast.Node document = parser.parse(markdown);
                String htmlContent = renderer.render(document);
                // Construct the full HTML for JEditorPane, CSS is now handled by HTMLEditorKit's StyleSheet
                // No need for inline <style> block anymore
                String fullHtml = "<html><head></head><body>" + htmlContent + "</body></html>";
                textPane.setText(fullHtml);
                // Reset caret position to avoid potential scrolling issues after update
                textPane.setCaretPosition(0);
                // Crucial: Revalidate the bubble after text change to update size
                revalidate();
                 if (getParent() != null) {
                     getParent().revalidate(); // Revalidate parent container (the Box)
                 }
            } catch (Exception e) {
                // Log error, display fallback text
                System.err.println("Error rendering Markdown: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for better debugging
                textPane.setText("<html><body>Error displaying message. See console for details.</body></html>");
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            renderTask.run();
        } else {
            SwingUtilities.invokeLater(renderTask);
        }
    }

    /** Converts StyleSheet rules to a String for inline styling (basic implementation).
     * @deprecated Inline styles are no longer needed as HTMLEditorKit uses the StyleSheet directly.
     */
    @Deprecated
    private static String styleSheetToString(StyleSheet ss) {
        // This is a simplified conversion; a more robust one might be needed for complex styles.
        // No longer used, HTMLEditorKit applies the stylesheet directly.
        return "";
    }

    /** Appends text snippet and re-renders. Assumes called on EDT. */
    public void appendText(String snippet) {
        if (isImage || textPane == null) return;
        renderMarkdown(this.rawMessage + snippet); // Re-render with appended text
    }


    /** Adds double-click listener for removal. */
    private void addDoubleClickListener() {
        MouseAdapter doubleClickAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Window window = SwingUtilities.getWindowAncestor(ChatBubble.this);
                    if (window instanceof ChatWindow) {
                        ((ChatWindow) window).removeChatBubble(ChatBubble.this);
                    }
                }
            }
        };
        this.addMouseListener(doubleClickAdapter);
        if (textPane != null) textPane.addMouseListener(doubleClickAdapter);
        if (imageLabel != null) imageLabel.addMouseListener(doubleClickAdapter);
    }

    // --- Size Calculation ---
    @Override
    public Dimension getPreferredSize() {
        // Let the layout manager handle the size based on content and constraints
        // JEditorPane with max-width CSS should calculate its preferred size correctly now.
        Dimension preferredSize = super.getPreferredSize();
        int width = preferredSize.width;

        // If it's a text pane, its width should theoretically be constrained by CSS max-width.
        // However, the *bubble itself* might still report a wider preferred size initially.
        // We can clamp the preferred width here as a fallback, but CSS is the primary mechanism.
        if (!isImage && textPane != null) {
            width = Math.min(width, MAX_BUBBLE_WIDTH_PX + PADDING.left + PADDING.right + 10); // Add padding + buffer
        } else if (isImage && imageLabel != null) {
             width = Math.min(width, MAX_BUBBLE_WIDTH_PX + PADDING.left + PADDING.right + 10);
        }


        // Ensure minimum width if needed
        int minWidth = 100;
        return new Dimension(Math.max(minWidth, width), preferredSize.height);
    }

     @Override
    public Dimension getMaximumSize() {
         // Respect the maximum width calculated from CSS + padding
         int maxWidth = MAX_BUBBLE_WIDTH_PX + PADDING.left + PADDING.right + 10; // Add buffer
         Dimension pref = getPreferredSize();
         // Allow height to grow, but cap width
         return new Dimension(Math.min(pref.width, maxWidth), Short.MAX_VALUE);
    }


    @Override
    public Dimension getMinimumSize() {
        // Provide a reasonable minimum size
        return new Dimension(80, 40);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Choose color based on user/assistant
        Color bubbleColor = isUser ? USER_BUBBLE_COLOR : ASSISTANT_BUBBLE_COLOR;
        g2.setColor(bubbleColor);

        // Get dimensions and draw the rounded rectangle background
        int width = getWidth();
        int height = getHeight();
        // Fill the rounded rectangle background shape
        g2.fill(new RoundRectangle2D.Double(0, 0, width, height, BUBBLE_ARC, BUBBLE_ARC));

        g2.dispose();
        // NO super.paintComponent(g) here if we want full control over painting and transparency.
        // Instead, manually paint children if needed, but JEditorPane/JLabel handle themselves
        // when added to the container and Opaque=false.
    }

    // --- Getters ---
    public ChatMessage getAssociatedMessage() { return associatedMessage; }
    public boolean isUserMessage() { return isUser; }


    // --- Deprecated Constructors (Keep for compatibility if needed, but mark) ---
    @Deprecated
    public ChatBubble(String message, boolean isUser) {
        this(new ChatMessage(isUser ? ChatMessageUserType.USER : ChatMessageUserType.ASSISTANT, new TextContent(message)), isUser);
    }

    @Deprecated
    public ChatBubble(ImageIcon imageIcon, boolean isUser) {
        // This is harder to map directly to the new structure without a File.
        // Consider removing or adapting if image pasting/loading always uses Files now.
        this(createDummyImageMessage(imageIcon), isUser); // Placeholder for dummy message
    }

    // Helper for deprecated constructor
    private static ChatMessage createDummyImageMessage(ImageIcon icon) {
         System.err.println("Warning: Using deprecated ChatBubble(ImageIcon) constructor. Image source file unknown.");
         // Create a dummy file or handle appropriately
         File dummyFile = new File("dummy_image_" + System.currentTimeMillis() + ".png");
         try {
             ImageIO.write(toBufferedImage(icon.getImage()), "png", dummyFile);
             dummyFile.deleteOnExit();
         } catch (IOException e) { /* ignore */ }
         return new ChatMessage(ChatMessageUserType.USER, new ImageContent(dummyFile));
    }
     /** Converts an Image to a BufferedImage. */
    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) return (BufferedImage) img;
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }
}
