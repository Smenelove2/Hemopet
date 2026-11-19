
package br.com.hemopet.view;

import br.com.hemopet.dao.DashboardDao;
import br.com.hemopet.model.DashboardMetric;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardPanel extends JPanel {

    private final DashboardDao dashboardDao = new DashboardDao();

    private final JPanel content = new JPanel();

    private final NumberFormat numberFormat =
            NumberFormat.getNumberInstance(new Locale("pt", "BR"));

    public DashboardPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);

        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);

        JButton refreshButton = new JButton("Atualizar");
        refreshButton.addActionListener(event -> loadDashboard());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UiUtils.BACKGROUND);

        JLabel title = new JLabel("Dashboard");
        title.setFont(UiUtils.titleFont());

        top.add(title, BorderLayout.WEST);
        top.add(refreshButton, BorderLayout.EAST);

        content.setLayout(new javax.swing.BoxLayout(
                content, javax.swing.BoxLayout.Y_AXIS));

        content.setBackground(UiUtils.BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(UiUtils.BACKGROUND);

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadDashboard();
    }

    private void loadDashboard() {
        try {
            content.removeAll();

            content.add(createMetricsPanel());
            content.add(createStatisticsPanel());
            content.add(createChartsPanel());

            content.revalidate();
            content.repaint();

        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    // -----------------------------------------------------
    // INDICADORES GERAIS
    // -----------------------------------------------------

    private JPanel createMetricsPanel() {

        JPanel panel = new JPanel(new GridLayout(2, 4, 12, 12));
        panel.setBackground(UiUtils.BACKGROUND);

        List<DashboardMetric> metrics = List.of(
                dashboardDao.countAnimals(),
                dashboardDao.countHospitals(),
                dashboardDao.countPeople(),
                dashboardDao.countVeterinarians(),
                dashboardDao.countCollections(),
                dashboardDao.countPendingRequests(),
                dashboardDao.countAvailableBags()
        );

        for (DashboardMetric metric : metrics) {
            panel.add(createMetricCard(
                    metric.label(),
                    String.valueOf(metric.value())
            ));
        }

        double percentage =
                dashboardDao.percentageAuthorizedAnimals();

        panel.add(createMetricCard(
                "Animais autorizados",
                formatNumber(percentage) + "%"
        ));

        return panel;
    }

    private JPanel createMetricCard(String label, String valueText) {

        JPanel card = UiUtils.card();
        card.setLayout(new BorderLayout(4, 4));

        JLabel value = new JLabel(valueText);
        value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        value.setForeground(UiUtils.PRIMARY);

        JLabel description = new JLabel(label);

        card.add(value, BorderLayout.CENTER);
        card.add(description, BorderLayout.SOUTH);

        return card;
    }

    // -----------------------------------------------------
    // MEDIDAS ESTATISTICAS
    // -----------------------------------------------------

    private JPanel createStatisticsPanel() {

        Map<String, Double> statistics =
                dashboardDao.getAnimalWeightStatistics();

        JPanel panel = new JPanel(new GridLayout(1, 5, 12, 12));
        panel.setBackground(UiUtils.BACKGROUND);

        panel.add(createMetricCard(
                "Media do peso (kg)",
                formatStatistic(statistics.get("media"))
        ));

        panel.add(createMetricCard(
                "Mediana (kg)",
                formatStatistic(statistics.get("mediana"))
        ));

        panel.add(createMetricCard(
                "Moda (kg)",
                formatStatistic(statistics.get("moda"))
        ));

        panel.add(createMetricCard(
                "Variancia (kg²)",
                formatStatistic(statistics.get("variancia"))
        ));

        panel.add(createMetricCard(
                "Desvio padrao (kg)",
                formatStatistic(statistics.get("desvio_padrao"))
        ));

        return panel;
    }

    private String formatStatistic(Double value) {
        return value == null ? "N/A" : formatNumber(value);
    }

    private String formatNumber(double value) {
        return numberFormat.format(value);
    }

    // -----------------------------------------------------
    // GRAFICOS
    // -----------------------------------------------------

    private JPanel createChartsPanel() {

        JPanel panel = new JPanel(new GridLayout(0, 2, 12, 12));
        panel.setBackground(UiUtils.BACKGROUND);

        // Grafico 1: animais por especie
        panel.add(createBarChart(
                "Animais por especie",
                "Quantidade",
                dashboardDao.countAnimalsBySpecies()
        ));

        // Grafico 2: bolsas por status
        panel.add(createPieChart(
                "Bolsas por status",
                dashboardDao.countBagsByStatus()
        ));

        // Grafico 3: solicitacoes por urgencia
        panel.add(createBarChart(
                "Solicitacoes por urgencia",
                "Quantidade",
                dashboardDao.countRequestsByUrgency()
        ));

        // Grafico 4: coletas por status de aprovacao
        panel.add(createPieChart(
                "Coletas por status de aprovacao",
                dashboardDao.countCollectionsByApprovalStatus()
        ));

        // Grafico 5: distribuicao dos pesos
        panel.add(createBarChart(
                "Distribuicao dos pesos dos animais",
                "Frequencia",
                dashboardDao.countAnimalsByWeightRange()
        ));

        // Grafico 6: volume coletado por mes
        panel.add(createLineChart(
                "Volume de coletas por mes",
                dashboardDao.sumCollectionVolumeByMonth()
        ));

        return panel;
    }

    // -----------------------------------------------------
    // GRAFICO DE BARRAS
    // -----------------------------------------------------

    private JPanel createBarChart(
            String title,
            String axisLabel,
            Map<String, Integer> values
    ) {

        DefaultCategoryDataset dataset =
                new DefaultCategoryDataset();

        values.forEach((label, value) ->
                dataset.addValue(value, "Total", label)
        );

        JFreeChart chart = ChartFactory.createBarChart(
                title,
                "",
                axisLabel,
                dataset
        );

        return createChartContainer(chart);
    }

    // -----------------------------------------------------
    // GRAFICO DE PIZZA
    // -----------------------------------------------------

    private JPanel createPieChart(
            String title,
            Map<String, Integer> values
    ) {

        DefaultPieDataset<String> dataset =
                new DefaultPieDataset<>();

        values.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(
                title,
                dataset,
                true,
                true,
                false
        );

        return createChartContainer(chart);
    }

    // -----------------------------------------------------
    // GRAFICO DE LINHA
    // -----------------------------------------------------

    private JPanel createLineChart(
            String title,
            Map<String, Double> values
    ) {

        DefaultCategoryDataset dataset =
                new DefaultCategoryDataset();

        values.forEach((month, volume) ->
                dataset.addValue(volume, "Volume (ml)", month)
        );

        JFreeChart chart = ChartFactory.createLineChart(
                title,
                "Periodo",
                "Volume (ml)",
                dataset
        );

        return createChartContainer(chart);
    }

    // -----------------------------------------------------
    // CONTAINER DOS GRAFICOS
    // -----------------------------------------------------

    private JPanel createChartContainer(JFreeChart chart) {

        chart.setBackgroundPaint(Color.WHITE);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));

        JPanel container = UiUtils.card();
        container.setLayout(new BorderLayout());
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        container.add(chartPanel, BorderLayout.CENTER);

        return container;
    }
}
