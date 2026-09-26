package br.com.hemopet.view;

import br.com.hemopet.dao.DatabaseException;
import br.com.hemopet.service.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public final class UiUtils {
    public static final Color BACKGROUND = new Color(245, 247, 250);
    public static final Color PRIMARY = new Color(142, 24, 48);
    public static final Color TEXT = new Color(38, 38, 38);

    private UiUtils() {
    }

    public static GridBagConstraints constraints(int gridX, int gridY) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return constraints;
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    public static Font titleFont() {
        return new Font(Font.SANS_SERIF, Font.BOLD, 18);
    }

    public static void showError(JPanel parent, RuntimeException exception) {
        String message = exception.getMessage();
        if (exception instanceof DatabaseException && exception.getCause() != null) {
            message = message + "\n\nDetalhe: " + exception.getCause().getMessage();
        }
        if (exception instanceof ValidationException) {
            message = exception.getMessage();
        }
        JOptionPane.showMessageDialog(parent, message, "Atencao", JOptionPane.WARNING_MESSAGE);
    }
}
