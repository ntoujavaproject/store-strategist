package bigproject;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

public class UIManager {

    private final Preferences prefs;
    private final Stage primaryStage;
    private final Scene mainScene;
    private final BorderPane mainLayout;
    private final Node mainCenterView; // The main ScrollPane view

    private static final String DEFAULT_FONT_FAMILY = "Serif";
    private static final double DEFAULT_FONT_SIZE = 12.0;
    private static final boolean DEFAULT_DARK_MODE = true;

    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_FONT_SIZE = "fontSize";
    private static final String KEY_DARK_MODE = "darkMode";

    // Flag to track if monthly report is showing
    private boolean isMonthlyReportShowing = false;
    private VBox monthlyReportView = null;
    
    // Flag to track if suggestions view is showing
    private boolean isSuggestionsShowing = false;
    
    // Interface for state change callbacks
    public interface StateChangeListener {
        void onMonthlyReportStateChanged(boolean isShowing);
        void onSuggestionsStateChanged(boolean isShowing);
    }
    
    private StateChangeListener stateChangeListener;
    
    /**
     * Sets a listener to be notified of state changes
     */
    public void setStateChangeListener(StateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    public UIManager(Preferences prefs, Stage primaryStage, Scene mainScene, BorderPane mainLayout, Node mainCenterView) {
        this.prefs = prefs;
        this.primaryStage = primaryStage;
        this.mainScene = mainScene;
        this.mainLayout = mainLayout;
        this.mainCenterView = mainCenterView;
    }

    public TextArea createStyledTextArea(String prompt, double prefHeight) {
        TextArea textArea = new TextArea();
        textArea.setPromptText(prompt);
        textArea.setEditable(false);
        textArea.setPrefHeight(prefHeight);
        textArea.setWrapText(true);
        VBox.setVgrow(textArea, Priority.SOMETIMES);
        return textArea;
    }

    public void showSettingsDialog() {
        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.WINDOW_MODAL);
        settingsStage.initOwner(primaryStage);
        settingsStage.setTitle("應用程式設定");

        VBox settingsLayout = new VBox(15);
        settingsLayout.setPadding(new Insets(20));
        settingsLayout.setAlignment(Pos.CENTER_LEFT);

        Label fontLabel = new Label("字型 (Font Family):");
        ComboBox<String> fontFamilyComboBox = new ComboBox<>();
        fontFamilyComboBox.getItems().addAll(Font.getFamilies());
        fontFamilyComboBox.setValue(prefs.get(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY));
        fontFamilyComboBox.setMaxWidth(Double.MAX_VALUE);

        Label sizeLabel = new Label("字體大小 (Font Size):");
        ComboBox<Double> fontSizeComboBox = new ComboBox<>();
        List<Double> fontSizes = Arrays.asList(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0);
        fontSizeComboBox.getItems().addAll(fontSizes);
        fontSizeComboBox.setValue(prefs.getDouble(KEY_FONT_SIZE, DEFAULT_FONT_SIZE));
        fontSizeComboBox.setMaxWidth(Double.MAX_VALUE);

        Label themeLabel = new Label("主題 (Theme):");
        ToggleButton themeToggle = new ToggleButton("深色模式 (Dark Mode)");
        themeToggle.setSelected(mainScene.getStylesheets().stream().anyMatch(s -> s.contains("modern_dark_theme.css")));

        Button applyButton = new Button("套用");
        applyButton.setOnAction(e -> {
            String selectedFontFamily = fontFamilyComboBox.getValue();
            double selectedFontSize = fontSizeComboBox.getValue();
            boolean useDarkMode = themeToggle.isSelected();

            updateFontStyle(selectedFontFamily, selectedFontSize);
            updateTheme(useDarkMode);

            prefs.put(KEY_FONT_FAMILY, selectedFontFamily);
            prefs.putDouble(KEY_FONT_SIZE, selectedFontSize);
            prefs.putBoolean(KEY_DARK_MODE, useDarkMode);
            System.out.println("Settings applied and saved.");

            settingsStage.close();
        });

        Button closeButton = new Button("關閉");
        closeButton.setOnAction(e -> settingsStage.close());

        HBox buttonBar = new HBox(15, applyButton, closeButton);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(20, 0, 0, 0));

        settingsLayout.getChildren().addAll(
            fontLabel, fontFamilyComboBox,
            sizeLabel, fontSizeComboBox,
            themeLabel, themeToggle,
            buttonBar
        );

        Scene settingsScene = new Scene(settingsLayout, 350, 300);
        if (mainScene.getStylesheets().stream().anyMatch(s -> s.contains("modern_dark_theme.css"))) {
             try {
                URL cssUrl = getClass().getResource("/bigproject/modern_dark_theme.css");
                if (cssUrl != null) {
                    settingsScene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    // System.err.println("Could not find modern_dark_theme.css for settings dialog.");
                }
            } catch (Exception ex) { /* Ignore */ }
        }
        settingsStage.setScene(settingsScene);
        settingsStage.showAndWait();
    }

    public void updateFontStyle(String fontFamily, double fontSize) {
        String style = String.format("-fx-font-family: \"%s\"; -fx-font-size: %.1fpt;",
                                   fontFamily.replace("\"", ""), fontSize);
        mainLayout.setStyle(style);
        System.out.println("Applied style: " + style);
    }

    public void updateTheme(boolean useDarkMode) {
        mainScene.getStylesheets().removeIf(s -> s.contains("dark_theme.css"));
        if (useDarkMode) {
            try {
                URL cssUrl = getClass().getResource("/bigproject/modern_dark_theme.css");
                if (cssUrl != null) {
                    mainScene.getStylesheets().add(cssUrl.toExternalForm());
                    System.out.println("Modern dark theme enabled.");
                } else {
                    // System.err.println("Could not find modern_dark_theme.css for enabling.");
                }
            } catch (Exception e) {
                // System.err.println("Error loading modern dark theme CSS: " + e.getMessage());
            }
        } else {
             System.out.println("Dark theme disabled (using default JavaFX Modena theme).");
        }
    }

    public void toggleMonthlyReport() {
        if (isMonthlyReportShowing) {
            // Hide the monthly report and show main content
            showMainView();
            isMonthlyReportShowing = false;
            // Make sure suggestions state is also updated
            isSuggestionsShowing = false;
        } else {
            // Show monthly report
            showInlineMonthlyReport();
            isMonthlyReportShowing = true;
            // Make sure suggestions state is also updated
            isSuggestionsShowing = false;
        }
        
        // Notify listener of state change
        if (stateChangeListener != null) {
            stateChangeListener.onMonthlyReportStateChanged(isMonthlyReportShowing);
            stateChangeListener.onSuggestionsStateChanged(isSuggestionsShowing);
        }
    }

    private void showInlineMonthlyReport() {
        // Create the monthly report content view if it doesn't exist
        if (monthlyReportView == null) {
            monthlyReportView = new VBox(15);
            monthlyReportView.setId("monthly-report-content");
            monthlyReportView.setPadding(new Insets(20));
            monthlyReportView.setAlignment(Pos.TOP_LEFT);
    
            Label reportTitle = new Label("月報 - 美味評分趨勢");
            reportTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
            reportTitle.getStyleClass().add("label-bright");
    
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis(0, 5, 0.5);
            xAxis.setLabel("月份");
            yAxis.setLabel("平均美味評分 (顆星)");
    
            BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setTitle("近六個月美味評分");
            barChart.setLegendVisible(false);
    
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("美味評分");
            Random random = new Random();
            YearMonth currentMonth = YearMonth.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
    
            for (int i = 5; i >= 0; i--) {
                YearMonth month = currentMonth.minusMonths(i);
                double rating = 3.0 + random.nextDouble() * 1.8;
                series.getData().add(new XYChart.Data<>(month.format(formatter), rating));
            }
            barChart.getData().add(series);
    
            Button exportButton = new Button("匯出報告 (PNG)");
            exportButton.setOnAction(e -> exportChart(primaryStage, barChart));
    
            HBox buttonBar = new HBox(15, exportButton);
            buttonBar.setAlignment(Pos.CENTER_RIGHT);
            buttonBar.setPadding(new Insets(20, 0, 0, 0));
    
            monthlyReportView.getChildren().addAll(reportTitle, barChart, buttonBar);
        }
    
        ScrollPane reportScrollPane = new ScrollPane(monthlyReportView);
        reportScrollPane.setFitToWidth(true);
        reportScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        reportScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
    
        mainLayout.setCenter(reportScrollPane);
        System.out.println("Switched to Monthly Report View");
    }

    // Replace the existing showMonthlyReport method with a popup version (for backup)
    public void showMonthlyReportPopup() {
        Stage reportStage = new Stage();
        reportStage.initModality(Modality.WINDOW_MODAL);
        reportStage.initOwner(primaryStage);
        reportStage.setTitle("月報 - 美味評分趨勢");

        VBox reportLayout = new VBox(15);
        reportLayout.setPadding(new Insets(20));
        reportLayout.setAlignment(Pos.CENTER);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 5, 0.5);
        xAxis.setLabel("月份");
        yAxis.setLabel("平均美味評分 (顆星)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("近六個月美味評分");
        barChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("美味評分");
        Random random = new Random();
        YearMonth currentMonth = YearMonth.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            double rating = 3.0 + random.nextDouble() * 1.8;
            series.getData().add(new XYChart.Data<>(month.format(formatter), rating));
        }
        barChart.getData().add(series);

        Button exportButton = new Button("匯出報告 (PNG)");
        exportButton.setOnAction(e -> exportChart(reportStage, barChart));

        Button closeButton = new Button("關閉");
        closeButton.setOnAction(e -> reportStage.close());

        HBox buttonBar = new HBox(15, exportButton, closeButton);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(20, 0, 0, 0));

        reportLayout.getChildren().addAll(barChart, buttonBar);

        Scene reportScene = new Scene(reportLayout, 600, 450);
        if (prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)) {
             try {
                URL cssUrl = getClass().getResource("/bigproject/modern_dark_theme.css");
                if (cssUrl != null) {
                    reportScene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                     System.err.println("Could not find modern_dark_theme.css for report dialog.");
                }
            } catch (Exception ex) { /* Ignore */ }
        }
        reportStage.setScene(reportScene);
        reportStage.showAndWait();
    }

    public void exportChart(Stage owner, BarChart<String, Number> chart) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("儲存圖表為 PNG");
        String defaultFileName = String.format("美味評分報告_%s.png",
                                              YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM")));
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Images", "*.png")
        );

        File file = fileChooser.showSaveDialog(owner);

        if (file != null) {
            try {
                WritableImage writableImage = chart.snapshot(null, null);
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                if (ImageIO.write(renderedImage, "png", file)) {
                     System.out.println("Chart exported successfully to: " + file.getAbsolutePath());
                 } else {
                     showErrorDialog("匯出失敗", "無法寫入 PNG 檔案。");
                 }
            } catch (IOException ex) {
                showErrorDialog("匯出錯誤", "儲存圖表時發生錯誤: " + ex.getMessage());
            } catch (Exception ex) {
                 showErrorDialog("匯出錯誤", "儲存圖表時發生未預期的錯誤: " + ex.getMessage());
            }
        } else {
            System.out.println("Chart export cancelled by user.");
        }
    }

    public void showErrorDialog(String title, String message) {
         Alert alert = new Alert(Alert.AlertType.ERROR);
         alert.setTitle(title);
         alert.setHeaderText(null);
         alert.setContentText(message);
         DialogPane dialogPane = alert.getDialogPane();
         if (prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)) {
              try {
                 URL cssUrl = getClass().getResource("/bigproject/modern_dark_theme.css");
                 if (cssUrl != null) {
                     dialogPane.getStylesheets().add(cssUrl.toExternalForm());
                     dialogPane.getStyleClass().add("root");
                 } else {
                     // System.err.println("Could not find modern_dark_theme.css for error dialog.");
                 }
             } catch (Exception ex) { /* Ignore */ }
         }
         alert.showAndWait();
     }

    /**
     * Returns whether the monthly report is currently showing
     */
    public boolean isMonthlyReportShowing() {
        return isMonthlyReportShowing;
    }
    
    /**
     * Returns whether the suggestions view is currently showing
     */
    public boolean isSuggestionsShowing() {
        return isSuggestionsShowing;
    }

    public void toggleSuggestionsView() {
        // Check if we're already showing suggestions
        isSuggestionsShowing = false;
        if (mainLayout.getCenter() instanceof ScrollPane && 
            ((ScrollPane) mainLayout.getCenter()).getContent() instanceof VBox && 
            ((VBox)((ScrollPane) mainLayout.getCenter()).getContent()).getId() != null && 
            ((VBox)((ScrollPane) mainLayout.getCenter()).getContent()).getId().equals("suggestions-content")) {
            isSuggestionsShowing = true;
        }
        
        if (isSuggestionsShowing) {
            // Currently showing suggestions view, switch back to main content
            showMainView(); 
            isSuggestionsShowing = false;
            // Make sure monthly report state is also updated
            isMonthlyReportShowing = false;
        } else {
            // Currently showing main content, switch to suggestions
            showSuggestionsView();
            isSuggestionsShowing = true;
            // Make sure monthly report state is also updated
            isMonthlyReportShowing = false;
        }
        
        // Notify listener of state change
        if (stateChangeListener != null) {
            stateChangeListener.onMonthlyReportStateChanged(isMonthlyReportShowing);
            stateChangeListener.onSuggestionsStateChanged(isSuggestionsShowing);
        }
    }

    private void showSuggestionsView() {
        VBox suggestionsContent = new VBox(15);
        suggestionsContent.setId("suggestions-content"); // Add ID for identification
        suggestionsContent.setPadding(new Insets(20));
        suggestionsContent.setAlignment(Pos.TOP_LEFT);

        Label suggestionsTitle = new Label("歷月經營建議");
        suggestionsTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        suggestionsTitle.getStyleClass().add("label-bright");

        suggestionsContent.getChildren().add(suggestionsTitle);
        for (int i = 4; i >= 1; i--) {
             VBox monthBox = new VBox(5);
             Label monthLabel = new Label(String.format("2024年 %02d月", i));
             monthLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
             monthLabel.getStyleClass().add("label-bright");
             Label suggestionText = new Label(" - 建議 " + i + ": 增加優惠活動吸引新客。\n - 建議 " + i + "B: 優化菜單，推出季節限定菜色。\n - 分析: 本月來客數較上月提升5%。");
             suggestionText.setWrapText(true);
             monthBox.getChildren().addAll(monthLabel, suggestionText);
             monthBox.setStyle("-fx-border-color: #555555; -fx-border-width: 0 0 1 0; -fx-padding: 10 0 10 0;"); // Use CSS classes later if preferred
             suggestionsContent.getChildren().add(monthBox);
        }

        ScrollPane suggestionsScrollPane = new ScrollPane(suggestionsContent);
        suggestionsScrollPane.setFitToWidth(true);
        suggestionsScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        suggestionsScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        mainLayout.setCenter(suggestionsScrollPane);
        // Ensure margin is set correctly if needed, though HBox structure might make this less critical
        // BorderPane.setMargin(suggestionsScrollPane, new Insets(0, 15, 0, 0)); 
        System.out.println("Switched to Suggestions View");
    }

    private void showMainView() {
        // If either monthly report or suggestions was showing, return to main view
        if (mainCenterView instanceof HBox) {
            // We have StackPane with HBox + floating search as the main view in compare.java
            if (mainCenterView.getParent() instanceof StackPane) {
                mainLayout.setCenter(mainCenterView.getParent());
            } else {
                mainLayout.setCenter(mainCenterView);
            }
            System.out.println("Switched back to Main Content View");
        } else {
            System.err.println("Error: Main center view is not the expected type.");
        }
    }
} 