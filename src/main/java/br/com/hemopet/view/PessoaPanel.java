
package br.com.hemopet.view;

import br.com.hemopet.dao.PessoaDao;
import br.com.hemopet.model.Pessoa;
import br.com.hemopet.service.PessoaService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

public class PessoaPanel extends JPanel {

    private final PessoaDao dao = new PessoaDao();
    private final PessoaService service = new PessoaService();

    private final JTable table = new JTable();

    private final JTextField cpfField = new JTextField(12);
    private final JTextField nomeField = new JTextField(20);
    private final JTextField ruaField = new JTextField(20);
    private final JTextField numeroField = new JTextField(8);
    private final JTextField bairroField = new JTextField(15);
    private final JTextField cidadeField = new JTextField(15);
    private final JTextField cepField = new JTextField(10);

    private boolean editing = false;

    public PessoaPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);

        add(createForm(), BorderLayout.NORTH);
        add(new TableFilterPanel(table), BorderLayout.CENTER);

        refreshTable();
    }

    private JPanel createForm() {
        JPanel form = UiUtils.card();
        form.setLayout(new GridBagLayout());

        form.add(new JLabel("CPF"), UiUtils.constraints(0, 0));
        form.add(cpfField, UiUtils.constraints(1, 0));
        form.add(new JLabel("Nome"), UiUtils.constraints(2, 0));
        form.add(nomeField, UiUtils.constraints(3, 0));

        form.add(new JLabel("Rua"), UiUtils.constraints(0, 1));
        form.add(ruaField, UiUtils.constraints(1, 1));
        form.add(new JLabel("Numero"), UiUtils.constraints(2, 1));
        form.add(numeroField, UiUtils.constraints(3, 1));

        form.add(new JLabel("Bairro"), UiUtils.constraints(0, 2));
        form.add(bairroField, UiUtils.constraints(1, 2));
        form.add(new JLabel("Cidade"), UiUtils.constraints(2, 2));
        form.add(cidadeField, UiUtils.constraints(3, 2));

        form.add(new JLabel("CEP"), UiUtils.constraints(0, 3));
        form.add(cepField, UiUtils.constraints(1, 3));

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

        form.add(buttons, UiUtils.constraints(3, 3));

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
            Pessoa pessoa = readPessoa();

            if (editing) {
                service.update(pessoa);
            } else {
                service.create(pessoa);
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
                        "Selecione uma pessoa para excluir."
                );
            }

            int confirmation = JOptionPane.showConfirmDialog(
                    this,
                    "A exclusao pode afetar registros vinculados a esta pessoa. Deseja continuar?",
                    "Confirmar exclusao",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirmation != JOptionPane.YES_OPTION) {
                return;
            }

            service.delete(cpfField.getText().trim());

            clearForm();
            refreshTable();

        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private Pessoa readPessoa() {
        Pessoa pessoa = new Pessoa();

        pessoa.setCpf(cpfField.getText().trim());
        pessoa.setNome(nomeField.getText().trim());
        pessoa.setRua(ruaField.getText().trim());
        pessoa.setNumero(numeroField.getText().trim());
        pessoa.setBairro(bairroField.getText().trim());
        pessoa.setCidade(cidadeField.getText().trim());
        pessoa.setCep(cepField.getText().trim());

        return pessoa;
    }

    private void fillFormFromTable() {
        int row = table.convertRowIndexToModel(table.getSelectedRow());

        cpfField.setText(valueAt(row, 0));
        nomeField.setText(valueAt(row, 1));
        ruaField.setText(valueAt(row, 2));
        numeroField.setText(valueAt(row, 3));
        bairroField.setText(valueAt(row, 4));
        cidadeField.setText(valueAt(row, 5));
        cepField.setText(valueAt(row, 6));

        // CPF nao pode ser alterado durante a edicao
        cpfField.setEditable(false);

        editing = true;
    }

    private String valueAt(int row, int column) {
        Object value = table.getModel().getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private void clearForm() {
        editing = false;

        cpfField.setEditable(true);

        cpfField.setText("");
        nomeField.setText("");
        ruaField.setText("");
        numeroField.setText("");
        bairroField.setText("");
        cidadeField.setText("");
        cepField.setText("");

        table.clearSelection();
    }
}
