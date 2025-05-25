package bigproject;

import javafx.application.Platform;
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
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
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
import java.util.function.Consumer;
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
    
    // Flag to track if restaurant not found view is showing
    private boolean isRestaurantNotFoundShowing = false;
    
    // Interface for state change callbacks
    public interface StateChangeListener {
        void onMonthlyReportStateChanged(boolean isShowing);
        void onSuggestionsStateChanged(boolean isShowing);
        void onSettingsStateChanged(boolean isShowing);
        void onRestaurantNotFoundStateChanged(boolean isShowing);
    }
    
    private StateChangeListener stateChangeListener;
    private Consumer<String> fullNameCollectCallback;
    private ProgressBar dataCollectionProgressBar;
    private Label dataCollectionStatusLabel;
    private VBox dataCollectionView;
    
    /**
     * Sets a listener to be notified of state changes
     */
    public void setStateChangeListener(StateChangeListener listener) {
        this.stateChangeListener = listener;
    }
    
    /**
     * Sets a callback to handle full restaurant name collection
     */
    public void setFullNameCollectCallback(Consumer<String> callback) {
        this.fullNameCollectCallback = callback;
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
        // 如果已經有 PreferencesManager，使用 PreferencesManager 來顯示設定
        if (mainLayout != null) {
            System.out.println("由於主程式已使用 PreferencesManager，不需再次開啟設定對話框");
            return;
        }
        
        // 舊的對話框顯示設定 (僅在 PreferencesManager 不可用時使用)
        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.initOwner(primaryStage);
        settingsStage.setTitle("應用程式設定");
        
        VBox settingsLayout = new VBox(15);
        settingsLayout.setPadding(new Insets(20));
        settingsLayout.setAlignment(Pos.CENTER);
        
        // Font Family Selection
        Label fontFamilyLabel = new Label("字型 (Font Family):");
        ComboBox<String> fontFamilyCombo = new ComboBox<>();
        fontFamilyCombo.getItems().addAll("System", "Serif", "SansSerif", "Monospaced", "Dialog");
        fontFamilyCombo.setValue(prefs.get(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY));
        
        // Font Size Selection
        Label fontSizeLabel = new Label("字體大小 (Font Size):");
        ComboBox<Double> fontSizeCombo = new ComboBox<>();
        fontSizeCombo.getItems().addAll(10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0);
        fontSizeCombo.setValue(prefs.getDouble(KEY_FONT_SIZE, DEFAULT_FONT_SIZE));
        
        // Theme Selection
        Label themeLabel = new Label("主題 (Theme):");
        ToggleButton darkModeToggle = new ToggleButton(prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE) ? "深色模式 (Dark Mode)" : "淺色模式 (Light Mode)");
        darkModeToggle.setSelected(prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE));
        darkModeToggle.setOnAction(e -> {
            boolean isSelected = darkModeToggle.isSelected();
            darkModeToggle.setText(isSelected ? "深色模式 (Dark Mode)" : "淺色模式 (Light Mode)");
        });
        
        // Apply Button
        Button applyButton = new Button("套用");
        applyButton.setOnAction(e -> {
            // Save settings
            prefs.put(KEY_FONT_FAMILY, fontFamilyCombo.getValue());
            prefs.putDouble(KEY_FONT_SIZE, fontSizeCombo.getValue());
            prefs.putBoolean(KEY_DARK_MODE, darkModeToggle.isSelected());
            
            // Apply settings
            updateFontStyle(fontFamilyCombo.getValue(), fontSizeCombo.getValue());
            updateTheme(darkModeToggle.isSelected());
            
            // Close dialog
            settingsStage.close();
        });
        
        // Cancel Button
        Button cancelButton = new Button("取消");
        cancelButton.setOnAction(e -> settingsStage.close());
        
        // Layout
        HBox buttonBox = new HBox(10, applyButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        settingsLayout.getChildren().addAll(
            fontFamilyLabel, fontFamilyCombo,
            fontSizeLabel, fontSizeCombo,
            themeLabel, darkModeToggle,
            buttonBox
        );
        
        Scene settingsScene = new Scene(settingsLayout, 300, 250);
        
        // Apply CSS
        if (prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)) {
            try {
                URL cssUrl = getClass().getResource("/bigproject/modern_dark_theme.css");
                if (cssUrl != null) {
                    settingsScene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        settingsStage.setScene(settingsScene);
        settingsStage.showAndWait();
    }

    public void updateFontStyle(String fontFamily, double fontSize) {
        // 先保存當前的樣式
        String currentStyle = mainLayout.getStyle();
        
        // 創建字體樣式
        String fontStyle = String.format("-fx-font-family: \"%s\"; -fx-font-size: %.1fpt;",
                                   fontFamily.replace("\"", ""), fontSize);
        
        // 檢查是否存在背景圖片設置
        if (currentStyle != null && currentStyle.contains("-fx-background-image")) {
            // 保留背景圖片和其他設置，但更新字體設置
            // 移除任何現有的字體設置
            String styleWithoutFont = currentStyle.replaceAll("-fx-font-family:[^;]+;", "")
                                                 .replaceAll("-fx-font-size:[^;]+;", "");
            
            // 添加新的字體設置
            mainLayout.setStyle(styleWithoutFont + " " + fontStyle);
        } else {
            // 如果沒有背景圖片設置，直接設置字體樣式
            mainLayout.setStyle(fontStyle);
        }
        
        System.out.println("Applied font style: " + fontStyle);
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

    public void showInfoDialog(String title, String message) {
         Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
                     // System.err.println("Could not find modern_dark_theme.css for info dialog.");
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

    /**
     * Returns whether the restaurant not found view is currently showing
     */
    public boolean isRestaurantNotFoundShowing() {
        return isRestaurantNotFoundShowing;
    }

    /**
     * Shows the restaurant not found view with collection options
     */
    public void showRestaurantNotFoundView(String query, Runnable collectAction, Runnable openMapAction) {
        showRestaurantNotFoundView(query, null, collectAction, openMapAction);
    }

    /**
     * Shows the restaurant not found view with collection options and found restaurant info
     */
    public void showRestaurantNotFoundView(String query, String foundRestaurantName, Runnable collectAction, Runnable openMapAction) {
        VBox notFoundContent = new VBox(20);
        notFoundContent.setId("restaurant-not-found-content");
        notFoundContent.setPadding(new Insets(40));
        notFoundContent.setAlignment(Pos.CENTER);

        // 標題
        Label titleLabel = new Label("餐廳未找到");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.getStyleClass().add("label-bright");
        titleLabel.setStyle("-fx-text-fill: #E67649;");

        // 主要信息
        Label messageLabel = new Label(String.format("在餐廳資料庫中找不到「%s」", query));
        messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        messageLabel.getStyleClass().add("label-bright");
        messageLabel.setWrapText(true);
        messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 說明文字
        Label descriptionLabel = new Label("由於搜尋詞可能不夠完整，建議您：");
        descriptionLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        descriptionLabel.getStyleClass().add("label-bright");
        descriptionLabel.setWrapText(true);

        // 建議步驟
        VBox stepsBox = new VBox(8);
        stepsBox.setAlignment(Pos.CENTER_LEFT);
        stepsBox.setStyle("-fx-background-color: rgba(220, 242, 204, 0.6); -fx-padding: 15; -fx-background-radius: 8;");
        
        Label step1 = new Label("1. 到 Google Maps 搜尋該餐廳");
        step1.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        
        Label step2 = new Label("2. 複製完整的餐廳名稱（例如：八方雲集 新竹金山店）");
        step2.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        
        Label step3 = new Label("3. 返回本系統，用完整名稱重新搜尋");
        step3.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        
        Label tip = new Label("💡 提示：完整名稱通常包含分店資訊，可幫助系統精確找到餐廳");
        tip.setStyle("-fx-text-fill: #1976D2; -fx-font-style: italic;");
        tip.setWrapText(true);
        
        stepsBox.getChildren().addAll(step1, step2, step3, tip);

        // 收集資料選項
        VBox collectOption = new VBox(12);
        collectOption.setAlignment(Pos.CENTER);
        collectOption.setStyle("-fx-background-color: rgba(255, 235, 210, 0.8); -fx-padding: 20; -fx-background-radius: 10;");
        
        Label collectTitle = new Label("📋 檢查資料庫並收集");
        collectTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        collectTitle.setStyle("-fx-text-fill: #F57C00;");
        
        Label collectDesc = new Label("請輸入完整的餐廳名稱（包含分店資訊）：");
        collectDesc.setWrapText(true);
        collectDesc.setStyle("-fx-text-fill: #F57C00; -fx-font-weight: bold;");
        
        // 添加輸入欄位
        TextField restaurantNameField = new TextField();
        restaurantNameField.setPromptText("例如：八方雲集 新竹金山店");
        restaurantNameField.setPrefWidth(300);
        restaurantNameField.setStyle("-fx-font-size: 14px; -fx-padding: 8;");
        
        Label inputTip = new Label("💡 請從 Google Maps 複製完整名稱貼上");
        inputTip.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px; -fx-font-style: italic;");
        
        Button collectButton = new Button("檢查並上傳餐廳資料");
        collectButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;");
        collectButton.setOnAction(e -> {
            String fullRestaurantName = restaurantNameField.getText().trim();
            if (fullRestaurantName.isEmpty()) {
                // 顯示提示
                Label errorLabel = new Label("⚠️ 請輸入餐廳名稱");
                errorLabel.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
                if (!collectOption.getChildren().contains(errorLabel)) {
                    collectOption.getChildren().add(collectOption.getChildren().size() - 1, errorLabel);
                    // 使用簡單的線程來移除錯誤提示
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            javafx.application.Platform.runLater(() -> {
                                collectOption.getChildren().remove(errorLabel);
                            });
                        } catch (InterruptedException ignored) {}
                    }).start();
                }
                return;
            }
            
            if (collectAction != null) {
                // 使用回調介面，讓 compare.java 處理完整名稱的收集
                if (fullNameCollectCallback != null) {
                    fullNameCollectCallback.accept(fullRestaurantName);
                }
            }
        });
        
        collectOption.getChildren().addAll(collectTitle, collectDesc, restaurantNameField, inputTip, collectButton);

        // 地圖開啟選項
        VBox mapOption = new VBox(10);
        mapOption.setAlignment(Pos.CENTER);
        mapOption.setStyle("-fx-background-color: rgba(220, 242, 204, 0.8); -fx-padding: 20; -fx-background-radius: 10;");
        
        Label mapTitle = new Label("🗺️ 到 Google Maps 查看");
        mapTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        mapTitle.setStyle("-fx-text-fill: #2E7D32;");
        
        Label mapDesc = new Label("開啟 Google Maps 搜尋該餐廳，\n找到完整名稱後複製回來重新搜尋。");
        mapDesc.setWrapText(true);
        mapDesc.setStyle("-fx-text-fill: #2E7D32;");
        
        Button mapButton = new Button("開啟 Google Maps");
        mapButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;");
        mapButton.setOnAction(e -> {
            if (openMapAction != null) {
                openMapAction.run();
            }
        });
        
        mapOption.getChildren().addAll(mapTitle, mapDesc, mapButton);

        // 返回按鈕
        Button backButton = new Button("返回主畫面");
        backButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;");
        backButton.setOnAction(e -> {
            showMainView();
            isRestaurantNotFoundShowing = false;
            if (stateChangeListener != null) {
                stateChangeListener.onRestaurantNotFoundStateChanged(false);
            }
        });

        // 按鈕懸停效果
        collectButton.setOnMouseEntered(e -> collectButton.setStyle("-fx-background-color: #45A049; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;"));
        collectButton.setOnMouseExited(e -> collectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;"));
        
        mapButton.setOnMouseEntered(e -> mapButton.setStyle("-fx-background-color: #F57C00; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;"));
        mapButton.setOnMouseExited(e -> mapButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;"));
        
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-background-color: #616161; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;"));

        // 選項容器
        HBox optionsBox = new HBox(30);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.getChildren().addAll(collectOption, mapOption);

        notFoundContent.getChildren().addAll(titleLabel, messageLabel, descriptionLabel, stepsBox, optionsBox, backButton);

        ScrollPane notFoundScrollPane = new ScrollPane(notFoundContent);
        notFoundScrollPane.setFitToWidth(true);
        notFoundScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        notFoundScrollPane.setStyle("-fx-background-color: rgba(247, 232, 221, 0.9); -fx-border-color: transparent;");

        mainLayout.setCenter(notFoundScrollPane);
        isRestaurantNotFoundShowing = true;
        
        // 確保其他視圖被關閉
        isMonthlyReportShowing = false;
        isSuggestionsShowing = false;
        
        if (stateChangeListener != null) {
            stateChangeListener.onRestaurantNotFoundStateChanged(true);
            stateChangeListener.onMonthlyReportStateChanged(false);
            stateChangeListener.onSuggestionsStateChanged(false);
        }
        
        System.out.println("Switched to Restaurant Not Found View for: " + query);
    }

    /**
     * 顯示資料收集進度視圖
     */
    public void showDataCollectionProgressView(String restaurantName) {
        dataCollectionView = new VBox(20);
        dataCollectionView.setId("data-collection-progress-content");
        dataCollectionView.setPadding(new Insets(40));
        dataCollectionView.setAlignment(Pos.CENTER);

        // 標題
        Label titleLabel = new Label("正在收集餐廳資料");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.getStyleClass().add("label-bright");
        titleLabel.setStyle("-fx-text-fill: #E67649;");

        // 餐廳名稱
        Label restaurantLabel = new Label("餐廳：" + restaurantName);
        restaurantLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        restaurantLabel.getStyleClass().add("label-bright");
        restaurantLabel.setWrapText(true);
        restaurantLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 進度條
        dataCollectionProgressBar = new ProgressBar(0.0);
        dataCollectionProgressBar.setPrefWidth(400);
        dataCollectionProgressBar.setPrefHeight(20);
        dataCollectionProgressBar.setStyle("-fx-accent: #4CAF50;");

        // 狀態標籤
        dataCollectionStatusLabel = new Label("準備開始...");
        dataCollectionStatusLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        dataCollectionStatusLabel.getStyleClass().add("label-bright");
        dataCollectionStatusLabel.setWrapText(true);
        dataCollectionStatusLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 進度訊息容器
        VBox progressContainer = new VBox(10);
        progressContainer.setAlignment(Pos.CENTER);
        progressContainer.setStyle("-fx-background-color: rgba(220, 242, 204, 0.8); -fx-padding: 20; -fx-background-radius: 10;");
        progressContainer.getChildren().addAll(dataCollectionProgressBar, dataCollectionStatusLabel);

        // 提示訊息
        Label tipLabel = new Label("請等待資料收集完成，過程中請勿關閉應用程式");
        tipLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        tipLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
        tipLabel.setWrapText(true);
        tipLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        dataCollectionView.getChildren().addAll(titleLabel, restaurantLabel, progressContainer, tipLabel);

        ScrollPane progressScrollPane = new ScrollPane(dataCollectionView);
        progressScrollPane.setFitToWidth(true);
        progressScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        progressScrollPane.setStyle("-fx-background-color: rgba(247, 232, 221, 0.9); -fx-border-color: transparent;");

        mainLayout.setCenter(progressScrollPane);
        
        System.out.println("Switched to Data Collection Progress View for: " + restaurantName);
    }

    /**
     * 更新資料收集進度
     */
    public void updateDataCollectionProgress(double progress, String statusMessage) {
        Platform.runLater(() -> {
            if (dataCollectionProgressBar != null) {
                dataCollectionProgressBar.setProgress(progress);
            }
            if (dataCollectionStatusLabel != null) {
                dataCollectionStatusLabel.setText(statusMessage);
            }
        });
    }

    /**
     * 顯示資料收集完成視圖
     */
    public void showDataCollectionCompleteView(String restaurantName, boolean success, String message) {
        Platform.runLater(() -> {
            if (dataCollectionView == null) return;

            // 清除現有內容
            dataCollectionView.getChildren().clear();

            // 標題
            Label titleLabel = new Label(success ? "資料收集完成！" : "資料收集失敗");
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
            titleLabel.setStyle("-fx-text-fill: " + (success ? "#4CAF50" : "#D32F2F") + ";");

            // 餐廳名稱
            Label restaurantLabel = new Label("餐廳：" + restaurantName);
            restaurantLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
            restaurantLabel.getStyleClass().add("label-bright");
            restaurantLabel.setWrapText(true);

            // 結果訊息
            Label messageLabel = new Label(message);
            messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
            messageLabel.getStyleClass().add("label-bright");
            messageLabel.setWrapText(true);
            messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            // 結果容器
            VBox resultContainer = new VBox(10);
            resultContainer.setAlignment(Pos.CENTER);
            resultContainer.setStyle("-fx-background-color: rgba(" + 
                (success ? "220, 242, 204" : "255, 235, 238") + ", 0.8); -fx-padding: 20; -fx-background-radius: 10;");
            resultContainer.getChildren().addAll(messageLabel);

            // 操作按鈕
            HBox buttonBox = new HBox(15);
            buttonBox.setAlignment(Pos.CENTER);

            if (success) {
                Button searchButton = new Button("重新搜尋餐廳");
                searchButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;");
                searchButton.setOnAction(e -> {
                    showMainView();
                    // 這裡可以觸發重新搜尋
                });
                buttonBox.getChildren().add(searchButton);
            }

            Button backButton = new Button("返回主畫面");
            backButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8;");
            backButton.setOnAction(e -> showMainView());
            buttonBox.getChildren().add(backButton);

            dataCollectionView.getChildren().addAll(titleLabel, restaurantLabel, resultContainer, buttonBox);
        });
    }

    public void showMainView() {
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