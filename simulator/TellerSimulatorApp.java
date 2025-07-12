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
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class TellerSimulatorApp extends Application {

    private ComboBox<String> algoCombo;
    private TextField simTimeField, maxTransField, chanceField, tellersField, queueLimitField;
    private TextArea logArea;
    private BarChart<String, Number> utilizationChart;
    private Button startBtn;
    
    // AI Recommendation UI components
    private TextArea scenarioTextArea;
    private Button askAIButton;
    private Text aiRecommendationText;
    private GroqAIRecommender aiRecommender;

    @Override
    public void start(Stage primaryStage) {
        // Initialize AI recommender
        aiRecommender = new GroqAIRecommender();
        
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

        // AI Recommendation section
        scenarioTextArea = new TextArea();
        scenarioTextArea.setPromptText("Describe your banking scenario here...\n\nExample:\n\"We have a busy downtown branch with 5 tellers. Customers arrive frequently with varying transaction times. We want to minimize wait times while ensuring fair teller utilization.\"");
        scenarioTextArea.setPrefRowCount(6);
        scenarioTextArea.setWrapText(true);
        
        askAIButton = new Button("Ask AI to Choose Algorithm");
        askAIButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        
        aiRecommendationText = new Text("AI (Groq) will recommend the best algorithm for your scenario.");
        aiRecommendationText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        aiRecommendationText.setWrappingWidth(600);

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

        // AI Recommendation section
        VBox aiSection = new VBox(10);
        aiSection.setPadding(new Insets(10));
        aiSection.setStyle("-fx-border-color: #2196F3; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-color: #E3F2FD;");
        
        Text aiTitle = new Text("🤖 AI Algorithm Recommender (Groq)");
        aiTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        aiSection.getChildren().addAll(
            aiTitle,
            new Label("Describe your banking scenario:"),
            scenarioTextArea,
            askAIButton,
            aiRecommendationText
        );

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

        VBox root = new VBox(10, inputGrid, aiSection, new Label("Simulation Log:"), logArea, utilizationChart);
        root.setPadding(new Insets(10));

        startBtn.setOnAction(e -> startSimulation());
        askAIButton.setOnAction(e -> askAIForRecommendation());

        Scene scene = new Scene(root, 800, 900);
        primaryStage.setTitle("Teller Simulation with AI Recommender");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * Handles AI recommendation request.
     */
    private void askAIForRecommendation() {
        String scenario = scenarioTextArea.getText().trim();
        if (scenario.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, 
                "Please describe your banking scenario before asking for AI recommendation.", 
                ButtonType.OK);
            alert.showAndWait();
            return;
        }
        
        // Disable button and show loading state
        askAIButton.setDisable(true);
        askAIButton.setText("Fetching AI suggestion...");
        aiRecommendationText.setText("🤔 AI (Groq) is analyzing your scenario...");
        
        // Make AI recommendation request
        aiRecommender.recommendAlgorithm(scenario)
            .thenAcceptAsync(recommendation -> {
                Platform.runLater(() -> {
                    if (recommendation.isSuccess()) {
                        String displayName = GroqAIRecommender.mapAlgorithmToDisplayName(recommendation.getAlgorithm());
                        algoCombo.setValue(displayName);
                        
                        String recommendationText = String.format(
                            "✅ AI (Groq) Recommendation: %s\n\n💡 Explanation: %s\n\n🚀 Algorithm automatically selected and ready to run!",
                            displayName, recommendation.getExplanation()
                        );
                        aiRecommendationText.setText(recommendationText);
                        
                        // Show success dialog
                        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                            "AI (Groq) has recommended '" + displayName + "' for your scenario.\n\n" +
                            "The algorithm has been automatically selected. You can now run the simulation!",
                            ButtonType.OK);
                        alert.setTitle("AI Recommendation Complete");
                        alert.showAndWait();
                        
                    } else {
                        aiRecommendationText.setText("ERROR: Failed to get AI recommendation. Using default algorithm.");
                        
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                            "Failed to get AI recommendation. Please check your internet connection and API key.\n\n" +
                            "Using default algorithm (Greedy).",
                            ButtonType.OK);
                        alert.setTitle("AI Recommendation Failed");
                        alert.showAndWait();
                    }
                    
                    // Re-enable button
                    askAIButton.setDisable(false);
                    askAIButton.setText("Ask AI to Choose Algorithm");
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    aiRecommendationText.setText("ERROR: " + throwable.getMessage());
                    
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Error getting AI recommendation: " + throwable.getMessage() + "\n\n" +
                        "Please check your internet connection and API key configuration.",
                        ButtonType.OK);
                    alert.setTitle("AI Recommendation Error");
                    alert.showAndWait();
                    
                    askAIButton.setDisable(false);
                    askAIButton.setText("Ask AI to Choose Algorithm");
                });
                return null;
            });
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