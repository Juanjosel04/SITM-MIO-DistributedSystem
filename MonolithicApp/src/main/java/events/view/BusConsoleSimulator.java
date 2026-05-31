package events.view;

import events.model.DriverEventRequest;
import events.model.EventProcessingResult;
import events.priority.EventPriorityAssigner;
import events.service.EventService;
import shared.enums.EventPriority;
import shared.enums.EventType;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;

public class BusConsoleSimulator extends JFrame {
    private final EventService eventService;
    private final EventPriorityAssigner priorityAssigner = new EventPriorityAssigner();
    private final EventOption[] options = new EventOption[]{
            new EventOption("Flat tire", EventType.FLAT_TIRE),
            new EventOption("Mechanical failure", EventType.MECHANICAL_FAILURE),
            new EventOption("Traffic jam", EventType.TRAFFIC_JAM),
            new EventOption("Collision", EventType.COLLISION),
            new EventOption("Door failure", EventType.DOOR_FAILURE),
            new EventOption("Security incident", EventType.SECURITY_INCIDENT),
            new EventOption("General event", EventType.GENERAL_OPERATIONAL_EVENT)
    };
    private int selectedIndex;
    private final JLabel eventLabel = new JLabel();
    private final JLabel priorityLabel = new JLabel();
    private final JLabel confirmationLabel = new JLabel("Ready");
    private final JTextField busCodeField = new JTextField("513327");
    private final JTextField routeIdField = new JTextField("2241");
    private final JTextArea descriptionArea = new JTextArea(3, 22);

    public BusConsoleSimulator(EventService eventService) {
        this.eventService = eventService;
        configureWindow();
        buildLayout();
        updateSelectedEvent();
    }

    public void showView() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setVisible(true);
            }
        });
    }

    private void configureWindow() {
        setTitle("Bus Console Simulator");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(460, 520));
        setLocationByPlatform(true);
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.setBackground(new Color(226, 232, 240));
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createConsolePanel(), BorderLayout.CENTER);
        root.add(createNavigationPanel(), BorderLayout.SOUTH);
        setContentPane(root);
        pack();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Driver Operational Console");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        return header;
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        panel.add(createFieldPanel());
        panel.add(createEventPanel());
        panel.add(createDescriptionPanel());
        return panel;
    }

    private JPanel createFieldPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setOpaque(false);
        panel.add(label("Bus code"));
        panel.add(busCodeField);
        panel.add(label("Route ID"));
        panel.add(routeIdField);
        return panel;
    }

    private JPanel createEventPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 6, 6));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));

        JLabel caption = label("Selected event");
        eventLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        eventLabel.setForeground(new Color(30, 64, 175));
        priorityLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        priorityLabel.setForeground(new Color(15, 23, 42));
        confirmationLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        confirmationLabel.setForeground(new Color(51, 65, 85));

        panel.add(caption);
        panel.add(eventLabel);
        panel.add(priorityLabel);
        panel.add(confirmationLabel);
        return panel;
    }

    private JPanel createDescriptionPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));
        panel.add(label("Short description"), BorderLayout.NORTH);
        panel.add(descriptionArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 10));
        panel.setOpaque(false);

        JButton previousButton = button("Previous");
        JButton nextButton = button("Next");
        JButton selectButton = button("Select");
        JButton sendButton = button("Send");

        previousButton.addActionListener(event -> moveSelection(-1));
        nextButton.addActionListener(event -> moveSelection(1));
        selectButton.addActionListener(event -> updateSelectedEvent());
        sendButton.addActionListener(event -> sendEvent());

        panel.add(previousButton);
        panel.add(nextButton);
        panel.add(selectButton);
        panel.add(sendButton);
        return panel;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(51, 65, 85));
        return label;
    }

    private JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(37, 99, 235));
        button.setFocusPainted(false);
        return button;
    }

    private void moveSelection(int direction) {
        selectedIndex = selectedIndex + direction;
        if (selectedIndex < 0) {
            selectedIndex = options.length - 1;
        }
        if (selectedIndex >= options.length) {
            selectedIndex = 0;
        }
        updateSelectedEvent();
    }

    private void updateSelectedEvent() {
        EventOption option = options[selectedIndex];
        EventPriority priority = priorityAssigner.assign(option.getEventType());
        eventLabel.setText(option.getDisplayName());
        priorityLabel.setText("Estimated priority: " + priority.name());
        confirmationLabel.setText("Selected with console knob controls");
    }

    private void sendEvent() {
        try {
            EventOption option = options[selectedIndex];
            DriverEventRequest request = new DriverEventRequest(busCodeField.getText().trim(),
                    Integer.parseInt(routeIdField.getText().trim()), option.getEventType().name(),
                    descriptionArea.getText(), LocalDateTime.now());
            EventProcessingResult result = eventService.processDriverEvent(request);
            confirmationLabel.setText(result.isSuccess() ? "Event sent successfully" : result.getMessage());
        } catch (RuntimeException exception) {
            confirmationLabel.setText("Event could not be sent");
        }
    }

    private static class EventOption {
        private final String displayName;
        private final EventType eventType;

        private EventOption(String displayName, EventType eventType) {
            this.displayName = displayName;
            this.eventType = eventType;
        }

        private String getDisplayName() {
            return displayName;
        }

        private EventType getEventType() {
            return eventType;
        }
    }
}
