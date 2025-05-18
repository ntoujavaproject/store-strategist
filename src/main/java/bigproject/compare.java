package bigproject;

import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Separator;
import javafx.scene.Cursor;
import java.awt.Desktop;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 餐廳市場分析系統主應用程式
 * (Layout: 70% Reviews, 30% Ratings/Sources, Top-Right Buttons)
 */
public class compare extends Application implements UIManager.StateChangeListener, PreferencesManager.SettingsStateChangeListener {

    private VBox competitorListVBox;
    private BorderPane mainLayout;
    private Scene mainScene;
    private HBox mainContentBox; // 將mainContentBox升級為類成員變數
    private VBox mainContainer; // 將mainContainer也升級為類成員變數
    private ScrollPane leftScrollPane; // 將leftScrollPane也升級為類成員變數
    private VBox rightPanel; // 將rightPanel也升級為類成員變數

    private Preferences prefs = Preferences.userNodeForPackage(compare.class);

    // API Key is used by GooglePlacesService instance
    private static final String API_KEY = "AIzaSyAfssp2jChrVBpRPFuAhBE6f6kXYDQaV0I";

    // Service and Manager instances
    private GooglePlacesService googlePlacesService;
    private DataManager dataManager;
    private UIManager uiManager;

    // UI references needed by logic in this class or passed to managers
    private StackPane ratingPane; 
    private TextArea reviewsArea;
    private TextArea featuresArea;
    private TextArea prosArea;
    private TextArea consArea;
    private Label ratingsHeader; 
    private VBox ratingsBox; 
    private Map<String, ProgressBar> ratingBars; 
    private FlowPane photosContainer; // 用於顯示評論照片的容器，改為FlowPane
    private ScrollPane photosScroll; // 添加ScrollPane包裹圖片容器
    
    // 添加 PreferencesManager 成員變量
    private PreferencesManager preferencesManager;

    // 新配色方案
    private static final String PALE_DARK_YELLOW = "#6F6732";
    private static final String MUTED_LIGHT_ORANGE = "#EBDACB";
    private static final String RICH_MIDTONE_RED = "#E67649";
    private static final String RICH_LIGHT_GREEN = "#DCF2CC";
    private static final String DARK_BACKGROUND = "#2C2C2C";
    private static final String LIGHT_TEXT = "#F5F5F5";

    private static final String DEFAULT_FONT_FAMILY = "System";
    private static final double DEFAULT_FONT_SIZE = 12.0;
    private static final boolean DEFAULT_DARK_MODE = true;

    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_FONT_SIZE = "fontSize";
    private static final String KEY_DARK_MODE = "darkMode";

    // --- Search History Storage ---
    private List<String> searchHistory = new ArrayList<>(Arrays.asList(
        "海大燒臘", "基隆海那邊小食堂", "基隆夜市美食", "基隆咖啡廳",
        "基隆特色餐廳", "海大附近美食", "基隆港邊海鮮"
    ));
    
    // 搜索對話框參考
    private Stage searchDialog = null;
    
    // 添加一個時間戳變數來防止對話框快速重開
    private long lastDialogCloseTime = 0;
    private static final long DIALOG_REOPEN_DELAY = 800; // 800毫秒的延遲防止閃爍
    
    // 按鈕樣式
    private String normalButtonStyle;
    private String activeButtonStyle;
    private String hoverButtonStyle;
    
    // 按鈕狀態
    private final boolean[] isSuggestionActive = {false};
    private final boolean[] isReportActive = {false};
    private final boolean[] isSettingsActive = {false}; // 添加設定狀態
    
    // 按鈕引用
    private Button suggestionButton;
    private Button reportButton;
    private Button settingsButton; // 添加設定按鈕引用

    private boolean isHorizontalLayout = true; // 記錄當前布局模式，true為水平布局(左右)，false為垂直布局(上下)
    private static final int LAYOUT_CHANGE_THRESHOLD = 50; // 防抖動閾值，避免在邊界值附近頻繁切換
    
    // 存儲所有頁面的Map
    private Map<String, TabContent> tabContents = new HashMap<>();
    private String currentTabId = null; // 當前選定的頁面ID
    private HBox tabBar; // 分頁欄

    // 添加AI聊天相關的狀態變量
    private final boolean[] isAIChatActive = {false};
    private Button aiChatBackButton;
    private VBox aiChatContainer;
    private TextArea chatHistoryArea;
    private TextField userInputField;
    private String currentChatContentType = "";
    private String currentChatInitialContent = "";

    @Override
    public void start(Stage primaryStage) {
        // 設置視窗最大化顯示 - 提前設置，確保啟動時就是全螢幕
        primaryStage.setMaximized(true);
        
        // 設置視窗標題和大小
        primaryStage.setTitle("餐廳分析");
        
        // 載入應用程式圖標
        ResourceManager.setAppIcon(primaryStage);
        
        // 創建主佈局
        mainLayout = new BorderPane();
        // 調整主布局邊距，上、左、右有邊距，底部無邊距，確保分頁欄不留空白
        mainLayout.setPadding(new Insets(15, 15, 0, 0)); // 完全移除底部邊距，右邊也移除以避免對分頁欄位置的影響
        mainLayout.setStyle("-fx-background-color: #2C2C2C;"); // 使用深色背景統一風格
        mainLayout.setPrefHeight(Double.MAX_VALUE); // 確保主佈局填滿整個高度
        mainLayout.setPrefWidth(Double.MAX_VALUE); // 確保主佈局填滿整個寬度
        
        // 設置主佈局初始不可見，用於後續動畫
        mainLayout.setOpacity(0);
        
        // 創建分頁欄
        tabBar = new HBox(5);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(5, 10, 0, 10)); // 移除底部內邊距
        tabBar.setStyle("-fx-background-color: #2A2A2A; -fx-border-width: 2 0 0 0; -fx-border-color: #E67649; -fx-padding: 5 10 0 10;"); // 移除底部內邊距
        tabBar.setMinHeight(45); // 增加高度
        tabBar.setPrefHeight(45);
        tabBar.setMaxHeight(45);
        
        // 確保分頁欄寬度填滿視窗，並設置貼緊底部
        tabBar.setPrefWidth(Double.MAX_VALUE);
        tabBar.setMaxWidth(Double.MAX_VALUE);
        tabBar.setSnapToPixel(true); // 確保精確對齊到像素
        
        // 添加一個"+"按鈕，用於創建新分頁
        Button addTabButton = new Button("+");
        addTabButton.setStyle("-fx-background-color: #444444; -fx-text-fill: white;");
        addTabButton.setOnAction(e -> showAddTabDialog(primaryStage));
        
        // 將"+"按鈕添加到分頁欄
        tabBar.getChildren().add(addTabButton);
        
        // Initialize Services first
        try {
            googlePlacesService = new GooglePlacesService(API_KEY);
            dataManager = new DataManager();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- Load user preferences ---
        String currentFontFamily = prefs.get(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY);
        double currentFontSize = prefs.getDouble(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        boolean useDarkMode = true;

        // --- Top Bar Setup (移除搜索框) ---
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("top-bar");
        
        // 使用普通按鈕而非ToggleButton，這樣我們可以直接控制其樣式
        suggestionButton = new Button("經營建議");
        reportButton = new Button("月報");
        settingsButton = new Button("⚙️");
        
        // 設置具體的樣式而不是使用CSS類
        normalButtonStyle = "-fx-text-fill: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 15 8 15;";
        activeButtonStyle = "-fx-background-color: #8B4513; " + normalButtonStyle + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 4, 0, 0, 1);";
        hoverButtonStyle = "-fx-background-color: #f08a6c; " + normalButtonStyle;
        normalButtonStyle = "-fx-background-color: #E67649; " + normalButtonStyle;
        
        suggestionButton.setStyle(normalButtonStyle);
        reportButton.setStyle(normalButtonStyle);
        settingsButton.setStyle(normalButtonStyle);
        
        // 添加鼠標懸浮效果
        suggestionButton.setOnMouseEntered(e -> {
            if (!isSuggestionActive[0]) {
                suggestionButton.setStyle(hoverButtonStyle);
            }
        });
        suggestionButton.setOnMouseExited(e -> {
            if (!isSuggestionActive[0]) {
                suggestionButton.setStyle(normalButtonStyle);
            }
        });
        
        reportButton.setOnMouseEntered(e -> {
            if (!isReportActive[0]) {
                reportButton.setStyle(hoverButtonStyle);
            }
        });
        reportButton.setOnMouseExited(e -> {
            if (!isReportActive[0]) {
                reportButton.setStyle(normalButtonStyle);
            }
        });
        
        settingsButton.setFont(Font.font(16));
        settingsButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2 5 2 5; -fx-text-fill: #CCCCCC;"); // Invisible style
        
        topBar.getChildren().addAll(suggestionButton, reportButton, settingsButton);
        
        // --- 創建搜索欄作為布局固定部分 ---
        HBox searchContainer = new HBox(10);
        searchContainer.setAlignment(Pos.CENTER);
        searchContainer.setPadding(new Insets(10, 20, 10, 20));
        searchContainer.setMaxWidth(Double.MAX_VALUE); // 允許最大寬度
        searchContainer.setPrefWidth(Double.MAX_VALUE); // 填滿可用寬度
        searchContainer.setMinHeight(50); // 確保足夠高度
        searchContainer.setStyle("-fx-background-color: rgba(80, 80, 80, 0.6); -fx-background-radius: 5;");
        
        // 創建圓角搜索框
        TextField searchField = new TextField();
        searchField.setPromptText("搜尋...");
        searchField.setPrefHeight(35);
        searchField.getStyleClass().add("search-history-field");
        searchField.setStyle("-fx-background-radius: 20; -fx-padding: 5 15 5 15; -fx-font-size: 14px;");
        HBox.setHgrow(searchField, Priority.ALWAYS); // 讓搜索框占用所有可用空間
        
        Button searchButton = new Button("搜尋");
        searchButton.setMinHeight(35);
        searchButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; -fx-background-radius: 20;");
        
        // 添加鼠標懸停效果
        searchButton.setOnMouseEntered(e -> {
            searchButton.setStyle("-fx-background-color: #F08159; -fx-text-fill: white; -fx-background-radius: 20;");
            searchButton.setCursor(javafx.scene.Cursor.HAND);
        });
        
        searchButton.setOnMouseExited(e -> {
            searchButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; -fx-background-radius: 20;");
            searchButton.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        searchContainer.getChildren().addAll(searchField, searchButton);
        
        // --- 創建主布局 --- 
        mainContainer = new VBox(5); // 使用VBox包含全部內容，包括搜索欄
        mainContainer.getChildren().addAll(searchContainer);
        mainContainer.setPrefHeight(Double.MAX_VALUE); // 確保填滿整個高度
        VBox.setVgrow(mainContainer, Priority.ALWAYS); // 確保主容器能擴展填滿

        // --- Main Content Area (HBox: Left 70%, Right 30%) ---
        mainContentBox = new HBox(0); // 移除左右間距，讓青蘋果欄完全貼緊右側邊界
        mainContentBox.setPadding(new Insets(10, 0, 0, 0)); // 移除底部和右側邊距，讓內容延伸到底並完全貼緊
        mainContentBox.setPrefHeight(Double.MAX_VALUE); // 確保內容區域填滿整個高度
        mainContentBox.setMinHeight(600); // 設置最小高度，避免內容區域過小
        mainContentBox.setStyle("-fx-background-color: transparent;"); // 透明背景讓子元素背景顯示
        mainContentBox.setMaxWidth(Double.MAX_VALUE); // 確保內容區域水平填滿

        // --- Left Panel (Reviews, Details) ---
        VBox leftPanel = new VBox(20);
        leftPanel.setPadding(new Insets(20, 20, 0, 20)); // 移除底部邊距，確保貼緊橘線
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setPrefHeight(Double.MAX_VALUE); // 確保預設高度撐滿
        leftPanel.setStyle("-fx-background-color: #F7E8DD;"); // 使用統一的膚色背景
        leftPanel.getStyleClass().add("content-panel");
        leftPanel.setMaxWidth(Double.MAX_VALUE); // 設置最大寬度，確保不超出可用空間

        // 將右側面板的內容整合到左側面板
        // 1. 首先添加評分區塊
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.TOP_CENTER);
        topPanel.setPrefWidth(Double.MAX_VALUE);
        
        // 左側評論區域
        VBox reviewsSection = new VBox(10);
        reviewsSection.setPrefWidth(Double.MAX_VALUE);
        HBox.setHgrow(reviewsSection, Priority.ALWAYS);
        
        Label reviewsLabel = new Label("精選評論");
        reviewsLabel.setFont(Font.font("System", FontWeight.BOLD, 18)); // 字體增大
        reviewsLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        reviewsArea = new TextArea();
        reviewsArea.setPromptText("正在載入評論...");
        reviewsArea.setEditable(false);
        reviewsArea.setWrapText(true);
        reviewsArea.setPrefHeight(350); // 從原來的250增加到350
        reviewsArea.setMinHeight(300); // 添加最小高度限制
        VBox.setVgrow(reviewsArea, Priority.ALWAYS); // 保持評論區能自動擴展
        
        reviewsSection.getChildren().addAll(reviewsLabel, reviewsArea);
        
        // 右側評分區域
        rightPanel = new VBox(15); // 初始化rightPanel，增加子元素間距
        rightPanel.setPrefWidth(450); // 固定為450寬度，確保與右側邊界貼緊
        rightPanel.setMinWidth(450); // 固定最小寬度，確保貼緊右側邊界
        rightPanel.setMaxWidth(450); // 固定最大寬度，確保貼緊右側邊界
        rightPanel.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + "; -fx-background-radius: 0;"); // 移除圓角，確保無間隙
        rightPanel.setPadding(new Insets(15, 0, 300, 15)); // 右側邊距為0，底部增加到300，確保內容可滾動到底
        rightPanel.setMinHeight(3000); // 極大增加最小高度，確保所有內容都能顯示且可滾動
        rightPanel.setPrefHeight(3500); // 更大的預設高度，確保所有內容都能顯示
        
        // Ratings Section
        ratingsHeader = new Label("綜合評分");
        ratingsHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        ratingsHeader.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        ratingsBox = new VBox(5);
        ratingsBox.setPadding(new Insets(5, 0, 15, 0)); // Adjust padding
        ratingsBox.setStyle("-fx-background-color: transparent;");
        ratingBars = new HashMap<>();
        String[] categories = {"餐點", "服務", "環境", "價格"};
        for (String category : categories) {
            HBox barBox = new HBox(10);
            barBox.setAlignment(Pos.CENTER_LEFT);
            Label catLabel = new Label(category + ":");
            catLabel.setMinWidth(40);
            catLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
            ProgressBar progressBar = new ProgressBar(0.0);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(progressBar, Priority.ALWAYS);
            progressBar.setStyle("-fx-accent: " + RICH_MIDTONE_RED + ";");
            ratingBars.put(category, progressBar);
            barBox.getChildren().addAll(catLabel, progressBar);
            ratingsBox.getChildren().add(barBox);
        }
        
        // Data Sources Section
        Label sourcesLabel = new Label("資料來源");
        sourcesLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        sourcesLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        competitorListVBox = new VBox(5);
        competitorListVBox.setPadding(new Insets(5, 0, 0, 0));
        competitorListVBox.getChildren().add(createCompetitorEntry("Haidai Roast Shop", "Haidai Roast Shop.json"));
        competitorListVBox.getChildren().add(createCompetitorEntry("Sea Side Eatery", "Sea Side Eatery Info.json"));
        
        // 近期評論區域
        Label recentReviewsLabel = new Label("近期評論");
        recentReviewsLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        recentReviewsLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");

        // 創建時間範圍選擇按鈕
        HBox timeRangeButtonsBox = new HBox(5);
        timeRangeButtonsBox.setAlignment(Pos.CENTER_LEFT);
        
        Button dayButton = new Button("近一天");
        Button weekButton = new Button("近一週");
        Button monthButton = new Button("近一個月");
        
        // 設置按鈕樣式
        String timeButtonStyle = "-fx-background-color: #DDDDDD; -fx-text-fill: #555555; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;";
        String timeButtonActiveStyle = "-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;";
        
        dayButton.setStyle(timeButtonStyle);
        weekButton.setStyle(timeButtonActiveStyle); // 預設選中一週
        monthButton.setStyle(timeButtonStyle);
        
        // 設置按鈕點擊事件
        dayButton.setOnAction(e -> {
            // 更改按鈕樣式
            dayButton.setStyle(timeButtonActiveStyle);
            weekButton.setStyle(timeButtonStyle);
            monthButton.setStyle(timeButtonStyle);
            
            // 強制更新顯示近一天的評論
            System.out.println("近一天按鈕被點擊，開始更新顯示...");
            updateRecentReviewsDisplay(1); // 1天
            System.out.println("近一天按鈕點擊處理完成");
        });
        
        weekButton.setOnAction(e -> {
            // 更改按鈕樣式
            dayButton.setStyle(timeButtonStyle);
            weekButton.setStyle(timeButtonActiveStyle);
            monthButton.setStyle(timeButtonStyle);
            
            // 強制更新顯示近一週的評論
            System.out.println("近一週按鈕被點擊，開始更新顯示...");
            updateRecentReviewsDisplay(7); // 7天
            System.out.println("近一週按鈕點擊處理完成");
        });
        
        monthButton.setOnAction(e -> {
            // 更改按鈕樣式
            dayButton.setStyle(timeButtonStyle);
            weekButton.setStyle(timeButtonStyle);
            monthButton.setStyle(timeButtonActiveStyle);
            
            // 強制更新顯示近一個月的評論
            System.out.println("近一個月按鈕被點擊，開始更新顯示...");
            
            // 確保立即顯示近一個月的評論
            updateRecentReviewsDisplay(30); // 30天
            
            System.out.println("近一個月按鈕點擊處理完成");
        });
        
        // 添加懸停效果
        dayButton.setOnMouseEntered(e -> {
            if (!dayButton.getStyle().contains(RICH_MIDTONE_RED)) {
                dayButton.setStyle("-fx-background-color: #EEEEEE; -fx-text-fill: #333333; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;");
            }
        });
        
        dayButton.setOnMouseExited(e -> {
            if (!dayButton.getStyle().contains(RICH_MIDTONE_RED)) {
                dayButton.setStyle(timeButtonStyle);
            }
        });
        
        weekButton.setOnMouseEntered(e -> {
            if (!weekButton.getStyle().contains(RICH_MIDTONE_RED)) {
                weekButton.setStyle("-fx-background-color: #EEEEEE; -fx-text-fill: #333333; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;");
            }
        });
        
        weekButton.setOnMouseExited(e -> {
            if (!weekButton.getStyle().contains(RICH_MIDTONE_RED)) {
                weekButton.setStyle(timeButtonStyle);
            }
        });
        
        monthButton.setOnMouseEntered(e -> {
            if (!monthButton.getStyle().contains(RICH_MIDTONE_RED)) {
                monthButton.setStyle("-fx-background-color: #EEEEEE; -fx-text-fill: #333333; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;");
            }
        });
        
        monthButton.setOnMouseExited(e -> {
            if (!monthButton.getStyle().contains(RICH_MIDTONE_RED)) {
                monthButton.setStyle(timeButtonStyle);
            }
        });
        
        timeRangeButtonsBox.getChildren().addAll(dayButton, weekButton, monthButton);
        
        // 改變預設設置：預設選中近一個月而非近一週
        // 更改按鈕樣式
        dayButton.setStyle(timeButtonStyle);
        weekButton.setStyle(timeButtonStyle);
        monthButton.setStyle(timeButtonActiveStyle);
        
        // 確保載入時就顯示近一個月的評論
        System.out.println("初始化時自動選擇近一個月按鈕");
        Platform.runLater(() -> {
            System.out.println("平台執行自動更新近一個月評論顯示");
            updateRecentReviewsDisplay(30); // 30天
        });
        
        // 在標題和時間範圍按鈕之間添加間距
        HBox reviewHeaderBox = new HBox(10);
        reviewHeaderBox.setAlignment(Pos.CENTER_LEFT);
        reviewHeaderBox.getChildren().addAll(recentReviewsLabel, timeRangeButtonsBox);

        // 創建近期評論列表容器
        VBox recentReviewsBox = new VBox(10);
        recentReviewsBox.setPadding(new Insets(5, 0, 15, 0));
        recentReviewsBox.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-padding: 10;");

        // 將近期評論區域添加到右側面板 - 移除對updateButtonBox的引用
        rightPanel.getChildren().addAll(ratingsHeader, ratingsBox, sourcesLabel, competitorListVBox, reviewHeaderBox, recentReviewsBox);
        
        // 添加到頂部面板
        topPanel.getChildren().addAll(reviewsSection, rightPanel);
        
        // 照片區域
        Label photosLabel = new Label("評論照片");
        photosLabel.setFont(Font.font("System", FontWeight.BOLD, 18)); // 字體增大
        photosLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        // 將VBox改為FlowPane以實現自適應排列
        photosContainer = new FlowPane();
        photosContainer.setHgap(0); // 完全消除水平間距
        photosContainer.setVgap(0); // 完全消除垂直間距
        photosContainer.setPrefWrapLength(800); // 設置一個較大的固定值，確保能換行
        photosContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        photosContainer.setPrefHeight(Region.USE_COMPUTED_SIZE); // 讓高度自動計算
        photosContainer.setMinHeight(250);
        photosContainer.setStyle("-fx-background-color: #222222; -fx-padding: 0; -fx-spacing: 0;"); // 設置深色背景，移除所有間距
        
        // 修改ScrollPane配置
        photosScroll = new ScrollPane();
        photosScroll.setContent(photosContainer); // 設置內容
        photosScroll.setFitToWidth(true); // 內容寬度適應ScrollPane
        photosScroll.setFitToHeight(false); // 不要讓內容高度適應ScrollPane - 這樣才能正確滾動
        photosScroll.setPrefHeight(350);
        photosScroll.setMinHeight(300);
        photosScroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED); // 需要時顯示水平滾動條
        photosScroll.setVbarPolicy(ScrollBarPolicy.ALWAYS); // 總是顯示垂直滾動條
        VBox.setVgrow(photosScroll, Priority.ALWAYS);
        
        // 特色、優點和缺點分析區塊
        Label featuresLabel = new Label("特色");
        featuresLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        featuresLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        featuresArea = new TextArea();
        featuresArea.setPromptText("載入中...");
        featuresArea.setEditable(false);
        featuresArea.setWrapText(true);
        featuresArea.setPrefHeight(120);
        
        Label prosLabel = new Label("優點");
        prosLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        prosLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        prosArea = new TextArea();
        prosArea.setPromptText("載入中...");
        prosArea.setEditable(false);
        prosArea.setWrapText(true);
        prosArea.setPrefHeight(120);
        
        Label consLabel = new Label("缺點");
        consLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        consLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        // 先添加評分和資料來源部分 - 在初始化rightPanel時已添加了這些元素
        // rightPanel.getChildren().addAll(ratingsHeader, ratingsBox, sourcesLabel, competitorListVBox); // 避免重複添加

                // --- 將左側面板整體放入ScrollPane以支持垂直滾動 ---
        leftScrollPane = new ScrollPane(leftPanel);
        leftScrollPane.setFitToWidth(true); // 讓內容適應寬度
        leftScrollPane.setFitToHeight(false); // 修改: 設為false讓內容可以滾動
        leftScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER); // 不顯示水平滾動條
        leftScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); // 總是顯示垂直滾動條
        leftScrollPane.setStyle("-fx-background-color: #F7E8DD; -fx-border-color: transparent;");
        leftScrollPane.setPannable(true); // 允許拖曳滾動
        leftScrollPane.setMinHeight(Region.USE_COMPUTED_SIZE);
        leftScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        leftScrollPane.setMaxHeight(Double.MAX_VALUE);
        // 添加寬度限制，確保不超過橘線
        leftScrollPane.setMaxWidth(700); // 設置最大寬度
        
        // 確保滾動面板正確處理內容的高度變化
        leftPanel.heightProperty().addListener((obs, oldVal, newVal) -> {
            leftScrollPane.layout();
        });
        
        // 將右側面板放入ScrollPane以支持垂直滾動
        ScrollPane rightScrollPane = new ScrollPane(rightPanel);
        rightScrollPane.setFitToWidth(true); // 讓內容適應寬度
        rightScrollPane.setFitToHeight(false); // 修改為false，允許內容超出可視區域並顯示滾動條
        rightScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER); // 不顯示水平滾動條
        rightScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); // 總是顯示垂直滾動條
        rightScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background: transparent; -fx-padding: 0 0 0 0; -fx-border-width: 0;");
        rightScrollPane.setPannable(true); // 允許拖曳滾動
        rightScrollPane.setMinHeight(Region.USE_COMPUTED_SIZE); // 使用計算高度
        rightScrollPane.setPrefHeight(Double.MAX_VALUE); // 使用最大高度填充
        rightScrollPane.setMaxHeight(Double.MAX_VALUE); // 允許最大高度擴展
        rightScrollPane.setVmin(0); // 確保滾動從頂部開始
        rightScrollPane.setVmax(1); // 確保滾動到底部
        rightScrollPane.setPrefWidth(450); // 固定寬度，確保貼緊右側邊界
        
        // 解決滑動問題：增加右側面板的滾動事件處理
        rightScrollPane.setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 2.0; // 增加滾動速度
            rightScrollPane.setVvalue(rightScrollPane.getVvalue() - deltaY / rightPanel.getHeight());
            event.consume(); // 防止事件傳播
        });
        
        // 設置右側面板大小，確保有足夠空間顯示所有內容
        rightPanel.setMinHeight(2000); // 設置足夠大的最小高度
        rightPanel.setPrefHeight(2200); // 設置足夠大的預設高度
        
        // 確保滾動面板貼緊分頁欄
        mainLayout.heightProperty().addListener((obs, oldVal, newVal) -> {
            updatePanelSizes(mainContentBox, leftScrollPane, mainLayout.getWidth(), newVal.doubleValue());
        });
        
        // 添加左側和右側面板到主內容區域
        mainContentBox.getChildren().addAll(leftScrollPane, rightScrollPane);
        HBox.setHgrow(leftScrollPane, Priority.ALWAYS); // 讓左側面板自動擴展填滿可用空間
        
        // 確保右側面板可以完全滾動，且不受其他設置影響
        rightScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); // 總是顯示垂直滾動條
        rightScrollPane.setFitToHeight(false); // 讓內容可完全顯示並允許滾動
        rightScrollPane.setFitToWidth(true); // 寬度適應容器
        
        // 將主內容區域加入主容器
        mainContainer.getChildren().add(mainContentBox);
        VBox.setVgrow(mainContentBox, Priority.ALWAYS); // 讓內容區域自動擴展
        
        // --- Setup Scene and UIManager --- 
        mainLayout.setCenter(mainContainer); // 使用包含搜索欄的容器作為主要內容
        mainLayout.setTop(topBar); // 設置頂部欄
        
        // 明確設置底部的分頁欄，固定在視窗底部
        mainLayout.setBottom(tabBar);
        BorderPane.setMargin(tabBar, new Insets(0, 0, 0, 0)); // 移除底部邊距，確保完全貼緊視窗底部
        
        mainLayout.setStyle("-fx-background-color: #2C2C2C; -fx-min-height: 100%;"); // 確保填滿整個空間
        
        // 初始化場景，並設置最小尺寸防止過小導致UI變形
        mainScene = new Scene(mainLayout, 1024, 768);
        mainScene.getRoot().setStyle("-fx-min-height: 100%;");

        // --- Initialize UIManager NOW --- 
        uiManager = new UIManager(prefs, primaryStage, mainScene, mainLayout, mainContentBox); // Pass main HBox instead
        
        // 初始化 PreferencesManager
        preferencesManager = new PreferencesManager(primaryStage, mainLayout, uiManager);
        
        // Set this class as the state change listener
        uiManager.setStateChangeListener(this);
        preferencesManager.setStateChangeListener(this);

        // --- Update font style using UIManager ---
        uiManager.updateFontStyle(currentFontFamily, currentFontSize);

        // --- Set Actions requiring uiManager ---
        settingsButton.setOnAction(e -> {
            // 切換設定視圖
            preferencesManager.toggleSettingsView();
            
            // 切換按鈕樣式
            if (isSettingsActive[0]) {
                // 關閉設定，恢復默認樣式
                settingsButton.setStyle(normalButtonStyle);
                isSettingsActive[0] = false;
            } else {
                // 顯示設定，使用深色樣式
                settingsButton.setStyle(activeButtonStyle);
                isSettingsActive[0] = true;
                
                // 如果建議視圖是活躍的，關閉它
                if (isSuggestionActive[0]) {
                    suggestionButton.setStyle(normalButtonStyle);
                    isSuggestionActive[0] = false;
                }
                
                // 如果月報視圖是活躍的，關閉它
                if (isReportActive[0]) {
                    reportButton.setStyle(normalButtonStyle);
                    isReportActive[0] = false;
                }
            }
        });
        
        // 添加設定按鈕的懸停效果
        settingsButton.setOnMouseEntered(e -> {
            if (!isSettingsActive[0]) {
                settingsButton.setStyle("-fx-background-color: rgba(80, 80, 80, 0.5); -fx-border-color: transparent; -fx-padding: 2 5 2 5; -fx-text-fill: white;");
                settingsButton.setCursor(javafx.scene.Cursor.HAND);
            }
        });
        
        settingsButton.setOnMouseExited(e -> {
            if (!isSettingsActive[0]) {
                settingsButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2 5 2 5; -fx-text-fill: #CCCCCC;");
                settingsButton.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
        
        reportButton.setOnAction(e -> {
            // 切換月報視圖
            uiManager.toggleMonthlyReport();
            
            // 切換按鈕樣式
            if (isReportActive[0]) {
                // 關閉報告，恢復默認樣式
                reportButton.setStyle(normalButtonStyle);
                isReportActive[0] = false;
            } else {
                // 顯示報告，使用深色樣式
                reportButton.setStyle(activeButtonStyle);
                isReportActive[0] = true;
                
                // 如果建議視圖是活躍的，關閉它
                if (isSuggestionActive[0]) {
                    suggestionButton.setStyle(normalButtonStyle);
                    isSuggestionActive[0] = false;
                }
            }
        });
        
        suggestionButton.setOnAction(e -> {
            // 切換建議視圖
            uiManager.toggleSuggestionsView();
            
            // 切換按鈕樣式
            if (isSuggestionActive[0]) {
                // 關閉建議，恢復默認樣式
                suggestionButton.setStyle(normalButtonStyle);
                isSuggestionActive[0] = false;
            } else {
                // 顯示建議，使用深色樣式
                suggestionButton.setStyle(activeButtonStyle);
                isSuggestionActive[0] = true;
                
                // 如果報告視圖是活躍的，關閉它
                if (isReportActive[0]) {
                    reportButton.setStyle(normalButtonStyle);
                    isReportActive[0] = false;
                }
            }
        });
        
        // --- 現在創建TextAreas，因為uiManager已初始化 ---
        featuresArea = uiManager.createStyledTextArea("特色描述 (從評論分析)...", 120);
        prosArea = uiManager.createStyledTextArea("優點分析 (從評論分析)...", 120);
        consArea = uiManager.createStyledTextArea("缺點分析 (從評論分析)...", 120);
        
        // 設置所有TextArea的額外屬性，確保在垂直佈局中可正確滾動
        TextArea[] textAreas = {featuresArea, prosArea, consArea};
        for (TextArea area : textAreas) {
            area.setMinHeight(120); // 增加最小高度
            area.setPrefHeight(150); // 增加預設高度
            VBox.setVgrow(area, Priority.SOMETIMES);
            area.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-cursor: hand;");
            
            // 添加懸停效果，提示可點擊
            area.setOnMouseEntered(e -> {
                area.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: " + RICH_MIDTONE_RED + "; -fx-border-width: 1.5; -fx-cursor: hand;");
            });
            
            area.setOnMouseExited(e -> {
                area.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-cursor: hand;");
            });
        }
        
        // 為每個區域添加特定的點擊事件 - 改成直接切換到AI聊天視圖
        featuresArea.setOnMouseClicked(e -> {
            toggleAIChatView("特色討論", featuresArea.getText(), "餐廳特色");
        });
        
        prosArea.setOnMouseClicked(e -> {
            toggleAIChatView("優點討論", prosArea.getText(), "餐廳優點");
        });
        
        consArea.setOnMouseClicked(e -> {
            toggleAIChatView("缺點討論", consArea.getText(), "餐廳缺點");
        });
        
        // 添加特色、優點、缺點區塊到右側面板
        rightPanel.getChildren().addAll(featuresLabel, featuresArea, prosLabel, prosArea, consLabel, consArea);
        
        // 增加一個空白區域，確保內容可以完全滾動到底部
        Region spacer = new Region();
        spacer.setMinHeight(200);
        spacer.setPrefHeight(200);
        rightPanel.getChildren().add(spacer);

        // 添加評論區和照片區到左側面板
        leftPanel.getChildren().addAll(reviewsLabel, reviewsArea, photosLabel, photosScroll);
        
        // --- Add Left and Right Panels to HBox --- 
        // 這段代碼現在被移到了上面，leftScrollPane在上面已完成初始化
        
        // 建立響應式設計的內容調整器
        // 強制使用小視窗模式 - 即只顯示青蘋果綠欄位，隱藏膚色欄位
        // 建立響應式設計的內容調整器
        // 強制使用小視窗模式 - 即只顯示青蘋果綠欄位，隱藏膚色欄位
        setupResponsiveLayout(primaryStage, mainContentBox, leftScrollPane, searchButton);
        
        // --- Apply Theme and Show Stage ---
        uiManager.updateTheme(true); // 強制使用深色模式
        
        // 確保樣式更新被應用
        String cssUrl = getClass().getResource("/bigproject/modern_dark_theme.css").toExternalForm();
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(cssUrl);
        
        primaryStage.setScene(mainScene);
        
        // 確保在應用程式完全啟動後設置正確的佈局模式
        Platform.runLater(() -> {
            // 確保右側面板的TextArea區域被正確初始化
            featuresArea.setPrefHeight(100);
            prosArea.setPrefHeight(100);
            consArea.setPrefHeight(100);
            
            // 確保分頁系統得到初始化並正確顯示
            mainLayout.setBottom(tabBar);
            tabBar.setVisible(true);
            tabBar.setManaged(true);
            tabBar.toFront();
            
            // 在初始化後強制進行一次佈局更新
            updatePanelSizes(mainContentBox, leftScrollPane, primaryStage.getWidth(), primaryStage.getHeight());
            
            // 調整主佈局，確保分頁欄位於底部且無邊距
            mainLayout.setPadding(new Insets(15, 15, 0, 0));
            
            // 設置分頁欄邊距為0，確保其貼緊視窗底部
            BorderPane.setMargin(tabBar, new Insets(0, 0, 0, 0));
            
            // 強制更新佈局
            mainLayout.layout();
            
            System.out.println("應用程式佈局初始化完成，分頁欄已設置");
        });

        // --- Initial Data Load ---
        loadAndDisplayRestaurantData("Haidai Roast Shop.json");
        
        // 創建默認的第一個分頁
        createNewTab("海大燒臘", "Haidai Roast Shop.json");
        
        // 底部加入分頁欄 (為了確保初始載入時分頁欄可見)
        mainLayout.setBottom(tabBar);
        tabBar.setVisible(true);
        tabBar.setManaged(true);
        tabBar.toFront(); // 確保分頁欄在最前端

        // --- Position Stage ---
        try {
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - mainScene.getWidth()) / 2);
            primaryStage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - mainScene.getHeight()) / 2);
        } catch (Exception e) {
            // Ignore
        }
        
        // 顯示主舞台
        primaryStage.show();
        
        // 使用新的動畫效果取代原來的淡入效果
        animateMainInterfaceElements(primaryStage);
        
        // 在窗口顯示後再次確保它在最上層和最大化狀態
        Platform.runLater(() -> {
            primaryStage.setAlwaysOnTop(false);
            primaryStage.setAlwaysOnTop(true);
            primaryStage.requestFocus(); // 請求焦點
            primaryStage.setMaximized(true); // 再次確保最大化
            
            // 確保分頁欄顯示在全螢幕模式
            ensureTabBarVisible();
            tabBar.toFront(); // 確保分頁欄在最前層
            
            // 確保佈局正確更新
            updatePanelSizes(mainContentBox, leftScrollPane, primaryStage.getWidth(), primaryStage.getHeight());
            
            // 調整主布局的邊距，確保分頁欄貼緊底部
            mainLayout.setPadding(new Insets(15, 15, 0, 0)); // 頂部和側邊有邊距，底部無邊距
            
            // 移除主內容區域的底部邊距
            VBox.setMargin(mainContentBox, new Insets(0, 0, 0, 0));
            
            // 設置分頁欄位置，確保其貼緊視窗底部
            BorderPane.setMargin(tabBar, new Insets(0, 0, 0, 0));
            tabBar.setPadding(new Insets(5, 10, 0, 10));
            tabBar.setSnapToPixel(true);
            
            // 強制更新佈局，確保變更生效
            mainLayout.layout();
            tabBar.layout();
            
            // 延遲 100ms 再次確認佈局，解決某些顯示問題
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    Platform.runLater(() -> {
                        ensureTabBarVisible();
                        // 二次確認邊距設置
                        mainLayout.setPadding(new Insets(15, 15, 0, 0));
                        BorderPane.setMargin(tabBar, new Insets(0, 0, 0, 0));
                        // 強制更新佈局
                        mainLayout.layout();
                        tabBar.layout();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });

        // --- Button Actions (Search Button) ---
        searchButton.setOnAction(event -> {
            String address = searchField.getText();
            if (address != null && !address.trim().isEmpty()) {
                String trimmedAddress = address.trim();
                if (trimmedAddress.equalsIgnoreCase("Haidai Roast Shop") || trimmedAddress.contains("海大")) {
                    loadAndDisplayRestaurantData("Haidai Roast Shop.json");
                } else if (trimmedAddress.equalsIgnoreCase("Sea Side Eatery") || trimmedAddress.contains("海那邊")) {
                    loadAndDisplayRestaurantData("Sea Side Eatery Info.json");
                } else {
                    openMapInBrowser(trimmedAddress);
                    clearRestaurantDataDisplay("搜尋結果：" + trimmedAddress);
                }
                
                // 更新搜索歷史
                updateSearchHistory(trimmedAddress);
            } else {
                // 空搜尋不做任何事
                System.out.println("Search field is empty. No action taken.");
            }
        });
        
        // 搜索欄按Enter鍵也觸發搜索
        searchField.setOnAction(searchButton.getOnAction());

        // --- API Key Check ---
        if (API_KEY == null || API_KEY.isEmpty()) {
             // Consider showing an alert using uiManager.showErrorDialog if needed
             System.out.println("Warning: Google Maps API Key not found or empty.");
        }

        // --- Add keyboard shortcut for search history (Cmd+T/Ctrl+T) ---
        mainScene.setOnKeyPressed(event -> {
            // Check for Cmd+T (Mac) or Ctrl+T (Windows/Linux)
            if (event.isShortcutDown() && event.getCode() == javafx.scene.input.KeyCode.T) {
                // 檢查對話框是否已經顯示，或者是否在緩衝時間內
                if (searchDialog != null && searchDialog.isShowing()) {
                    // 對話框已經顯示，不做任何動作，讓對話框自己處理這個快捷鍵
                    return;
                }
                
                // 檢查是否太快嘗試重新顯示對話框
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDialogCloseTime < DIALOG_REOPEN_DELAY) {
                    // 距離上次關閉時間太短，不處理此事件
                    event.consume();
                    return;
                }
                
                toggleSearchHistoryDialog(primaryStage);
                event.consume();
            }
        });

        // 設置窗口監聽以更新搜尋按鈕大小
        final String fullButtonText = "搜尋";
        final String compactButtonText = "+";
        
        // 初始調整按鈕大小
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            // 當寬度小於800像素時使用緊湊顯示
            if (newVal.doubleValue() < 800) {
                searchButton.setText(compactButtonText);
                searchButton.setPrefWidth(40);
            } else {
                searchButton.setText(fullButtonText);
                searchButton.setPrefWidth(75);
            }
        });
        
        // 設置初始按鈕文字（基於初始窗口大小）
        Platform.runLater(() -> {
            if (primaryStage.getWidth() < 800) {
                searchButton.setText(compactButtonText);
                searchButton.setPrefWidth(40);
            } else {
                searchButton.setText(fullButtonText);
                searchButton.setPrefWidth(75);
            }
        });
    }

    private VBox createCompetitorEntry(String displayName, String jsonFilePath) {
        VBox entryBox = new VBox(5);
        entryBox.setId("competitor-entry");
        entryBox.setStyle("-fx-background-color: white; -fx-padding: 5; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1;");
        Label nameLabel = new Label(displayName);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");

        Button loadDataButton = new Button("載入資料");
        loadDataButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white;");
        loadDataButton.setOnAction(e -> {
            System.out.println("Load Data button clicked for: " + displayName);
            loadAndDisplayRestaurantData(jsonFilePath);
        });

        Button showOnMapButton = new Button("在地圖上顯示");
        showOnMapButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white;");
        showOnMapButton.setOnAction(e -> {
            System.out.println("Map Button clicked for: " + displayName);
            String mapQuery = displayName;
             if ("Haidai Roast Shop".equals(displayName)) mapQuery = "海大燒臘";
             else if ("Sea Side Eatery".equals(displayName)) mapQuery = "海那邊小食堂 基隆";
            openMapInBrowser(mapQuery);
        });

        HBox buttonBox = new HBox(5, loadDataButton, showOnMapButton);
        entryBox.getChildren().addAll(nameLabel, buttonBox);
        return entryBox;
    }

    private void openMapInBrowser(String query) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String url = "https://www.google.com/maps/search/?api=1&query=" + encodedQuery;
                // System.out.println("Opening URL: " + url);
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                if (uiManager != null) uiManager.showErrorDialog("地圖錯誤", "無法開啟瀏覽器顯示地圖。");
            }
        } else {
             if (uiManager != null) uiManager.showErrorDialog("地圖錯誤", "此平台不支援開啟瀏覽器。");
        }
    }

    // --- Data Handling Methods (Delegated to DataManager) ---
    private void loadAndDisplayRestaurantData(String jsonFilePath) {
        dataManager.loadAndDisplayRestaurantData(jsonFilePath, ratingsHeader, ratingsBox, ratingBars, reviewsArea, photosContainer, featuresArea, prosArea, consArea);
        
        // 計算平均消費中位數
        String medianExpense = calculateMedianExpense(jsonFilePath);
        
        // 更新標題以包含消費資訊
        String originalTitle = "綜合評分";
        ratingsHeader.setText(originalTitle);
        
        // 在評分區域添加一個專門顯示平均消費的標籤
        if (ratingsBox != null && !medianExpense.equals("未知")) {
            // 檢查是否已經有消費標籤
            boolean hasExpenseLabel = false;
            for (Node node : ratingsBox.getChildren()) {
                if (node instanceof HBox && ((HBox) node).getId() != null && ((HBox) node).getId().equals("expenseBox")) {
                    hasExpenseLabel = true;
                    // 更新現有標籤
                    HBox expenseBox = (HBox) node;
                    // 第二個元素是VBox，不是Label
                    VBox labelBox = (VBox) expenseBox.getChildren().get(1);
                    // 從labelBox中獲取第二個元素，即expenseValueLabel
                    Label expenseValueLabel = (Label) labelBox.getChildren().get(1);
                    expenseValueLabel.setText(medianExpense);
                    break;
                }
            }
            
            // 如果沒有消費標籤，創建一個新的
            if (!hasExpenseLabel) {
                HBox expenseBox = new HBox(10);
                expenseBox.setId("expenseBox");
                expenseBox.setAlignment(Pos.CENTER_LEFT);
                expenseBox.setPadding(new Insets(10, 10, 10, 10));
                expenseBox.setStyle("-fx-background-color: rgba(111, 103, 50, 0.15); -fx-background-radius: 5; -fx-border-color: rgba(111, 103, 50, 0.3); -fx-border-radius: 5; -fx-border-width: 1;");
                
                // 創建一個小圖標區域
                StackPane iconPane = new StackPane();
                iconPane.setMinSize(24, 24);
                iconPane.setMaxSize(24, 24);
                iconPane.setStyle("-fx-background-color: #3A7734; -fx-background-radius: 12;");
                
                Label iconLabel = new Label("$");
                iconLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                iconPane.getChildren().add(iconLabel);
                
                // 創建標籤和值的VBox
                VBox labelBox = new VBox(3);
                
                Label expenseLabel = new Label("平均消費中位數");
                expenseLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + "; -fx-font-weight: bold;");
                
                Label expenseValueLabel = new Label(medianExpense);
                expenseValueLabel.setStyle("-fx-text-fill: #3A7734; -fx-font-weight: bold; -fx-font-size: 14px;");
                
                labelBox.getChildren().addAll(expenseLabel, expenseValueLabel);
                
                // 將圖標和標籤添加到HBox
                expenseBox.getChildren().addAll(iconPane, labelBox);
                
                // 添加分隔線
                Separator separator = new Separator();
                separator.setStyle("-fx-background-color: " + PALE_DARK_YELLOW + "; -fx-opacity: 0.3;");
                
                // 將消費標籤和分隔線添加到評分區域的頂部
                ratingsBox.getChildren().add(0, expenseBox);
                ratingsBox.getChildren().add(1, separator);
            }
        }
        
        // 更新近期評論顯示 - 預設選中近一個月按鈕
        int selectedTimeRange = 30; // 默認一個月
        
        // 找到按鈕並設置為活躍狀態
        for (Node node : rightPanel.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                for (Node child : hbox.getChildren()) {
                    if (child instanceof HBox) {
                        HBox timeButtonsContainer = (HBox) child;
                        
                        for (Node btn : timeButtonsContainer.getChildren()) {
                            if (btn instanceof Button) {
                                Button timeBtn = (Button) btn;
                                
                                // 重設所有按鈕樣式
                                if (timeBtn.getText().equals("近一天")) {
                                    timeBtn.setStyle("-fx-background-color: #DDDDDD; -fx-text-fill: #555555; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;");
                                } else if (timeBtn.getText().equals("近一週")) {
                                    timeBtn.setStyle("-fx-background-color: #DDDDDD; -fx-text-fill: #555555; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;");
                                } else if (timeBtn.getText().equals("近一個月")) {
                                    // 將近一個月按鈕設為活躍狀態
                                    timeBtn.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;");
                                    
                                    // 直接觸發近一個月按鈕的點擊事件
                                    System.out.println("觸發近一個月按鈕點擊事件");
                                    timeBtn.fire();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 使用選定的時間範圍強制更新顯示
        System.out.println("loadAndDisplayRestaurantData: 更新時間範圍為 " + selectedTimeRange + " 天");
        
        // 立即更新評論顯示
        updateRecentReviewsDisplay(selectedTimeRange);
        
        // 確保在UI更新完成後也執行一次更新，以防止任何時序問題
        Platform.runLater(() -> {
            System.out.println("UI更新後再次強制更新近一個月評論顯示");
            updateRecentReviewsDisplay(30);
        });
    }

     private void clearRestaurantDataDisplay(String message) {
         dataManager.clearRestaurantDataDisplay(message, ratingsHeader, ratingsBox, ratingBars, reviewsArea, photosContainer, featuresArea, prosArea, consArea);
    }

    /**
     * 添加淡入效果和彈跳動畫到主窗口
     */
    private void addFadeInWithBounceEffect(Node node, Stage stage) {
        // 使用 AnimationManager 提供的效果，而不是手動創建動畫
        AnimationManager.zoomIn(node, Duration.millis(2000));
    }
    
    /**
     * 顯示主界面動畫效果
     * 在 start 方法最後調用，使各元素有序地顯示
     */
    private void animateMainInterfaceElements(Stage primaryStage) {
        // 先讓主佈局顯示出來
        mainLayout.setOpacity(1);
        
        // 依次添加動畫效果到各個主要元素
        
        // 1. 頂部欄動畫效果
        Node topBar = mainLayout.getTop();
        if (topBar != null) {
            AnimationManager.slideInFromTop(topBar, Duration.millis(800));
        }
        
        // 2. 搜尋欄動畫效果 (延遲300毫秒)
        javafx.animation.PauseTransition pause1 = new javafx.animation.PauseTransition(Duration.millis(300));
        pause1.setOnFinished(e -> {
            // 尋找搜尋欄
            if (mainContainer.getChildren().size() > 0) {
                Node searchContainer = mainContainer.getChildren().get(0);
                AnimationManager.slideInFromTop(searchContainer, Duration.millis(600));
            }
        });
        pause1.play();
        
        // 3. 左側面板動畫效果 (延遲600毫秒)
        javafx.animation.PauseTransition pause2 = new javafx.animation.PauseTransition(Duration.millis(600));
        pause2.setOnFinished(e -> {
            if (leftScrollPane != null) {
                AnimationManager.slideInFromLeft(leftScrollPane, Duration.millis(800));
            }
        });
        pause2.play();
        
        // 4. 右側面板動畫效果 (延遲900毫秒)
        javafx.animation.PauseTransition pause3 = new javafx.animation.PauseTransition(Duration.millis(900));
        pause3.setOnFinished(e -> {
            if (rightPanel != null) {
                AnimationManager.slideInFromRight(rightPanel, Duration.millis(800));
            }
        });
        pause3.play();
        
        // 5. 底部分頁欄動畫效果 (延遲1200毫秒)
        javafx.animation.PauseTransition pause4 = new javafx.animation.PauseTransition(Duration.millis(1200));
        pause4.setOnFinished(e -> {
            if (tabBar != null) {
                AnimationManager.slideInFromBottom(tabBar, Duration.millis(600));
                
                // 添加分頁按鈕的序列動畫
                javafx.animation.PauseTransition pause5 = new javafx.animation.PauseTransition(Duration.millis(300));
                pause5.setOnFinished(event -> {
                    if (tabBar.getChildren().size() > 0) {
                        AnimationManager.showChildrenSequentially(tabBar, 100);
                    }
                });
                pause5.play();
            }
        });
        pause4.play();
    }

    /**
     * 更新搜索歷史
     */
    private void updateSearchHistory(String query) {
        if (searchHistory.contains(query)) {
            searchHistory.remove(query);
        }
        searchHistory.add(0, query);
        while (searchHistory.size() > 10) {
            searchHistory.remove(searchHistory.size() - 1);
        }
    }

    /**
     * 顯示或隱藏搜索歷史對話框 (Cmd+T/Ctrl+T)
     */
    private void toggleSearchHistoryDialog(Stage primaryStage) {
        // 先檢查是否在防反彈時間內
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDialogCloseTime < DIALOG_REOPEN_DELAY) {
            // 太快嘗試重新打開，直接忽略這次請求
            return;
        }
        
        if (searchDialog != null && searchDialog.isShowing()) {
            searchDialog.close();
            searchDialog = null;
            lastDialogCloseTime = System.currentTimeMillis();
            return;
        }
        
        // 檢查是否太快嘗試重新顯示對話框
        if (currentTime - lastDialogCloseTime < DIALOG_REOPEN_DELAY) {
            // 距離上次關閉太近，不重新打開
            return;
        }
        
        // 創建一個半透明背景的對話框
        searchDialog = new Stage();
        searchDialog.initModality(Modality.NONE);
        searchDialog.initOwner(primaryStage);
        searchDialog.initStyle(StageStyle.TRANSPARENT);
        
        // 設置對話框位置和大小 - 正確置中
        searchDialog.setX(primaryStage.getX() + (primaryStage.getWidth() - 600) / 2);
        searchDialog.setY(primaryStage.getY() + (primaryStage.getHeight() - 500) / 2);
        searchDialog.setWidth(600);
        searchDialog.setHeight(500);
        
        // 創建主容器
        VBox root = new VBox(10);
        root.getStyleClass().add("search-history-dialog");
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20, 20, 20, 20));
        
        // 創建標題
        Label title = new Label("搜尋");
        title.getStyleClass().add("search-history-title");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
        
        // 創建搜索欄
        TextField searchField = new TextField();
        searchField.setPromptText("搜尋...");
        searchField.setPrefWidth(550);
        searchField.setPrefHeight(40);
        searchField.getStyleClass().add("search-history-field");
        searchField.setStyle("-fx-background-radius: 20; -fx-padding: 5 15 5 15; -fx-font-size: 14px;");
        
        // 創建歷史記錄列表
        VBox historyList = new VBox(5);
        historyList.setPadding(new Insets(15, 0, 0, 0));
        historyList.setAlignment(Pos.TOP_LEFT);
        
        // 建立歷史項目列表，用於鍵盤導航
        List<HBox> historyItems = new ArrayList<>();
        
        // 添加歷史記錄項目
        for (String item : searchHistory) {
            HBox historyItem = createHistoryItem(item, searchDialog, searchField);
            historyItems.add(historyItem);
            historyList.getChildren().add(historyItem);
        }
        
        // 添加搜索邏輯
        searchField.setOnAction(e -> {
            String text = searchField.getText().trim();
            if (!text.isEmpty()) {
                // 更新搜索歷史
                updateSearchHistory(text);
                
                // 關閉對話框並執行搜索
                searchDialog.close();
                searchDialog = null;
                handleSearch(text);
            }
        });
        
        // 添加捲動功能
        ScrollPane scrollPane = new ScrollPane(historyList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("search-history-scroll");
        scrollPane.setPannable(true);
        
        // 設置更合適的樣式，確保滾動條可見並增加邊距
        scrollPane.setStyle("-fx-background-color: transparent; " +
                           "-fx-border-color: transparent; " +
                           "-fx-padding: 0; " +
                           "-fx-background-insets: 0; " +
                           "-fx-border-width: 0;");
                           
        // 添加拖拽滾動功能
        final double[] dragStartY = {0};
        final double[] initialVvalue = {0};
        
        scrollPane.setOnMousePressed(event -> {
            dragStartY[0] = event.getY();
            initialVvalue[0] = scrollPane.getVvalue();
            event.consume();
        });
        
        scrollPane.setOnMouseDragged(event -> {
            double deltaY = dragStartY[0] - event.getY();
            double contentHeight = historyList.getHeight();
            double viewportHeight = scrollPane.getHeight();
            
            // 調整滾動量，使滾動更流暢
            if (contentHeight > viewportHeight) {
                double scrollableRange = contentHeight - viewportHeight;
                double newVvalue = initialVvalue[0] + (deltaY / scrollableRange);
                scrollPane.setVvalue(Math.min(1, Math.max(0, newVvalue)));
            }
            
            event.consume();
        });
        
        // 捕捉滾輪事件
        scrollPane.setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.25; // 減少滾動速度
            scrollPane.setVvalue(scrollPane.getVvalue() - deltaY / historyList.getHeight());
            event.consume(); // 防止事件傳播
        });
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // 組裝UI
        root.getChildren().addAll(title, searchField, scrollPane);
        
        Scene dialogScene = new Scene(root);
        dialogScene.setFill(Color.TRANSPARENT);
        
        // 載入CSS
        dialogScene.getStylesheets().add(getClass().getResource("/bigproject/modern_dark_theme.css").toExternalForm());
        
        // 當前選中項目索引（用於鍵盤導航）
        final int[] selectedIndex = {-1};
        
        // 添加焦點和鍵盤導航
        dialogScene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE:
                    searchDialog.close();
                    searchDialog = null;
                    e.consume(); // 確保事件不會傳播
                    break;
                case T:
                    // 支持在搜索對話框中再次按下 Cmd+T/Ctrl+T 來關閉對話框
                    if (e.isShortcutDown()) {
                        searchDialog.close();
                        searchDialog = null;
                        e.consume(); // 確保事件不會傳播到主場景
                    }
                    break;
                case DOWN:
                    // 向下移動選擇
                    if (historyItems.size() > 0) {
                        // 取消當前選中項的樣式
                        if (selectedIndex[0] >= 0 && selectedIndex[0] < historyItems.size()) {
                            historyItems.get(selectedIndex[0]).setStyle("-fx-background-color: transparent;");
                        }
                        
                        // 下移選擇
                        selectedIndex[0] = (selectedIndex[0] + 1) % historyItems.size();
                        
                        // 設置新選中項樣式
                        historyItems.get(selectedIndex[0]).setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-background-radius: 5;");
                        
                        // 確保選中項可見
                        double contentHeight = historyList.getHeight();
                        double viewportHeight = scrollPane.getViewportBounds().getHeight();
                        double scrollTop = scrollPane.getVvalue() * (contentHeight - viewportHeight);
                        double scrollBottom = scrollTop + viewportHeight;
                        
                        Node selectedNode = historyItems.get(selectedIndex[0]);
                        double nodeTop = selectedNode.getBoundsInParent().getMinY();
                        double nodeBottom = selectedNode.getBoundsInParent().getMaxY();
                        
                        if (nodeTop < scrollTop) {
                            // 節點在視圖上方，需要上滾
                            scrollPane.setVvalue(nodeTop / (contentHeight - viewportHeight));
                        } else if (nodeBottom > scrollBottom) {
                            // 節點在視圖下方，需要下滾
                            scrollPane.setVvalue((nodeBottom - viewportHeight) / (contentHeight - viewportHeight));
                        }
                    }
                    e.consume();
                    break;
                case UP:
                    // 向上移動選擇
                    if (historyItems.size() > 0) {
                        // 取消當前選中項的樣式
                        if (selectedIndex[0] >= 0 && selectedIndex[0] < historyItems.size()) {
                            historyItems.get(selectedIndex[0]).setStyle("-fx-background-color: transparent;");
                        }
                        
                        // 上移選擇
                        selectedIndex[0] = (selectedIndex[0] - 1 + historyItems.size()) % historyItems.size();
                        
                        // 設置新選中項樣式
                        historyItems.get(selectedIndex[0]).setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-background-radius: 5;");
                        
                        // 確保選中項可見
                        double contentHeight = historyList.getHeight();
                        double viewportHeight = scrollPane.getViewportBounds().getHeight();
                        double scrollTop = scrollPane.getVvalue() * (contentHeight - viewportHeight);
                        double scrollBottom = scrollTop + viewportHeight;
                        
                        Node selectedNode = historyItems.get(selectedIndex[0]);
                        double nodeTop = selectedNode.getBoundsInParent().getMinY();
                        double nodeBottom = selectedNode.getBoundsInParent().getMaxY();
                        
                        if (nodeTop < scrollTop) {
                            // 節點在視圖上方，需要上滾
                            scrollPane.setVvalue(nodeTop / (contentHeight - viewportHeight));
                        } else if (nodeBottom > scrollBottom) {
                            // 節點在視圖下方，需要下滾
                            scrollPane.setVvalue((nodeBottom - viewportHeight) / (contentHeight - viewportHeight));
                        }
                    }
                    e.consume();
                    break;
                case ENTER:
                    // 如果有選中項，則使用該項
                    if (selectedIndex[0] >= 0 && selectedIndex[0] < historyItems.size()) {
                        String selectedText = searchHistory.get(selectedIndex[0]);
                        searchDialog.close();
                        searchDialog = null;
                        handleSearch(selectedText);
                        e.consume();
                    }
                    break;
            }
        });
        
        // 搜索欄文字變化時更新列表
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            historyList.getChildren().clear();
            historyItems.clear();
            selectedIndex[0] = -1;
            
            // 過濾歷史記錄
            for (String item : searchHistory) {
                if (newText.isEmpty() || item.toLowerCase().contains(newText.toLowerCase())) {
                    HBox historyItem = createHistoryItem(item, searchDialog, searchField);
                    historyItems.add(historyItem);
                    historyList.getChildren().add(historyItem);
                }
            }
        });
        
        // 點擊外部關閉
        root.setOnMouseClicked(e -> {
            if (e.getTarget() == root) {
                searchDialog.close();
                searchDialog = null;
                // 更新對話框關閉時間戳
                lastDialogCloseTime = System.currentTimeMillis();
                e.consume(); // 防止事件傳播
            }
        });
        
        // 確保對話框在關閉時徹底清理引用
        searchDialog.setOnHidden(e -> {
            searchDialog = null;
            // 更新對話框關閉時間戳
            lastDialogCloseTime = System.currentTimeMillis();
        });
        
        // 添加漂亮的淡入效果，使用更長的動畫時間提高穩定性
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);
        
        // 添加輕微向上滑動效果，增加視覺平滑感
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), root);
        slideIn.setFromY(10);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        
        // 同時執行兩個動畫
        ParallelTransition animation = new ParallelTransition(fadeIn, slideIn);
        
        // 顯示對話框並聚焦搜索欄
        searchDialog.setScene(dialogScene);
        searchDialog.show();
        animation.play();
        
        // 動畫結束後請求焦點
        animation.setOnFinished(e -> {
            Platform.runLater(() -> {
                searchField.requestFocus();
                searchField.selectAll();
            });
        });
    }
    
    /**
     * 創建歷史記錄項目
     */
    private HBox createHistoryItem(String text, Stage dialog, TextField searchField) {
        HBox item = new HBox(10);
        item.getStyleClass().add("search-history-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 15, 10, 15));
        item.setMinHeight(40);
        
        // 歷史記錄圖標
        Label icon = new Label("🕒");
        icon.getStyleClass().add("search-history-item-icon");
        icon.setStyle("-fx-font-size: 14px;");
        
        // 歷史記錄文字
        Label label = new Label(text);
        label.getStyleClass().add("search-history-item-text");
        label.setStyle("-fx-font-size: 14px;");
        HBox.setHgrow(label, Priority.ALWAYS);
        
        // 添加滑鼠事件
        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-background-radius: 5;");
            // 改變滑鼠指標為手形，提示可點擊
            item.setCursor(javafx.scene.Cursor.HAND);
        });
        
        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: transparent;");
            item.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        item.setOnMousePressed(e -> {
            // 點擊效果
            item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3); -fx-background-radius: 5;");
        });
        
        item.setOnMouseReleased(e -> {
            // 恢復懸停效果
            item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-background-radius: 5;");
        });
        
        item.setOnMouseClicked(e -> {
            if (text != null && !text.trim().isEmpty()) {
                searchField.setText(text);
                dialog.close();
                searchDialog = null;
                handleSearch(text);
                e.consume(); // 確保事件不傳播
            }
        });
        
        item.getChildren().addAll(icon, label);
        return item;
    }
    
    /**
     * 處理搜索請求
     */
    private void handleSearch(String query) {
        if (query != null && !query.trim().isEmpty()) {
            String trimmedQuery = query.trim();
            if (trimmedQuery.equalsIgnoreCase("Haidai Roast Shop") || trimmedQuery.contains("海大")) {
                loadAndDisplayRestaurantData("Haidai Roast Shop.json");
            } else if (trimmedQuery.equalsIgnoreCase("Sea Side Eatery") || trimmedQuery.contains("海那邊")) {
                loadAndDisplayRestaurantData("Sea Side Eatery Info.json");
            } else {
                openMapInBrowser(trimmedQuery);
                clearRestaurantDataDisplay("搜尋結果：" + trimmedQuery);
            }
        }
        // 空查詢不執行任何操作
    }

    /**
     * 實現 UIManager.StateChangeListener 接口的方法
     */
    @Override
    public void onMonthlyReportStateChanged(boolean isShowing) {
        isReportActive[0] = isShowing;
        reportButton.setStyle(isShowing ? activeButtonStyle : normalButtonStyle);
        
        // 如果顯示月報，確保AI聊天視圖被隱藏
        if (isShowing && isAIChatActive[0]) {
            hideAIChatView();
        }
        
        // 如果顯示月報，確保設定視圖關閉
        if (isShowing && isSettingsActive[0]) {
            isSettingsActive[0] = false;
            settingsButton.setStyle(normalButtonStyle);
        }
    }
    
    @Override
    public void onSuggestionsStateChanged(boolean isShowing) {
        isSuggestionActive[0] = isShowing;
        suggestionButton.setStyle(isShowing ? activeButtonStyle : normalButtonStyle);
        
        // 如果顯示建議，確保AI聊天視圖被隱藏
        if (isShowing && isAIChatActive[0]) {
            hideAIChatView();
        }
        
        // 如果顯示建議，確保設定視圖關閉
        if (isShowing && isSettingsActive[0]) {
            isSettingsActive[0] = false;
            settingsButton.setStyle(normalButtonStyle);
        }
    }
    
    // 切換顯示AI聊天視圖
    private void toggleAIChatView(String title, String initialContent, String contentType) {
        // 如果AI聊天已經是活躍的，先隱藏它
        if (isAIChatActive[0]) {
            hideAIChatView();
            return;
        }
        
        // 確保報告和建議視圖都被關閉
        if (isReportActive[0]) {
            uiManager.toggleMonthlyReport();
            isReportActive[0] = false;
            reportButton.setStyle(normalButtonStyle);
        }
        
        if (isSuggestionActive[0]) {
            uiManager.toggleSuggestionsView();
            isSuggestionActive[0] = false;
            suggestionButton.setStyle(normalButtonStyle);
        }
        
        // 保存聊天內容類型和初始內容
        currentChatContentType = contentType;
        currentChatInitialContent = initialContent;
        
        // 創建或顯示AI聊天視圖
        showAIChatView(title, initialContent, contentType);
        
        // 更新狀態標誌
        isAIChatActive[0] = true;
    }
    
    // 隱藏AI聊天視圖
    private void hideAIChatView() {
        if (aiChatContainer != null && mainLayout.getCenter() == aiChatContainer) {
            // 恢復原來的主內容
            mainLayout.setCenter(mainContainer); // 直接使用已存在的mainContainer
            
            // 更新狀態標誌
            isAIChatActive[0] = false;
        }
    }
    
    // 顯示AI聊天視圖（在主界面上而非彈出窗口）
    private void showAIChatView(String title, String initialContent, String contentType) {
        // 創建AI聊天容器
        aiChatContainer = new VBox(15);
        aiChatContainer.setPadding(new Insets(20));
        aiChatContainer.setStyle("-fx-background-color: #2C2C2C;");
        
        // 創建頂部欄，包含標題和返回按鈕
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 15, 0));
        
        // 返回按鈕
        aiChatBackButton = new Button("← 返回");
        aiChatBackButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCCCCC; -fx-cursor: hand;");
        aiChatBackButton.setOnAction(e -> hideAIChatView());
        
        // 添加懸停效果
        aiChatBackButton.setOnMouseEntered(e -> {
            aiChatBackButton.setStyle("-fx-background-color: #444444; -fx-text-fill: white; -fx-cursor: hand;");
        });
        
        aiChatBackButton.setOnMouseExited(e -> {
            aiChatBackButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCCCCC; -fx-cursor: hand;");
        });
        
        // 標題標籤
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setStyle("-fx-text-fill: white;");
        
        topBar.getChildren().addAll(aiChatBackButton, titleLabel);
        
        // 創建聊天記錄區域
        chatHistoryArea = new TextArea();
        chatHistoryArea.setEditable(false);
        chatHistoryArea.setWrapText(true);
        chatHistoryArea.setPrefHeight(400);
        chatHistoryArea.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 14px;");
        VBox.setVgrow(chatHistoryArea, Priority.ALWAYS);
        
        // 初始化聊天記錄，添加AI歡迎消息
        StringBuilder chatHistory = new StringBuilder();
        chatHistory.append("AI助手: 您好！我們來討論這家餐廳的").append(contentType).append("。\n\n");
        chatHistory.append("以下是分析結果：\n").append(initialContent).append("\n\n");
        chatHistory.append("AI助手: 您對這些").append(contentType).append("有什麼想法或問題嗎？\n");
        chatHistoryArea.setText(chatHistory.toString());
        
        // 創建用戶輸入區域
        userInputField = new TextField();
        userInputField.setPromptText("輸入您的問題或想法...");
        userInputField.setPrefHeight(40);
        userInputField.setStyle("-fx-background-color: #444444; -fx-text-fill: white; -fx-font-size: 14px;");
        
        // 創建發送按鈕
        Button sendButton = new Button("發送");
        sendButton.setPrefHeight(40);
        sendButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white;");
        
        // 添加懸停效果
        sendButton.setOnMouseEntered(e -> {
            sendButton.setStyle("-fx-background-color: #F08159; -fx-text-fill: white;");
        });
        
        sendButton.setOnMouseExited(e -> {
            sendButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white;");
        });
        
        // 創建輸入區域佈局
        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.getChildren().addAll(userInputField, sendButton);
        HBox.setHgrow(userInputField, Priority.ALWAYS);
        
        // 處理發送消息的邏輯
        Runnable sendMessageAction = () -> {
            String userMessage = userInputField.getText().trim();
            if (!userMessage.isEmpty()) {
                // 添加用戶消息到聊天記錄
                chatHistory.append("\n您: ").append(userMessage).append("\n");
                chatHistoryArea.setText(chatHistory.toString());
                
                // 清空輸入框
                userInputField.clear();
                
                // 添加"AI思考中"的提示
                chatHistory.append("\nAI助手: 思考中...");
                chatHistoryArea.setText(chatHistory.toString());
                
                // 滾動到底部
                chatHistoryArea.positionCaret(chatHistoryArea.getText().length());
                
                // 使用CompletableFuture在後台處理AI響應
                CompletableFuture.runAsync(() -> {
                    try {
                        // 獲取AI響應 (實際調用Ollama API)
                        String aiResponse = callOllamaAPI(userMessage, contentType, initialContent);
                        
                        // 更新UI (必須在JavaFX線程中進行)
                        Platform.runLater(() -> {
                            // 移除"思考中"的提示
                            chatHistory.delete(chatHistory.length() - 13, chatHistory.length());
                            // 添加AI的響應
                            chatHistory.append("\nAI助手: ").append(aiResponse).append("\n");
                            chatHistoryArea.setText(chatHistory.toString());
                            
                            // 滾動到底部
                            chatHistoryArea.positionCaret(chatHistoryArea.getText().length());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            // 處理錯誤情況
                            chatHistory.delete(chatHistory.length() - 13, chatHistory.length());
                            chatHistory.append("\nAI助手: 抱歉，我遇到了一些問題，無法回應您的問題。\n");
                            chatHistoryArea.setText(chatHistory.toString());
                        });
                        e.printStackTrace();
                    }
                });
            }
        };
        
        // 綁定發送按鈕和回車鍵
        sendButton.setOnAction(e -> sendMessageAction.run());
        userInputField.setOnAction(e -> sendMessageAction.run());
        
        // 組裝UI
        aiChatContainer.getChildren().addAll(topBar, chatHistoryArea, inputBox);
        
        // 替換主佈局中的內容
        mainLayout.setCenter(aiChatContainer);
        
        // 聚焦到輸入框
        Platform.runLater(() -> userInputField.requestFocus());
    }

    // 已移除 setupInitialResponsiveLayout 方法，因為它不再被使用且引用了已刪除的方法

    /**
     * 建立響應式設計的版面配置
     * 針對不同螢幕寬度調整元素位置和大小
     */
    private void setupResponsiveLayout(Stage primaryStage, HBox mainContentBox, 
                                      ScrollPane leftScrollPane,
                                      Button searchButton) {
        // 設置最小寬度以確保UI不會被壓縮
        mainContentBox.setMinWidth(400);
        
        // 保存最後一次檢測到的寬度，用於減少不必要的布局切換
        final double[] lastWidth = {primaryStage.getWidth()};
        final double[] lastHeight = {primaryStage.getHeight()};
        
        // 更改視窗大小監聽器，使其能夠即時更新面板大小
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue();
            double oldWidth = oldVal.doubleValue();
            
            // 即時更新面板大小，不需等待顯著變化
            if (Math.abs(width - oldWidth) > 1) {
                updatePanelSizes(mainContentBox, leftScrollPane, width, primaryStage.getHeight());
                
                // 確保分頁欄顯示並與視窗底部貼緊
                ensureTabBarVisible();
                
                // 重新調整主佈局，確保分頁欄在底部位置正確
                mainLayout.layout();
            }
            
            // 調整搜尋按鈕大小
            if (width < 800) {
                searchButton.setText("+");
                searchButton.setPrefWidth(40);
            } else {
                searchButton.setText("搜尋");
                searchButton.setPrefWidth(75);
            }
        });
        
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            double height = newVal.doubleValue();
            double oldHeight = oldVal.doubleValue();
            
            // 即時更新面板高度
            if (Math.abs(height - oldHeight) > 1) {
                adjustPanelHeights(mainContentBox, leftScrollPane, height);
                updatePanelSizes(mainContentBox, leftScrollPane, primaryStage.getWidth(), height);
                
                // 確保分頁欄顯示並與視窗底部貼緊
                ensureTabBarVisible();
                
                // 重新調整主佈局，確保分頁欄在底部位置正確
                mainLayout.layout();
                
                // 延遲確認分頁欄可見性，解決某些情況下分頁欄會消失的問題
                Platform.runLater(() -> {
                    ensureTabBarVisible();
                    mainLayout.layout();
                });
            }
        });
        
        // 初始更新一次確保佈局正確
        Platform.runLater(() -> {
            updatePanelSizes(mainContentBox, leftScrollPane, primaryStage.getWidth(), primaryStage.getHeight());
            
            // 確保分頁欄顯示並與視窗底部貼緊
            ensureTabBarVisible();
            
            // 調整搜尋按鈕大小
            if (primaryStage.getWidth() < 800) {
                searchButton.setText("+");
                searchButton.setPrefWidth(40);
            } else {
                searchButton.setText("搜尋");
                searchButton.setPrefWidth(75);
            }
            
            // 重新調整主佈局，確保分頁欄在底部位置正確
            mainLayout.layout();
        });
    }
    
    /**
     * 調整面板高度以適應視窗高度變化
     */
    private void adjustPanelHeights(HBox mainContentBox, ScrollPane leftScrollPane, double windowHeight) {
        // 計算可用高度（減去頂部工具欄和搜索欄的高度，以及分頁欄的高度）
        double topBarHeight = 60; // 頂部工具欄高度
        double searchBarHeight = 50; // 搜索欄高度
        double tabBarHeight = 45; // 分頁欄高度
        double bottomMargin = 0; // 底部無邊距，確保貼緊分頁欄
        
        // 計算實際可用高度
        double availableHeight = windowHeight - topBarHeight - searchBarHeight - tabBarHeight - bottomMargin - 10; // 減小內邊距確保更貼近分頁欄
        
        // 確保可用高度不小於最小值
        availableHeight = Math.max(availableHeight, 500);
        
        // 設置左側滾動面板高度
        leftScrollPane.setPrefHeight(availableHeight);
        leftScrollPane.setMinHeight(availableHeight);
        
        // 設置右側面板高度與左側一致，但可以完全填充到分頁欄頂部
        if (rightPanel != null) {
            // 讓右側面板完全貼緊分頁欄頂部
            rightPanel.setPrefHeight(availableHeight + 10); // 略微增加高度確保貼近分頁欄
            rightPanel.setMinHeight(availableHeight + 10);
        }
        
        System.out.println("調整面板高度完成 - 可用高度: " + availableHeight + "px");
    }
    
    /**
     * 更新面板大小，確保它們隨視窗大小變化
     */
    private void updatePanelSizes(HBox mainContentBox, ScrollPane leftScrollPane, double width, double height) {
        // 添加靜態標記，防止無限遞迴
        if (isUpdatingPanels) {
            return;
        }
        isUpdatingPanels = true;
        
        try {
            System.out.println("更新面板尺寸: 寬度=" + width + ", 高度=" + height);
            
            // 調整面板高度，確保不超出邊界且貼緊橘線
            // 分頁欄高度為45，橘線厚度約為2
            double tabBarHeight = 45;
            double orangeLineThickness = 2;
            // 計算可用高度，扣除頂部工具欄、搜索欄和分頁欄的高度
            double topBarHeight = 60; // 頂部工具欄高度
            double searchBarHeight = 50; // 搜索欄高度
            double availableHeight = height - topBarHeight - searchBarHeight - tabBarHeight - orangeLineThickness;
            
            // 計算左側和右側面板的寬度 - 修改布局，確保青蘋果欄完全貼緊
            double rightPanelWidth = 450; // 固定青蘋果欄位寬度為450
            double spacing = 0; // 完全移除間距，使青蘋果欄貼緊右側
            double leftPanelWidth = width - rightPanelWidth - spacing - 10; // 縮小左側邊距，確保右側面板完全貼緊
            
            // 確保左側面板不會太窄也不會太寬
            leftPanelWidth = Math.max(leftPanelWidth, 500);
            // 限制左側面板最大寬度不超過可用空間的70%，讓右側面板可以完全貼緊右邊界
            leftPanelWidth = Math.min(leftPanelWidth, (width - 15) * 0.7);
            
            // 應用新的尺寸設置
            leftScrollPane.setPrefWidth(leftPanelWidth);
            leftScrollPane.setMaxWidth(leftPanelWidth);
            leftScrollPane.setPrefHeight(availableHeight);
            leftScrollPane.setMaxHeight(availableHeight);
            
            // 設置左側面板內容的底部內邊距，確保內容不被截斷
            VBox leftPanel = (VBox) leftScrollPane.getContent();
            if (leftPanel != null) {
                leftPanel.setPadding(new Insets(20, 20, 20, 20)); // 增加底部內邊距，確保內容不被截斷
            }
            
            // 確保右側面板寬度和高度能貼緊橘線和右側邊界
            if (rightPanel != null) {
                // 固定寬度為450，確保完全貼緊右側邊界
                rightPanel.setPrefWidth(450);
                rightPanel.setMinWidth(450);
                rightPanel.setMaxWidth(450);
                
                // 調整右側面板內容的內邊距，清除右側邊距，確保完全貼緊右側邊界
                rightPanel.setPadding(new Insets(15, 0, 300, 15)); // 右側邊距為0，大幅增加底部內邊距，確保內容可完全滾動
                
                // 設置高度，確保貼緊橘線
                ScrollPane parentScrollPane = (ScrollPane) rightPanel.getParent().getParent();
                if (parentScrollPane != null) {
                    parentScrollPane.setPrefHeight(availableHeight);
                    parentScrollPane.setMinHeight(Math.min(600, availableHeight));
                    parentScrollPane.setMaxHeight(availableHeight);
                    
                    // 確保背景透明，避免黑色區塊
                    parentScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background: transparent; -fx-padding: 0;");
                    
                    // 強制顯示垂直滾動條，確保內容可滾動
                    parentScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
                    parentScrollPane.setFitToHeight(false); // 確保內容可滾動而非自動縮放
                    parentScrollPane.setVmax(1.0); // 確保可以滾動到底部
                    parentScrollPane.setVvalue(0); // 重置滾動位置到頂部
                    
                    // 修復滾動問題
                    parentScrollPane.setOnScroll(event -> {
                        double deltaY = event.getDeltaY() * 3.0; // 增加滾動靈敏度
                        double newValue = parentScrollPane.getVvalue() - deltaY / rightPanel.getHeight() * 10;
                        parentScrollPane.setVvalue(Math.max(0, Math.min(1, newValue)));
                        event.consume();
                    });
                    
                    // 確保父容器沒有任何邊距，使面板完全貼緊右側邊界
                    parentScrollPane.setPadding(new Insets(0, 0, 0, 0));
                    ((HBox)parentScrollPane.getParent()).setSpacing(0); // 移除HBox間距
                    
                    // 重要：設置合適的面板最小高度，確保內容超出時可滾動
                    rightPanel.setMinHeight(Math.max(2000, availableHeight * 1.5)); // 設定較大的最小高度
                }
            }
            
            // 立即強制佈局更新
            mainContentBox.requestLayout();
            leftScrollPane.requestLayout();
            
            // 不再調用 ensureTabBarVisible() 以避免無限循環
        } finally {
            isUpdatingPanels = false;
        }
    }
    
    // 添加靜態標記，用於防止無限遞迴調用
    private static boolean isUpdatingPanels = false;
    private static boolean isEnsuringTabBar = false;

    // 確保分頁欄顯示並固定在視窗底部
    private void ensureTabBarVisible() {
        // 添加靜態標記，防止無限遞迴
        if (isEnsuringTabBar) {
            return;
        }
        isEnsuringTabBar = true;
        
        try {
            if (mainLayout != null && tabBar != null) {
                // 先檢查分頁欄是否已經添加到 mainLayout
                Node currentBottom = mainLayout.getBottom();
                if (currentBottom != tabBar) {
                    // 只有當目前底部不是分頁欄時才添加
                    mainLayout.setBottom(tabBar);
                }
                
                // 確保分頁欄可見且管理
                tabBar.setVisible(true);
                tabBar.setManaged(true);
                
                // 調整分頁欄樣式，確保完全貼緊橘線
                tabBar.setStyle("-fx-background-color: #2A2A2A; -fx-border-width: 2 0 0 0; -fx-border-color: #E67649; -fx-padding: 5 0 0 10;");
                
                // 確保高度適當
                tabBar.setMinHeight(45);
                tabBar.setPrefHeight(45);
                tabBar.setMaxHeight(45);
                
                // 確保寬度填滿整個視窗寬度
                tabBar.setPrefWidth(Double.MAX_VALUE);
                
                // 移除底部邊距，確保分頁欄完全貼緊視窗底部
                BorderPane.setMargin(tabBar, new Insets(0, 0, 0, 0));
                
                // 確保分頁欄正確貼緊視窗底部
                tabBar.setSnapToPixel(true);
                
                // 如果需要調整面板大小，直接獲取當前維度而不再調用 updatePanelSizes
                // 注釋掉以下代碼以避免無限循環
                /*
                double height = mainLayout.getHeight();
                double width = mainLayout.getWidth();
                if (height > 0 && width > 0) {
                    updatePanelSizes(mainContentBox, leftScrollPane, width, height);
                }
                */
                
                // 確保主佈局知道在底部區域發生了變化
                mainLayout.layout();
                
                // 將分頁欄置於頂層，確保不會被其他元素覆蓋
                tabBar.toFront();
            }
        } finally {
            isEnsuringTabBar = false;
        }
    }

    public static void main(String[] args) {
        // 設置應用程式在系統中顯示的名稱
        System.setProperty("apple.awt.application.name", "Restaurant Analyzer");
        System.setProperty("javafx.preloader", "bigproject.CustomSplashScreen");
        
        launch(args);
    }

    // 添加或更新照片到照片容器的方法
    public void addPhotoToContainer(Image image) {
        // 設置照片的大小 - 使用標準規格，保持一致的大小
        double photoWidth = 153;  // 設置統一的寬度
        double photoHeight = 153; // 設置統一的高度
        
        // 創建StackPane容器來托管照片，確保尺寸固定
        StackPane photoContainer = new StackPane();
        photoContainer.setMaxSize(photoWidth, photoHeight);
        photoContainer.setMinSize(photoWidth, photoHeight);
        photoContainer.setPrefSize(photoWidth, photoHeight);
        photoContainer.setStyle("-fx-background-color: #222222; -fx-padding: 0; -fx-background-insets: 0; -fx-border-width: 0;");
        
        // 創建ImageView顯示照片
        ImageView photoView = new ImageView(image);
        photoView.setFitWidth(photoWidth);
        photoView.setFitHeight(photoHeight);
        photoView.setPreserveRatio(true);
        photoView.setSmooth(true);
        
        // 設置無邊框效果
        photoView.setStyle("-fx-border-color: transparent; -fx-background-color: #222222; -fx-padding: 0; -fx-background-insets: 0;");
        
        // 添加照片到StackPane
        photoContainer.getChildren().add(photoView);
        
        // 設置點擊放大效果
        photoContainer.setOnMouseClicked(e -> {
            openFullSizePhotoViewer(image);
        });
        
        // 設置鼠標懸停效果但不改變大小，保持圖片間距
        photoContainer.setOnMouseEntered(e -> {
            photoView.setOpacity(0.8);
            photoContainer.setCursor(javafx.scene.Cursor.HAND);
        });
        
        photoContainer.setOnMouseExited(e -> {
            photoView.setOpacity(1.0);
            photoContainer.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        // 將照片容器添加到FlowPane中
        photosContainer.getChildren().add(photoContainer);
    }

    /**
     * 打開全屏照片查看器
     */
    private void openFullSizePhotoViewer(Image image) {
        Stage photoStage = new Stage();
        photoStage.initModality(Modality.APPLICATION_MODAL);
        photoStage.setTitle("查看照片");
        
        // 創建包含圖片的面板
        StackPane photoPane = new StackPane();
        photoPane.setStyle("-fx-background-color: #222222;");
        
        // 創建可縮放的ImageView
        ImageView photoView = new ImageView(image);
        photoView.setPreserveRatio(true);
        
        // 計算合適的尺寸，不超過螢幕大小的80%
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double maxWidth = screenBounds.getWidth() * 0.8;
        double maxHeight = screenBounds.getHeight() * 0.8;
        
        // 根據圖片和螢幕尺寸調整窗口大小
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        
        // 如果圖片太大，調整大小以適應螢幕
        if (imageWidth > maxWidth || imageHeight > maxHeight) {
            double widthRatio = maxWidth / imageWidth;
            double heightRatio = maxHeight / imageHeight;
            double scaleFactor = Math.min(widthRatio, heightRatio);
            
            photoView.setFitWidth(imageWidth * scaleFactor);
            photoView.setFitHeight(imageHeight * scaleFactor);
        } else {
            // 使用原始大小
            photoView.setFitWidth(imageWidth);
            photoView.setFitHeight(imageHeight);
        }
        
        // 添加關閉按鈕
        Button closeButton = new Button("關閉");
        closeButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; -fx-background-radius: 20;");
        closeButton.setOnAction(e -> photoStage.close());
        
        // 為關閉按鈕添加懸停效果
        closeButton.setOnMouseEntered(e -> {
            closeButton.setStyle("-fx-background-color: #F08159; -fx-text-fill: white; -fx-background-radius: 20;");
            closeButton.setCursor(javafx.scene.Cursor.HAND);
        });
        
        closeButton.setOnMouseExited(e -> {
            closeButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; -fx-background-radius: 20;");
            closeButton.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        // 使用BorderPane佈局，底部放關閉按鈕
        BorderPane root = new BorderPane();
        root.setCenter(photoPane);
        photoPane.getChildren().add(photoView);
        
        // 底部放置關閉按鈕
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(15));
        buttonBox.getChildren().add(closeButton);
        root.setBottom(buttonBox);
        
        // 設置場景並顯示窗口
        Scene scene = new Scene(root);
        photoStage.setScene(scene);
        
        // 應用CSS樣式
        scene.getStylesheets().add(getClass().getResource("/bigproject/modern_dark_theme.css").toExternalForm());
        
        // 在主窗口中央顯示
        photoStage.setX(Screen.getPrimary().getVisualBounds().getMinX() + 
                        (Screen.getPrimary().getVisualBounds().getWidth() - photoView.getFitWidth()) / 2);
        photoStage.setY(Screen.getPrimary().getVisualBounds().getMinY() + 
                        (Screen.getPrimary().getVisualBounds().getHeight() - photoView.getFitHeight()) / 2);
        
        // 顯示窗口
        photoStage.show();
    }

    // 修改照片滾動面板設置，確保可以正常滾動
    private void setupPhotoScrollPane() {
        photosScroll.setFitToWidth(true);
        photosScroll.setFitToHeight(false);
        photosScroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        photosScroll.setVbarPolicy(ScrollBarPolicy.ALWAYS);
        photosScroll.setPannable(true);
        
        // 設置更好的樣式，確保滾動條可見
        photosScroll.setStyle("-fx-background-color: #222222; " +
                             "-fx-border-color: transparent; " +
                             "-fx-padding: 0; " +
                             "-fx-background-insets: 0; " +
                             "-fx-border-width: 0;");
        
        // 清除並重新設置滾動事件
        photosScroll.setOnMousePressed(null);
        photosScroll.setOnMouseDragged(null);
        photosScroll.setOnScroll(null);
        
        // 添加改進的拖拽滾動功能
        final double[] dragStartY = {0};
        final double[] initialVvalue = {0};
        
        photosScroll.setOnMousePressed(event -> {
            dragStartY[0] = event.getY();
            initialVvalue[0] = photosScroll.getVvalue();
            event.consume();
        });
        
        photosScroll.setOnMouseDragged(event -> {
            double deltaY = dragStartY[0] - event.getY();
            double contentHeight = photosContainer.getHeight();
            double viewportHeight = photosScroll.getHeight();
            
            // 調整滾動量，使滾動更流暢
            if (contentHeight > viewportHeight) {
                double scrollableRange = contentHeight - viewportHeight;
                double newVvalue = initialVvalue[0] + (deltaY / scrollableRange);
                photosScroll.setVvalue(Math.min(1, Math.max(0, newVvalue)));
            }
            
            event.consume();
        });
        
        // 捕捉滾輪事件
        photosScroll.setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.01; // 減少滾動速度
            photosScroll.setVvalue(photosScroll.getVvalue() - deltaY);
            event.consume(); // 防止事件傳播
        });
    }

    /**
     * 顯示添加新分頁的對話框
     */
    private void showAddTabDialog(Stage primaryStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("新增分頁");
        
        VBox dialogVBox = new VBox(10);
        dialogVBox.setPadding(new Insets(15));
        dialogVBox.setAlignment(Pos.CENTER);
        
        Label nameLabel = new Label("請選擇要新增的店家：");
        
        ListView<String> restaurantList = new ListView<>();
        restaurantList.getItems().addAll("海大燒臘", "海那邊小食堂");
        restaurantList.setPrefHeight(150);
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button cancelButton = new Button("取消");
        Button addButton = new Button("新增");
        
        cancelButton.setOnAction(e -> dialog.close());
        
        addButton.setOnAction(e -> {
            String selected = restaurantList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String jsonFile = "";
                if (selected.equals("海大燒臘")) {
                    jsonFile = "Haidai Roast Shop.json";
                } else if (selected.equals("海那邊小食堂")) {
                    jsonFile = "Sea Side Eatery Info.json";
                }
                
                createNewTab(selected, jsonFile);
                dialog.close();
            }
        });
        
        buttonBox.getChildren().addAll(cancelButton, addButton);
        dialogVBox.getChildren().addAll(nameLabel, restaurantList, buttonBox);
        
        Scene dialogScene = new Scene(dialogVBox, 300, 250);
        dialog.setScene(dialogScene);
        dialog.show();
    }
    
    /**
     * 創建新的分頁
     */
    private void createNewTab(String displayName, String jsonFilePath) {
        // 檢查是否已存在相同標題的分頁
        for (TabContent tab : tabContents.values()) {
            if (tab.displayName.equals(displayName)) {
                selectTab(tab.id); // 如果已存在則選中它
                return;
            }
        }
        
        // 生成唯一ID
        String tabId = "tab_" + System.currentTimeMillis();
        
        // 創建分頁按鈕
        HBox tabBox = createTabButton(tabId, displayName, jsonFilePath);
        
        // 添加到分頁欄
        tabBar.getChildren().add(tabBar.getChildren().size() - 1, tabBox);
        
        // 計算平均消費
        String medianExpense = calculateMedianExpense(jsonFilePath);
        
        // 創建並存儲分頁內容
        TabContent content = new TabContent(tabId, displayName, jsonFilePath, tabBox);
        content.medianExpense = medianExpense;
        tabContents.put(tabId, content);
        
        // 選擇新創建的分頁
        selectTab(tabId);
        
        // 確保分頁欄可見
        ensureTabBarVisible();
        
        // 確保近期評論時間範圍按鈕正確初始化，默認選擇近一個月
        for (Node node : rightPanel.getChildren()) {
            if (node instanceof HBox) {
                for (Node child : ((HBox) node).getChildren()) {
                    if (child instanceof HBox) {
                        for (Node timeBtn : ((HBox) child).getChildren()) {
                            if (timeBtn instanceof Button) {
                                Button button = (Button) timeBtn;
                                if (button.getText().equals("近一個月")) {
                                    // 觸發近一個月按鈕的點擊事件
                                    System.out.println("創建新分頁時自動觸發近一個月按鈕點擊");
                                    button.fire();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 為防止按鈕觸發失敗，手動執行一次更新
        Platform.runLater(() -> {
            System.out.println("新分頁創建後手動更新近一個月評論");
            updateRecentReviewsDisplay(30); // 30天
        });
        
        System.out.println("創建了新分頁: " + displayName + " (平均消費: " + medianExpense + ")");
    }
    
    /**
     * 創建分頁按鈕
     */
    private HBox createTabButton(String tabId, String displayName, String jsonFilePath) {
        HBox tabBox = new HBox(5);
        tabBox.setAlignment(Pos.CENTER_LEFT);
        tabBox.setPadding(new Insets(8, 15, 8, 15)); // 增加內邊距以便於點擊
        tabBox.setStyle("-fx-background-color: #333333; -fx-background-radius: 5 5 0 0; -fx-cursor: hand;");
        tabBox.setMinHeight(30); // 確保最小高度
        
        // 設置tabId作為識別屬性，用於在視窗大小變化時識別當前選中的分頁
        tabBox.setId(tabId);
        
        Label tabLabel = new Label(displayName);
        tabLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px;"); // 提高對比度
        
        Button closeButton = new Button("×");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCCCCC; -fx-padding: 0 0 0 5; -fx-font-size: 16px;");
        closeButton.setOnAction(e -> closeTab(tabId));
        
        // 添加懸停效果
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-padding: 0 0 0 5;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCCCCC; -fx-padding: 0 0 0 5;"));
        
        tabBox.getChildren().addAll(tabLabel, closeButton);
        
        // 點擊分頁切換
        tabBox.setOnMouseClicked(e -> {
            selectTab(tabId);
        });
        
        // 添加懸停效果
        tabBox.setOnMouseEntered(e -> {
            if (!tabId.equals(currentTabId)) {
                tabBox.setStyle("-fx-background-color: #444444; -fx-background-radius: 5 5 0 0; -fx-cursor: hand;");
            }
        });
        
        tabBox.setOnMouseExited(e -> {
            if (!tabId.equals(currentTabId)) {
                tabBox.setStyle("-fx-background-color: #333333; -fx-background-radius: 5 5 0 0; -fx-cursor: hand;");
            }
        });
        
        return tabBox;
    }
    
    /**
     * 選擇分頁
     */
    private void selectTab(String tabId) {
        TabContent tab = tabContents.get(tabId);
        if (tab == null) return;
        
        // 保存之前的選擇分頁，用於動畫效果
        String previousTabId = currentTabId;
        
        // 更新所有分頁樣式
        for (TabContent t : tabContents.values()) {
            if (t.id.equals(tabId)) {
                t.tabBox.setStyle("-fx-background-color: #4D4D4D; -fx-background-radius: 5 5 0 0; -fx-cursor: hand;");
                ((Label)t.tabBox.getChildren().get(0)).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                // 增加選中標籤的動畫效果
                AnimationManager.pulse(t.tabBox);
            } else {
                t.tabBox.setStyle("-fx-background-color: #333333; -fx-background-radius: 5 5 0 0; -fx-cursor: hand;");
                ((Label)t.tabBox.getChildren().get(0)).setStyle("-fx-text-fill: #CCCCCC;");
            }
        }
        
        // 切換內容
        currentTabId = tabId;
        
        // 判斷是否需要切換動畫效果
        if (previousTabId != null && !previousTabId.equals(tabId)) {
            // 有切換分頁的情況，加入滑動過渡效果
            
            // 獲取目前捲動位置
            double scrollPosition = leftScrollPane.getVvalue();
            
            // 記錄原本的內容區域
            Node originalContent = leftScrollPane;
            
            // 先載入新的資料
            loadAndDisplayRestaurantData(tab.jsonFilePath);
            
            // 套用動畫效果 (基於分頁在頁籤欄的位置決定方向)
            int direction = 1; // 默認是從右向左滑動
            
            try {
                // 嘗試根據分頁在頁籤欄的位置確定滑動方向
                double prevTabX = tabContents.get(previousTabId).tabBox.getLayoutX();
                double newTabX = tab.tabBox.getLayoutX();
                
                if (prevTabX > newTabX) {
                    // 新分頁在左側
                    direction = -1;
                }
            } catch (Exception e) {
                // 發生錯誤時使用預設方向
                System.err.println("無法判斷分頁切換方向: " + e.getMessage());
            }
            
            // 創建臨時容器以便應用動畫效果
            StackPane tempContainer = new StackPane();
            leftScrollPane.setOpacity(0);
            
            // 應用淡入效果
            AnimationManager.fadeIn(leftScrollPane, Duration.millis(400));
            
            // 恢復原本的捲動位置
            Platform.runLater(() -> leftScrollPane.setVvalue(scrollPosition));
        } else {
            // 直接載入內容，沒有之前的分頁
            loadAndDisplayRestaurantData(tab.jsonFilePath);
            AnimationManager.fadeIn(leftScrollPane, Duration.millis(400));
        }
        
        // 確保分頁欄可見
        ensureTabBarVisible();
        
        System.out.println("選擇分頁: " + tab.displayName + " (平均消費: " + tab.medianExpense + ")");
    }
    
    /**
     * 關閉分頁
     */
    private void closeTab(String tabId) {
        TabContent tab = tabContents.get(tabId);
        if (tab == null) return;
        
        // 不允許關閉最後一個分頁
        if (tabContents.size() <= 1) {
            return;
        }
        
        // 從UI和存儲中移除
        tabBar.getChildren().remove(tab.tabBox);
        tabContents.remove(tabId);
        
        // 如果關閉的是當前分頁，則選擇另一個分頁
        if (tabId.equals(currentTabId)) {
            String nextTabId = tabContents.keySet().iterator().next();
            selectTab(nextTabId);
        }
    }
    
    /**
     * 分頁內容類
     */
    private static class TabContent {
        String id;
        String displayName;
        String jsonFilePath;
        HBox tabBox;
        String medianExpense; // 添加平均消費字段
        
        public TabContent(String id, String displayName, String jsonFilePath, HBox tabBox) {
            this.id = id;
            this.displayName = displayName;
            this.jsonFilePath = jsonFilePath;
            this.tabBox = tabBox;
            this.medianExpense = "未知"; // 默認值
        }
    }

    /**
     * 計算JSON數據中平均消費的中位數
     * @param jsonFilePath JSON文件路徑
     * @return 消費中位數的文字描述
     */
    private String calculateMedianExpense(String jsonFilePath) {
        try {
            // 讀取JSON文件
            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            JSONArray reviews = new JSONArray(content);
            
            // 用於存儲消費範圍的列表
            List<String> expenseRanges = new ArrayList<>();
            
            // 遍歷所有評論
            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                if (!review.isNull("平均每人消費")) {
                    String expense = review.getString("平均每人消費");
                    if (expense != null && !expense.isEmpty()) {
                        expenseRanges.add(expense);
                    }
                }
            }
            
            // 如果沒有數據，返回未知
            if (expenseRanges.isEmpty()) {
                return "未知";
            }
            
            // 解析消費範圍並轉換為數值
            Map<String, Integer> expenseCount = new HashMap<>();
            for (String range : expenseRanges) {
                expenseCount.put(range, expenseCount.getOrDefault(range, 0) + 1);
            }
            
            // 找出出現次數最多的消費範圍
            String mostCommonRange = "";
            int maxCount = 0;
            for (Map.Entry<String, Integer> entry : expenseCount.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    mostCommonRange = entry.getKey();
                }
            }
            
            // 將編碼轉換為可讀的範圍
            String readableRange = convertExpenseCodeToReadable(mostCommonRange);
            
            return readableRange;
        } catch (Exception e) {
            e.printStackTrace();
            return "未知";
        }
    }
    
    /**
     * 將消費編碼轉換為可讀的範圍描述
     * @param expenseCode 消費編碼，如 "E:TWD_1_TO_200"
     * @return 可讀的範圍描述
     */
    private String convertExpenseCodeToReadable(String expenseCode) {
        // 消費編碼映射表
        Map<String, String> codeToRange = new HashMap<>();
        codeToRange.put("E:TWD_1_TO_200", "NT$1-200");
        codeToRange.put("E:TWD_201_TO_400", "NT$201-400");
        codeToRange.put("E:TWD_401_TO_800", "NT$401-800");
        codeToRange.put("E:TWD_801_TO_1200", "NT$801-1200");
        codeToRange.put("E:TWD_1201_TO_1600", "NT$1201-1600");
        codeToRange.put("E:TWD_1601_OR_MORE", "NT$1601以上");
        
        // 返回更詳細的描述
        String baseRange = codeToRange.getOrDefault(expenseCode, "未知");
        if (!baseRange.equals("未知")) {
            return baseRange + " (中位數)";
        }
        return baseRange;
    }

    /**
     * 調用Ollama API獲取AI回應
     */
    private String callOllamaAPI(String userMessage, String contentType, String initialContent) {
        try {
            // Ollama服務器的URL (默認為本地運行的Ollama服務)
            String ollamaUrl = "http://localhost:11434/api/generate";
            
            // 構建請求體
            String prompt = "你是一個餐廳專業顧問。以下是關於一家餐廳的" + contentType + "的資訊：\n\n" 
                + initialContent + "\n\n用戶的問題或評論是：" + userMessage 
                + "\n\n請以專業、友好的方式回應，提供有價值的見解或建議。";
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "llama3"); // 使用Ollama支持的模型，如llama3、mistral等
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            // 創建HTTP請求
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(ollamaUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
            
            // 發送請求並獲取響應
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 解析響應
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                return jsonResponse.getString("response");
            } else {
                // 如果API調用失敗，返回備用回應
                System.err.println("Ollama API調用失敗，狀態碼: " + response.statusCode());
                return getBackupAIResponse(userMessage, contentType);
            }
        } catch (Exception e) {
            // 出現異常時，記錄錯誤並返回備用回應
            System.err.println("Ollama API調用異常: " + e.getMessage());
            e.printStackTrace();
            return getBackupAIResponse(userMessage, contentType);
        }
    }
    
    /**
     * 獲取備用AI回應（當API調用失敗時使用）
     */
    private String getBackupAIResponse(String userMessage, String contentType) {
        // 簡易的備用回覆邏輯，與之前的getAIResponse相同
        String response;
        
        // 檢測用戶是否在問一般性問題
        if (userMessage.contains("推薦") || userMessage.contains("建議")) {
            response = "基於這家餐廳的" + contentType + "，我建議您可以關注他們的特色菜品，並提前了解價格區間。";
        } else if (userMessage.contains("價格") || userMessage.contains("多少錢")) {
            response = "根據分析，這家餐廳的價格屬於中等水平，適合一般消費者。";
        } else if (userMessage.contains("人氣") || userMessage.contains("熱門")) {
            response = "這家餐廳在當地確實頗受歡迎，尤其是週末可能需要排隊。";
        } else if (userMessage.contains("改進") || userMessage.contains("提升")) {
            response = "如果這家餐廳想要提升體驗，可以考慮改善服務速度和增加一些創新菜品。";
        } else {
            // 根據內容類型給出相應回覆
            switch (contentType) {
                case "餐廳特色":
                    response = "這家餐廳的特色主要體現在獨特的菜品風格和氛圍營造上。您能告訴我您對哪方面更感興趣嗎？";
                    break;
                case "餐廳優點":
                    response = "從顧客評價來看，這家餐廳的菜品口味和性價比是其最大的優勢。您想了解更具體的內容嗎？";
                    break;
                case "餐廳缺點":
                    response = "每家餐廳都有可改進之處，根據評價，這家餐廳可能在服務速度和高峰期的座位安排上有待提升。您有遇到過類似的問題嗎？";
                    break;
                default:
                    response = "這是一個有趣的問題。您能告訴我更多您的想法嗎？";
            }
        }
        
        return response;
    }

    // 實現 PreferencesManager.SettingsStateChangeListener 接口的方法
    @Override
    public void onSettingsStateChanged(boolean isShowing) {
        isSettingsActive[0] = isShowing;
        settingsButton.setStyle(isShowing ? activeButtonStyle : normalButtonStyle);
        
        // 如果顯示設定視圖，確保其他視圖被關閉
        if (isShowing) {
            // 如果建議視圖是活躍的，關閉它
            if (isSuggestionActive[0]) {
                isSuggestionActive[0] = false;
                suggestionButton.setStyle(normalButtonStyle);
            }
            
            // 如果月報視圖是活躍的，關閉它
            if (isReportActive[0]) {
                isReportActive[0] = false;
                reportButton.setStyle(normalButtonStyle);
            }
            
            // 如果AI聊天視圖是活躍的，關閉它
            if (isAIChatActive[0]) {
                hideAIChatView();
            }
        }
    }

    /**
     * 更新近期評論顯示
     * @param days 要顯示的天數範圍 (1=近一天, 7=近一週, 30=近一個月)
     */
    private void updateRecentReviewsDisplay(int days) {
        VBox recentReviewsBox = null;
        
        // 尋找近期評論容器 - 使用更寬鬆的條件
        System.out.println("開始尋找評論容器 - 右側面板子元素數量: " + rightPanel.getChildren().size());
        
        // 檢查是否有初始化過的評論容器
        int targetIndex = -1;
        for (int i = 0; i < rightPanel.getChildren().size(); i++) {
            Node node = rightPanel.getChildren().get(i);
            if (node instanceof VBox) {
                System.out.println("檢查 VBox #" + i + ": " + node);
                // 查找白色背景的容器或在節點和標籤之後的 VBox
                if (node.getStyle() != null && node.getStyle().contains("background-color: white")) {
                    recentReviewsBox = (VBox) node;
                    targetIndex = i;
                    System.out.println("找到評論容器 (白色背景): " + recentReviewsBox);
                    break;
                }
            }
        }
        
        // 如果找不到現有容器，尋找 Label 為"近期評論"的元素的下一個 VBox
        if (recentReviewsBox == null) {
            System.out.println("未找到現有評論容器，嘗試尋找'近期評論'標籤後的VBox");
            boolean foundReviewsLabel = false;
            
            for (int i = 0; i < rightPanel.getChildren().size(); i++) {
                Node node = rightPanel.getChildren().get(i);
                
                // 檢查是否是"近期評論"標籤
                if (node instanceof HBox) {
                    for (Node child : ((HBox) node).getChildren()) {
                        if (child instanceof Label && ((Label) child).getText().equals("近期評論")) {
                            foundReviewsLabel = true;
                            System.out.println("找到'近期評論'標籤，索引: " + i);
                            break;
                        }
                    }
                }
                
                // 如果找到標籤且當前索引小於總數-1，檢查下一個元素
                if (foundReviewsLabel && i < rightPanel.getChildren().size() - 1) {
                    Node nextNode = rightPanel.getChildren().get(i + 1);
                    if (nextNode instanceof VBox) {
                        recentReviewsBox = (VBox) nextNode;
                        targetIndex = i + 1;
                        System.out.println("找到評論容器 (在標籤後): " + recentReviewsBox);
                        break;
                    }
                }
            }
        }
        
        // 如果仍然找不到，則創建一個新的評論容器
        if (recentReviewsBox == null) {
            System.out.println("無法找到評論容器，創建新的容器");
            recentReviewsBox = new VBox(5);
            recentReviewsBox.setPadding(new Insets(5, 0, 15, 0));
            recentReviewsBox.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-padding: 10;");
            
            // 首先尋找"近期評論"標籤
            boolean foundLabel = false;
            for (int i = 0; i < rightPanel.getChildren().size(); i++) {
                Node node = rightPanel.getChildren().get(i);
                if (node instanceof HBox) {
                    for (Node child : ((HBox) node).getChildren()) {
                        if (child instanceof Label && ((Label) child).getText().equals("近期評論")) {
                            foundLabel = true;
                            // 標籤後添加新容器
                            rightPanel.getChildren().add(i + 1, recentReviewsBox);
                            targetIndex = i + 1;
                            System.out.println("已添加新評論容器到標籤後");
                            break;
                        }
                    }
                }
                if (foundLabel) break;
            }
            
            // 如果找不到標籤，將容器添加到末尾
            if (!foundLabel) {
                rightPanel.getChildren().add(recentReviewsBox);
                targetIndex = rightPanel.getChildren().size() - 1;
                System.out.println("已添加新評論容器到末尾");
            }
        }
        
        if (recentReviewsBox == null) return;
        
        // 清空現有內容
        recentReviewsBox.getChildren().clear();
        
        try {
            // 獲取當前餐廳的名稱
            if (currentTabId == null || !tabContents.containsKey(currentTabId)) return;
            String restaurantName = tabContents.get(currentTabId).displayName;
            
            // 構建 reviews_data 目錄中的評論檔案路徑 - 改進檔案名稱比對邏輯
            String reviewsFilePath = "";
            
            // 根據顯示名稱確定正確的檔案名稱
            if (restaurantName.contains("海大") || restaurantName.equalsIgnoreCase("Haidai Roast Shop")) {
                reviewsFilePath = "reviews_data/海大燒臘_reviews.json";
                System.out.println("使用海大燒臘評論檔案: " + reviewsFilePath);
            } else if (restaurantName.contains("海那邊") || restaurantName.equalsIgnoreCase("Sea Side Eatery")) {
                reviewsFilePath = "reviews_data/海那邊小食堂_reviews.json";
                System.out.println("使用海那邊小食堂評論檔案: " + reviewsFilePath);
            } else {
                // 使用原來的邏輯作為後備
                reviewsFilePath = "reviews_data/" + restaurantName.replace(" ", "_") + "_reviews.json";
                System.out.println("使用預設評論檔案路徑: " + reviewsFilePath);
            }
            
            // 檢查檔案是否存在
            if (!Files.exists(Paths.get(reviewsFilePath))) {
                Label noReviews = new Label("尚未抓取此餐廳的評論資料");
                noReviews.setStyle("-fx-text-fill: #555555; -fx-font-style: italic; -fx-font-size: 13px;");
                recentReviewsBox.getChildren().add(noReviews);
                return;
            }
            
            // 讀取 JSON 數據
            String content = new String(Files.readAllBytes(Paths.get(reviewsFilePath)));
            JSONArray reviews;
            
            try {
                // 首先嘗試作為對象讀取（含有 "reviews" 字段）
                JSONObject reviewsData = new JSONObject(content);
                reviews = reviewsData.getJSONArray("reviews");
                System.out.println("成功從 JSONObject 中讀取評論數組，評論數: " + reviews.length());
            } catch (Exception e) {
                // 如果失敗，直接嘗試作為數組讀取
                try {
                    reviews = new JSONArray(content);
                    System.out.println("成功直接讀取評論數組，評論數: " + reviews.length());
                } catch (Exception e2) {
                    // 如果兩種方式都失敗，拋出異常
                    throw new RuntimeException("無法解析評論數據: " + e2.getMessage() + "\n原始錯誤: " + e.getMessage());
                }
            }
            
            // 獲取當前時間
            long currentTimeMs = System.currentTimeMillis();
            long daysInMs = days * 24 * 60 * 60 * 1000L;
            
            // 新增：顯示當前時間和時間範圍資訊，幫助診斷
            System.out.println("當前時間: " + new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date(currentTimeMs)));
            System.out.println("時間範圍: " + days + " 天 (" + new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date(currentTimeMs - daysInMs)) + " 至今)");
            System.out.println("評論總數: " + reviews.length());
            
            // 過濾最近的評論
            List<JSONObject> recentReviews = new ArrayList<>();
            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                
                // 檢查評論時間
                String dateStr = review.optString("留言日期", "");
                boolean added = false;
                
                if (!dateStr.isEmpty()) {
                    try {
                        // 解析日期
                        String[] parts = dateStr.split(" ")[0].split("/");
                        if (parts.length >= 3) {
                            int year = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            int day = Integer.parseInt(parts[2]);
                            
                            java.util.Calendar calendar = java.util.Calendar.getInstance();
                            calendar.set(year, month - 1, day);
                            long reviewTimeMs = calendar.getTimeInMillis();
                            
                            // 新增：顯示解析後的評論時間
                            System.out.println("評論 #" + i + " - 日期字串: " + dateStr + ", 解析時間: " + 
                                             new java.text.SimpleDateFormat("yyyy/MM/dd").format(new java.util.Date(reviewTimeMs)));
                            
                            if (currentTimeMs - reviewTimeMs <= daysInMs) {
                                recentReviews.add(review);
                                added = true;
                                System.out.println("  -> 已添加到最近評論列表 (在" + days + "天範圍內)");
                            } else {
                                System.out.println("  -> 不在時間範圍內，跳過");
                            }
                        }
                    } catch (Exception e) {
                        // 日期解析錯誤，忽略此評論
                        System.err.println("日期解析錯誤: " + dateStr + " - " + e.getMessage());
                    }
                } else if (review.has("time")) {
                    // 處理另一種時間格式（Google Places API）
                    long reviewTimeMs = review.getLong("time") * 1000L;
                    
                    // 新增：顯示時間戳解析後的評論時間
                    System.out.println("評論 #" + i + " - 時間戳: " + review.getLong("time") + ", 解析時間: " + 
                                     new java.text.SimpleDateFormat("yyyy/MM/dd").format(new java.util.Date(reviewTimeMs)));
                    
                    if (currentTimeMs - reviewTimeMs <= daysInMs) {
                        recentReviews.add(review);
                        added = true;
                        System.out.println("  -> 已添加到最近評論列表 (在" + days + "天範圍內)");
                    } else {
                        System.out.println("  -> 不在時間範圍內，跳過");
                    }
                } else {
                    System.out.println("評論 #" + i + " - 無有效時間信息，跳過");
                }
                
                // 新增：打印評論作者和內容摘要，幫助識別
                if (added) {
                    String reviewer = review.optString("評論者", review.optString("author_name", "匿名"));
                    String commentText = review.optString("評論", review.optString("text", "無評論內容"));
                    String shortComment = commentText.length() > 20 ? commentText.substring(0, 20) + "..." : commentText;
                    System.out.println("  -> 評論者: " + reviewer + ", 評論: " + shortComment);
                }
            }
            
            System.out.println("符合時間範圍的評論數: " + recentReviews.size());
            
            // 按時間從新到舊排序評論
            recentReviews.sort((a, b) -> {
                long timeA = 0;
                long timeB = 0;
                
                // 解析時間並比較
                if (a.has("time") && b.has("time")) {
                    timeA = a.getLong("time");
                    timeB = b.getLong("time");
                } else {
                    // 嘗試從留言日期解析
                    String dateStrA = a.optString("留言日期", "");
                    String dateStrB = b.optString("留言日期", "");
                    
                    if (!dateStrA.isEmpty() && !dateStrB.isEmpty()) {
                        try {
                            String[] partsA = dateStrA.split(" ")[0].split("/");
                            String[] partsB = dateStrB.split(" ")[0].split("/");
                            
                            if (partsA.length >= 3 && partsB.length >= 3) {
                                java.util.Calendar calA = java.util.Calendar.getInstance();
                                calA.set(Integer.parseInt(partsA[0]), Integer.parseInt(partsA[1]) - 1, Integer.parseInt(partsA[2]));
                                
                                java.util.Calendar calB = java.util.Calendar.getInstance();
                                calB.set(Integer.parseInt(partsB[0]), Integer.parseInt(partsB[1]) - 1, Integer.parseInt(partsB[2]));
                                
                                timeA = calA.getTimeInMillis();
                                timeB = calB.getTimeInMillis();
                            }
                        } catch (Exception e) {
                            // 日期解析錯誤，不改變順序
                        }
                    }
                }
                
                // 從新到舊排序
                return Long.compare(timeB, timeA);
            });
            
            // 添加時間範圍指示器，使用更好的樣式
            String timeRangeText;
            if (days == 1) {
                timeRangeText = "最近24小時";
            } else if (days == 7) {
                timeRangeText = "最近一週";
            } else if (days == 30) {
                timeRangeText = "最近一個月";
            } else {
                timeRangeText = "最近" + days + "天";
            }
            
            // 添加帶有圖標的時間範圍標籤
            HBox timeRangeBox = new HBox(8);
            timeRangeBox.setAlignment(Pos.CENTER_LEFT);
            timeRangeBox.setPadding(new Insets(5, 0, 5, 0));
            
            Label clockIcon = new Label("🕒");
            clockIcon.setStyle("-fx-text-fill: #444444; -fx-font-size: 14px;");
            
            Label timeRangeLabel = new Label("顯示" + timeRangeText + "的評論");
            timeRangeLabel.setStyle("-fx-text-fill: #444444; -fx-font-size: 12px;");
            
            timeRangeBox.getChildren().addAll(clockIcon, timeRangeLabel);
            recentReviewsBox.getChildren().add(timeRangeBox);
            
            // 添加分隔線，使用更美觀的樣式
            Separator separator = new Separator();
            separator.setStyle("-fx-background-color: " + PALE_DARK_YELLOW + "; -fx-opacity: 0.3;");
            recentReviewsBox.getChildren().add(separator);
            
            // 沒有找到近期評論時顯示提示
            if (recentReviews.isEmpty()) {
                VBox noReviewsBox = new VBox(5);
                noReviewsBox.setAlignment(Pos.CENTER);
                noReviewsBox.setPadding(new Insets(20, 0, 20, 0));
                
                Label noReviewsIcon = new Label("📝");
                noReviewsIcon.setStyle("-fx-text-fill: #666666; -fx-font-size: 24px;");
                
                Label noRecentReviews = new Label("沒有" + timeRangeText + "的評論");
                noRecentReviews.setStyle("-fx-text-fill: #666666; -fx-font-style: italic; -fx-font-size: 14px;");
                
                noReviewsBox.getChildren().addAll(noReviewsIcon, noRecentReviews);
                recentReviewsBox.getChildren().add(noReviewsBox);
                return;
            }
            
            // 顯示近期評論，使用更美觀的卡片樣式
            for (JSONObject review : recentReviews) {
                // 創建評論卡片容器
                VBox reviewCard = new VBox(10); // 增加元素間距
                reviewCard.setStyle("-fx-background-color: #F0F0F0; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #D0D0D0; -fx-border-radius: 8; -fx-border-width: 1;");
                reviewCard.setEffect(new javafx.scene.effect.DropShadow(2, 0, 1, Color.rgb(0, 0, 0, 0.1)));
                reviewCard.setPadding(new Insets(15)); // 增加内邊距
                // 增加卡片之間的間距，使滾動更明顯
                VBox.setMargin(reviewCard, new Insets(10, 0, 10, 0));
                
                // 獲取評論數據
                String reviewer = review.optString("評論者", review.optString("author_name", "匿名"));
                double rating = review.optDouble("評論分數", review.optDouble("rating", 0));
                String commentText = review.optString("評論", review.optString("text", "無評論內容"));
                String date = review.optString("留言日期", review.optString("relative_time_description", "未知時間"));
                
                // 評論者頭像和名稱區域
                HBox reviewerBox = new HBox(10);
                reviewerBox.setAlignment(Pos.CENTER_LEFT);
                
                // 建立頭像
                StackPane avatarPane = new StackPane();
                avatarPane.setMinSize(36, 36);
                avatarPane.setMaxSize(36, 36);
                avatarPane.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-background-radius: 18;");
                
                Label avatarLabel = new Label(reviewer.substring(0, 1).toUpperCase());
                avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
                avatarPane.getChildren().add(avatarLabel);
                
                // 評論者資訊區
                VBox reviewerInfo = new VBox(2);
                
                Label reviewerLabel = new Label(reviewer);
                reviewerLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 14px;");
                
                // 評分區域
                Label ratingLabel = new Label(formatStars(rating));
                ratingLabel.setStyle("-fx-text-fill: #E67649; -fx-font-size: 14px;");
                
                reviewerInfo.getChildren().addAll(reviewerLabel, ratingLabel);
                
                // 時間標籤
                Label dateLabel = new Label(date);
                dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                HBox.setHgrow(reviewerInfo, Priority.ALWAYS);
                
                reviewerBox.getChildren().addAll(avatarPane, reviewerInfo, dateLabel);
                
                // 評論內容，使用標籤而非文本區域
                Label commentLabel = new Label(commentText);
                commentLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px;");
                commentLabel.setWrapText(true);
                commentLabel.setPrefWidth(Double.MAX_VALUE);
                
                // 添加到評論卡片
                reviewCard.getChildren().addAll(reviewerBox, commentLabel);
                
                // 將評論卡片添加到容器
                recentReviewsBox.getChildren().add(reviewCard);
            }
        } catch (Exception e) {
            e.printStackTrace();
            VBox errorBox = new VBox(5);
            errorBox.setAlignment(Pos.CENTER);
            errorBox.setPadding(new Insets(20));
            
            Label errorIcon = new Label("❌");
            errorIcon.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 18px;");
            
            Label errorLabel = new Label("載入評論時發生錯誤");
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 14px;");
            
            errorBox.getChildren().addAll(errorIcon, errorLabel);
            recentReviewsBox.getChildren().add(errorBox);
        }
    }

    /**
     * 格式化星級評分
     */
    private String formatStars(double rating) {
        StringBuilder stars = new StringBuilder();
        int fullStars = (int) rating;
        boolean halfStar = (rating - fullStars) >= 0.5;
        
        // 填充實心星星
        for (int i = 0; i < fullStars; i++) {
            stars.append("★");
        }
        
        // 添加半星（如果適用）
        if (halfStar) {
            stars.append("½");
        }
        
        // 填充空心星星
        int filledStars = fullStars + (halfStar ? 1 : 0);
        for (int i = filledStars; i < 5; i++) {
            stars.append("☆");
        }
        
        return stars.toString();
    }
}


