package br.com.hemopet.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.util.Locale;

/** Busca literal nas tabelas, preservando a conversao de indices usada pelos formularios. */
public final class TableFilterPanel extends JPanel {
    private final JTable table;
    private final JTextField search = new JTextField(24);
    private final JLabel count = new JLabel();

    public TableFilterPanel(JTable table) {
        super(new BorderLayout(4, 4));
        this.table = table;
        JPanel toolbar = UiUtils.card();
        toolbar.add(new JLabel("Buscar na tabela")); toolbar.add(search);
        JButton clear = new JButton("Limpar"); toolbar.add(clear); toolbar.add(count);
        add(toolbar, BorderLayout.NORTH); add(new JScrollPane(table), BorderLayout.CENTER);
        table.setAutoCreateRowSorter(false);
        table.addPropertyChangeListener("model", e -> apply());
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { apply(); }
            public void removeUpdate(DocumentEvent e) { apply(); }
            public void changedUpdate(DocumentEvent e) { apply(); }
        });
        clear.addActionListener(e -> search.setText(""));
        apply();
    }
    private void apply() {
        String needle = search.getText().toLowerCase(Locale.ROOT);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        if (!needle.isBlank()) sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                for (int i = 0; i < entry.getValueCount(); i++)
                    if (entry.getStringValue(i).toLowerCase(Locale.ROOT).contains(needle)) return true;
                return false;
            }
        });
        table.setRowSorter(sorter);
        count.setText(table.getRowCount() + " / " + table.getModel().getRowCount() + " registros");
    }
}
