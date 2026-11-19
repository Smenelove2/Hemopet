package br.com.hemopet.view;

import br.com.hemopet.dao.QueryDao;
import br.com.hemopet.model.QueryFilter;
import javax.swing.*;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.Set;

public class QueryPanel extends JPanel {
    private final QueryDao dao = new QueryDao();
    private final JComboBox<String> query = new JComboBox<>();
    private final JTextField text = new JTextField(18);
    private final JComboBox<String> species = new JComboBox<>(new String[]{"Todas", "CAO", "GATO"});
    private final JComboBox<String> status = new JComboBox<>(new String[]{"Todos", "EM_ESTOQUE", "RESERVADA", "DISTRIBUIDA", "DESCARTADA"});
    private final JComboBox<String> urgency = new JComboBox<>(new String[]{"Todas", "ALTA", "MEDIA", "BAIXA"});
    private final JTextField start = new JTextField(10);
    private final JTextField end = new JTextField(10);
    private final JTable table = new JTable();
    private final JLabel count = new JLabel(" ");
    private int generation;

    public QueryPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);
        dao.getQueryNames().keySet().forEach(query::addItem);
        JPanel controls = new JPanel(new java.awt.GridLayout(0, 1, 4, 4));
        JPanel selection = UiUtils.card();
        selection.add(new JLabel("Consulta")); selection.add(query);
        JPanel filters = UiUtils.card();
        filters.add(new JLabel("Buscar")); filters.add(text);
        filters.add(new JLabel("Especie")); filters.add(species);
        filters.add(new JLabel("Status da bolsa")); filters.add(status);
        filters.add(new JLabel("Urgencia")); filters.add(urgency);
        JPanel dates = UiUtils.card();
        dates.add(new JLabel("De (AAAA-MM-DD)")); dates.add(start);
        dates.add(new JLabel("Ate (AAAA-MM-DD)")); dates.add(end);
        JButton apply = new JButton("Aplicar filtros");
        JButton clear = new JButton("Limpar");
        dates.add(apply); dates.add(clear); dates.add(count);
        controls.add(selection); controls.add(filters); controls.add(dates);
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.setAutoCreateRowSorter(true);
        text.addActionListener(e -> execute());
        apply.addActionListener(e -> execute());
        clear.addActionListener(e -> { reset(); execute(); });
        query.addActionListener(e -> { reset(); updateControls(); execute(); });
        updateControls();
        execute();
    }

    private void reset() {
        text.setText(""); start.setText(""); end.setText("");
        species.setSelectedIndex(0); status.setSelectedIndex(0); urgency.setSelectedIndex(0);
    }
    private void updateControls() {
        Set<String> supported = dao.getSupportedFilters((String) query.getSelectedItem());
        text.setEnabled(supported.contains("text")); species.setEnabled(supported.contains("species"));
        status.setEnabled(supported.contains("status")); urgency.setEnabled(supported.contains("urgency"));
        start.setEnabled(supported.contains("date")); end.setEnabled(supported.contains("date"));
        start.setToolTipText("Inclui o dia inteiro; vazio permite qualquer data.");
        end.setToolTipText(start.getToolTipText());
    }
    private static String selected(JComboBox<String> box) {
        return box.isEnabled() && box.getSelectedIndex() > 0 ? (String) box.getSelectedItem() : null;
    }
    private static LocalDate date(JTextField field) {
        return field.isEnabled() && !field.getText().isBlank() ? LocalDate.parse(field.getText().trim()) : null;
    }
    private void execute() {
        try {
            QueryFilter filter = new QueryFilter(text.getText(), selected(species), selected(status), selected(urgency), date(start), date(end));
            String name = (String) query.getSelectedItem();
            int request = ++generation;
            count.setText("Carregando...");
            UiUtils.loadAsync(this, () -> dao.execute(name, filter), result -> {
                if (request != generation) return;
                table.setModel(result);
                count.setText(result.getRowCount() + " registros");
            }, error -> {
                if (request != generation) return;
                count.setText("Falha ao carregar");
                UiUtils.showError(this, error);
            });
        } catch (RuntimeException e) { UiUtils.showError(this, e); }
    }
}
