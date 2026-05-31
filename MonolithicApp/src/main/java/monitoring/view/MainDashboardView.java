package monitoring.view;

import events.service.EventService;
import events.view.BusConsoleSimulator;
import monitoring.controller.MonitoringController;
import monitoring.map.MapPanel;
import monitoring.model.AlertPanelModel;
import monitoring.model.MonitoringMetric;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainDashboardView extends JFrame {
    private final MonitoringController controller;
    private final EventService eventService;
    private final Map<String, JLabel> metricValues = new LinkedHashMap<String, JLabel>();
    private final JLabel streamStatusLabel = new JLabel("Idle");
    private final MapPanel mapPanel = new MapPanel();
    private final ControllerMonitoringView controllerMonitoringView = new ControllerMonitoringView();
    private final JPanel alertPanel = new JPanel();

    public MainDashboardView(MonitoringController controller) {
        this(controller, null);
    }

    public MainDashboardView(MonitoringController controller, EventService eventService) {
        this.controller = controller;
        this.eventService = eventService;
        configureWindow();
        buildLayout();
        controller.addStateListener(new RunnableStateListener(this));
        refresh();
    }

    public void showView() {
        setVisible(true);
    }

    public void refresh() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateMetrics();
                mapPanel.updateMarkers(controller.getCurrentBuses());
                controllerMonitoringView.updateData(controller.getCurrentBuses(), controller.getAlerts(),
                        controller.getRecentEvents());
                updateAlerts(controller.getAlerts());
            }
        });
    }

    private void configureWindow() {
        setTitle("SITM-MIO Monitoring Center");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setLocationByPlatform(true);
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.setBackground(new Color(226, 232, 240));

        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createContent(), BorderLayout.CENTER);
        setContentPane(root);
        pack();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("SITM-MIO Monitoring Center");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);

        streamStatusLabel.setOpaque(true);
        streamStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        streamStatusLabel.setForeground(Color.WHITE);
        streamStatusLabel.setBackground(new Color(37, 99, 235));
        streamStatusLabel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        JButton consoleButton = new JButton("Open Bus Console");
        consoleButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        consoleButton.setForeground(new Color(15, 23, 42));
        consoleButton.setBackground(Color.WHITE);
        consoleButton.setFocusPainted(false);
        consoleButton.addActionListener(event -> openBusConsole());
        actions.add(consoleButton, BorderLayout.WEST);
        actions.add(streamStatusLabel, BorderLayout.EAST);

        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout(14, 14));
        content.setOpaque(false);
        content.add(createMetricsPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createMapSection(), createRightSection());
        splitPane.setResizeWeight(0.58);
        splitPane.setBorder(null);
        content.add(splitPane, BorderLayout.CENTER);
        return content;
    }

    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 7, 12, 12));
        panel.setOpaque(false);
        for (MonitoringMetric metric : controller.getMetrics()) {
            panel.add(createMetricCard(metric.getName(), metric.getValue(), metric.getDescription()));
        }
        return panel;
    }

    private JPanel createMetricCard(String name, String value, String description) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameLabel.setForeground(new Color(51, 65, 85));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        valueLabel.setForeground(new Color(30, 64, 175));

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        descriptionLabel.setForeground(new Color(100, 116, 139));

        metricValues.put(name, valueLabel);
        card.add(nameLabel);
        card.add(valueLabel);
        card.add(descriptionLabel);
        return card;
    }

    private JPanel createMapSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        panel.add(mapPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRightSection() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.add(controllerMonitoringView, BorderLayout.CENTER);

        alertPanel.setLayout(new BoxLayout(alertPanel, BoxLayout.Y_AXIS));
        alertPanel.setBackground(Color.WHITE);
        alertPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JScrollPane scrollPane = new JScrollPane(alertPanel);
        scrollPane.setPreferredSize(new Dimension(360, 150));
        panel.add(scrollPane, BorderLayout.SOUTH);
        return panel;
    }

    private void updateMetrics() {
        streamStatusLabel.setText(controller.getStreamStatus());
        for (MonitoringMetric metric : controller.getMetrics()) {
            JLabel valueLabel = metricValues.get(metric.getName());
            if (valueLabel != null) {
                valueLabel.setText(metric.getValue());
            }
        }
    }

    private void updateAlerts(List<AlertPanelModel> alerts) {
        alertPanel.removeAll();
        if (alerts.isEmpty()) {
            JLabel empty = new JLabel("No alerts yet");
            empty.setForeground(new Color(100, 116, 139));
            alertPanel.add(empty);
        } else {
            for (AlertPanelModel alert : alerts) {
                JLabel item = new JLabel(alert.getLevel().name() + "  " + alert.getTitle() + " - " + alert.getMessage());
                item.setFont(new Font("SansSerif", Font.PLAIN, 12));
                item.setForeground(new Color(15, 23, 42));
                item.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
                alertPanel.add(item);
            }
        }
        alertPanel.revalidate();
        alertPanel.repaint();
    }

    private void openBusConsole() {
        if (eventService == null) {
            return;
        }
        BusConsoleSimulator simulator = new BusConsoleSimulator(eventService);
        simulator.showView();
    }

    private static class RunnableStateListener implements monitoring.service.MonitoringStateListener {
        private final MainDashboardView view;

        private RunnableStateListener(MainDashboardView view) {
            this.view = view;
        }

        @Override
        public void onMonitoringStateChanged() {
            view.refresh();
        }
    }
}
