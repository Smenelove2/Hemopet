package br.com.hemopet.view;

import br.com.hemopet.service.Etapa5Service;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class Etapa5Panel extends JPanel {

    private final Etapa5Service service = new Etapa5Service();

    private final JTextField idAnimalField = new JTextField(8);
    private final JTextField idBolsaField = new JTextField(8);
    private final JTextField dataReferenciaField = new JTextField(10);
    private final JTextField idSolicitacaoField = new JTextField(8);

    private final JLabel idadeResult = new JLabel("-");
    private final JLabel validadeResult = new JLabel("-");
    private final JLabel descarteResult = new JLabel("-");

    private final JTable discardedTable = new JTable();
    private final JTable summaryTable = new JTable();
    private final JTable problemsTable = new JTable();
    private final JTable logTable = new JTable();

    public Etapa5Panel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Funcoes", createFunctionsPanel());
        tabs.addTab("Procedimentos", createProceduresPanel());
        tabs.addTab("Triggers", createTriggersPanel());

        add(tabs, BorderLayout.CENTER);

        refreshLogs();
        refreshDiscardedBags();
    }

private JPanel createFunctionsPanel() {
    JPanel panel = UiUtils.card();
    panel.setLayout(new GridBagLayout());

    panel.add(new JLabel("ID do animal"), UiUtils.constraints(0, 0));
    panel.add(idAnimalField, UiUtils.constraints(1, 0));

    JButton ageButton = new JButton("Consultar idade");
    ageButton.addActionListener(event -> consultAge());

    panel.add(ageButton, UiUtils.constraints(2, 0));
    panel.add(idadeResult, UiUtils.constraints(3, 0));

    panel.add(new JLabel("ID da bolsa"), UiUtils.constraints(0, 1));
    panel.add(idBolsaField, UiUtils.constraints(1, 1));

    JButton validityButton = new JButton("Consultar validade");
    validityButton.addActionListener(event -> consultValidity());

    panel.add(validityButton, UiUtils.constraints(2, 1));
    panel.add(validadeResult, UiUtils.constraints(3, 1));

    return panel;
}

private void consultAge() {
    try {
        int id = Integer.parseInt(idAnimalField.getText().trim());

        Integer idade = service.consultarIdadeAnimal(id);

        idadeResult.setText(
                idade == null ? "Animal inexistente" : idade + " anos"
        );

    } catch (RuntimeException exception) {
        UiUtils.showError(this, exception);
    }
}

private void consultValidity() {
    try {
        int id = Integer.parseInt(idBolsaField.getText().trim());

        String status = service.consultarStatusValidade(id);

        validadeResult.setText(status);

    } catch (RuntimeException exception) {
        UiUtils.showError(this, exception);
    }
}

private JPanel createProceduresPanel() {
    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBackground(UiUtils.BACKGROUND);

    JPanel actions = new JPanel();
    actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));

    JPanel discardCard = UiUtils.card();

    discardCard.add(new JLabel("Data de referencia (AAAA-MM-DD):"));
    discardCard.add(dataReferenciaField);

    JButton discardButton = new JButton("Descartar bolsas vencidas");
    discardButton.addActionListener(event -> discardExpiredBags());

    discardCard.add(discardButton);
    discardCard.add(descarteResult);

    JPanel requestCard = UiUtils.card();

    requestCard.add(new JLabel("ID da solicitacao:"));
    requestCard.add(idSolicitacaoField);

    JButton processButton = new JButton("Processar solicitacao");
    processButton.addActionListener(event -> processRequest());

    requestCard.add(processButton);

    actions.add(discardCard);
    actions.add(Box.createVerticalStrut(10));
    actions.add(requestCard);

    JPanel results = new JPanel(new GridLayout(1, 3, 8, 8));
    results.add(createTablePanel("Bolsas descartadas", discardedTable));
    results.add(createTablePanel("Resumo da solicitacao", summaryTable));
    results.add(createTablePanel("Problemas encontrados", problemsTable));

    panel.add(actions, BorderLayout.NORTH);
    panel.add(results, BorderLayout.CENTER);

    return panel;
}

private JPanel createTablePanel(String title, JTable table) {
    JPanel panel = UiUtils.card();
    panel.setLayout(new BorderLayout(5, 5));

    panel.add(new JLabel(title), BorderLayout.NORTH);
    panel.add(new JScrollPane(table), BorderLayout.CENTER);

    return panel;
}

private void discardExpiredBags() {
    try {
        String text = dataReferenciaField.getText().trim();

        LocalDate dataReferencia = text.isBlank()
                ? null
                : LocalDate.parse(text);

        int total = service.descartarBolsasVencidas(dataReferencia);

        descarteResult.setText(total + " bolsas descartadas.");

        refreshDiscardedBags();
        refreshLogs();

    } catch (RuntimeException exception) {
        UiUtils.showError(this, exception);
    }
}

private void processRequest() {
    try {
        int id = Integer.parseInt(idSolicitacaoField.getText().trim());

        List<javax.swing.table.DefaultTableModel> results =
                service.processarSolicitacao(id);

        summaryTable.setModel(results.get(0));
        problemsTable.setModel(results.get(1));

        refreshLogs();

        JOptionPane.showMessageDialog(
                this,
                "Solicitacao processada. Confira o resumo e os problemas encontrados.",
                "Processamento concluido",
                JOptionPane.INFORMATION_MESSAGE
        );

        refreshDiscardedBags();

    } catch (RuntimeException exception) {
        UiUtils.showError(this, exception);
    }
}
private JPanel createTriggersPanel() {
    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBackground(UiUtils.BACKGROUND);

    JPanel top = UiUtils.card();

    top.add(new JLabel("Historico de alteracoes de status das bolsas"));

    JButton refreshButton = new JButton("Atualizar historico");
    refreshButton.addActionListener(event -> refreshLogs());

    top.add(refreshButton);

    JButton testButton = new JButton("Demonstrar bloqueio de coleta");
    testButton.addActionListener(event -> {
        try {
            JOptionPane.showMessageDialog(this, service.testarTriggerColeta());
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    });
    top.add(testButton);

    panel.add(top, BorderLayout.NORTH);
    panel.add(new JScrollPane(logTable), BorderLayout.CENTER);

    return panel;
}

private void refreshLogs() {
    try {
        logTable.setModel(service.listLogBolsa());
    } catch (RuntimeException exception) {
        UiUtils.showError(this, exception);
    }
}

private void refreshDiscardedBags() {
    try {
        discardedTable.setModel(service.listBolsasDescartadas());
    } catch (RuntimeException exception) {
        UiUtils.showError(this, exception);
    }
}
}
