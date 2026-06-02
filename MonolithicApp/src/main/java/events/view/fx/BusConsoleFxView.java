package events.view.fx;

import events.model.DriverEventRequest;
import events.model.EventProcessingResult;
import events.priority.EventPriorityAssigner;
import events.service.EventService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import monitoring.controller.MonitoringController;
import shared.enums.EventPriority;
import shared.enums.EventType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BusConsoleFxView {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final EventService eventService;
    private final MonitoringController monitoringController;
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

    private int knobIndex;
    private int selectedIndex;
    private final Stage stage = new Stage();
    private final Label eventLabel = new Label();
    private final Label selectedLabel = new Label();
    private final Label priorityLabel = new Label();
    private final Label confirmationLabel = new Label("Ready");
    private final Label lastEventLabel = new Label("No event sent yet");
    private final Label routePreviewLabel = new Label();
    private final TextField busCodeField = new TextField("513327");
    private final TextField routeIdField = new TextField("2241");
    private final TextArea descriptionArea = new TextArea();
    private final Button sendButton = new Button("Send");
    private BorderPane root;

    public BusConsoleFxView(EventService eventService) {
        this(eventService, null);
    }

    public BusConsoleFxView(EventService eventService, MonitoringController monitoringController) {
        this.eventService = eventService;
        this.monitoringController = monitoringController;
        routeIdField.textProperty().addListener((observable, oldValue, newValue) -> updateRoutePreview());
        configureWindow();
        updateEventPreview();
        updateRoutePreview();
        selectCurrentEvent();
    }

    public void showView() {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 500, 560);
            stage.setScene(scene);
        }
        stage.show();
        stage.toFront();
    }

    public Node getView() {
        return root;
    }

    private void configureWindow() {
        root = new BorderPane();
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #e2e8f0;");
        root.setTop(createHeader());
        root.setCenter(createConsolePanel());
        root.setBottom(createNavigationPanel());

        stage.setTitle("Bus Console Simulator");
        stage.setMinWidth(460);
        stage.setMinHeight(520);
    }

    private VBox createHeader() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(16, 18, 16, 18));
        header.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 4;");

        Label title = new Label("Bus Console Simulator");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        Label subtitle = new Label("Connected to EventService");
        subtitle.setTextFill(Color.web("#cbd5e1"));
        subtitle.setFont(Font.font("System", 12));

        header.getChildren().addAll(title, subtitle);
        BorderPane.setMargin(header, new Insets(0, 0, 14, 0));
        return header;
    }

    private VBox createConsolePanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        panel.getChildren().addAll(createFieldPanel(), createEventPanel(), createDescriptionPanel(), createResultPanel());
        return panel;
    }

    private GridPane createFieldPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(label("Bus code"), 0, 0);
        grid.add(busCodeField, 1, 0);
        grid.add(label("Route ID"), 0, 1);
        grid.add(routeIdField, 1, 1);
        routePreviewLabel.setTextFill(Color.web("#64748b"));
        routePreviewLabel.setFont(Font.font("System", 12));
        routePreviewLabel.setWrapText(true);
        grid.add(routePreviewLabel, 1, 2);
        GridPane.setHgrow(busCodeField, Priority.ALWAYS);
        GridPane.setHgrow(routeIdField, Priority.ALWAYS);
        return grid;
    }

    private VBox createEventPanel() {
        VBox panel = new VBox(7);
        panel.setPadding(new Insets(10, 0, 4, 0));

        Label caption = label("Console knob event");
        eventLabel.setTextFill(Color.web("#1e40af"));
        eventLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        selectedLabel.setTextFill(Color.web("#334155"));
        selectedLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        priorityLabel.setTextFill(Color.web("#0f172a"));
        priorityLabel.setFont(Font.font("System", FontWeight.BOLD, 15));

        panel.getChildren().addAll(caption, eventLabel, selectedLabel, priorityLabel);
        return panel;
    }

    private VBox createDescriptionPanel() {
        VBox panel = new VBox(8);
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);
        descriptionArea.setPromptText("Short description");
        panel.getChildren().addAll(label("Short description"), descriptionArea);
        return panel;
    }

    private VBox createResultPanel() {
        VBox panel = new VBox(7);
        confirmationLabel.setTextFill(Color.web("#334155"));
        confirmationLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        lastEventLabel.setTextFill(Color.web("#64748b"));
        lastEventLabel.setWrapText(true);
        panel.getChildren().addAll(label("Result"), confirmationLabel, lastEventLabel);
        return panel;
    }

    private HBox createNavigationPanel() {
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(14, 0, 0, 0));
        controls.setAlignment(Pos.CENTER);

        Button previousButton = button("Previous");
        Button nextButton = button("Next");
        Button selectButton = button("Select");
        stylePrimaryButton(sendButton);

        previousButton.setOnAction(event -> moveSelection(-1));
        nextButton.setOnAction(event -> moveSelection(1));
        selectButton.setOnAction(event -> selectCurrentEvent());
        sendButton.setOnAction(event -> sendEvent());

        controls.getChildren().addAll(previousButton, nextButton, selectButton, sendButton);
        return controls;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#334155"));
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        return label;
    }

    private Button button(String text) {
        Button button = new Button(text);
        stylePrimaryButton(button);
        return button;
    }

    private void stylePrimaryButton(Button button) {
        button.setTextFill(Color.WHITE);
        button.setFont(Font.font("System", FontWeight.BOLD, 13));
        button.setMinWidth(104);
        button.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 4; -fx-padding: 8 12 8 12;");
    }

    private void moveSelection(int direction) {
        knobIndex = knobIndex + direction;
        if (knobIndex < 0) {
            knobIndex = options.length - 1;
        }
        if (knobIndex >= options.length) {
            knobIndex = 0;
        }
        updateEventPreview();
        confirmationLabel.setText("Use Select to confirm this event");
        confirmationLabel.setTextFill(Color.web("#334155"));
    }

    private void selectCurrentEvent() {
        selectedIndex = knobIndex;
        updateEventPreview();
        confirmationLabel.setText("Selected with console knob controls");
        confirmationLabel.setTextFill(Color.web("#334155"));
    }

    private void updateEventPreview() {
        EventOption knobOption = options[knobIndex];
        EventOption selectedOption = options[selectedIndex];
        EventPriority priority = priorityAssigner.assign(knobOption.getEventType());
        eventLabel.setText(knobOption.getDisplayName());
        selectedLabel.setText("Selected event: " + selectedOption.getDisplayName());
        priorityLabel.setText("Estimated priority: " + priority.name());
    }

    private void sendEvent() {
        EventOption option = options[selectedIndex];
        String busCode = busCodeField.getText() == null ? "" : busCodeField.getText().trim();
        String routeText = routeIdField.getText() == null ? "" : routeIdField.getText().trim();

        if (busCode.isEmpty()) {
            showError("Bus code is required");
            return;
        }

        int routeId;
        try {
            routeId = Integer.parseInt(routeText);
        } catch (NumberFormatException exception) {
            showError("Route ID must be numeric");
            return;
        }

        DriverEventRequest request = new DriverEventRequest(busCode, routeId, option.getEventType().name(),
                descriptionArea.getText(), LocalDateTime.now());

        sendButton.setDisable(true);
        confirmationLabel.setText("Sending event...");
        confirmationLabel.setTextFill(Color.web("#334155"));

        Task<EventProcessingResult> task = new Task<EventProcessingResult>() {
            @Override
            protected EventProcessingResult call() {
                return eventService.processDriverEvent(request);
            }
        };

        task.setOnSucceeded(event -> {
            sendButton.setDisable(false);
            showResult(task.getValue(), option);
        });
        task.setOnFailed(event -> {
            sendButton.setDisable(false);
            showError("Event could not be sent");
        });

        Thread thread = new Thread(task, "bus-console-event-send");
        thread.setDaemon(true);
        thread.start();
    }

    private void showResult(EventProcessingResult result, EventOption option) {
        if (result != null && result.isSuccess()) {
            confirmationLabel.setText("Event sent successfully");
            confirmationLabel.setTextFill(Color.web("#15803d"));
        } else {
            confirmationLabel.setText(result == null ? "Event could not be sent" : result.getMessage());
            confirmationLabel.setTextFill(Color.web("#b91c1c"));
        }
        lastEventLabel.setText(option.getDisplayName() + " | bus " + busCodeField.getText().trim() +
                " | Route: " + routeFilterLabel(routeIdField.getText().trim()) + " | " +
                TIME_FORMATTER.format(LocalDateTime.now()));
    }

    private void showError(String message) {
        confirmationLabel.setText(message);
        confirmationLabel.setTextFill(Color.web("#b91c1c"));
    }

    private void updateRoutePreview() {
        String routeText = routeIdField.getText() == null ? "" : routeIdField.getText().trim();
        if (routeText.isEmpty()) {
            routePreviewLabel.setText("Recognized route: Route ID pending");
            return;
        }
        try {
            int routeId = Integer.parseInt(routeText);
            routePreviewLabel.setText("Recognized route: " + routeFullDisplayName(routeId));
        } catch (NumberFormatException exception) {
            routePreviewLabel.setText("Recognized route: Route ID must be numeric");
        }
    }

    private String routeFilterLabel(String routeText) {
        try {
            int routeId = Integer.parseInt(routeText);
            return monitoringController == null
                    ? "Route " + routeId
                    : monitoringController.getRouteFilterLabel(routeId);
        } catch (NumberFormatException exception) {
            return routeText;
        }
    }

    private String routeFullDisplayName(int routeId) {
        return monitoringController == null
                ? "Route " + routeId
                : monitoringController.getRouteFullDisplayName(routeId);
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
