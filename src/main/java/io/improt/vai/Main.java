package io.improt.vai;
import io.improt.vai.frame.ClientFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientFrame::new);
    }
}
