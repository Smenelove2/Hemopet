package br.com.hemopet.view;

import br.com.hemopet.dao.DashboardDao;
import br.com.hemopet.model.DashboardMetric;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel {
    private final DashboardDao dashboardDao = new DashboardDao();
    private final JPanel content = new JPanel(new BorderLayout(12, 12));

    public DashboardPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiUtils.BACKGROUND);

        JButton refreshButton = new JButton("Atualizar");
        refreshButton.addActionListener(event -> loadDashboard());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UiUtils.BACKGROUND);
        JLabel title = new JLabel("Dashboard");
        title.setFont(UiUtils.titleFont());
        top.add(title, BorderLayout.WEST);
        top.add(refreshButton, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        loadDashboard();
    }

    private void loadDashboard() {
        try {
            content.removeAll();
            content.add(createMetricsPanel(), BorderLayout.NORTH);
            content.add(createChartsPanel(), BorderLayout.CENTER);
            content.revalidate();
            content.repaint();
        } catch (RuntimeException exception) {
            UiUtils.showError(this, exception);
        }
    }

    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 12));
        panel.setBackground(UiUtils.BACKGROUND);

        List<DashboardMetric> metrics = List.of(
                dashboardDao.countAnimals(),
                dashboardDao.countHospitals(),
                dashboardDao.countPendingRequests(),
                dashboardDao.countAvailableBags()
        );

        for (DashboardMetric metric : metrics) {
            JPanel card = UiUtils.card();
            card.setLayout(new BorderLayout());
            JLabel value = new JLabel(String.valueOf(metric.value()));
            value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            value.setForeground(UiUtils.PRIMARY);
            JLabel label = new JLabel(metric.label());
            card.add(value, BorderLayout.NORTH);
            card.add(label, BorderLayout.SOUTH);
            panel.add(card);
        }

        return panel;
    }

    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 12, 12));
        panel.setBackground(UiUtils.BACKGROUND);
        panel.add(createChart("Animais por especie", dashboardDao.countAnimalsBySpecies()));
        panel.add(createChart("Bolsas por status", dashboardDao.countBagsByStatus()));
        panel.add(createChart("Solicitacoes por urgencia", dashboardDao.countRequestsByUrgency()));
        return panel;
    }

    private ChartPanel createChart(String title, Map<String, Integer> values) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        values.forEach((label, value) -> dataset.addValue(value, "Total", label));
        JFreeChart chart = ChartFactory.createBarChart(title, "", "Quantidade", dataset);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        return new ChartPanel(chart);
    }
}
