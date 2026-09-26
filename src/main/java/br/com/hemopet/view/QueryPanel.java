package br.com.hemopet.view;

import br.com.hemopet.dao.QueryDao;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;

public class QueryPanel extends JPanel {
    private final QueryDao queryDao = new QueryDao();
    private final JComboBox<String> queryField = new JComboBox<>();
    private final JTable table = new JTable();

    public QueryPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);

        for (String queryName : queryDao.getQueryNames().keySet()) {
            queryField.addItem(queryName);
        }

        JButton executeButton = new JButton("Executar consulta");
        executeButton.addActionListener(event -> executeSelectedQuery());

        JPanel top = UiUtils.card();
        top.add(new JLabel("Consulta"));
        top.add(queryField);
        top.add(executeButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        executeSelectedQuery();
    }

    private void executeSelectedQuery() {
        try {
            String queryName = (String) queryField.getSelectedItem();
            table.setModel(queryDao.execute(queryName));
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }
}
