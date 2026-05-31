package monitoring.view;

import events.model.OperationalEvent;
import monitoring.model.AlertPanelModel;
import monitoring.model.BusMarker;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ControllerMonitoringView extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DefaultTableModel busTableModel;
    private final DefaultTableModel alertTableModel;
    private final DefaultTableModel eventTableModel;

    public ControllerMonitoringView() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        busTableModel = new DefaultTableModel(new Object[]{"Bus", "Route", "Latitude", "Longitude", "Speed", "Time"}, 0);
        JTable busTable = createTable(busTableModel);

        alertTableModel = new DefaultTableModel(new Object[]{"Level", "Title", "Message"}, 0);
        JTable alertTable = createTable(alertTableModel);

        eventTableModel = new DefaultTableModel(new Object[]{"Type", "Priority", "Source", "Bus", "Route", "Status"}, 0);
        JTable eventTable = createTable(eventTableModel);

        JSplitPane lowerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, wrap("Operational events", eventTable),
                wrap("Alerts", alertTable));
        lowerSplit.setResizeWeight(0.55);
        lowerSplit.setBorder(null);

        add(wrap("Active buses", busTable), BorderLayout.CENTER);
        add(lowerSplit, BorderLayout.SOUTH);
    }

    public void updateData(List<BusMarker> buses, List<AlertPanelModel> alerts, List<OperationalEvent> events) {
        busTableModel.setRowCount(0);
        for (BusMarker bus : buses) {
            busTableModel.addRow(new Object[]{
                    bus.getBusCode(),
                    String.valueOf(bus.getRouteId()),
                    String.format("%.6f", bus.getLatitude()),
                    String.format("%.6f", bus.getLongitude()),
                    bus.getSpeed() == null ? "N/A" : String.format("%.1f", bus.getSpeed()),
                    bus.getLastUpdate() == null ? "" : TIME_FORMATTER.format(bus.getLastUpdate())
            });
        }

        alertTableModel.setRowCount(0);
        for (AlertPanelModel alert : alerts) {
            alertTableModel.addRow(new Object[]{
                    alert.getLevel().name(),
                    alert.getTitle(),
                alert.getMessage()
            });
        }

        eventTableModel.setRowCount(0);
        for (OperationalEvent event : events) {
            eventTableModel.addRow(new Object[]{
                    event.getEventType().name(),
                    event.getPriority().name(),
                    event.getSourceType().name(),
                    event.getBusCode() == null ? "" : event.getBusCode(),
                    String.valueOf(event.getRouteId()),
                    event.getStatus().name()
            });
        }
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(30, 64, 175));
        table.getTableHeader().setForeground(Color.WHITE);
        return table;
    }

    private JPanel wrap(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)), title));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
