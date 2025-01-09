package io.improt.vai;
import io.improt.vai.frame.Client;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
