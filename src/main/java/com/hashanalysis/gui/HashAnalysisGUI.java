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
        primaryStage.setTitle("Analiza hash funkcija");
        
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
        Tab resultsTab = new Tab("Rezultati");
        resultsTab.setClosable(false);
        resultsContainer = new VBox(10);
        resultsContainer.setPadding(new Insets(10));
        resultsTab.setContent(resultsContainer);
        
        // settings tab
        Tab settingsTab = new Tab("Podešavanja");
        settingsTab.setClosable(false);
        settingsTab.setContent(createSettingsPanel());
        
        // detailed results tab
        Tab detailedTab = new Tab("Detaljna analiza");
        detailedTab.setClosable(false);
        detailedTab.setContent(createDetailedResultsPanel());
        
        // report tab
        Tab serbianReportTab = new Tab("Izveštaj");
        serbianReportTab.setClosable(false);
        serbianReportTab.setContent(createSerbianReportPanel());
        
        tabPane.getTabs().addAll(resultsTab, detailedTab, serbianReportTab, settingsTab);
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

        JFXButton analyzeButton = new JFXButton("Analiziraj fajlove");
        analyzeButton.setGraphic(new FontIcon(MaterialDesignF.FILE_DOCUMENT));
        analyzeButton.getStyleClass().add("button-primary");
        analyzeButton.setOnAction(e -> analyzeFiles());

        JFXButton exportButton = new JFXButton("Export rezultata");
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
        Label iterationsLabel = new Label("Broj iteracija:");
        Spinner<Integer> iterationsSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 1000000, 10000, 1000));
        iterationsBox.getChildren().addAll(iterationsLabel, iterationsSpinner);

        // Threads setting
        HBox threadsBox = new HBox(10);
        threadsBox.setAlignment(Pos.CENTER_LEFT);
        Label threadsLabel = new Label("Threads (niti):");
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

        statusLabel = new Label("Spremno za analizu");
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
        xAxis.setLabel("Bit pozicija");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Broj bitova");
        bitDistChart = new BarChart<>(xAxis, yAxis);
        bitDistChart.setTitle("Analiza distribucije bitova");
        bitDistChart.setLegendVisible(true);
        bitDistChart.setAnimated(false);

        // Collision Analysis Section
        VBox collisionPanel = new VBox(10);
        collisionPanel.setPadding(new Insets(10));
        collisionTable.setEditable(false);
        collisionTable.setPlaceholder(new Label("Pokrenite analizu da biste videli rezultate testa kolizija"));
        
        // Hash Function: The name of the hash algorithm being analyzed
        TableColumn<CollisionData, String> hashCol = new TableColumn<>("Hash funkcija");
        hashCol.setCellValueFactory(data -> data.getValue().hashNameProperty());
        hashCol.setPrefWidth(200);

        // Collisions Found: The number of times two different inputs produced the same hash (collision) during testing
        TableColumn<CollisionData, Integer> collisionsCol = new TableColumn<>("Pronađene kolizije");
        collisionsCol.setCellValueFactory(data -> data.getValue().collisionsProperty().asObject());
        collisionsCol.setPrefWidth(150);

        // Collision Rate: The ratio of collisions to total tests (lower is better)
        TableColumn<CollisionData, Double> rateCol = new TableColumn<>("Stopa kolizija");
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
        TitledPane collisionPane = new TitledPane("Analiza kolizija", collisionPanel);
        collisionPane.setExpanded(true);

        // Avalanche Effect Section
        VBox avalanchePanel = new VBox(10);
        avalanchePanel.setPadding(new Insets(10));
        avalancheTable.setEditable(false);
        avalancheTable.setPlaceholder(new Label("Pokrenite analizu da biste videli rezultate testa avalanche efekta"));

        // Hash Function: The name of the hash algorithm being analyzed
        TableColumn<AvalancheData, String> hashCol2 = new TableColumn<>("Hash funkcija");
        hashCol2.setCellValueFactory(data -> data.getValue().hashNameProperty());
        hashCol2.setPrefWidth(200);

        // Avalanche Effect: The average proportion of output bits that change when a single input bit is flipped (higher is better)
        TableColumn<AvalancheData, Double> effectCol = new TableColumn<>("Avalanche efekat");
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

        TableColumn<AvalancheData, Double> stdDevCol = new TableColumn<>("Standardna devijacija");
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
        TitledPane avalanchePane = new TitledPane("Analiza avalanche efekta", avalanchePanel);
        avalanchePane.setExpanded(true);

        // Bit Distribution Section
        VBox bitDistPanel = new VBox(10);
        bitDistPanel.setPadding(new Insets(10));

        // placeholder label
        Label placeholder = new Label("Pokrenite analizu da biste videli rezultate analize distribucije bitova");
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
        TitledPane bitDistPane = new TitledPane("Analiza distribucije bitova", bitDistPanel);
        bitDistPane.setExpanded(true);

        panel.getChildren().addAll(collisionPane, avalanchePane, bitDistPane);
        return panel;
    }

    private VBox createSerbianReportPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("detailed-results-panel");

        // Create text area for Serbian report
        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefRowCount(30);
        reportArea.setPrefColumnCount(80);
        reportArea.getStyleClass().add("report-text-area");

        // Update report content when results change
        results.addListener((ListChangeListener<HashBenchmark.BenchmarkResult>) change -> {
            StringBuilder report = new StringBuilder();
            report.append("=== IZVEŠTAJ O ANALIZI HASH FUNKCIJA ===\n\n");
            
            // Individual analysis for each hash function
            report.append("DETALJNA ANALIZA PO HASH FUNKCIJAMA:\n");
            report.append("=====================================\n\n");
            
            for (HashBenchmark.BenchmarkResult result : results) {
                report.append(String.format("Hash funkcija: %s\n", result.getHashName()));
                report.append(String.format("Brzina: %.2f ms\n", result.getSpeed()));
                report.append(String.format("Avalanche efekat: %.4f\n", result.getAvalancheEffect()));
                report.append(String.format("Stopa kolizija: %.6f\n", result.getCollisionRate()));
                report.append(String.format("Dužina hasha: %d bita\n", result.getHashLength()));
                
                // Add interpretation
                report.append("\nInterpretacija rezultata:\n");
                report.append("- Brzina: ");
                if (result.getSpeed() < 1.0) {
                    report.append("Izuzetno brza izvršavanja\n");
                } else if (result.getSpeed() < 5.0) {
                    report.append("Dobra brzina izvršavanja\n");
                } else {
                    report.append("Sporija izvršavanja\n");
                }
                
                report.append("- Avalanche efekat: ");
                if (result.getAvalancheEffect() > 0.5) {
                    report.append("Odličan avalanche efekat, mala promena ulaza dovodi do velike promene izlaza\n");
                } else if (result.getAvalancheEffect() > 0.3) {
                    report.append("Dobar avalanche efekat\n");
                } else {
                    report.append("Slabiji avalanche efekat\n");
                }
                
                report.append("- Stopa kolizija: ");
                if (result.getCollisionRate() < 0.0001) {
                    report.append("Izuzetno niska stopa kolizija\n");
                } else if (result.getCollisionRate() < 0.001) {
                    report.append("Prihvatljiva stopa kolizija\n");
                } else {
                    report.append("Povećana stopa kolizija\n");
                }
                
                report.append("\n");
            }

            // Overall analysis
            report.append("\nSVEUKUPNA ANALIZA I PREPORUKE:\n");
            report.append("==============================\n\n");
            
            // Find best performing hash function in each category
            HashBenchmark.BenchmarkResult fastest = results.stream()
                .min(Comparator.comparingDouble(HashBenchmark.BenchmarkResult::getSpeed))
                .orElse(null);
                
            HashBenchmark.BenchmarkResult bestAvalanche = results.stream()
                .max(Comparator.comparingDouble(HashBenchmark.BenchmarkResult::getAvalancheEffect))
                .orElse(null);
                
            HashBenchmark.BenchmarkResult lowestCollision = results.stream()
                .min(Comparator.comparingDouble(HashBenchmark.BenchmarkResult::getCollisionRate))
                .orElse(null);

            report.append("Najbolje performanse po kategorijama:\n");
            if (fastest != null) {
                report.append(String.format("- Najbrži algoritam: %s (%.2f ms)\n", 
                    fastest.getHashName(), fastest.getSpeed()));
            }
            if (bestAvalanche != null) {
                report.append(String.format("- Najbolji avalanche efekat: %s (%.4f)\n", 
                    bestAvalanche.getHashName(), bestAvalanche.getAvalancheEffect()));
            }
            if (lowestCollision != null) {
                report.append(String.format("- Najniža stopa kolizija: %s (%.6f)\n", 
                    lowestCollision.getHashName(), lowestCollision.getCollisionRate()));
            }

            // Overall recommendation
            report.append("\nPreporuka za korišćenje:\n");
            report.append("Za opštu upotrebu i sigurnost, preporučuje se korišćenje hash funkcije koja ");
            report.append("kombinuje dobru brzinu izvršavanja sa niskom stopom kolizija i dobrim avalanche efektom.\n");
            report.append("Na osnovu analize, najbolje performanse pokazuje kombinacija sledećih karakteristika:\n");
            report.append("1. Brzina izvršavanja ispod 5ms\n");
            report.append("2. Avalanche efekat iznad 0.4\n");
            report.append("3. Stopa kolizija ispod 0.0001\n\n");
            
            report.append("Za specifične slučajeve upotrebe:\n");
            report.append("- Za aplikacije koje zahtevaju maksimalnu brzinu: " + 
                (fastest != null ? fastest.getHashName() : "N/A") + "\n");
            report.append("- Za aplikacije koje zahtevaju maksimalnu sigurnost: " + 
                (lowestCollision != null ? lowestCollision.getHashName() : "N/A") + "\n");
            report.append("- Za aplikacije koje zahtevaju dobar avalanche efekat: " + 
                (bestAvalanche != null ? bestAvalanche.getHashName() : "N/A") + "\n");

            reportArea.setText(report.toString());
        });

        panel.getChildren().add(reportArea);
        return panel;
    }

    private void analyzeFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Izaberite fajlove za analizu");
        fileChooser.setInitialDirectory(new File("files"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            progressBar.setVisible(true);
            progressBar.setProgress(0);
            statusLabel.setText("Analiziram fajlove...");

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
                        statusLabel.setText("Analiza završena");
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
                        statusLabel.setText(String.format("Analiziram... (%.0f%%)", 100 * completedTasks[0] / totalTasks));
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
        speedChart.setPrefSize(500, 350); // Reverted to original size
        avalancheChart.setPrefSize(500, 350); // Reverted to original size
        collisionChart.setPrefSize(500, 350); // Reverted to original size

        // create container for charts
        HBox chartsBox = new HBox(20); // Reverted to HBox for horizontal layout
        chartsBox.setPadding(new Insets(20));
        chartsBox.setAlignment(Pos.CENTER);
        chartsBox.getChildren().addAll(speedChart, avalancheChart, collisionChart);

        // Add tooltips to charts
        addTooltipsToChart(speedChart, "ms");
        addTooltipsToChart(avalancheChart, "");
        addTooltipsToChart(collisionChart, "");

        // add charts to results container
        resultsContainer.getChildren().add(chartsBox);

        // update detailed results
        updateDetailedResults();
    }

    private void addTooltipsToChart(BarChart<String, Number> chart, String unit) {
        for (XYChart.Series<String, Number> series : chart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Tooltip tooltip = new Tooltip();
                tooltip.setText(String.format("%s: %.4f%s", 
                    data.getXValue(), 
                    data.getYValue().doubleValue(),
                    unit));
                Tooltip.install(data.getNode(), tooltip);
            }
        }
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

                // Write Serbian report
                writer.println("\n\n=== IZVEŠTAJ O ANALIZI HASH FUNKCIJA ===");
                writer.println("\nRezultati performansi:");
                for (HashBenchmark.BenchmarkResult result : results) {
                    writer.printf("\nHash funkcija: %s\n", result.getHashName());
                    writer.printf("Brzina: %.2f ms\n", result.getSpeed());
                    writer.printf("Avalanche efekat: %.4f\n", result.getAvalancheEffect());
                    writer.printf("Stopa kolizija: %.6f\n", result.getCollisionRate());
                    writer.printf("Dužina hasha: %d bita\n", result.getHashLength());
                }

                writer.println("\nRezultati testa kolizija:");
                for (CollisionData data : collisionTable.getItems()) {
                    writer.printf("\nHash funkcija: %s\n", data.hashNameProperty().get());
                    writer.printf("Pronađene kolizije: %d\n", data.collisionsProperty().get());
                    writer.printf("Stopa kolizija: %.6f\n", data.rateProperty().get());
                }

                writer.println("\nRezultati avalanche efekta:");
                for (AvalancheData data : avalancheTable.getItems()) {
                    writer.printf("\nHash funkcija: %s\n", data.hashNameProperty().get());
                    writer.printf("Avalanche efekat: %.4f\n", data.effectProperty().get());
                    writer.printf("Standardna devijacija: %.4f\n", data.stdDevProperty().get());
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