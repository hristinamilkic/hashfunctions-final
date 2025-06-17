package com.hashanalysis.gui;

import com.hashanalysis.benchmark.HashBenchmark;
import com.hashanalysis.hash.HashFunction;
import com.hashanalysis.hash.implementations.HashImplementations;
import com.hashanalysis.test.*;
import com.jfoenix.controls.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HashAnalysisGUI extends Application {
    private final HashBenchmark benchmark;
    private final List<HashFunction> hashFunctions;
    private TabPane tabPane;
    private ProgressBar progressBar;
    private Label statusLabel;
    private VBox resultsContainer;
    private ObservableList<HashBenchmark.BenchmarkResult> results;
    private Map<String, List<CollisionTest.CollisionTestResult>> collisionResults;
    private Map<String, List<AvalancheTest.AvalancheTestResult>> avalancheResults;
    private TableView<CollisionData> collisionTable;
    private TableView<AvalancheData> avalancheTable;
    private BarChart<String, Number> bitDistChart;

    public HashAnalysisGUI() {
        benchmark = new HashBenchmark();
        hashFunctions = Arrays.asList(HashImplementations.getAllImplementations());
        results = FXCollections.observableArrayList();
        collisionResults = new HashMap<>();
        avalancheResults = new HashMap<>();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hash Functions Analysis");
        
        // main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("main-layout");
        
        // toolbar
        HBox toolbar = createToolbar();
        mainLayout.setTop(toolbar);
        
        // content area
        tabPane = new TabPane();
        tabPane.getStyleClass().add("content-tabs");
        
        // results tab
        Tab resultsTab = new Tab("Results");
        resultsTab.setClosable(false);
        resultsContainer = new VBox(10);
        resultsContainer.setPadding(new Insets(10));
        resultsTab.setContent(resultsContainer);
        
        // settings tab
        Tab settingsTab = new Tab("Settings");
        settingsTab.setClosable(false);
        settingsTab.setContent(createSettingsPanel());
        
        // detailed results tab
        Tab detailedTab = new Tab("Detailed Analysis");
        detailedTab.setClosable(false);
        detailedTab.setContent(createDetailedResultsPanel());
        
        tabPane.getTabs().addAll(resultsTab, detailedTab, settingsTab);
        mainLayout.setCenter(tabPane);
        
        // status bar
        HBox statusBar = createStatusBar();
        mainLayout.setBottom(statusBar);
        
        // scene
        Scene scene = new Scene(mainLayout, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");

        JFXButton analyzeButton = new JFXButton("Analyze Files");
        analyzeButton.setGraphic(new FontIcon(MaterialDesignF.FILE_DOCUMENT));
        analyzeButton.getStyleClass().add("button-primary");
        analyzeButton.setOnAction(e -> analyzeFiles());

        JFXButton exportButton = new JFXButton("Export Results");
        exportButton.setGraphic(new FontIcon(MaterialDesignF.FILE_EXPORT));
        exportButton.getStyleClass().add("button-secondary");
        exportButton.setOnAction(e -> exportResults());

        toolbar.getChildren().addAll(analyzeButton, exportButton);
        return toolbar;
    }

    private VBox createSettingsPanel() {
        VBox settingsPanel = new VBox(20);
        settingsPanel.setPadding(new Insets(20));
        settingsPanel.getStyleClass().add("settings-panel");

        // Iterations setting
        HBox iterationsBox = new HBox(10);
        iterationsBox.setAlignment(Pos.CENTER_LEFT);
        Label iterationsLabel = new Label("Iterations:");
        Spinner<Integer> iterationsSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 1000000, 10000, 1000));
        iterationsBox.getChildren().addAll(iterationsLabel, iterationsSpinner);

        // Threads setting
        HBox threadsBox = new HBox(10);
        threadsBox.setAlignment(Pos.CENTER_LEFT);
        Label threadsLabel = new Label("Threads:");
        Spinner<Integer> threadsSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 4, 1));
        threadsBox.getChildren().addAll(threadsLabel, threadsSpinner);

        settingsPanel.getChildren().addAll(iterationsBox, threadsBox);
        return settingsPanel;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");

        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");

        statusBar.getChildren().addAll(progressBar, statusLabel);
        return statusBar;
    }

    private VBox createDetailedResultsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("detailed-results-panel");

        // tables and chart
        collisionTable = new TableView<>();
        avalancheTable = new TableView<>();
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Bit Position");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Bit Count");
        bitDistChart = new BarChart<>(xAxis, yAxis);
        bitDistChart.setTitle("Bit Distribution Analysis");
        bitDistChart.setLegendVisible(true);
        bitDistChart.setAnimated(false);

        // Collision Analysis Section
        VBox collisionPanel = new VBox(10);
        collisionPanel.setPadding(new Insets(10));
        collisionTable.setEditable(false);
        collisionTable.setPlaceholder(new Label("Run analysis to see collision test results"));
        
        // Hash Function: The name of the hash algorithm being analyzed
        TableColumn<CollisionData, String> hashCol = new TableColumn<>("Hash Function");
        hashCol.setCellValueFactory(data -> data.getValue().hashNameProperty());
        hashCol.setPrefWidth(200);

        // Collisions Found: The number of times two different inputs produced the same hash (collision) during testing
        TableColumn<CollisionData, Integer> collisionsCol = new TableColumn<>("Collisions Found");
        collisionsCol.setCellValueFactory(data -> data.getValue().collisionsProperty().asObject());
        collisionsCol.setPrefWidth(150);

        // Collision Rate: The ratio of collisions to total tests (lower is better)
        TableColumn<CollisionData, Double> rateCol = new TableColumn<>("Collision Rate");
        rateCol.setCellValueFactory(data -> data.getValue().rateProperty().asObject());
        rateCol.setPrefWidth(150);
        rateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double rate, boolean empty) {
                super.updateItem(rate, empty);
                if (empty || rate == null) {
                    setText(null);
                } else {
                    setText(String.format("%.6f", rate));
                }
            }
        });

        collisionTable.getColumns().addAll(hashCol, collisionsCol, rateCol);
        collisionPanel.getChildren().add(collisionTable);
        TitledPane collisionPane = new TitledPane("Collision Analysis", collisionPanel);
        collisionPane.setExpanded(true);

        // Avalanche Effect Section
        VBox avalanchePanel = new VBox(10);
        avalanchePanel.setPadding(new Insets(10));
        avalancheTable.setEditable(false);
        avalancheTable.setPlaceholder(new Label("Run analysis to see avalanche effect results"));

        // Hash Function: The name of the hash algorithm being analyzed
        TableColumn<AvalancheData, String> hashCol2 = new TableColumn<>("Hash Function");
        hashCol2.setCellValueFactory(data -> data.getValue().hashNameProperty());
        hashCol2.setPrefWidth(200);

        // Avalanche Effect: The average proportion of output bits that change when a single input bit is flipped (higher is better)
        TableColumn<AvalancheData, Double> effectCol = new TableColumn<>("Avalanche Effect");
        effectCol.setCellValueFactory(data -> data.getValue().effectProperty().asObject());
        effectCol.setPrefWidth(150);
        effectCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double effect, boolean empty) {
                super.updateItem(effect, empty);
                if (empty || effect == null) {
                    setText(null);
                } else {
                    setText(String.format("%.4f", effect));
                }
            }
        });

        // Standard Deviation: The variability of the avalanche effect (lower means more consistent avalanche behavior)
        TableColumn<AvalancheData, Double> stdDevCol = new TableColumn<>("Standard Deviation");
        stdDevCol.setCellValueFactory(data -> data.getValue().stdDevProperty().asObject());
        stdDevCol.setPrefWidth(150);
        stdDevCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double stdDev, boolean empty) {
                super.updateItem(stdDev, empty);
                if (empty || stdDev == null) {
                    setText(null);
                } else {
                    setText(String.format("%.4f", stdDev));
                }
            }
        });

        avalancheTable.getColumns().addAll(hashCol2, effectCol, stdDevCol);
        avalanchePanel.getChildren().add(avalancheTable);
        TitledPane avalanchePane = new TitledPane("Avalanche Effect Analysis", avalanchePanel);
        avalanchePane.setExpanded(true);

        // Bit Distribution Section
        VBox bitDistPanel = new VBox(10);
        bitDistPanel.setPadding(new Insets(10));

        // placeholder label
        Label placeholder = new Label("Run analysis to see bit distribution results");
        placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");

        // Use StackPane to overlay placeholder on chart
        StackPane chartContainer = new StackPane();
        chartContainer.getChildren().addAll(bitDistChart, placeholder);
        StackPane.setAlignment(placeholder, Pos.CENTER);

        // Bind placeholder visibility to chart data
        BooleanProperty hasData = new SimpleBooleanProperty(false);
        placeholder.visibleProperty().bind(hasData.not());
        bitDistChart.getData().addListener((ListChangeListener<XYChart.Series<String, Number>>) change -> {
            hasData.set(!bitDistChart.getData().isEmpty());
        });

        bitDistPanel.getChildren().add(chartContainer);
        TitledPane bitDistPane = new TitledPane("Bit Distribution Analysis", bitDistPanel);
        bitDistPane.setExpanded(true);

        panel.getChildren().addAll(collisionPane, avalanchePane, bitDistPane);
        return panel;
    }

    private void analyzeFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Analyze");
        fileChooser.setInitialDirectory(new File("files"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            progressBar.setVisible(true);
            progressBar.setProgress(0);
            statusLabel.setText("Analyzing files...");

            // clear previous results
            results.clear();
            collisionResults.clear();
            avalancheResults.clear();

            // Collect all analysis futures
            List<CompletableFuture<Void>> allFutures = new ArrayList<>();
            double totalTasks = selectedFiles.size() * hashFunctions.size();
            final double[] completedTasks = {0};

            for (File file : selectedFiles) {
                try {
                    Path filePath = file.toPath();
                    benchmark.runBenchmark(filePath);
                    allFutures.addAll(runAdvancedTests(filePath, 10000, 4, totalTasks, completedTasks));
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Error analyzing " + file.getName());
                        showError("Analysis Error", "Error analyzing file: " + file.getName(), ex);
                    });
                }
            }

            // Wait for all analyses to finish, then update UI
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        statusLabel.setText("Analysis complete");
                        displayResults();
                        updateDetailedResults();
                    });
                });
        }
    }

    private List<CompletableFuture<Void>> runAdvancedTests(Path filePath, int iterations, int threads, double totalTasks, double[] completedTasks) throws IOException {
        byte[] data = Files.readAllBytes(filePath);
        String fileName = filePath.getFileName().toString();

        collisionResults.put(fileName, new ArrayList<>());
        avalancheResults.put(fileName, new ArrayList<>());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (HashFunction function : hashFunctions) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Run collision tests
                    CollisionTest collisionTest = new CollisionTest(function, iterations, threads);
                    CollisionTest.CollisionTestResult collisionResult = collisionTest.runTests();
                    synchronized (collisionResults) {
                        collisionResults.get(fileName).add(collisionResult);
                    }

                    // Run avalanche tests
                    AvalancheTest avalancheTest = new AvalancheTest(function, iterations);
                    AvalancheTest.AvalancheTestResult avalancheResult = avalancheTest.runTests();
                    synchronized (avalancheResults) {
                        avalancheResults.get(fileName).add(avalancheResult);
                    }

                    Platform.runLater(() -> {
                        results.add(new HashBenchmark.BenchmarkResult(
                            function.getName(),
                            function.getSpeed(data),
                            function.getAvalancheEffect(data),
                            function.getHashLength(),
                            function.getCollisionRate(data)
                        ));
                        // Update progress bar
                        completedTasks[0]++;
                        progressBar.setProgress(completedTasks[0] / totalTasks);
                        statusLabel.setText(String.format("Analyzing... (%.0f%%)", 100 * completedTasks[0] / totalTasks));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showError("Test Error", "Error running tests for " + function.getName(), e);
                    });
                }
            });
            futures.add(future);
        }
        return futures;
    }

    private void displayResults() {
        resultsContainer.getChildren().clear();

        // Speed Chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Hash Function");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Speed (ms)");
        BarChart<String, Number> speedChart = new BarChart<>(xAxis, yAxis);
        speedChart.setTitle("Hash Speed Comparison");
        speedChart.setLegendVisible(false);
        speedChart.setAnimated(false);

        XYChart.Series<String, Number> speedSeries = new XYChart.Series<>();
        speedSeries.setName("Speed (ms)");

        // Avalanche Chart
        CategoryAxis xAxis2 = new CategoryAxis();
        xAxis2.setLabel("Hash Function");
        NumberAxis yAxis2 = new NumberAxis();
        yAxis2.setLabel("Avalanche Effect");
        BarChart<String, Number> avalancheChart = new BarChart<>(xAxis2, yAxis2);
        avalancheChart.setTitle("Avalanche Effect");
        avalancheChart.setLegendVisible(false);
        avalancheChart.setAnimated(false);

        XYChart.Series<String, Number> avalancheSeries = new XYChart.Series<>();
        avalancheSeries.setName("Avalanche Effect");

        // Collision Chart
        CategoryAxis xAxis3 = new CategoryAxis();
        xAxis3.setLabel("Hash Function");
        NumberAxis yAxis3 = new NumberAxis();
        yAxis3.setLabel("Collision Rate");
        BarChart<String, Number> collisionChart = new BarChart<>(xAxis3, yAxis3);
        collisionChart.setTitle("Collision Rate");
        collisionChart.setLegendVisible(false);
        collisionChart.setAnimated(false);

        XYChart.Series<String, Number> collisionSeries = new XYChart.Series<>();
        collisionSeries.setName("Collision Rate");

        // Add data to series
        for (HashBenchmark.BenchmarkResult result : results) {
            speedSeries.getData().add(new XYChart.Data<>(result.getHashName(), result.getSpeed()));
            avalancheSeries.getData().add(new XYChart.Data<>(result.getHashName(), result.getAvalancheEffect()));
            collisionSeries.getData().add(new XYChart.Data<>(result.getHashName(), result.getCollisionRate()));
        }

        // add series to charts
        speedChart.getData().add(speedSeries);
        avalancheChart.getData().add(avalancheSeries);
        collisionChart.getData().add(collisionSeries);

        // set chart sizes
        speedChart.setPrefSize(500, 350); // Larger chart
        avalancheChart.setPrefSize(500, 350); // Larger chart
        collisionChart.setPrefSize(500, 350); // Larger chart

        // create container for charts
        HBox chartsBox = new HBox(20);
        chartsBox.setPadding(new Insets(20));
        chartsBox.setAlignment(Pos.CENTER);
        chartsBox.getChildren().addAll(speedChart, avalancheChart, collisionChart);

        // add charts to results container
        resultsContainer.getChildren().add(chartsBox);

        // update detailed results
        updateDetailedResults();
    }

    private void updateDetailedResults() {
        // update collision analysis
        ObservableList<CollisionData> collisionData = FXCollections.observableArrayList();
        for (Map.Entry<String, List<CollisionTest.CollisionTestResult>> entry : collisionResults.entrySet()) {
            for (CollisionTest.CollisionTestResult result : entry.getValue()) {
                collisionData.add(new CollisionData(
                    result.hashFunction.getName(),
                    result.collisions,
                    result.collisionRate
                ));
            }
        }
        collisionTable.setItems(collisionData);

        // update avalanche analysis
        ObservableList<AvalancheData> avalancheData = FXCollections.observableArrayList();
        for (Map.Entry<String, List<AvalancheTest.AvalancheTestResult>> entry : avalancheResults.entrySet()) {
            for (AvalancheTest.AvalancheTestResult result : entry.getValue()) {
                avalancheData.add(new AvalancheData(
                    result.hashFunction.getName(),
                    result.avalancheEffect,
                    result.standardDeviation
                ));
            }
        }
        avalancheTable.setItems(avalancheData);

        // update bit distribution chart
        bitDistChart.getData().clear();

        for (HashFunction function : hashFunctions) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(function.getName());
            
            // calculating bit distribution
            byte[] testData = new byte[1024];
            new Random().nextBytes(testData);
            byte[] hash = function.hash(testData);
            
            int[] bitCounts = new int[8];
            for (byte b : hash) {
                for (int i = 0; i < 8; i++) {
                    if ((b & (1 << i)) != 0) {
                        bitCounts[i]++;
                    }
                }
            }
            
            for (int i = 0; i < 8; i++) {
                series.getData().add(new XYChart.Data<>("Bit " + i, bitCounts[i]));
            }
            
            bitDistChart.getData().add(series);
        }
    }

    private void exportResults() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Results");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("hash_analysis_results.csv");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // write header
                writer.println("Hash Function,Speed (ms),Avalanche Effect,Collision Rate,Hash Length");

                // write results
                for (HashBenchmark.BenchmarkResult result : results) {
                    writer.printf("%s,%.2f,%.4f,%.6f,%d%n",
                        result.getHashName(),
                        result.getSpeed(),
                        result.getAvalancheEffect(),
                        result.getCollisionRate(),
                        result.getHashLength()
                    );
                }

                // write collision test results
                writer.println("\nCollision Test Results");
                writer.println("Hash Function,Collisions Found,Collision Rate");
                for (CollisionData data : collisionTable.getItems()) {
                    writer.printf("%s,%d,%.6f%n",
                        data.hashNameProperty().get(),
                        data.collisionsProperty().get(),
                        data.rateProperty().get()
                    );
                }

                // write avalanche test results
                writer.println("\nAvalanche Effect Results");
                writer.println("Hash Function,Avalanche Effect,Standard Deviation");
                for (AvalancheData data : avalancheTable.getItems()) {
                    writer.printf("%s,%.4f,%.4f%n",
                        data.hashNameProperty().get(),
                        data.effectProperty().get(),
                        data.stdDevProperty().get()
                    );
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export Successful");
                    alert.setHeaderText(null);
                    alert.setContentText("Results have been exported to: " + file.getAbsolutePath());
                    alert.showAndWait();
                });
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    showError("Export Error", "Failed to export results", ex);
                });
            }
        }
    }

    private void showError(String title, String message, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message + "\n" + ex.getMessage());
        alert.showAndWait();
    }

    // data classes for detailed results
    public static class CollisionData {
        private final StringProperty hashName;
        private final IntegerProperty collisions;
        private final DoubleProperty rate;

        public CollisionData(String hashName, int collisions, double rate) {
            this.hashName = new SimpleStringProperty(hashName);
            this.collisions = new SimpleIntegerProperty(collisions);
            this.rate = new SimpleDoubleProperty(rate);
        }

        public StringProperty hashNameProperty() { return hashName; }
        public IntegerProperty collisionsProperty() { return collisions; }
        public DoubleProperty rateProperty() { return rate; }
    }

    public static class AvalancheData {
        private final StringProperty hashName;
        private final DoubleProperty effect;
        private final DoubleProperty stdDev;

        public AvalancheData(String hashName, double effect, double stdDev) {
            this.hashName = new SimpleStringProperty(hashName);
            this.effect = new SimpleDoubleProperty(effect);
            this.stdDev = new SimpleDoubleProperty(stdDev);
        }

        public StringProperty hashNameProperty() { return hashName; }
        public DoubleProperty effectProperty() { return effect; }
        public DoubleProperty stdDevProperty() { return stdDev; }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 