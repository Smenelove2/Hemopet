package br.com.hemopet.view;

import br.com.hemopet.dao.AnimalDoadorDao;
import br.com.hemopet.model.AnimalDoador;
import br.com.hemopet.model.OptionItem;
import br.com.hemopet.service.AnimalDoadorService;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AnimalDoadorPanel extends JPanel {
    private final AnimalDoadorDao dao = new AnimalDoadorDao();
    private final AnimalDoadorService service = new AnimalDoadorService();
    private final JTable table = new JTable();
    private final JTextField idField = new JTextField(8);
    private final JTextField nomeField = new JTextField(20);
    private final JComboBox<String> especieField = new JComboBox<>(new String[]{"CAO", "GATO"});
    private final JTextField racaField = new JTextField(20);
    private final JTextField tipoSanguineoField = new JTextField(20);
    private final JTextField pesoField = new JTextField(8);
    private final JTextField dataNascimentoField = new JTextField(10);
    private final JCheckBox autorizacaoField = new JCheckBox("Autorizado");
    private final JComboBox<OptionItem> tutorField = new JComboBox<>();

    public AnimalDoadorPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);
        idField.setEditable(false);
        add(createForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        loadTutors();
        refreshTable();
    }

    private JPanel createForm() {
        JPanel form = UiUtils.card();
        form.setLayout(new GridBagLayout());

        form.add(new JLabel("ID"), UiUtils.constraints(0, 0));
        form.add(idField, UiUtils.constraints(1, 0));
        form.add(new JLabel("Nome"), UiUtils.constraints(2, 0));
        form.add(nomeField, UiUtils.constraints(3, 0));
        form.add(new JLabel("Especie"), UiUtils.constraints(4, 0));
        form.add(especieField, UiUtils.constraints(5, 0));

        form.add(new JLabel("Raca"), UiUtils.constraints(0, 1));
        form.add(racaField, UiUtils.constraints(1, 1));
        form.add(new JLabel("Tipo sanguineo"), UiUtils.constraints(2, 1));
        form.add(tipoSanguineoField, UiUtils.constraints(3, 1));
        form.add(new JLabel("Peso"), UiUtils.constraints(4, 1));
        form.add(pesoField, UiUtils.constraints(5, 1));

        form.add(new JLabel("Nascimento"), UiUtils.constraints(0, 2));
        form.add(dataNascimentoField, UiUtils.constraints(1, 2));
        form.add(new JLabel("Tutor"), UiUtils.constraints(2, 2));
        form.add(tutorField, UiUtils.constraints(3, 2));
        form.add(autorizacaoField, UiUtils.constraints(4, 2));

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
        form.add(buttons, UiUtils.constraints(5, 2));

        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                fillFormFromTable();
            }
        });

        return form;
    }

    private void loadTutors() {
        try {
            tutorField.removeAllItems();
            List<OptionItem> tutors = dao.listTutors();
            for (OptionItem tutor : tutors) {
                tutorField.addItem(tutor);
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
            service.save(readAnimal());
            clearForm();
            refreshTable();
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private void delete() {
        try {
            if (idField.getText().isBlank()) {
                throw new IllegalArgumentException("Selecione um animal para excluir.");
            }
            service.delete(Integer.parseInt(idField.getText()));
            clearForm();
            refreshTable();
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private AnimalDoador readAnimal() {
        AnimalDoador animal = new AnimalDoador();
        if (!idField.getText().isBlank()) {
            animal.setIdAnimal(Integer.parseInt(idField.getText()));
        }
        animal.setNome(nomeField.getText().trim());
        animal.setEspecie((String) especieField.getSelectedItem());
        animal.setRaca(racaField.getText().trim());
        animal.setTipoSanguineo(tipoSanguineoField.getText().trim());
        animal.setPeso(new BigDecimal(pesoField.getText().trim().replace(",", ".")));
        animal.setDataNascimento(LocalDate.parse(dataNascimentoField.getText().trim()));
        animal.setAutorizacaoDoacao(autorizacaoField.isSelected());
        OptionItem tutor = (OptionItem) tutorField.getSelectedItem();
        animal.setCpfTutor(tutor == null ? null : tutor.id());
        return animal;
    }

    private void fillFormFromTable() {
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        idField.setText(valueAt(row, 0));
        nomeField.setText(valueAt(row, 1));
        especieField.setSelectedItem(valueAt(row, 2));
        racaField.setText(valueAt(row, 3));
        tipoSanguineoField.setText(valueAt(row, 4));
        pesoField.setText(valueAt(row, 5));
        dataNascimentoField.setText(valueAt(row, 6));
        autorizacaoField.setSelected(Boolean.parseBoolean(valueAt(row, 7)) || "1".equals(valueAt(row, 7)));
        selectTutor(valueAt(row, 8));
    }

    private void selectTutor(String cpfTutor) {
        for (int index = 0; index < tutorField.getItemCount(); index++) {
            if (tutorField.getItemAt(index).id().equals(cpfTutor)) {
                tutorField.setSelectedIndex(index);
                return;
            }
        }
    }

    private String valueAt(int row, int column) {
        Object value = table.getModel().getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private void clearForm() {
        idField.setText("");
        nomeField.setText("");
        especieField.setSelectedIndex(0);
        racaField.setText("");
        tipoSanguineoField.setText("");
        pesoField.setText("");
        dataNascimentoField.setText("2020-01-01");
        autorizacaoField.setSelected(true);
        if (tutorField.getItemCount() > 0) {
            tutorField.setSelectedIndex(0);
        }
    }
}
