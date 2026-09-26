package br.com.hemopet.app;

import br.com.hemopet.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class HemoPetApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
