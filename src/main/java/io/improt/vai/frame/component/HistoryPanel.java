// HistoryPanel.java
package io.improt.vai.frame.component;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HistoryPanel extends JPanel {
    private final List<HistoryEntry> entries = new ArrayList<>();
    private JPanel entryContainer;
    private final int MAX_ENTRIES = 5;

    public HistoryPanel() {
        setLayout(new BorderLayout());
        entryContainer = new JPanel();
        entryContainer.setLayout(new BoxLayout(entryContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(entryContainer);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addEntry(String description, String backupPath) {
        if (entries.size() == MAX_ENTRIES) {
            entries.remove(0);
            entryContainer.remove(0);
        }
        HistoryEntry entry = new HistoryEntry(description, backupPath);
        entries.add(entry);
        entryContainer.add(entry);
        revalidate();
        repaint();
    }
}

class HistoryEntry extends JPanel {
    public HistoryEntry(String description, String backupPath) {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel descLabel = new JLabel(description);
        System.out.println(Paths.get("").toAbsolutePath());
        JButton revertButton = new JButton(new ImageIcon("images/trash.png"));
        revertButton.setContentAreaFilled(false);
        revertButton.setBorderPainted(false);
        revertButton.setFocusPainted(false);
        revertButton.addActionListener(e -> {
            // Display the revert message
            JOptionPane.showMessageDialog(this, "Reverting from backup: " + backupPath);

            // Remove this entry from the parent panel and trigger a UI update
            Container parent = this.getParent();
            if (parent != null) {
                parent.remove(this);
                parent.revalidate();
                parent.repaint();
            }

            // TODO: Restore all folders from the incremental backup path.
        });
        add(descLabel);
        add(revertButton);
    }
}
