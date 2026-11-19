package br.com.hemopet.view;

import br.com.hemopet.dao.DashboardDao;
import br.com.hemopet.model.DashboardFilter;
import br.com.hemopet.model.DashboardMetric;
import org.jfree.chart.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardPanel extends JPanel {
    private final JPanel content = new JPanel();
    private final JComboBox<String> species = new JComboBox<>(new String[]{"Todas", "CAO", "GATO"});
    private final JComboBox<String> authorized = new JComboBox<>(new String[]{"Todos", "Autorizados", "Nao autorizados"});
    private final JTextField start = new JTextField(10);
    private final JTextField end = new JTextField(10);
    private final JLabel state = new JLabel(" ");
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
    private int generation;
    private record Data(List<DashboardMetric> metrics, double percentage, Map<String, Double> stats,
                        List<Map<String, Integer>> groups, Map<String, Double> monthly) { }

    public DashboardPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        JPanel top = new JPanel(new GridLayout(0, 1, 4, 4));
        JPanel filters = UiUtils.card();
        filters.add(new JLabel("Especie")); filters.add(species);
        filters.add(new JLabel("Doacao")); filters.add(authorized);
        filters.add(new JLabel("De (AAAA-MM-DD)")); filters.add(start);
        filters.add(new JLabel("Ate (AAAA-MM-DD)")); filters.add(end);
        JButton apply = new JButton("Aplicar / atualizar");
        JButton clear = new JButton("Limpar");
        filters.add(apply); filters.add(clear);
        JPanel help = UiUtils.card();
        help.setLayout(new BorderLayout());
        help.add(new JLabel("<html>Especie e autorizacao filtram animais e seus vinculos. Periodo: coletas e solicitacoes; bolsas pela data da coleta.<br>"
                + "Pessoas, hospitais e veterinarios mostram totais gerais. Graficos: passe o mouse para detalhes; use o menu do botao direito para zoom e exportacao.</html>"), BorderLayout.CENTER);
        help.add(state, BorderLayout.EAST);
        top.add(filters); top.add(help);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UiUtils.BACKGROUND);
        JScrollPane scroll = new JScrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setBorder(null);
        add(top, BorderLayout.NORTH); add(scroll, BorderLayout.CENTER);
        apply.addActionListener(e -> refresh());
        clear.addActionListener(e -> {
            species.setSelectedIndex(0); authorized.setSelectedIndex(0);
            start.setText(""); end.setText(""); refresh();
        });
        refresh();
    }
    public void refresh() {
        try {
            DashboardFilter filter = new DashboardFilter(
                    species.getSelectedIndex() == 0 ? null : (String) species.getSelectedItem(),
                    authorized.getSelectedIndex() == 0 ? null : authorized.getSelectedIndex() == 1,
                    date(start), date(end));
            int request = ++generation;
            state.setText("Carregando...");
            UiUtils.loadAsync(this, () -> load(filter), data -> {
                if (request != generation) return;
                display(data);
                state.setText("Atualizado");
            }, error -> {
                if (request != generation) return;
                state.setText("Falha ao carregar");
                UiUtils.showError(this, error);
            });
        } catch (RuntimeException e) { UiUtils.showError(this, e); }
    }
    private static LocalDate date(JTextField field) {
        return field.getText().isBlank() ? null : LocalDate.parse(field.getText().trim());
    }
    private Data load(DashboardFilter filter) {
        DashboardDao dao = new DashboardDao(filter);
        return new Data(List.of(dao.countAnimals(), dao.countHospitals(), dao.countPeople(), dao.countVeterinarians(),
                dao.countCollections(), dao.countPendingRequests(), dao.countAvailableBags()),
                dao.percentageAuthorizedAnimals(), dao.getAnimalWeightStatistics(),
                List.of(dao.countAnimalsBySpecies(), dao.countBagsByStatus(), dao.countRequestsByUrgency(),
                        dao.countCollectionsByApprovalStatus(), dao.countAnimalsByWeightRange()),
                dao.sumCollectionVolumeByMonth());
    }
    private void display(Data data) {
        content.removeAll();
        JPanel metrics = new JPanel(new GridLayout(2, 4, 12, 12));
        for (DashboardMetric m : data.metrics()) metrics.add(card(m.label(), String.valueOf(m.value())));
        metrics.add(card("Animais autorizados", numberFormat.format(data.percentage()) + "%"));
        JPanel stats = new JPanel(new GridLayout(1, 5, 12, 12));
        String[] keys = {"media", "mediana", "moda", "variancia", "desvio_padrao"};
        String[] labels = {"Media (kg)", "Mediana (kg)", "Moda (kg)", "Variancia (kg²)", "Desvio padrao (kg)"};
        for (int i = 0; i < keys.length; i++) {
            Double value = data.stats().get(keys[i]);
            stats.add(card(labels[i], value == null ? "N/A" : numberFormat.format(value)));
        }
        JPanel charts = new JPanel(new GridLayout(0, 2, 12, 12));
        charts.add(bar("Animais por especie", data.groups().get(0)));
        charts.add(pie("Bolsas por status", data.groups().get(1)));
        charts.add(bar("Solicitacoes por urgencia", data.groups().get(2)));
        charts.add(pie("Coletas por aprovacao", data.groups().get(3)));
        charts.add(bar("Distribuicao dos pesos", data.groups().get(4)));
        DefaultCategoryDataset monthly = new DefaultCategoryDataset();
        data.monthly().forEach((label, value) -> monthly.addValue(value, "Volume (ml)", label));
        charts.add(chart(ChartFactory.createLineChart("Volume coletado por mes", "Periodo", "Volume (ml)", monthly)));
        for (JPanel panel : List.of(metrics, stats, charts)) {
            panel.setBackground(UiUtils.BACKGROUND);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(panel); content.add(Box.createVerticalStrut(12));
        }
        content.revalidate(); content.repaint();
    }
    private JPanel card(String label, String text) {
        JPanel card = UiUtils.card(); card.setLayout(new BorderLayout(4, 4));
        JLabel value = new JLabel(text);
        value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24)); value.setForeground(UiUtils.PRIMARY);
        card.add(value, BorderLayout.CENTER); card.add(new JLabel(label), BorderLayout.SOUTH);
        return card;
    }
    private JPanel bar(String title, Map<String, Integer> values) {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        values.forEach((label, value) -> data.addValue(value, "Total", label));
        return chart(ChartFactory.createBarChart(title, "", "Quantidade", data));
    }
    private JPanel pie(String title, Map<String, Integer> values) {
        DefaultPieDataset<String> data = new DefaultPieDataset<>();
        values.forEach(data::setValue);
        return chart(ChartFactory.createPieChart(title, data, true, true, false));
    }
    private JPanel chart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getPlot().setNoDataMessage("Sem dados para os filtros selecionados.");
        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(500, 300));
        panel.setMouseWheelEnabled(true);
        panel.setDisplayToolTips(true);
        JPanel container = UiUtils.card(); container.setLayout(new BorderLayout());
        container.add(panel, BorderLayout.CENTER);
        return container;
    }
}
