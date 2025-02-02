package io.improt.vai.frame.dialogs;

import io.improt.vai.backend.plugin.AbstractPlugin;
import io.improt.vai.backend.plugin.PluginManager;
import io.improt.vai.backend.plugin.PluginStateManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FeaturesDialog extends JDialog {
    private final Map<AbstractPlugin, JCheckBox> pluginCheckboxes = new HashMap<>();

    public FeaturesDialog(Frame owner) {
        super(owner, "Configure Features", true);
        setLayout(new BorderLayout());
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));

        // Iterate over plugins
        for (AbstractPlugin plugin : PluginManager.getInstance().getPlugins()) {
            JCheckBox checkBox = new JCheckBox(plugin.getFeatureDescription(), plugin.isActive());
            pluginCheckboxes.put(plugin, checkBox);
            checkboxPanel.add(checkBox);
        }
        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        add(buttonsPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            // Update each plugin's active state
            for (Map.Entry<AbstractPlugin, JCheckBox> entry : pluginCheckboxes.entrySet()) {
                entry.getKey().setActive(entry.getValue().isSelected());
            }
            // Persist the plugin states
            PluginStateManager.saveStates(PluginManager.getInstance().getPlugins());
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());

        setSize(400, 300);
        setLocationRelativeTo(owner);
    }
}
