package br.com.hemopet.view;

import br.com.hemopet.config.DatabaseConfig;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("HemoPet - Banco de Sangue Veterinario");
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        if (DatabaseConfig.canConnect()) {
            tabs.addTab("Dashboard", new DashboardPanel());
            tabs.addTab("Animais", new AnimalDoadorPanel());
            tabs.addTab("Hospitais", new HospitalVeterinarioPanel());
            tabs.addTab("Consultas", new QueryPanel());
            tabs.addTab("Dados Gerais", new GeneralDataPanel());
        } else {
            tabs.addTab("Conexao", createConnectionErrorPanel());
            JOptionPane.showMessageDialog(
                    this,
                    "Nao foi possivel conectar ao MySQL. Confira src/main/resources/db.properties ou as variaveis HEMOPET_DB_*.",
                    "Conexao com banco",
                    JOptionPane.WARNING_MESSAGE
            );
        }
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiUtils.PRIMARY);
        JLabel title = new JLabel("  HemoPet");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        JLabel subtitle = new JLabel("Banco de sangue veterinario  ");
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);
        return header;
    }

    private JPanel createConnectionErrorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiUtils.BACKGROUND);
        JLabel message = new JLabel(
                "<html><h2>Conexao nao configurada</h2><p>Confira o arquivo src/main/resources/db.properties e reinicie a aplicacao.</p></html>",
                SwingConstants.CENTER
        );
        message.setForeground(UiUtils.TEXT);
        panel.add(message, BorderLayout.CENTER);
        return panel;
    }
}
