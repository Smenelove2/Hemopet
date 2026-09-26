package br.com.hemopet.view;

import br.com.hemopet.dao.HospitalVeterinarioDao;
import br.com.hemopet.model.HospitalVeterinario;
import br.com.hemopet.service.HospitalVeterinarioService;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;

public class HospitalVeterinarioPanel extends JPanel {
    private final HospitalVeterinarioDao dao = new HospitalVeterinarioDao();
    private final HospitalVeterinarioService service = new HospitalVeterinarioService();
    private final JTable table = new JTable();
    private final JTextField idField = new JTextField(8);
    private final JTextField cnpjField = new JTextField(18);
    private final JTextField nomeField = new JTextField(28);
    private final JCheckBox ativoField = new JCheckBox("Ativo");

    public HospitalVeterinarioPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);
        idField.setEditable(false);
        add(createForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        clearForm();
        refreshTable();
    }

    private JPanel createForm() {
        JPanel form = UiUtils.card();
        form.setLayout(new GridBagLayout());

        form.add(new JLabel("ID"), UiUtils.constraints(0, 0));
        form.add(idField, UiUtils.constraints(1, 0));
        form.add(new JLabel("CNPJ"), UiUtils.constraints(2, 0));
        form.add(cnpjField, UiUtils.constraints(3, 0));
        form.add(new JLabel("Nome fantasia"), UiUtils.constraints(4, 0));
        form.add(nomeField, UiUtils.constraints(5, 0));
        form.add(ativoField, UiUtils.constraints(6, 0));

        JButton saveButton = new JButton("Salvar");
        JButton newButton = new JButton("Novo");
        JButton deleteButton = new JButton("Excluir");
        JButton refreshButton = new JButton("Atualizar");
        saveButton.addActionListener(event -> save());
        newButton.addActionListener(event -> clearForm());
        deleteButton.addActionListener(event -> delete());
        refreshButton.addActionListener(event -> refreshTable());

        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(newButton);
        buttons.add(deleteButton);
        buttons.add(refreshButton);
        form.add(buttons, UiUtils.constraints(7, 0));

        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                fillFormFromTable();
            }
        });

        return form;
    }

    private void refreshTable() {
        try {
            table.setModel(dao.listTableModel());
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private void save() {
        try {
            service.save(readHospital());
            clearForm();
            refreshTable();
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private void delete() {
        try {
            if (idField.getText().isBlank()) {
                throw new IllegalArgumentException("Selecione um hospital para excluir.");
            }
            service.delete(Integer.parseInt(idField.getText()));
            clearForm();
            refreshTable();
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private HospitalVeterinario readHospital() {
        HospitalVeterinario hospital = new HospitalVeterinario();
        if (!idField.getText().isBlank()) {
            hospital.setIdHospital(Integer.parseInt(idField.getText()));
        }
        hospital.setCnpj(cnpjField.getText().trim());
        hospital.setNomeFantasia(nomeField.getText().trim());
        hospital.setAtivo(ativoField.isSelected());
        return hospital;
    }

    private void fillFormFromTable() {
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(valueAt(row, 0));
        cnpjField.setText(valueAt(row, 1));
        nomeField.setText(valueAt(row, 2));
        ativoField.setSelected(Boolean.parseBoolean(valueAt(row, 3)) || "1".equals(valueAt(row, 3)));
    }

    private String valueAt(int row, int column) {
        Object value = table.getModel().getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private void clearForm() {
        idField.setText("");
        cnpjField.setText("");
        nomeField.setText("");
        ativoField.setSelected(true);
    }
}
