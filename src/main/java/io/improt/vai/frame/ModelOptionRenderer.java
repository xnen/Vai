package io.improt.vai.frame;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;

public class ModelOptionRenderer extends BasicComboBoxRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof ModelOption) {
            ModelOption option = (ModelOption) value;
            setText(option.label);
            if (!option.enabled) {
                setForeground(new Color(150, 150, 150));
            } else {
                setForeground(Color.black);
            }
        }
        return c;
    }
}
