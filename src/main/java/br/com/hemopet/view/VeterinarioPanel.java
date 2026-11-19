
package br.com.hemopet.view;

import br.com.hemopet.dao.VeterinarioDao;
import br.com.hemopet.model.Veterinario;
import br.com.hemopet.model.OptionItem;
import br.com.hemopet.service.VeterinarioService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.List;

public class VeterinarioPanel extends JPanel {

    private final VeterinarioDao dao = new VeterinarioDao();
    private final VeterinarioService service = new VeterinarioService();

    private final JTable table = new JTable();

    private final JTextField cpfField = new JTextField(12);
    private final JTextField crmvField = new JTextField(15);

    private final JComboBox<OptionItem> supervisorField = new JComboBox<>();

    private boolean editing = false;

    public VeterinarioPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);

        add(createForm(), BorderLayout.NORTH);
        add(new TableFilterPanel(table), BorderLayout.CENTER);

        loadSupervisors(null);
        refreshTable();
    }

    private JPanel createForm() {
        JPanel form = UiUtils.card();
        form.setLayout(new GridBagLayout());

        form.add(new JLabel("CPF"), UiUtils.constraints(0, 0));
        form.add(cpfField, UiUtils.constraints(1, 0));

        form.add(new JLabel("CRMV"), UiUtils.constraints(2, 0));
        form.add(crmvField, UiUtils.constraints(3, 0));

        form.add(new JLabel("Supervisor"), UiUtils.constraints(4, 0));
        form.add(supervisorField, UiUtils.constraints(5, 0));

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

        form.add(buttons, UiUtils.constraints(5, 1));

        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                fillFormFromTable();
            }
        });

        return form;
    }

    private void loadSupervisors(String cpfAtual) {
        try {
            supervisorField.removeAllItems();

            // Opção para veterinários que não possuem supervisor
            supervisorField.addItem(new OptionItem("", "Sem supervisor"));

            List<OptionItem> supervisors = dao.listSupervisors(cpfAtual);

            for (OptionItem supervisor : supervisors) {
                supervisorField.addItem(supervisor);
            }

        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
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
            Veterinario veterinario = readVeterinario();

            if (editing) {
                service.update(veterinario);
            } else {
                service.create(veterinario);
            }

            clearForm();
            refreshTable();

        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private void delete() {
        try {
            if (!editing || cpfField.getText().isBlank()) {
                throw new IllegalArgumentException(
                        "Selecione um veterinario para excluir."
                );
            }

            service.delete(cpfField.getText().trim());

            clearForm();
            refreshTable();

        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private Veterinario readVeterinario() {
        Veterinario veterinario = new Veterinario();

        veterinario.setCpf(cpfField.getText().trim());
        veterinario.setCrmv(crmvField.getText().trim());

        OptionItem supervisor =
                (OptionItem) supervisorField.getSelectedItem();

        veterinario.setCpfSupervisor(
                supervisor == null || supervisor.id().isBlank()
                        ? null
                        : supervisor.id()
        );

        return veterinario;
    }

    private void fillFormFromTable() {
        int row = table.convertRowIndexToModel(table.getSelectedRow());

        cpfField.setText(valueAt(row, 0));
        crmvField.setText(valueAt(row, 2));

        // Impede a alteração do CPF durante a edição
        cpfField.setEditable(false);

        editing = true;

        // Recarrega os supervisores sem incluir o próprio veterinário
        loadSupervisors(cpfField.getText());

        selectSupervisor(valueAt(row, 3));
    }

    private void selectSupervisor(String cpfSupervisor) {
        if (cpfSupervisor == null || cpfSupervisor.isBlank()) {
            supervisorField.setSelectedIndex(0);
            return;
        }

        for (int index = 0; index < supervisorField.getItemCount(); index++) {
            if (supervisorField.getItemAt(index).id().equals(cpfSupervisor)) {
                supervisorField.setSelectedIndex(index);
                return;
            }
        }

        supervisorField.setSelectedIndex(0);
    }

    private String valueAt(int row, int column) {
        Object value = table.getModel().getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private void clearForm() {
        editing = false;

        cpfField.setEditable(true);

        cpfField.setText("");
        crmvField.setText("");

        loadSupervisors(null);
        supervisorField.setSelectedIndex(0);

        table.clearSelection();
    }
}
