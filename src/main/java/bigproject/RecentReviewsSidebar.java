package bigproject;

import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * 近期評論側欄組件
 * 可以從右側滑入和滑出的側欄，用於顯示餐廳的近期評論
 */
public class RecentReviewsSidebar extends StackPane {
    
    // 顏色設定
    private static final String PALE_DARK_YELLOW = "#6F6732";
    private static final String RICH_MIDTONE_RED = "#E67649";
    private static final String RICH_LIGHT_GREEN = "#DCF2CC";
    
    // UI 元件
    private VBox sidebarContent;
    private VBox recentReviewsBox;
    private Button monthButton, weekButton, dayButton;
    private int currentSelectedDays = 30; // 預設為近一個月
    
    // 動畫和狀態
    private boolean isVisible = false;
    private TranslateTransition slideTransition;
    private FadeTransition fadeTransition;
    
    // 父視窗參考和管理器
    private compare parentComponent;
    private LatestReviewsManager reviewsManager;
    
    // 當前餐廳資訊
    private String currentRestaurantName;
    private String currentRestaurantId;  
    private String currentPlaceId;
    private String currentJsonFilePath;
    
    // 近期評論詳細視圖相關
    private boolean isReviewDetailMode = false;
    private String originalFeaturesContent = "";
    
    /**
     * 建構函數
     * @param parentComponent 父元件參考
     */
    public RecentReviewsSidebar(compare parentComponent) {
        this.parentComponent = parentComponent;
        
        // 初始化最新評論管理器
        this.reviewsManager = new LatestReviewsManager("AIzaSyAfssp2jChrVBpRPFuAhBE6f6kXYDQaV0I");
        
        // 設置側欄樣式和位置
        setupSidebarLayout();
        
        // 初始化內容
        initializeSidebarContent();
        
        // 設置動畫
        setupAnimations();
        
        // 初始狀態：隱藏並確保不攔截事件
        setVisible(false);
        setManaged(false);
        setMouseTransparent(true);
        setTranslateX(400);
    }
    
    /**
     * 設置側欄布局
     */
    private void setupSidebarLayout() {
        // 設置側欄基本屬性
        setPrefWidth(400); // 固定寬度
        setMinWidth(400);
        setMaxWidth(400);
        
        // 設置樣式
        setStyle("-fx-background-color: linear-gradient(to bottom, #1A1A1A 0%, #2C2C2C 100%); " +
                "-fx-background-radius: 15 0 0 15; " +
                "-fx-border-color: linear-gradient(to bottom, #4A4A4A, #6A6A6A); " +
                "-fx-border-width: 2 0 2 2; " +
                "-fx-border-radius: 15 0 0 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 15, 0, -5, 0);");
        
        // 確保側欄在最上層
        setViewOrder(-1);
    }
    
    /**
     * 初始化側欄內容
     */
    private void initializeSidebarContent() {
        sidebarContent = new VBox(20);
        sidebarContent.setPadding(new Insets(30, 20, 30, 20));
        sidebarContent.setStyle("-fx-background-color: transparent;");
        
        // 標題區域
        initializeTitleSection();
        
        // 時間選擇按鈕
        initializeTimeRangeButtons();
        
        // 評論內容區域
        initializeReviewsContent();
        
        getChildren().add(sidebarContent);
    }
    
    /**
     * 初始化標題區域
     */
    private void initializeTitleSection() {
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("近期評論");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 4, 0, 0, 2);");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 關閉按鈕
        Button closeButton = new Button("✕");
        closeButton.setFont(Font.font("System", FontWeight.BOLD, 16));
        closeButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 15; " +
                            "-fx-padding: 5 10 5 10; " +
                            "-fx-cursor: hand;");
        closeButton.setOnAction(e -> hideSidebar());
        
        // 添加懸停效果
        closeButton.setOnMouseEntered(e -> 
            closeButton.setStyle("-fx-background-color: #D32F2F; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 15; " +
                                "-fx-padding: 5 10 5 10; " +
                                "-fx-cursor: hand;"));
        closeButton.setOnMouseExited(e -> 
            closeButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 15; " +
                                "-fx-padding: 5 10 5 10; " +
                                "-fx-cursor: hand;"));
        
        titleBox.getChildren().addAll(titleLabel, spacer, closeButton);
        sidebarContent.getChildren().add(titleBox);
        
        // 添加分隔線
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-opacity: 0.8;");
        sidebarContent.getChildren().add(separator);
    }
    
    /**
     * 初始化時間範圍按鈕
     */
    private void initializeTimeRangeButtons() {
        HBox timeRangeButtons = new HBox(8);
        timeRangeButtons.setAlignment(Pos.CENTER);
        
        monthButton = new Button("近一個月");
        weekButton = new Button("近一週");
        dayButton = new Button("近一天");
        
        // 設置按鈕樣式 - 統一顏色
        String normalButtonStyle = "-fx-background-color: " + RICH_MIDTONE_RED + "; " +
                                  "-fx-text-fill: white; " +
                                  "-fx-background-radius: 18; " +
                                  "-fx-padding: 8 15 8 15; " +
                                  "-fx-font-size: 12px; " +
                                  "-fx-font-weight: bold; " +
                                  "-fx-cursor: hand; " +
                                  "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 2, 0, 0, 1);";
                                  
        String activeButtonStyle = "-fx-background-color: #D45A32; " +
                                  "-fx-text-fill: white; " +
                                  "-fx-background-radius: 18; " +
                                  "-fx-padding: 8 15 8 15; " +
                                  "-fx-font-size: 12px; " +
                                  "-fx-font-weight: bold; " +
                                  "-fx-cursor: hand; " +
                                  "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 3, 0, 0, 2);";
        
        monthButton.setStyle(normalButtonStyle);
        weekButton.setStyle(normalButtonStyle);
        dayButton.setStyle(normalButtonStyle);
        
        // 設置按鈕點擊事件
        monthButton.setOnAction(e -> {
            monthButton.setStyle(activeButtonStyle);
            weekButton.setStyle(normalButtonStyle);
            dayButton.setStyle(normalButtonStyle);
            currentSelectedDays = 30;
            updateRecentReviewsDisplay(30);
        });
        
        weekButton.setOnAction(e -> {
            monthButton.setStyle(normalButtonStyle);
            weekButton.setStyle(activeButtonStyle);
            dayButton.setStyle(normalButtonStyle);
            currentSelectedDays = 7;
            updateRecentReviewsDisplay(7);
        });
        
        dayButton.setOnAction(e -> {
            monthButton.setStyle(normalButtonStyle);
            weekButton.setStyle(normalButtonStyle);
            dayButton.setStyle(activeButtonStyle);
            currentSelectedDays = 1;
            updateRecentReviewsDisplay(1);
        });
        
        // 預設選中近一個月按鈕
        monthButton.setStyle(activeButtonStyle);
        currentSelectedDays = 30;
        
        // 添加懸停效果
        addHoverEffect(monthButton, normalButtonStyle);
        addHoverEffect(weekButton, normalButtonStyle);
        addHoverEffect(dayButton, normalButtonStyle);
        
        timeRangeButtons.getChildren().addAll(monthButton, weekButton, dayButton);
        sidebarContent.getChildren().add(timeRangeButtons);
    }
    
    /**
     * 初始化評論內容區域
     */
    private void initializeReviewsContent() {
        // 創建評論容器
        recentReviewsBox = new VBox(12);
        recentReviewsBox.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                                 "-fx-background-radius: 12; " +
                                 "-fx-padding: 15; " +
                                 "-fx-border-color: rgba(255,255,255,0.2); " +
                                 "-fx-border-width: 1; " +
                                 "-fx-border-radius: 12;");
        recentReviewsBox.setMinHeight(400);
        
        // 添加初始提示訊息
        Label welcomeLabel = new Label("🏪 歡迎使用近期評論功能！\n\n" +
                                     "📋 請先搜尋餐廳，我們將為您顯示最新的評論資訊。\n\n" +
                                     "⏰ 使用上方的時間按鈕來選擇要查看的評論範圍");
        welcomeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); " +
                            "-fx-font-style: italic; " +
                            "-fx-text-alignment: center; " +
                            "-fx-alignment: center; " +
                            "-fx-wrap-text: true; " +
                            "-fx-font-size: 13px; " +
                            "-fx-line-spacing: 2px;");
        welcomeLabel.setWrapText(true);
        recentReviewsBox.getChildren().add(welcomeLabel);
        
        ScrollPane reviewsScrollPane = new ScrollPane(recentReviewsBox);
        reviewsScrollPane.setFitToWidth(true);
        reviewsScrollPane.setFitToHeight(false);
        reviewsScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        reviewsScrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        reviewsScrollPane.setStyle("-fx-background: transparent; " +
                                 "-fx-background-color: transparent; " +
                                 "-fx-border-color: transparent;");
        reviewsScrollPane.setPrefHeight(500);
        VBox.setVgrow(reviewsScrollPane, Priority.ALWAYS);
        
        sidebarContent.getChildren().add(reviewsScrollPane);
    }
    
    /**
     * 設置動畫效果
     */
    private void setupAnimations() {
        // 滑動動畫
        slideTransition = new TranslateTransition(Duration.millis(400), this);
        slideTransition.setInterpolator(javafx.animation.Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        
        // 淡入淡出動畫
        fadeTransition = new FadeTransition(Duration.millis(300), this);
    }
    
    /**
     * 顯示側欄
     */
    public void showSidebar() {
        if (!isVisible) {
            isVisible = true;
            setVisible(true);
            setManaged(true);
            setMouseTransparent(false);
            
            // 🔧 確保側欄出現在正確位置：從右側外面滑入
            // 設置初始位置為完全在右側外面（400px外）
            setTranslateX(getWidth() > 0 ? getWidth() : 400);
            setOpacity(0);
            
            // 確保側欄在最上層
            toFront();
            
            // 滑入動畫：從右側外面滑入到正確位置
            slideTransition.setFromX(getWidth() > 0 ? getWidth() : 400);
            slideTransition.setToX(0);
            
            // 淡入動畫
            fadeTransition.setFromValue(0);
            fadeTransition.setToValue(1);
            
            // 同時執行兩個動畫
            ParallelTransition parallelTransition = new ParallelTransition(slideTransition, fadeTransition);
            parallelTransition.play();
            
            System.out.println("🎯 側欄正在從右側滑入，初始X位置: " + getTranslateX());
        }
    }
    
    /**
     * 隱藏側欄
     */
    public void hideSidebar() {
        if (isVisible) {
            isVisible = false;
            
            // 🔧 滑出動畫：從當前位置滑出到右側外面
            slideTransition.setFromX(0);
            slideTransition.setToX(getWidth() > 0 ? getWidth() : 400);
            
            // 淡出動畫
            fadeTransition.setFromValue(1);
            fadeTransition.setToValue(0);
            
            // 同時執行兩個動畫
            ParallelTransition parallelTransition = new ParallelTransition(slideTransition, fadeTransition);
            parallelTransition.setOnFinished(e -> {
                setVisible(false);
                setManaged(false);
                setMouseTransparent(true);
            });
            parallelTransition.play();
            
            System.out.println("🏠 側欄正在滑出到右側，目標X位置: " + (getWidth() > 0 ? getWidth() : 400));
        }
    }
    
    /**
     * 切換側欄顯示狀態
     */
    public void toggleSidebar() {
        if (isVisible) {
            hideSidebar();
        } else {
            showSidebar();
        }
    }
    
    /**
     * 添加懸停效果
     */
    private void addHoverEffect(Button button, String normalStyle) {
        String hoverStyle = "-fx-background-color: #F08A6C; " +
                           "-fx-text-fill: white; " +
                           "-fx-background-radius: 18; " +
                           "-fx-padding: 8 15 8 15; " +
                           "-fx-font-size: 12px; " +
                           "-fx-font-weight: bold; " +
                           "-fx-cursor: hand; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 2, 0, 0, 1);";
        
        button.setOnMouseEntered(e -> {
            if (!button.getStyle().contains("#8B4513")) { // 如果不是活動狀態
                button.setStyle(hoverStyle);
            }
        });
        
        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains("#8B4513")) { // 如果不是活動狀態
                button.setStyle(normalStyle);
            }
        });
    }
    
    /**
     * 更新近期評論顯示
     */
    public void updateRecentReviewsDisplay(int days) {
        System.out.println("側欄更新近期評論顯示，顯示近 " + days + " 天的評論...");
        System.out.println("🔍 當前餐廳信息檢查:");
        System.out.println("  - currentPlaceId: '" + currentPlaceId + "'");
        System.out.println("  - currentRestaurantName: '" + currentRestaurantName + "'");
        System.out.println("  - currentRestaurantId: '" + currentRestaurantId + "'");
        
        // 清空現有內容
        recentReviewsBox.getChildren().clear();
        
        // 添加載入指示
        Label loadingLabel = new Label("🔄 正在載入近 " + days + " 天的評論...");
        loadingLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); " +
                            "-fx-font-style: italic; " +
                            "-fx-font-size: 13px;");
        recentReviewsBox.getChildren().add(loadingLabel);
        
        // 優先使用搜尋到的餐廳資訊
        if ((currentPlaceId != null && !currentPlaceId.isEmpty()) || 
            (currentRestaurantName != null && !currentRestaurantName.isEmpty())) {
            System.out.println("使用搜尋到的餐廳資訊獲取評論:");
            System.out.println("  - 餐廳名稱: " + currentRestaurantName);
            System.out.println("  - Place ID: " + currentPlaceId);
            
            reviewsManager.fetchAndDisplayReviewsWithFallback(currentPlaceId, currentRestaurantName, days, recentReviewsBox, parentComponent);
            return;
        }
        
        // 如果沒有搜尋到的餐廳資訊，顯示提示
        if (currentJsonFilePath == null || currentJsonFilePath.isEmpty()) {
            recentReviewsBox.getChildren().clear();
            Label errorLabel = new Label("❌ 尚未載入餐廳資料\n\n請先搜尋餐廳以查看評論");
            errorLabel.setStyle("-fx-text-fill: #FFB3B3; " +
                              "-fx-font-style: italic; " +
                              "-fx-text-alignment: center; " +
                              "-fx-alignment: center; " +
                              "-fx-wrap-text: true;");
            errorLabel.setWrapText(true);
            recentReviewsBox.getChildren().add(errorLabel);
            return;
        }
        
        // 使用JSON檔案中的評論作為備用方案
        System.out.println("使用 JSON 檔案中的評論: " + currentJsonFilePath);
        reviewsManager.updateRecentReviewsDisplay(currentJsonFilePath, days, recentReviewsBox, parentComponent);
    }
    
    /**
     * 設置當前餐廳資訊
     */
    public void setCurrentRestaurantInfo(String name, String id, String placeId) {
        this.currentRestaurantName = name;
        this.currentRestaurantId = id;
        this.currentPlaceId = placeId;
        
        System.out.println("✅ 側欄已設置餐廳資訊: " + name + " (ID: " + id + ", PlaceID: " + placeId + ")");
        
        // 🎯 自動初始化「近一個月」的評論
        if (name != null && !name.isEmpty()) {
            System.out.println("🚀 自動載入近一個月的評論...");
            
            // 確保月份按鈕保持選中狀態
            String activeButtonStyle = "-fx-background-color: #D45A32; " +
                                     "-fx-text-fill: white; " +
                                     "-fx-background-radius: 18; " +
                                     "-fx-padding: 8 15 8 15; " +
                                     "-fx-font-size: 12px; " +
                                     "-fx-font-weight: bold; " +
                                     "-fx-cursor: hand; " +
                                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 3, 0, 0, 2);";
            
            String normalButtonStyle = "-fx-background-color: " + RICH_MIDTONE_RED + "; " +
                                      "-fx-text-fill: white; " +
                                      "-fx-background-radius: 18; " +
                                      "-fx-padding: 8 15 8 15; " +
                                      "-fx-font-size: 12px; " +
                                      "-fx-font-weight: bold; " +
                                      "-fx-cursor: hand; " +
                                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 2, 0, 0, 1);";
            
            if (monthButton != null && weekButton != null && dayButton != null) {
                monthButton.setStyle(activeButtonStyle);
                weekButton.setStyle(normalButtonStyle);
                dayButton.setStyle(normalButtonStyle);
            }
            
            currentSelectedDays = 30;
            
            // 自動載入近一個月的評論
            updateRecentReviewsDisplay(30);
        }
    }
    
    /**
     * 設置當前JSON檔案路徑
     */
    public void setCurrentJsonFilePath(String jsonFilePath) {
        this.currentJsonFilePath = jsonFilePath;
    }
    
    /**
     * 檢查側欄是否可見
     */
    public boolean isSidebarVisible() {
        return isVisible;
    }
} 