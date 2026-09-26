package br.com.hemopet.view;

import br.com.hemopet.dao.GeneralDataDao;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;

public class GeneralDataPanel extends JPanel {
    private final GeneralDataDao generalDataDao = new GeneralDataDao();
    private final JComboBox<String> viewField = new JComboBox<>();
    private final JTable table = new JTable();

    public GeneralDataPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);

        for (String viewName : generalDataDao.getViews().keySet()) {
            viewField.addItem(viewName);
        }

        JButton loadButton = new JButton("Carregar");
        loadButton.addActionListener(event -> loadSelectedView());

        JPanel top = UiUtils.card();
        top.add(new JLabel("Visualizacao"));
        top.add(viewField);
        top.add(loadButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        loadSelectedView();
    }

    private void loadSelectedView() {
        try {
            String viewName = (String) viewField.getSelectedItem();
            table.setModel(generalDataDao.list(viewName));
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }
}
