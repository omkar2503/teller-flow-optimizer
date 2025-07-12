package simulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class TellerSimulatorApp extends Application {

    private ComboBox<String> algoCombo;
    private TextField simTimeField, maxTransField, chanceField, tellersField, queueLimitField;
    private TextArea logArea;
    private BarChart<String, Number> utilizationChart;
    private Button startBtn;

    @Override
    public void start(Stage primaryStage) {
        // Algorithm selection
        algoCombo = new ComboBox<>();
        algoCombo.getItems().addAll("Greedy (Least Finish Time)", "Round Robin", "Least Work Left");
        algoCombo.getSelectionModel().selectFirst();

        // Parameter fields
        simTimeField = new TextField("100");
        maxTransField = new TextField("20");
        chanceField = new TextField("50");
        tellersField = new TextField("3");
        queueLimitField = new TextField("10");

        // Labels and layout
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setPadding(new Insets(10));
        inputGrid.add(new Label("Algorithm:"), 0, 0);
        inputGrid.add(algoCombo, 1, 0);
        inputGrid.add(new Label("Simulation Time:"), 0, 1);
        inputGrid.add(simTimeField, 1, 1);
        inputGrid.add(new Label("Max Transaction Time:"), 0, 2);
        inputGrid.add(maxTransField, 1, 2);
        inputGrid.add(new Label("New Customer Chance (%):"), 0, 3);
        inputGrid.add(chanceField, 1, 3);
        inputGrid.add(new Label("Number of Tellers:"), 0, 4);
        inputGrid.add(tellersField, 1, 4);
        inputGrid.add(new Label("Queue Limit:"), 0, 5);
        inputGrid.add(queueLimitField, 1, 5);

        startBtn = new Button("Start Simulation");
        inputGrid.add(startBtn, 1, 6);

        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(15);

        // BarChart for utilization
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Teller");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Utilization (%)");
        utilizationChart = new BarChart<>(xAxis, yAxis);
        utilizationChart.setTitle("Teller Utilization");

        VBox root = new VBox(10, inputGrid, new Label("Simulation Log:"), logArea, utilizationChart);
        root.setPadding(new Insets(10));

        startBtn.setOnAction(e -> startSimulation());

        Scene scene = new Scene(root, 700, 700);
        primaryStage.setTitle("Teller Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startSimulation() {
        // Input validation
        int simTime, maxTrans, chance, tellers, queueLimit;
        String algorithm = algoCombo.getValue();
        try {
            simTime = Integer.parseInt(simTimeField.getText());
            if (simTime < 1 || simTime > 10000) throw new NumberFormatException();
            maxTrans = Integer.parseInt(maxTransField.getText());
            if (maxTrans < 1 || maxTrans > 500) throw new NumberFormatException();
            chance = Integer.parseInt(chanceField.getText());
            if (chance < 1 || chance > 100) throw new NumberFormatException();
            tellers = Integer.parseInt(tellersField.getText());
            if (tellers < 1 || tellers > 10) throw new NumberFormatException();
            queueLimit = Integer.parseInt(queueLimitField.getText());
            if (queueLimit < 1 || queueLimit > 50) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter valid numbers in all fields (within allowed ranges).", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // Disable the button and show progress
        startBtn.setDisable(true);
        logArea.setText("Simulation running...");
        utilizationChart.getData().clear();

        Task<SimulationResult> task = new Task<>() {
            @Override
            protected SimulationResult call() {
                return TellerFlowOptimizer.runWithParams(
                    simTime, maxTrans, chance, tellers, queueLimit, algorithm
                );
            }
        };

        task.setOnSucceeded(e -> {
            SimulationResult result = task.getValue();
            logArea.setText(result.getLog());
            utilizationChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (int i = 0; i < result.getTellerUtilizations().size(); i++) {
                series.getData().add(new XYChart.Data<>("Teller " + (i+1), result.getTellerUtilizations().get(i)));
            }
            utilizationChart.getData().add(series);
            startBtn.setDisable(false);
        });

        task.setOnFailed(e -> {
            logArea.setText("Simulation failed: " + task.getException().getMessage());
            startBtn.setDisable(false);
        });

        new Thread(task).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 