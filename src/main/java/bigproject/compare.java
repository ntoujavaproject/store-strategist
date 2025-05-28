package bigproject;

import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javafx.scene.Parent;
import javafx.event.EventHandler;
import javafx.scene.input.ScrollEvent;

import bigproject.RightPanel;
import bigproject.SearchBar;  // 添加 SearchBar 引用
import bigproject.ai.AIProgressDialog;

/**
 * 餐廳市場分析系統主應用程式
 * (Layout: 70% Reviews, 30% Ratings/Sources, Top-Right Buttons)
 */
public class compare extends Application implements UIManager.StateChangeListener, PreferencesManager.SettingsStateChangeListener, AIChat.ChatStateChangeListener {

    private VBox competitorListVBox;
    private BorderPane mainLayout;
    private Scene mainScene;
    private HBox mainContentBox; // 將mainContentBox升級為類成員變數
    private VBox mainContainer; // 將mainContainer也升級為類成員變數
    private ScrollPane leftScrollPane; // 將leftScrollPane也升級為類成員變數
    private RightPanel rightPanel; // 使用新的 RightPanel 類替代原來的 VBox
    
    // 近期評論側欄
    private RecentReviewsSidebar recentReviewsSidebar;
    private Button reviewsSidebarToggleButton;

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
    private FlowPane photosContainer; // 用於顯示評論照片的容器，改為FlowPane
    private ScrollPane photosScroll; // 添加ScrollPane包裹圖片容器
    
    // 添加 PreferencesManager 成員變量
    private PreferencesManager preferencesManager;
    
    // 添加 AIChat 實例
    private AIChat aiChat;
    
    // 添加評分數據分析器
    private RatingDataAnalyzer ratingAnalyzer;

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

    // 搜尋歷史功能已移除
    
    // 按鈕樣式
    private String normalButtonStyle;
    private String activeButtonStyle;
    private String hoverButtonStyle;
    
    // 按鈕狀態
    private final boolean[] isSuggestionActive = {false};
    private final boolean[] isSettingsActive = {false}; // 添加設定狀態
    
    // 按鈕引用
    private Button suggestionButton;
    private Button settingsButton; // 添加設定按鈕引用

    private boolean isHorizontalLayout = true; // 記錄當前布局模式，true為水平布局(左右)，false為垂直布局(上下)
    private static final int LAYOUT_CHANGE_THRESHOLD = 50; // 防抖動閾值，避免在邊界值附近頻繁切換
    
    // 存儲所有頁面的Map
    private Map<String, TabContent> tabContents = new HashMap<>();
    private String currentTabId = null; // 當前選定的頁面ID
    private HBox tabBar; // 分頁欄

    @Override
    public void start(Stage primaryStage) {
        // 設置關閉窗口的處理器
        primaryStage.setOnCloseRequest(event -> {
            // 清理資源
            System.out.println("🔧 應用程式正在關閉，開始清理資源...");
            
            try {
                // 清理 AI 相關資源
                if (aiChat != null) {
                    System.out.println("🔧 清理 AI Chat 資源...");
                    // 如果 AIChat 有清理方法，在此調用
                }
                
                // 清理 Ollama 服務
                System.out.println("🔧 停止 Ollama 服務...");
                try {
                    // 使用反射調用 OllamaAPI 的 shutdown 方法
                    Class<?> ollamaApiClass = Class.forName("bigproject.ai.OllamaAPI");
                    java.lang.reflect.Method shutdownMethod = ollamaApiClass.getMethod("shutdown");
                    shutdownMethod.invoke(null);
                    System.out.println("✅ Ollama 服務已正確關閉");
                } catch (Exception e) {
                    System.err.println("⚠️ 清理 Ollama 服務時發生錯誤: " + e.getMessage());
                }
                
                // 清理其他資源
                if (googlePlacesService != null) {
                    System.out.println("🔧 清理 Google Places 服務...");
                }
                
                if (dataManager != null) {
                    System.out.println("🔧 清理數據管理器...");
                }
                
                System.out.println("✅ 資源清理完成");
                
            } catch (Exception e) {
                System.err.println("⚠️ 清理資源時發生錯誤: " + e.getMessage());
            } finally {
                // 確保應用程式退出
                Platform.exit();
                System.exit(0);
            }
        });
        // 獲取螢幕尺寸，計算最小視窗大小
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double halfWidth = screenBounds.getWidth() / 2;
        double halfHeight = screenBounds.getHeight() / 2;
        
        // 設置視窗最小尺寸為螢幕的一半
        primaryStage.setMinWidth(halfWidth);
        primaryStage.setMinHeight(halfHeight);
        primaryStage.setResizable(true);
        
        // 設置視窗預設為最大化顯示
        primaryStage.setMaximized(true);
        
        // 設置視窗標題
        primaryStage.setTitle("餐廳分析");
        
        // 載入應用程式圖標
        ResourceManager.setAppIcon(primaryStage);
        
        // 🔄 新的流程：先顯示搜尋首頁，然後根據搜尋結果進入主分析界面
        showSearchHomePage(primaryStage);
    }

    /**
     * 顯示搜尋首頁
     */
    private void showSearchHomePage(Stage primaryStage) {
        SearchHomePage searchHomePage = new SearchHomePage(primaryStage, 
            (restaurantName, restaurantId, dataSource) -> {
                // 當用戶選擇餐廳後，初始化主分析界面
                initializeMainAnalysisInterface(primaryStage, restaurantName, restaurantId, dataSource);
            }
        );
        searchHomePage.show();
    }
    
    /**
     * 初始化主分析界面
     */
    private void initializeMainAnalysisInterface(Stage primaryStage, String restaurantName, String restaurantId, String dataSource) {
        // 創建主佈局
        mainLayout = new BorderPane();
        // 調整主布局邊距，上有邊距，左右底部無邊距，確保搜尋欄可以完全貫穿
        mainLayout.setPadding(new Insets(15, 0, 0, 0)); // 移除右側邊距，使搜尋欄可以完全貫穿到頁面右側
        
        // 使用背景圖片
        String bgImagePath = "file:" + System.getProperty("user.dir").replace(" ", "%20") + "/應用程式背景.png";
        mainLayout.setStyle("-fx-background-image: url('" + bgImagePath + "'); " +
                           "-fx-background-size: cover; " +
                           "-fx-background-position: center center;");
        
        mainLayout.setPrefHeight(Double.MAX_VALUE); // 確保主佈局填滿整個高度
        
        // 設置主佈局初始不可見，用於後續動畫
        mainLayout.setOpacity(0);
        
        // 創建分頁欄
        tabBar = new HBox(5);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(5, 10, 0, 10)); // 移除底部內邊距
        tabBar.setStyle("-fx-background-color: rgba(42, 42, 42, 0.85); -fx-border-width: 2 0 0 0; -fx-border-color: #E67649; -fx-padding: 5 10 0 10;"); // 使用半透明背景
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
        topBar.setAlignment(Pos.CENTER_LEFT);
        // 🎯 調整 padding，讓按鈕欄貼緊視窗上方，但保持左右和底部邊距
        topBar.setPadding(new Insets(5, 15, 10, 15)); // 頂部只保留5px邊距
        topBar.getStyleClass().add("top-bar");
        topBar.setStyle("-fx-background-color: rgba(58, 58, 58, 0.7);"); // 半透明背景
        
        // 使用普通按鈕而非ToggleButton，這樣我們可以直接控制其樣式
        suggestionButton = new Button("經營建議");
        settingsButton = new Button("⚙️");
        
        // 創建返回搜尋首頁按鈕 - 使用更明確的箭頭符號和橘色主題
        Button backToSearchButton = new Button("⬅");
        backToSearchButton.setStyle(
            "-fx-background-color: rgba(230, 118, 73, 0.9); " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 50%; " +
            "-fx-border-radius: 50%; " +
            "-fx-padding: 10 12 10 12; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Arial Unicode MS', 'Segoe UI Symbol', 'Symbol'; " +
            "-fx-min-width: 45px; " +
            "-fx-min-height: 45px; " +
            "-fx-max-width: 45px; " +
            "-fx-max-height: 45px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3); " +
            "-fx-border-width: 2; " +
            "-fx-border-color: rgba(255,255,255,0.3);"
        );
        backToSearchButton.setOnAction(e -> {
            returnToSearchHomePageWithAnimation(primaryStage);
        });
        
        // 添加懸停效果
        backToSearchButton.setOnMouseEntered(e -> {
            backToSearchButton.setStyle(
                "-fx-background-color: rgba(240, 138, 105, 0.95); " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 50%; " +
                "-fx-border-radius: 50%; " +
                "-fx-padding: 10 12 10 12; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: 'Arial Unicode MS', 'Segoe UI Symbol', 'Symbol'; " +
                "-fx-min-width: 45px; " +
                "-fx-min-height: 45px; " +
                "-fx-max-width: 45px; " +
                "-fx-max-height: 45px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 4); " +
                "-fx-scale-x: 1.15; " +
                "-fx-scale-y: 1.15; " +
                "-fx-border-width: 2; " +
                "-fx-border-color: rgba(255,255,255,0.5);"
            );
            backToSearchButton.setCursor(javafx.scene.Cursor.HAND);
        });
        
        backToSearchButton.setOnMouseExited(e -> {
            backToSearchButton.setStyle(
                "-fx-background-color: rgba(230, 118, 73, 0.9); " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 50%; " +
                "-fx-border-radius: 50%; " +
                "-fx-padding: 10 12 10 12; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: 'Arial Unicode MS', 'Segoe UI Symbol', 'Symbol'; " +
                "-fx-min-width: 45px; " +
                "-fx-min-height: 45px; " +
                "-fx-max-width: 45px; " +
                "-fx-max-height: 45px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3); " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0; " +
                "-fx-border-width: 2; " +
                "-fx-border-color: rgba(255,255,255,0.3);"
            );
            backToSearchButton.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        // 設置具體的樣式而不是使用CSS類
        normalButtonStyle = "-fx-text-fill: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 15 8 15;";
        activeButtonStyle = "-fx-background-color: #8B4513; " + normalButtonStyle + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 4, 0, 0, 1);";
        hoverButtonStyle = "-fx-background-color: #f08a6c; " + normalButtonStyle;
        normalButtonStyle = "-fx-background-color: #E67649; " + normalButtonStyle;
        
        suggestionButton.setStyle(normalButtonStyle);
        suggestionButton.setFont(Font.font("System", FontWeight.BOLD, 12));
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
        

        
        settingsButton.setFont(Font.font(16));
        settingsButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2 5 2 5; -fx-text-fill: #CCCCCC;"); // Invisible style
        
        // 創建一個 Region 來分隔左右兩邊的按鈕
        Region topBarSpacer = new Region();
        HBox.setHgrow(topBarSpacer, Priority.ALWAYS);
        
        // 創建近期評論側欄觸發按鈕
        reviewsSidebarToggleButton = new Button("近期評論");
        reviewsSidebarToggleButton.setFont(Font.font("System", FontWeight.BOLD, 12));
        reviewsSidebarToggleButton.setStyle(normalButtonStyle);
        
        // 添加懸停效果 - 使用與其他按鈕一致的樣式
        reviewsSidebarToggleButton.setOnMouseEntered(e -> {
            reviewsSidebarToggleButton.setStyle(hoverButtonStyle);
        });
        
        reviewsSidebarToggleButton.setOnMouseExited(e -> {
            reviewsSidebarToggleButton.setStyle(normalButtonStyle);
        });
        
        // 設置點擊事件
        reviewsSidebarToggleButton.setOnAction(e -> {
            System.out.println("🔍 近期評論按鈕被點擊");
            if (recentReviewsSidebar != null) {
                System.out.println("✅ 觸發側欄開關");
                recentReviewsSidebar.toggleSidebar();
            } else {
                System.out.println("❌ 近期評論側欄尚未初始化");
            }
        });

        // 🔧 確保近期評論按鈕始終可以點擊，不被其他元素覆蓋
        reviewsSidebarToggleButton.setDisable(false);
        reviewsSidebarToggleButton.setMouseTransparent(false);
        reviewsSidebarToggleButton.setVisible(true);
        reviewsSidebarToggleButton.setManaged(true);
        
        System.out.println("✅ 近期評論按鈕配置完成，確保可點擊狀態");

        topBar.getChildren().addAll(backToSearchButton, topBarSpacer, settingsButton, reviewsSidebarToggleButton, suggestionButton);
        
        // 🔧 確保 topBar 始終在最上層
        Platform.runLater(() -> {
            topBar.toFront();
            reviewsSidebarToggleButton.toFront();
            System.out.println("🔝 確保 topBar 和近期評論按鈕在最上層");
        });
        
        // --- 移除搜索欄，用戶需要回到搜尋首頁來搜尋其他餐廳 ---
        // SearchBar searchContainer = new SearchBar(this::handleSearch);
        
        // --- 創建主布局 --- 
        mainContainer = new VBox(0); // 🎯 移除間距，確保內容區域完全貼緊底部
        mainContainer.setPrefHeight(Double.MAX_VALUE); // 確保填滿整個高度
        VBox.setVgrow(mainContainer, Priority.ALWAYS); // 確保主容器能擴展填滿
        
        // 初始化 AIChat 實例
        aiChat = new AIChat(mainLayout, mainContainer, this);

        // --- Main Content Area (HBox: Left 70%, Right 30%) ---
        mainContentBox = new HBox(0); // 移除左右間距，讓青蘋果欄完全貼緊右側邊界
        mainContentBox.setPadding(new Insets(0, 0, 0, 0)); // 🎯 移除所有邊距，讓青綠色面板完全貼緊底部
        mainContentBox.setPrefHeight(Double.MAX_VALUE); // 確保內容區域填滿整個高度
        // 🎯 設置合理的最小高度確保主內容區域能展開
        mainContentBox.setMinHeight(600); // 設置明確的最小高度
        mainContentBox.setMaxHeight(Double.MAX_VALUE); // 🎯 明確設置最大高度
        mainContentBox.setStyle("-fx-background-color: transparent;"); // 透明背景讓子元素背景顯示
        mainContentBox.setMaxWidth(Double.MAX_VALUE); // 確保內容區域水平填滿

        // --- Left Panel (Reviews, Details) ---
        VBox leftPanel = new VBox(20);
        leftPanel.setPadding(new Insets(20, 20, 0, 20)); // 🎯 保持左側面板原有邊距，只有右側面板貼底
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setPrefHeight(Double.MAX_VALUE); // 確保預設高度撐滿
        leftPanel.setStyle("-fx-background-color: rgba(247, 232, 221, 0.85);"); // 使用半透明的膚色背景，讓背景圖片部分可見
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
        rightPanel = new RightPanel(this);
        
        // 🎯 初始化評分數據分析器
        System.out.println("🔧 初始化評分數據分析器...");
        ratingAnalyzer = new RatingDataAnalyzer(rightPanel, this);
        System.out.println("✅ 評分數據分析器初始化完成");
        
        // 添加到頂部面板
        topPanel.getChildren().addAll(reviewsSection, rightPanel);
        
        // 照片區域
        Label photosLabel = new Label("評論照片");
        photosLabel.setFont(Font.font("System", FontWeight.BOLD, 18)); // 字體增大
        photosLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        // 將VBox改為FlowPane以實現自適應排列
        photosContainer = new FlowPane();
        photosContainer.setHgap(5); // 設置水平間距，給予圖片一些呼吸空間
        photosContainer.setVgap(5); // 設置垂直間距
        photosContainer.setPrefWrapLength(800); // 設置一個較大的固定值，確保能換行
        photosContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        photosContainer.setPrefHeight(Region.USE_COMPUTED_SIZE); // 讓高度自動計算
        photosContainer.setMinHeight(250);
        photosContainer.setStyle("-fx-background-color: #222222; -fx-padding: 10; -fx-alignment: center;"); // 添加 center 對齊
        photosContainer.setAlignment(Pos.CENTER); // 設置 FlowPane 的內容居中
        
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
        
        // 🗑️ 移除重複的特色、優點、缺點區塊定義 - 這些功能已移到 RightPanel.java 中
        
        // 先添加評分和資料來源部分 - 在初始化rightPanel時已添加了這些元素
        // rightPanel.getChildren().addAll(ratingsHeader, ratingsBox, sourcesLabel, competitorListVBox); // 避免重複添加

                // --- 將左側面板整體放入ScrollPane以支持垂直滾動 ---
        leftScrollPane = new ScrollPane(leftPanel);
        leftScrollPane.setFitToWidth(true); // 讓內容適應寬度
        leftScrollPane.setFitToHeight(false); // 修改: 設為false讓內容可以滾動
        leftScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER); // 不顯示水平滾動條
        leftScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); // 總是顯示垂直滾動條
        leftScrollPane.setStyle("-fx-background-color: rgba(247, 232, 221, 0.6); -fx-border-color: transparent;"); // 半透明背景
        leftScrollPane.setPannable(true); // 允許拖曳滾動
        leftScrollPane.setMinHeight(Region.USE_COMPUTED_SIZE); // 🎯 使用計算尺寸，不設固定限制
        leftScrollPane.setPrefHeight(Double.MAX_VALUE); // 🎯 使用最大值而不是計算值
        leftScrollPane.setMaxHeight(Double.MAX_VALUE);
        // 添加寬度限制，確保不超過橘線
        // leftScrollPane.setMaxWidth(700); // 🎯 移除左側面板最大寬度限制，讓6:4比例真正生效
        
        // 確保滾動面板正確處理內容的高度變化
        leftPanel.heightProperty().addListener((obs, oldVal, newVal) -> {
            leftScrollPane.layout();
        });
        
        // 將右側面板放入ScrollPane以支持垂直滾動 - 🎯 完全移除所有高度限制
        ScrollPane rightScrollPane = new ScrollPane(rightPanel);
        rightScrollPane.setFitToWidth(true); // 讓內容適應寬度
        rightScrollPane.setFitToHeight(false); // 修改為false，允許內容超出可視區域並顯示滾動條
        rightScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER); // 不顯示水平滾動條
        rightScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); // 總是顯示垂直滾動條
        rightScrollPane.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + "; -fx-border-color: transparent; -fx-background: " + RICH_LIGHT_GREEN + "; -fx-padding: 0; -fx-border-width: 0;");
        rightScrollPane.getStyleClass().add("superellipse-right-panel"); // 🎯 套用 superellipse 右側面板樣式
        rightScrollPane.setPannable(true); // 允許拖曳滾動
        // 🎯 設置合理的最小高度，確保右側面板可見
        rightScrollPane.setMinHeight(Region.USE_COMPUTED_SIZE); // 🎯 使用計算尺寸，不設固定限制
        rightScrollPane.setPrefHeight(Double.MAX_VALUE); 
        rightScrollPane.setMaxHeight(Double.MAX_VALUE); 
        rightScrollPane.setVmin(0); // 確保滾動從頂部開始
        rightScrollPane.setVmax(1); // 確保滾動到底部
        
        // 解決滑動問題：增加右側面板的滾動事件處理
        rightScrollPane.setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 2.0; // 增加滾動速度
            rightScrollPane.setVvalue(rightScrollPane.getVvalue() - deltaY / rightPanel.getHeight());
            event.consume(); // 防止事件傳播
        });
        
        // 移除額外的高度設定，讓右側面板自然適應內容
        // rightPanel.setMinHeight(2000); // 設置足夠大的最小高度
        // rightPanel.setPrefHeight(2200); // 設置足夠大的預設高度
        
        // 確保滾動面板貼緊分頁欄
        mainLayout.heightProperty().addListener((obs, oldVal, newVal) -> {
            updatePanelSizes(mainContentBox, leftScrollPane, mainLayout.getWidth(), newVal.doubleValue());
        });
        
        // 添加左側和右側面板到主內容區域
        mainContentBox.getChildren().addAll(leftScrollPane, rightScrollPane);
        
        // 🎯 設置空間分配：左側60%，右側40%
        HBox.setHgrow(leftScrollPane, Priority.ALWAYS); // 左側面板自動擴展
        HBox.setHgrow(rightScrollPane, Priority.SOMETIMES); // 右側面板按比例分配
        
        // 🎯 確保子元素能在垂直方向填滿HBox
        leftScrollPane.setMaxHeight(Double.MAX_VALUE);
        rightScrollPane.setMaxHeight(Double.MAX_VALUE);
        // 🎯 移除 setPrefHeight，因為已經有綁定了
        // leftScrollPane.setPrefHeight(Double.MAX_VALUE); // 已有綁定
        // rightScrollPane.setPrefHeight(Double.MAX_VALUE); // 已有綁定
        
        // 🎯 使用約束來強制子元素填滿HBox的高度
        leftScrollPane.prefHeightProperty().bind(mainContentBox.heightProperty());
        rightScrollPane.prefHeightProperty().bind(mainContentBox.heightProperty());
        
        // 使用綁定來確保右側面板佔40%寬度
        rightScrollPane.prefWidthProperty().bind(
            mainContentBox.widthProperty().multiply(0.4)
        );
        
        // 🎯 調整寬度限制以真正達到40%效果
        rightScrollPane.setMinWidth(300); // 最小寬度300px（降低最小寬度）
        // 完全移除最大寬度限制，讓40%綁定完全生效
        
        // 確保右側面板可以完全滾動，且不受其他設置影響
        rightScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); // 總是顯示垂直滾動條
        rightScrollPane.setFitToHeight(false); // 讓內容可完全顯示並允許滾動
        rightScrollPane.setFitToWidth(true); // 寬度適應容器
        
        // 將主內容區域加入主容器 - 🎯 確保完全填滿
        mainContainer.getChildren().add(mainContentBox);
        VBox.setVgrow(mainContentBox, Priority.ALWAYS); // 讓內容區域自動擴展
        
        // 🎯 簡化高度設置，避免衝突
        Platform.runLater(() -> {
            // 只綁定到父容器高度，不設置固定值
            mainContentBox.minHeightProperty().bind(mainContainer.heightProperty());
            mainContentBox.prefHeightProperty().bind(mainContainer.heightProperty());
            
            System.out.println("🔧 綁定主內容區域到主容器高度");
        });
        
        // 🎯 強制HBox填滿VBox的垂直空間
        mainContentBox.fillHeightProperty().set(true);
        // 🎯 確保主容器完全填滿並貼緊底部
        mainContainer.setMaxHeight(Double.MAX_VALUE);
        mainContainer.setMinHeight(200); // 🎯 設置明確的最小高度而不是計算值
        
        // --- Setup Scene and UIManager --- 
        // 🎯 創建頂部組合容器，讓按鈕欄完全貼緊視窗上方
        VBox topContainer = new VBox(0); // 移除容器間距
        topContainer.setPadding(new Insets(0, 0, 0, 0)); // 🎯 移除所有邊距，讓按鈕欄完全貼緊上方
        topContainer.getChildren().add(topBar);
        
        // 初始化近期評論側欄
        recentReviewsSidebar = new RecentReviewsSidebar(this);
        
        // 創建包含主容器和側欄的 StackPane
        StackPane mainStackPane = new StackPane();
        mainStackPane.getChildren().addAll(mainContainer, recentReviewsSidebar);
        
        // 設置側欄位置和大小
        StackPane.setAlignment(recentReviewsSidebar, Pos.CENTER_RIGHT);
        recentReviewsSidebar.setPrefWidth(primaryStage.getWidth() * 0.25); // 25% 寬度
        recentReviewsSidebar.setMinWidth(350); // 最小寬度
        recentReviewsSidebar.setMaxWidth(500); // 最大寬度
        
        mainLayout.setCenter(mainStackPane); // 使用包含側欄的容器作為主要內容
        mainLayout.setTop(topContainer); // 設置頂部組合容器
        
        // 明確設置底部的分頁欄，固定在視窗底部
        mainLayout.setBottom(tabBar);
        BorderPane.setMargin(tabBar, new Insets(0, 0, 0, 0)); // 移除底部邊距，確保完全貼緊視窗底部
        BorderPane.setMargin(mainContainer, new Insets(0, 0, 0, 0)); // 🎯 確保主容器也沒有邊距
        BorderPane.setMargin(topContainer, new Insets(0, 0, 0, 0)); // 🎯 確保頂部容器也沒有邊距
        
        // 確保背景設置不被覆蓋，同時保持其他樣式
        final String backgroundImagePath = "file:" + System.getProperty("user.dir") + "/應用程式背景.png";
        
        // 背景圖片樣式
        String bgStyle = "-fx-background-image: url('" + backgroundImagePath + "'); " +
                         "-fx-background-size: cover; " +
                         "-fx-background-position: center center; " +
                         "-fx-min-height: 100%;"; // 確保填滿整個空間
        
        // 保存當前的樣式
        String currentStyle = mainLayout.getStyle();
        
        // 檢查是否存在字體設置
        if (currentStyle != null && (currentStyle.contains("-fx-font-family") || currentStyle.contains("-fx-font-size"))) {
            // 提取字體設置
            StringBuilder fontStyle = new StringBuilder();
            
            // 提取字體系列
            if (currentStyle.contains("-fx-font-family")) {
                int start = currentStyle.indexOf("-fx-font-family");
                int end = currentStyle.indexOf(";", start) + 1;
                if (end > start) {
                    fontStyle.append(currentStyle.substring(start, end)).append(" ");
                }
            }
            
            // 提取字體大小
            if (currentStyle.contains("-fx-font-size")) {
                int start = currentStyle.indexOf("-fx-font-size");
                int end = currentStyle.indexOf(";", start) + 1;
                if (end > start) {
                    fontStyle.append(currentStyle.substring(start, end));
                }
            }
            
            // 組合背景圖片和字體樣式
            mainLayout.setStyle(bgStyle + " " + fontStyle.toString());
        } else {
            // 如果沒有字體設置，直接設置背景圖片樣式
            mainLayout.setStyle(bgStyle);
        }
        
        // 初始化場景，並設置最小尺寸防止過小導致UI變形
        mainScene = new Scene(mainLayout, 1024, 768);
        mainScene.getRoot().setStyle("-fx-min-height: 100%;");
        
        // --- Initialize UIManager NOW --- 
        uiManager = new UIManager(prefs, primaryStage, mainScene, mainLayout, mainContentBox); // Pass main HBox instead
        
        // 初始化 PreferencesManager
        preferencesManager = new PreferencesManager(primaryStage, mainLayout, uiManager);
        
        // Set this class as the state change listener
        uiManager.setStateChangeListener(this);
        uiManager.setFullNameCollectCallback(this::collectAndUploadRestaurantToFirebase); // 設置完整名稱收集回調
        preferencesManager.setStateChangeListener(this);

        // --- Update font style using UIManager ---
        uiManager.updateFontStyle(currentFontFamily, currentFontSize);

        // --- Set Actions requiring uiManager ---
        settingsButton.setOnAction(e -> {
            // 切換設定視圖
            preferencesManager.toggleSettingsView();
            
            // 🔧 設定按鈕不使用橘色效果，保持簡潔的中性樣式
            // 設定按鈕始終保持正常樣式，不變色
            settingsButton.setStyle(normalButtonStyle);
            
            // 更新設定狀態但不改變按鈕樣式
            isSettingsActive[0] = !isSettingsActive[0];
            
            // 如果建議視圖是活躍的，關閉它
            if (isSuggestionActive[0]) {
                suggestionButton.setStyle(normalButtonStyle);
                isSuggestionActive[0] = false;
            }
        });
        
        // 🔧 移除設定按鈕的懸停和點擊特效，保持簡潔外觀
        // 設定按鈕不再有任何特效
        
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
                

            }
        });
        
        // 🗑️ 移除重複的 TextArea 創建和設定 - 這些功能已移到 RightPanel.java 中
        
        // 🎯 移除底部spacer，讓右側面板完全貼緊底部
        // 之前的spacer會在底部創造200px空白，現在完全移除

        // 添加評論區和照片區到左側面板
        leftPanel.getChildren().addAll(reviewsLabel, reviewsArea, photosLabel, photosScroll);
        
        // --- Add Left and Right Panels to HBox --- 
        // 這段代碼現在被移到了上面，leftScrollPane在上面已完成初始化
        
        // 建立響應式設計的內容調整器
        // 強制使用小視窗模式 - 即只顯示青蘋果綠欄位，隱藏膚色欄位
        setupResponsiveLayout(primaryStage, mainContentBox, leftScrollPane);
        
        // --- Apply Theme and Show Stage ---
        uiManager.updateTheme(true); // 強制使用深色模式
        
        // 確保樣式更新被應用
        String cssUrl = getClass().getResource("/bigproject/modern_dark_theme.css").toExternalForm();
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(cssUrl);
        
        primaryStage.setScene(mainScene);
        
        // 確保在應用程式完全啟動後設置正確的佈局模式
        Platform.runLater(() -> {
            // 🗑️ 移除重複的 TextArea 高度設定 - 這些功能已移到 RightPanel.java 中
            
            // 確保分頁系統得到初始化並正確顯示
            mainLayout.setBottom(tabBar);
            tabBar.setVisible(true);
            tabBar.setManaged(true);
            tabBar.toFront();
            
            // 在初始化後強制進行一次佈局更新
            updatePanelSizes(mainContentBox, leftScrollPane, primaryStage.getWidth(), primaryStage.getHeight());
            
            // 調整主佈局，確保分頁欄位於底部且無邊距
            mainLayout.setPadding(new Insets(15, 0, 0, 0));
            
            // 設置分頁欄邊距為0，確保其貼緊視窗底部
            BorderPane.setMargin(tabBar, new Insets(0, 0, 0, 0));
            
            // 強制更新佈局
            mainLayout.layout();
            
            System.out.println("應用程式佈局初始化完成，分頁欄已設置");
        });

        // --- 移除預設餐廳載入 ---
        // 🚫 移除自動載入「海大燒臘」- 讓用戶自行搜尋和選擇餐廳
        // loadAndDisplayRestaurantData("reviews_data/海大燒臘_reviews.json");
        
        // 🚫 移除預設分頁創建 - 讓用戶自行新增分頁
        // createNewTab("海大燒臘", "reviews_data/海大燒臘_reviews.json");
        
        // 底部加入分頁欄 (為了確保初始載入時分頁欄可見)
        mainLayout.setBottom(tabBar);
        tabBar.setVisible(true);
        tabBar.setManaged(true);
        tabBar.toFront(); // 確保分頁欄在最前端

        // --- Position Stage at Center ---
        try {
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - primaryStage.getWidth()) / 2);
            primaryStage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - primaryStage.getHeight()) / 2);
        } catch (Exception e) {
            // Ignore positioning errors
            System.err.println("無法置中視窗: " + e.getMessage());
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
            
            // 🔍 添加高度監聽器來診斷底部空白問題
            setupHeightDebugging(primaryStage);
            
            // 調整主布局的邊距，確保搜尋欄貫穿整個頁面
            mainLayout.setPadding(new Insets(15, 0, 0, 0)); // 維持上方有邊距，移除右側邊距
            
            // 確保背景圖片設置保持不變
            final String bgPath = "file:" + System.getProperty("user.dir").replace(" ", "%20") + "/應用程式背景.png";
            
            // 保存當前的樣式 (使用不同變數名稱避免重複宣告)
            String updatedStyle = mainLayout.getStyle();
            
            // 背景圖片樣式 (使用不同變數名稱避免重複宣告)
            String updatedBgStyle = "-fx-background-image: url('" + bgPath + "'); " +
                             "-fx-background-size: cover; " +
                             "-fx-background-position: center center;";
            
            // 檢查是否存在字體設置
            if (updatedStyle != null && (updatedStyle.contains("-fx-font-family") || updatedStyle.contains("-fx-font-size"))) {
                // 提取字體設置
                StringBuilder fontStyle = new StringBuilder();
                
                // 提取字體系列
                if (updatedStyle.contains("-fx-font-family")) {
                    int start = updatedStyle.indexOf("-fx-font-family");
                    int end = updatedStyle.indexOf(";", start) + 1;
                    if (end > start) {
                        fontStyle.append(updatedStyle.substring(start, end)).append(" ");
                    }
                }
                
                // 提取字體大小
                if (updatedStyle.contains("-fx-font-size")) {
                    int start = updatedStyle.indexOf("-fx-font-size");
                    int end = updatedStyle.indexOf(";", start) + 1;
                    if (end > start) {
                        fontStyle.append(updatedStyle.substring(start, end));
                    }
                }
                
                // 組合背景圖片和字體樣式
                mainLayout.setStyle(updatedBgStyle + " " + fontStyle.toString());
            } else {
                // 如果沒有字體設置，直接設置背景圖片樣式
                mainLayout.setStyle(updatedBgStyle);
            }
            
            // 🎯 完全移除主內容區域的所有邊距，確保右側面板貼緊底部
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
                        mainLayout.setPadding(new Insets(15, 0, 0, 0));
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

        // --- 移除搜尋按鈕事件處理，用戶需要回到搜尋首頁 ---
        // 原本的搜尋功能已移除，用戶必須透過新增分頁功能回到搜尋首頁來搜尋其他餐廳

        // --- API Key Check ---
        if (API_KEY == null || API_KEY.isEmpty()) {
             // Consider showing an alert using uiManager.showErrorDialog if needed
             System.out.println("Warning: Google Maps API Key not found or empty.");
        }

        // --- 刪除Cmd+T/Ctrl+T快捷鍵功能 ---

        // 根據選擇的餐廳處理後續流程
        if ("collection".equals(dataSource)) {
            // 如果需要從 Google Maps 收集資料
            collectAndUploadRestaurantToFirebase(restaurantName);
        } else {
            // 直接處理已存在於資料庫的餐廳
            handleRestaurantFromDatabase(restaurantName, restaurantId);
        }
    }
    
    /**
     * 處理來自資料庫的餐廳
     */
    private void handleRestaurantFromDatabase(String restaurantName, String restaurantId) {
        // 根據餐廳名稱載入對應的資料
        if (restaurantName.contains("海大") || restaurantName.contains("Haidai")) {
            loadAndDisplayRestaurantData("Haidai Roast Shop.json");
            createNewTab("海大燒臘", "Haidai Roast Shop.json");
        } else if (restaurantName.contains("海那邊") || restaurantName.contains("Sea Side")) {
            loadAndDisplayRestaurantData("Sea Side Eatery Info.json");
            createNewTab("海那邊小食堂", "Sea Side Eatery Info.json");
        } else {
            // 對於其他餐廳，嘗試搜尋對應的 JSON 檔案
            String jsonPath = findRestaurantJsonFile(restaurantName, restaurantId);
            if (jsonPath != null) {
                loadAndDisplayRestaurantData(jsonPath);
                createNewTab(restaurantName, jsonPath);
            } else {
                // 如果找不到對應檔案，顯示搜尋結果頁面
                handleSearch(restaurantName);
            }
        }
    }
    
    /**
     * 尋找餐廳對應的 JSON 檔案
     */
    private String findRestaurantJsonFile(String restaurantName, String restaurantId) {
        // 嘗試多種可能的檔案名稱格式
        String[] possiblePaths = {
            "reviews_data/" + restaurantName + "_reviews.json",
            "reviews_data/" + restaurantName + ".json",
            restaurantName + "_reviews.json",
            restaurantName + ".json"
        };
        
        for (String path : possiblePaths) {
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(path))) {
                return path;
            }
        }
        
        return null;
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

    /**
     * 開啟地圖顯示
     */
    public void openMapInBrowser(String query) {
        SearchBar.openMapInBrowser(query);
    }

    // --- Data Handling Methods (Delegated to DataManager) ---
    private void loadAndDisplayRestaurantData(String jsonFilePath) {
        // 🎯 使用改進的數據載入方法，同時更新評分數值顯示
        loadAndDisplayRestaurantDataWithRatingValues(jsonFilePath);
        
        // 計算平均消費中位數
        String medianExpense = calculateMedianExpense(jsonFilePath);
        
        // 更新右側面板的平均消費中位數
        rightPanel.updateMedianExpense(medianExpense);
        
        // 設置當前JSON檔案路徑，供近期評論功能使用
        rightPanel.setCurrentJsonFilePath(jsonFilePath);
        
        // 🚫 移除自動載入評論 - 讓用戶手動點擊時間按鈕來載入評論
        // rightPanel.updateRecentReviewsDisplay(30); // 30天
    }
    
    /**
     * 為當前餐廳更新消費中位數（優先使用 Firebase 真實數據）
     */
    public void updateCurrentRestaurantExpense(String restaurantName) {
        if (rightPanel != null) {
            // 🔥 優先嘗試從 Firebase 獲取真實數據
            String realExpense = null;
            String restaurantId = getCurrentRestaurantId();
            if (restaurantId != null && !restaurantId.isEmpty()) {
                try {
                    realExpense = FirebaseExpenseManager.getMedianExpenseFromFirebase(restaurantId);
                } catch (Exception e) {
                    System.err.println("⚠️ 從 Firebase 獲取消費數據失敗: " + e.getMessage());
                }
            }
            
            // 如果 Firebase 沒有數據，才使用估算
            String finalExpense;
            if (realExpense != null && !realExpense.trim().isEmpty()) {
                finalExpense = realExpense;
                System.out.println("✅ 使用 Firebase 真實消費數據: " + finalExpense);
            } else {
                finalExpense = estimateExpenseFromRestaurantName(restaurantName);
                System.out.println("⚠️ Firebase 無數據，使用估算: " + finalExpense);
            }
            
            rightPanel.updateMedianExpense(finalExpense);
            System.out.println("💰 為餐廳 " + restaurantName + " 更新消費數據: " + finalExpense);
        }
    }
    
    /**
     * 獲取當前餐廳 ID
     */
    private String getCurrentRestaurantId() {
        // 優先從 rightPanel 獲取當前餐廳 ID
        if (rightPanel != null) {
            String restaurantId = rightPanel.getCurrentRestaurantId();
            if (restaurantId != null && !restaurantId.isEmpty()) {
                return restaurantId;
            }
        }
        
        // 備用：從當前分頁獲取餐廳 ID
        if (currentTabId != null && tabContents.containsKey(currentTabId)) {
            TabContent currentTab = tabContents.get(currentTabId);
            // 檢查是否是餐廳 ID 格式（包含冒號的 Google Maps ID）
            if (currentTab.id != null && currentTab.id.contains(":")) {
                return currentTab.id;
            }
        }
        
        return null;
    }
    
    /**
     * 根據餐廳名稱估算消費範圍
     */
    private String estimateExpenseFromRestaurantName(String restaurantName) {
        if (restaurantName == null) return "NT$150-350 (估算)";
        
        String name = restaurantName.toLowerCase();
        
        // 根據餐廳名稱進行更精確的估算
        if (name.contains("ruth") && name.contains("coffee") || name.contains("茹絲") && name.contains("咖啡")) {
            return "NT$120-280 (咖啡店)";
        } else if (name.contains("coffee") || name.contains("咖啡")) {
            return "NT$100-300 (咖啡店)";
        } else if (name.contains("燒臘") || name.contains("roast")) {
            return "NT$80-200 (燒臘店)";
        } else if (name.contains("小食堂") || name.contains("eatery")) {
            return "NT$150-400 (小食堂)";
        } else if (name.contains("火鍋") || name.contains("hotpot")) {
            return "NT$300-600 (火鍋店)";
        } else if (name.contains("餐廳") || name.contains("restaurant")) {
            return "NT$200-500 (餐廳)";
        } else if (name.contains("快餐") || name.contains("fast food")) {
            return "NT$50-150 (快餐)";
        } else if (name.contains("牛排") || name.contains("steak")) {
            return "NT$400-800 (牛排)";
        } else if (name.contains("日式") || name.contains("japanese") || name.contains("壽司") || name.contains("sushi")) {
            return "NT$250-600 (日式)";
        } else if (name.contains("義式") || name.contains("italian") || name.contains("披薩") || name.contains("pizza")) {
            return "NT$300-700 (義式)";
        } else {
            return "NT$150-350 (一般)";
        }
    }
    
    /**
     * 載入並顯示餐廳資料，同時更新評分數值顯示
     */
    private void loadAndDisplayRestaurantDataWithRatingValues(String jsonFilePath) {
        System.out.println("Loading data from: " + jsonFilePath);
        try {
            // 載入 JSON 評論數據
            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            JSONArray reviews = new JSONArray(content);
            
            // 清空現有顯示
            rightPanel.getRatingsBox().getChildren().removeIf(node -> node.getId() != null && node.getId().equals("message-label"));
            photosContainer.getChildren().clear();

            if (reviews != null && !reviews.isEmpty()) {
                // 計算平均評分
                Map<String, Double> averageScores = calculateAverageRatingsFromJson(reviews);
                
                Platform.runLater(() -> {
                    // 🎯 使用新的評分更新方法，同時更新進度條和數值
                    System.out.println("🔢 計算得到的平均評分：");
                    for (Map.Entry<String, Double> entry : averageScores.entrySet()) {
                        String category = entry.getKey();
                        double rating = entry.getValue();
                        System.out.println("  - " + category + ": " + rating);
                        rightPanel.updateRatingDisplay(category, rating);
                    }
                    
                    // 更新其他區域
                    updateReviewsAreaFromJson(reviews);
                    updatePhotosContainerFromJson(reviews);
                    String restaurantName = jsonFilePath.replace(".json", "").replace(" Info", "");
                    // 🎯 啟動 Firestore 特色分析
                    startFirestoreFeatureAnalysis(getCurrentRestaurantId(), restaurantName);
                    rightPanel.getRatingsHeader().setText(restaurantName + " - 綜合評分");
                });
            } else {
                Platform.runLater(() -> clearRestaurantDataDisplay("無法從 " + jsonFilePath + " 載入評論資料"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> clearRestaurantDataDisplay("讀取檔案時發生錯誤: " + jsonFilePath));
        }
    }
    
    /**
     * 從 JSON 評論數據計算平均評分
     */
    private Map<String, Double> calculateAverageRatingsFromJson(JSONArray reviews) {
        Map<String, Double> averageScores = new HashMap<>();
        if (reviews == null || reviews.length() == 0) {
            return averageScores;
        }

        double totalMealScore = 0, totalServiceScore = 0, totalAmbianceScore = 0;
        int mealCount = 0, serviceCount = 0, ambianceCount = 0;
        List<String> priceLevels = new ArrayList<>();

        for (int i = 0; i < reviews.length(); i++) {
            try {
                JSONObject review = reviews.getJSONObject(i);
                if (review.has("餐點") && !review.isNull("餐點")) {
                    totalMealScore += review.optDouble("餐點", 0.0);
                    mealCount++;
                }
                if (review.has("服務") && !review.isNull("服務")) {
                    totalServiceScore += review.optDouble("服務", 0.0);
                    serviceCount++;
                }
                if (review.has("氣氛") && !review.isNull("氣氛")) {
                    totalAmbianceScore += review.optDouble("氣氛", 0.0);
                    ambianceCount++;
                }
                if (review.has("平均每人消費") && !review.isNull("平均每人消費")) {
                    priceLevels.add(review.getString("平均每人消費"));
                }
            } catch (JSONException e) {
                // Skip review on error
            }
        }

        averageScores.put("餐點", (mealCount > 0) ? totalMealScore / mealCount : 0.0);
        averageScores.put("服務", (serviceCount > 0) ? totalServiceScore / serviceCount : 0.0);
        averageScores.put("環境", (ambianceCount > 0) ? totalAmbianceScore / ambianceCount : 0.0);
        averageScores.put("價格", estimatePriceRatingFromJson(priceLevels));

        return averageScores;
    }
    
    /**
     * 估算價格評分（從消費資料）
     */
    private double estimatePriceRatingFromJson(List<String> priceLevels) {
        if (priceLevels.isEmpty()) return 0.0;
        Map<String, Long> counts = priceLevels.stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        String mostFrequent = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");

        switch (mostFrequent) {
            case "E:TWD_1_TO_200": return 4.5;
            case "E:TWD_200_TO_400": return 3.5;
            case "E:TWD_400_TO_600": return 2.5;
            case "E:TWD_600_TO_800": return 1.5;
            case "E:TWD_800_TO_1000":
            case "E:TWD_OVER_1000": return 0.5;
            default: return 0.0;
        }
    }
    
    /**
     * 從 JSON 更新評論區域
     */
    private void updateReviewsAreaFromJson(JSONArray reviews) {
        // 簡化的評論顯示邏輯
        StringBuilder reviewsText = new StringBuilder();
        for (int i = 0; i < Math.min(reviews.length(), 10); i++) {
            try {
                JSONObject review = reviews.getJSONObject(i);
                String text = review.optString("評論內容", "");
                if (!text.isEmpty()) {
                    reviewsText.append("• ").append(text).append("\n\n");
                }
            } catch (JSONException e) {
                // Skip review on error
            }
        }
        reviewsArea.setText(reviewsText.toString());
    }
    
    /**
     * 從 JSON 更新照片容器（簡化版）
     */
    private void updatePhotosContainerFromJson(JSONArray reviews) {
        // 這裡可以加載照片，但現在先簡化處理
        photosContainer.getChildren().clear();
    }

    private void clearRestaurantDataDisplay(String message) {
        dataManager.clearRestaurantDataDisplay(message, 
            rightPanel.getRatingsHeader(), 
            rightPanel.getRatingsBox(), 
            rightPanel.getRatingBars(), 
            reviewsArea, 
            photosContainer, 
            rightPanel.getFeaturesArea());
            
        // 同時更新右側面板的顯示
        rightPanel.clearDataDisplay(message);
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
                    
                    // AI 初始化已移至 AppLauncher，這裡不再需要延遲初始化
                    System.out.println("主界面動畫完成，AI 功能應該已在啟動時初始化");
                });
                pause5.play();
            }
        });
        pause4.play();
    }

    // 搜尋歷史功能已移除
    
    /**
     * 處理搜索請求
     */
    private void handleSearch(String query) {
        // 清空現有評論數據
        clearRestaurantDataDisplay("正在搜尋 '" + query + "'...");
        
        if (query != null && !query.trim().isEmpty()) {
            String trimmedQuery = query.trim();
            
            // 創建新的執行緒執行 Algolia 搜尋，避免阻塞 UI
            new Thread(() -> {
                try {
                    // 使用 AlgoliaRestaurantSearch 進行搜尋
                    org.json.JSONObject searchResult = new bigproject.search.AlgoliaRestaurantSearch().performSearch(trimmedQuery, true);
                    int hitsCount = searchResult.getInt("nbHits");
                    
                    // 在主執行緒中更新 UI
                    Platform.runLater(() -> {
                        if (hitsCount > 0) {
                            // 找到搜尋結果，顯示第一個結果
                            org.json.JSONArray hits = searchResult.getJSONArray("hits");
                            org.json.JSONObject firstHit = hits.getJSONObject(0);
                            String restaurantName = firstHit.getString("name");
                            String restaurantId = firstHit.optString("objectID", "");
                            
                            // 在控制台顯示搜尋結果摘要
                            System.out.println("找到 " + hitsCount + " 家與「" + trimmedQuery + "」相關的餐廳");
                            for (int i = 0; i < Math.min(hits.length(), 3); i++) {
                                org.json.JSONObject hit = hits.getJSONObject(i);
                                System.out.println((i + 1) + ". " + hit.getString("name") + " - " + hit.optString("address", "無地址資訊"));
                            }
                            
                            // 根據餐廳名稱載入對應的 JSON 檔案，或者使用 data-collector 獲取新資料
                            if (restaurantName.contains("海大") || restaurantName.contains("Haidai")) {
                                loadAndDisplayRestaurantData("Haidai Roast Shop.json");
                            } else if (restaurantName.contains("海那邊") || restaurantName.contains("Sea Side")) {
                                loadAndDisplayRestaurantData("Sea Side Eatery Info.json");
                            } else {
                                // 使用 data-collector 獲取餐廳的精選評論和照片
                                collectFeaturedReviewsAndPhotos(restaurantName, restaurantId);
                            }
                            
                        } else {
                            // 如果 Algolia 沒有結果，顯示餐廳未找到的整個畫面
                            showRestaurantNotFoundView(trimmedQuery);
                        }
                    });
                    
                } catch (Exception e) {
                    // 如果搜尋過程出錯，顯示錯誤訊息
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        clearRestaurantDataDisplay("搜尋時發生錯誤：" + e.getMessage());
                        SearchBar.openMapInBrowser(trimmedQuery);
                    });
                }
            }).start();
        }
        // 空查詢不執行任何操作
    }
    
    /**
     * 使用 data-collector 收集餐廳的精選評論和照片
     */
    private void collectFeaturedReviewsAndPhotos(String restaurantName, String restaurantId) {
        // 🔧 修復：設置當前餐廳信息到 RightPanel
        System.out.println("🏪 設置餐廳信息到 RightPanel:");
        System.out.println("  - 餐廳名稱: " + restaurantName);
        System.out.println("  - 餐廳ID: " + restaurantId);
        rightPanel.setCurrentRestaurantInfo(restaurantName, restaurantId, null);
            
        // 同步更新側欄的餐廳資訊
        if (recentReviewsSidebar != null) {
            recentReviewsSidebar.setCurrentRestaurantInfo(restaurantName, restaurantId, null);
        }
            
        // 更新消費中位數估算（當沒有本地 JSON 文件時）
        updateCurrentRestaurantExpense(restaurantName);
        
        // 🎯 搜尋成功後立即啟動 Firestore 特色分析
        System.out.println("🚀 搜尋成功，啟動 Firestore 特色分析...");
        startFirestoreFeatureAnalysis(restaurantId, restaurantName);
        
        // 🎯 啟動評分數據分析
        System.out.println("🔍 啟動餐廳評分分析...");
        if (ratingAnalyzer != null) {
            ratingAnalyzer.analyzeRestaurantRatingsAsync(restaurantId, restaurantName);
        } else {
            System.out.println("⚠️ RatingDataAnalyzer 尚未初始化");
        }
        
        Platform.runLater(() -> {
            clearRestaurantDataDisplay("正在收集 " + restaurantName + " 的精選評論和照片...");
        });
        
        new Thread(() -> {
            try {
                // 使用新的 featured_collector.py 腳本
                String[] command = {
                    ".venv/bin/python", 
                    "data-collector/featured_collector.py", 
                    "--id", restaurantId,
                    "--name", restaurantName,
                    "--pages", "3",
                    "--output", "temp_featured_data.json"
                };
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File("."));
                pb.redirectErrorStream(true);
                
               
                
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    Platform.runLater(() -> {
                        parseAndDisplayCollectedData(restaurantName);
                    });
                } else {
                    Platform.runLater(() -> {
                        clearRestaurantDataDisplay("無法收集 " + restaurantName + " 的資料，請稍後再試");
                        SearchBar.openMapInBrowser(restaurantName);
                    });
                }
                
                // 清理臨時檔案
                cleanupTempFiles("temp_featured_data.json");
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearRestaurantDataDisplay("收集資料時發生錯誤：" + e.getMessage());
                    SearchBar.openMapInBrowser(restaurantName);
                });
            }
        }).start();
    }
    
    /**
     * 顯示餐廳未找到的整個畫面視圖
     */
    private void showRestaurantNotFoundView(String query) {
        Platform.runLater(() -> {
            clearRestaurantDataDisplay("在資料庫中找不到「" + query + "」");
            
            // 使用 UIManager 的新方法來顯示整個畫面
            uiManager.showRestaurantNotFoundView(query, 
                // 收集資料的動作 - 注意：這個回調不會被使用，
                // 實際的餐廳名稱會透過 fullNameCollectCallback 傳入
                () -> collectAndUploadRestaurantToFirebase(query),
                // 開啟地圖的動作
                () -> SearchBar.openMapInBrowser(query)
            );
        });
    }
    
    /**
     * 收集餐廳資料並上傳到 Firebase
     */
    private void collectAndUploadRestaurantToFirebase(String query) {
        Platform.runLater(() -> {
            uiManager.showDataCollectionProgressView(query);
            uiManager.updateDataCollectionProgress(0.1, "正在檢查「" + query + "」是否已存在於資料庫中...");
        });
        
        new Thread(() -> {
            try {
                // 🔍 先檢查餐廳是否已存在於 Firebase 中
                boolean existsInFirebase = checkRestaurantExistsInFirebase(query);
                
                if (existsInFirebase) {
                    Platform.runLater(() -> {
                        uiManager.updateDataCollectionProgress(0.9, "「" + query + "」已存在於資料庫中，正在同步到搜尋引擎...");
                    });
                    
                    // 餐廳已存在於 Firebase，直接同步到 Algolia
                    syncRestaurantToAlgolia(query);
                    
                    Platform.runLater(() -> {
                        uiManager.updateDataCollectionProgress(1.0, "✅ 同步完成！");
                        uiManager.showDataCollectionCompleteView(query, true, "餐廳資料已存在，已成功同步到搜尋引擎。");
                        
                        // 延遲返回主視圖並搜尋餐廳
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(() -> {
                                    uiManager.showMainView();
                                    handleSearch(query);
                                });
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    });
                    return;
                }
                
                // 餐廳不存在於 Firebase，從 Google Maps 搜尋
                Platform.runLater(() -> {
                    uiManager.updateDataCollectionProgress(0.15, "資料庫中未找到「" + query + "」，正在從 Google Maps 搜尋...");
                });
                
                String foundRestaurantName = checkRestaurantNameFromGoogleMaps(query);
                
                if (foundRestaurantName != null && !foundRestaurantName.isEmpty()) {
                    Platform.runLater(() -> {
                        uiManager.updateDataCollectionProgress(0.2, "在 Google Maps 找到：「" + foundRestaurantName + "」\n\n正在收集餐廳資料...");
                    });
                    
                    // 直接收集資料
                    proceedWithDataCollection(query);
                } else {
                    Platform.runLater(() -> {
                        clearRestaurantDataDisplay("很抱歉，無法在 Google Maps 中找到「" + query + "」\n\n請檢查餐廳名稱是否正確，\n或嘗試使用更完整的餐廳名稱。");
                        
                        // 顯示提示對話框，然後回到主視圖
                        javafx.scene.control.Alert warningAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                        warningAlert.setTitle("找不到餐廳");
                        warningAlert.setHeaderText("Google Maps 中找不到餐廳");
                        warningAlert.setContentText("無法在 Google Maps 中找到「" + query + "」。\n\n建議：\n1. 檢查餐廳名稱拼寫\n2. 使用更完整的餐廳名稱\n3. 嘗試包含地區或分店資訊");
                        
                        warningAlert.showAndWait().ifPresent(response -> {
                            uiManager.showMainView();
                        });
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearRestaurantDataDisplay("檢查時發生錯誤：" + e.getMessage());
                    
                    javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    errorAlert.setTitle("系統錯誤");
                    errorAlert.setHeaderText("檢查餐廳時發生錯誤");
                    errorAlert.setContentText("發生錯誤：" + e.getMessage());
                    
                    errorAlert.showAndWait().ifPresent(response -> {
                        uiManager.showMainView();
                    });
                });
            }
        }).start();
    }
    
    /**
     * 檢查餐廳是否已存在於 Firebase 中
     */
    private boolean checkRestaurantExistsInFirebase(String query) {
        try {
            System.out.println("🔍 檢查餐廳是否存在於 Firebase: " + query);
            
            String[] command = {
                ".venv/bin/python", 
                "scripts/check_firebase_restaurant.py",
                query
            };
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("EXISTS:")) {
                        String existsStr = line.substring("EXISTS:".length()).trim();
                        boolean exists = "true".equalsIgnoreCase(existsStr);
                        if (exists) {
                            System.out.println("✅ 餐廳已存在於 Firebase: " + query);
                        } else {
                            System.out.println("❌ 餐廳不存在於 Firebase: " + query);
                        }
                        return exists;
                    }
                    System.out.println("Firebase 檢查: " + line);
                }
            }
            
            process.waitFor();
            return false; // 如果沒有收到明確回應，假設不存在
            
        } catch (Exception e) {
            System.err.println("檢查 Firebase 時發生錯誤：" + e.getMessage());
            return false; // 發生錯誤時假設不存在，繼續正常流程
        }
    }
    
    /**
     * 從 Google Maps 檢查餐廳名稱（不執行收集）
     * 會嘗試多種搜尋詞組變化以提高成功率
     */
    private String checkRestaurantNameFromGoogleMaps(String query) {
        // 生成多種搜尋詞組變化
        String[] searchVariants = generateSearchVariants(query);
        
        for (String variant : searchVariants) {
            try {
                System.out.println("🔍 嘗試搜尋變體：" + variant);
                
                // 使用專用的Python腳本檢查餐廳是否存在
                String[] command = {
                    ".venv/bin/python", 
                    "scripts/check_restaurant.py",
                    variant
                };
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File("."));
                pb.redirectErrorStream(true);
                
                Process process = pb.start();
                
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("FOUND_NAME:")) {
                            String foundName = line.substring("FOUND_NAME:".length()).trim();
                            System.out.println("✅ 成功找到餐廳：" + foundName + " (使用搜尋詞：" + variant + ")");
                            return foundName;
                        }
                    }
                }
                
                process.waitFor();
            } catch (Exception e) {
                System.err.println("檢查餐廳名稱時發生錯誤（搜尋詞：" + variant + "）：" + e.getMessage());
            }
        }
        
        System.out.println("❌ 嘗試了所有搜尋變體都未找到餐廳");
        return null;
    }
    
    /**
     * 為給定的查詢生成多種搜尋詞組變化，提高搜尋成功率
     */
    private String[] generateSearchVariants(String query) {
        java.util.List<String> variants = new java.util.ArrayList<>();
        
        // 1. 原始查詢
        variants.add(query);
        
        // 2. 基於常見分隔符分割
        String[] words = query.split("[\\s\\-－_&]+");
        if (words.length > 1) {
            // 嘗試前1-3個單詞的組合
            for (int i = 1; i <= Math.min(3, words.length); i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < i; j++) {
                    if (j > 0) sb.append(" ");
                    sb.append(words[j]);
                }
                String variant = sb.toString().trim();
                if (!variant.isEmpty() && variant.length() > 2) {
                    variants.add(variant);
                }
            }
        }
        
        // 3. 針對中文餐廳名稱的智能分割（如果沒有明顯分隔符）
        if (words.length == 1 && query.length() > 3) {
            // 嘗試常見的餐廳名稱模式
            if (query.contains("廣東粥")) {
                String baseName = query.replace("廣東粥", "").trim();
                if (!baseName.isEmpty() && baseName.length() >= 2) {
                    variants.add(baseName);  // 例如：好粥到廣東粥 -> 好粥到
                }
            }
            
            if (query.contains("冰沙豆花")) {
                String baseName = query.replace("冰沙豆花", "").trim();
                if (!baseName.isEmpty() && baseName.length() >= 2) {
                    variants.add(baseName);  // 例如：好豆味冰沙豆花 -> 好豆味
                }
            }
            
            // 針對前2-4個字符作為餐廳主名稱的嘗試
            if (query.length() >= 4) {
                for (int len = 2; len <= Math.min(4, query.length() - 1); len++) {
                    String prefix = query.substring(0, len);
                    if (prefix.length() >= 2) {
                        variants.add(prefix);
                    }
                }
            }
        }
        
        // 4. 移除分店和地區資訊
        String withoutBranch = query.replaceAll("[-－].*店.*", "")
                                   .replaceAll("[-－].*分店.*", "")
                                   .replaceAll("[-－].*門市.*", "")
                                   .replaceAll("[-－].*SOGO.*", "")
                                   .replaceAll("[-－].*新竹.*", "")
                                   .replaceAll("[-－].*台北.*", "")
                                   .replaceAll("[-－].*台中.*", "")
                                   .replaceAll("[-－].*高雄.*", "")
                                   .replaceAll("\\s*(新竹|台北|台中|高雄|竹北|竹南).*", "")
                                   .trim();
        if (!withoutBranch.equals(query) && !withoutBranch.isEmpty() && withoutBranch.length() > 2) {
            variants.add(withoutBranch);
        }
        
        // 5. 只保留中文和英文主要部分
        String cleanName = query.replaceAll("[\\s\\-－_()（）\\[\\]]+", " ")
                               .replaceAll("\\s+", " ")
                               .trim();
        if (!cleanName.equals(query) && !cleanName.isEmpty()) {
            variants.add(cleanName);
        }
        
        // 6. 如果包含英文，嘗試只保留英文部分
        if (query.matches(".*[a-zA-Z].*")) {
            String englishOnly = query.replaceAll("[^a-zA-Z\\s&]", " ")
                                     .replaceAll("\\s+", " ")
                                     .trim();
            if (!englishOnly.isEmpty() && englishOnly.length() > 2) {
                variants.add(englishOnly);
            }
        }
        
        // 7. 如果包含中文，嘗試只保留中文部分
        String chineseOnly = query.replaceAll("[a-zA-Z0-9\\s\\-－_()（）\\[\\]&]+", "")
                                 .trim();
        if (!chineseOnly.isEmpty() && chineseOnly.length() > 2) {
            variants.add(chineseOnly);
        }
        
        // 移除重複項目並保持原始順序
        java.util.LinkedHashSet<String> uniqueVariants = new java.util.LinkedHashSet<>(variants);
        String[] result = uniqueVariants.toArray(new String[0]);
        
        System.out.println("📝 生成的搜尋變體：" + java.util.Arrays.toString(result));
        return result;
    }
    
    /**
     * 計算兩個字串的相似度
     */
    private double calculateNameSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) return 0.0;
        if (str1.equals(str2)) return 1.0;
        
        // 檢查是否包含關係
        if (str2.contains(str1) || str1.contains(str2)) {
            return 0.8; // 如果一個包含另一個，認為相似度較高
        }
        
        // 使用簡單的編輯距離算法
        int len1 = str1.length();
        int len2 = str2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            for (int j = 0; j <= len2; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        
        int maxLen = Math.max(len1, len2);
        return maxLen == 0 ? 1.0 : 1.0 - (double) dp[len1][len2] / maxLen;
    }
    
    /**
     * 執行實際的資料收集工作
     */
    private void proceedWithDataCollection(String query) {
        Platform.runLater(() -> {
            uiManager.showDataCollectionProgressView(query);
            uiManager.updateDataCollectionProgress(0.2, "正在收集並上傳「" + query + "」到 Firebase...");
        });
        
        try {
            // 使用 search_res_by_name_upload_firebase.py 腳本
            String[] command = {
                ".venv/bin/python", 
                "data-collector/search_res_by_name_upload_firebase.py", 
                query
            };
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 讀取輸出以獲得進度信息
            final boolean[] hasConflictError = {false};
            final String[] lastSuccessName = {null};
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                
                // 讀取輸出，因為使用了redirectErrorStream(true)，錯誤也會在標準輸出中
                String line;
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line;
                    Platform.runLater(() -> {
                        System.out.println("Data collection output: " + outputLine);
                        
                        // 根據輸出內容更新進度條
                        if (outputLine.contains("正在搜尋餐廳")) {
                            uiManager.updateDataCollectionProgress(0.3, "🔍 正在搜尋餐廳...");
                        } else if (outputLine.contains("找到餐廳 ID")) {
                            uiManager.updateDataCollectionProgress(0.4, "✅ 找到餐廳 ID");
                        } else if (outputLine.contains("餐廳資訊：")) {
                            uiManager.updateDataCollectionProgress(0.5, "📍 已獲取餐廳資訊");
                        } else if (outputLine.contains("正在收集評論資料")) {
                            uiManager.updateDataCollectionProgress(0.6, "💬 正在收集評論資料...");
                        } else if (outputLine.contains("正在上傳到 Firestore")) {
                            uiManager.updateDataCollectionProgress(0.8, "☁️ 正在上傳到 Firestore...");
                        } else if (outputLine.contains("已成功上傳至 Firestore")) {
                            uiManager.updateDataCollectionProgress(0.95, "🎉 資料上傳完成！");
                        }
                    });
                    
                    // 檢查是否有 409 衝突錯誤
                    if (line.contains("409") && line.contains("Conflict")) {
                        hasConflictError[0] = true;
                    }
                    
                    // 檢查是否有成功上傳的餐廳名稱
                    if (line.startsWith("餐廳名稱：")) {
                        lastSuccessName[0] = line.substring("餐廳名稱：".length()).trim();
                    }
                }
            }
            
            // 設定超時時間為60秒
            boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            int exitCode;
            
            if (!finished) {
                // 如果超時，強制終止進程
                process.destroyForcibly();
                exitCode = -1;
                System.err.println("Python script timed out after 60 seconds");
                Platform.runLater(() -> {
                    uiManager.updateDataCollectionProgress(0.0, "❌ 資料收集超時");
                    uiManager.showDataCollectionCompleteView(query, false, "資料收集過程超時，請稍後再試。");
                });
                return;
            } else {
                exitCode = process.exitValue();
                System.out.println("Python script finished with exit code: " + exitCode);
            }
            
            if (exitCode == 0) {
                final String finalSuccessName = lastSuccessName[0];
                Platform.runLater(() -> {
                    // 更新進度條到100%
                    uiManager.updateDataCollectionProgress(1.0, "✅ 完成！");
                    
                    String successMessage = hasConflictError[0] ? 
                        String.format("✅ 好消息！「%s」已經在資料庫中了！\n\n🏪 實際餐廳名稱：%s\n\n💡 為什麼剛才搜尋不到？\n• 資料庫同步需要 2-5 分鐘時間\n• 搜尋引擎正在更新索引\n\n🔍 系統將自動重新搜尋：\n• 3秒後自動重新搜尋「%s」\n• 如果還是找不到，請等待2-3分鐘後再試", 
                                    query, 
                                    finalSuccessName != null ? finalSuccessName : query,
                                    finalSuccessName != null ? finalSuccessName : query) :
                        String.format("🎉 成功！「%s」的資料已經收集完成！\n\n📊 資料已上傳到 Firebase\n🔍 系統將自動重新搜尋這家餐廳！", query);
                    
                    // 使用新的完成視圖
                    uiManager.showDataCollectionCompleteView(
                        finalSuccessName != null ? finalSuccessName : query, 
                        true, 
                        successMessage
                    );
                    
                    // 自動同步到Algolia並延遲足夠時間後重新搜尋
                    new Thread(() -> {
                        try {
                            // 先等待1秒讓Firebase寫入完成
                            Thread.sleep(1000);
                            
                            // 自動同步到Algolia
                            syncRestaurantToAlgolia(finalSuccessName != null ? finalSuccessName : query);
                            
                            // 等待8秒確保Algolia索引更新完成
                            Thread.sleep(8000);
                            Platform.runLater(() -> {
                                uiManager.showMainView();
                                if (hasConflictError[0] && finalSuccessName != null && !finalSuccessName.equals(query)) {
                                    handleSearch(finalSuccessName);
                                } else {
                                    handleSearch(query);
                                }
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                });
            } else {
                Platform.runLater(() -> {
                    uiManager.updateDataCollectionProgress(0.0, "❌ 上傳失敗");
                    
                    String errorMessage = "無法從 Google Maps 找到「" + query + "」的資料，\n請確認餐廳名稱是否正確，或嘗試使用更精確的關鍵字。\n\n您也可以選擇在 Google Maps 中手動搜尋。";
                    
                    uiManager.showDataCollectionCompleteView(query, false, errorMessage);
                });
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                clearRestaurantDataDisplay("收集資料時發生錯誤：" + e.getMessage());
                SearchBar.openMapInBrowser(query);
            });
        }
    }
    
    /**
     * 自動同步餐廳到Algolia搜尋引擎
     */
    private void syncRestaurantToAlgolia(String restaurantName) {
        try {
            System.out.println("正在同步餐廳到Algolia：" + restaurantName);
            
            String[] command = {
                ".venv/bin/python", 
                "scripts/auto_sync_restaurant.py", 
                restaurantName
            };
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 讀取輸出
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Algolia sync: " + line);
                }
            }
            
            // 增加超時保護，防止無限等待
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                // 如果超時，強制終止進程
                process.destroyForcibly();
                System.out.println("⚠️ Algolia同步超時，已強制終止");
            } else {
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    System.out.println("✅ 成功同步餐廳到Algolia：" + restaurantName);
                } else {
                    System.out.println("⚠️ Algolia同步失敗，退出碼：" + exitCode);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Algolia同步發生錯誤：" + e.getMessage());
        }
    }

    /**
     * 直接從 Google Maps 搜尋並收集餐廳資料
     */
    private void collectRestaurantDataFromGoogleMaps(String query) {
        Platform.runLater(() -> {
            clearRestaurantDataDisplay("正在 Google Maps 中搜尋 " + query + "...");
        });
        
        new Thread(() -> {
            try {
                String[] command = {
                    ".venv/bin/python", 
                    "data-collector/featured_collector.py", 
                    "--search", query,
                    "--pages", "2",
                    "--output", "temp_featured_data.json"
                };
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File("."));
                pb.redirectErrorStream(true);
                
                Platform.runLater(() -> {
                    clearRestaurantDataDisplay("正在收集餐廳資料...");
                });
                
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    Platform.runLater(() -> {
                        parseAndDisplayCollectedData(query);
                    });
                } else {
                    Platform.runLater(() -> {
                        clearRestaurantDataDisplay("在餐廳資料庫中找不到「" + query + "」，已在 Google Maps 中開啟搜尋");
                        SearchBar.openMapInBrowser(query);
                    });
                }
                
                cleanupTempFiles("temp_featured_data.json");
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    clearRestaurantDataDisplay("搜尋時發生錯誤：" + e.getMessage());
                    SearchBar.openMapInBrowser(query);
                });
            }
        }).start();
    }
    
    /**
     * 解析並顯示收集到的資料
     */
    private void parseAndDisplayCollectedData(String restaurantName) {
        try {
            String jsonPath = "temp_featured_data.json";
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(jsonPath))) {
                String jsonContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(jsonPath)));
                org.json.JSONObject result = new org.json.JSONObject(jsonContent);
                
                String actualRestaurantName = result.optString("restaurant_name", restaurantName);
                org.json.JSONArray featuredReviews = result.optJSONArray("featured_reviews");
                org.json.JSONArray featuredPhotos = result.optJSONArray("featured_photos");
                int totalReviews = result.optInt("total_reviews", 0);
                
                // 更新評論區域
                StringBuilder reviewsText = new StringBuilder();
                reviewsText.append("餐廳：").append(actualRestaurantName).append("\n");
                reviewsText.append("共收集到 ").append(totalReviews).append(" 則評論，以下為精選評論：\n\n");
                
                if (featuredReviews != null && featuredReviews.length() > 0) {
                    for (int i = 0; i < featuredReviews.length(); i++) {
                        org.json.JSONObject review = featuredReviews.getJSONObject(i);
                        String reviewerName = review.optString("reviewer_name", "匿名");
                        int rating = review.optInt("star_rating", 0);
                        String comment = review.optString("comment", "");
                        String date = review.optString("comment_date", "");
                        
                        reviewsText.append("【").append(reviewerName).append("】")
                                   .append(" ★".repeat(rating))
                                   .append(" (").append(rating).append("/5)\n");
                        if (!date.isEmpty()) {
                            reviewsText.append("日期：").append(date).append("\n");
                        }
                        reviewsText.append(comment).append("\n\n");
                    }
                } else {
                    reviewsText.append("暫無精選評論\n");
                }
                
                reviewsArea.setText(reviewsText.toString());
                
                // 載入精選照片
                photosContainer.getChildren().clear();
                if (featuredPhotos != null && featuredPhotos.length() > 0) {
                    for (int i = 0; i < featuredPhotos.length(); i++) {
                        String photoUrl = featuredPhotos.getString(i);
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            loadPhotoFromUrl(photoUrl);
                        }
                    }
                } else {
                    // 如果沒有照片，顯示提示
                    javafx.scene.control.Label noPhotosLabel = new javafx.scene.control.Label("暫無精選照片");
                    noPhotosLabel.setStyle("-fx-text-fill: #777777; -fx-font-style: italic; -fx-padding: 20;");
                    photosContainer.getChildren().add(noPhotosLabel);
                }
                
                // 顯示統計信息到右側面板
                showCollectedDataStats(actualRestaurantName, totalReviews, featuredReviews != null ? featuredReviews.length() : 0);
                
            } else {
                clearRestaurantDataDisplay("無法找到收集到的資料");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            clearRestaurantDataDisplay("解析收集到的資料時發生錯誤：" + e.getMessage());
        }
    }
    
    /**
     * 顯示收集到的數據統計
     */
    private void showCollectedDataStats(String restaurantName, int totalReviews, int featuredReviews) {
        // 更新右側面板的標題
        rightPanel.getRatingsHeader().setText("綜合評分 - " + restaurantName);
        
        // 計算並顯示簡單的評分
        double baseScore = totalReviews > 0 ? Math.min(0.9, (double) featuredReviews / totalReviews + 0.3) : 0.5;
        
        // 更新評分條
        Map<String, ProgressBar> bars = rightPanel.getRatingBars();
        bars.get("餐點").setProgress(Math.min(1.0, baseScore + (Math.random() - 0.5) * 0.2));
        bars.get("服務").setProgress(Math.min(1.0, baseScore + (Math.random() - 0.5) * 0.2));
        bars.get("環境").setProgress(Math.min(1.0, baseScore + (Math.random() - 0.5) * 0.2));
        bars.get("價格").setProgress(Math.min(1.0, baseScore + (Math.random() - 0.5) * 0.2));
        
        // 更新特色區域
        rightPanel.getFeaturesArea().setText("即時收集的精選評論：\n總共 " + totalReviews + " 則評論\n精選 " + featuredReviews + " 則高品質評論");
        
        // 更新右側面板的優點和缺點區域（透過RightPanel的方法）
        rightPanel.updateAnalysisAreas(
            "優點：\n• 評論來源真實可靠\n• 篩選高評分內容\n• 包含用戶上傳照片",
            "注意：\n• 資料即時收集，可能需要等待\n• 評論數量取決於餐廳人氣\n• 建議參考多個來源"
        );
    }
    
    /**
     * 從URL載入照片
     */
    private void loadPhotoFromUrl(String photoUrl) {
        new Thread(() -> {
            try {
                javafx.scene.image.Image image = new javafx.scene.image.Image(photoUrl, true);
                Platform.runLater(() -> {
                    if (!image.isError()) {
                        addPhotoToContainer(image);
                    }
                });
            } catch (Exception e) {
                System.err.println("載入照片失敗: " + photoUrl + " - " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 清理臨時檔案
     */
    private void cleanupTempFiles(String filename) {
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filename));
        } catch (IOException e) {
            System.err.println("清理臨時檔案時發生錯誤: " + e.getMessage());
        }
    }

    /**
     * 實現 UIManager.StateChangeListener 接口的方法
     */
    @Override
    public void onMonthlyReportStateChanged(boolean isShowing) {
        // 月報功能已移除，不做任何處理
    }
    
    @Override
    public void onSuggestionsStateChanged(boolean isShowing) {
        isSuggestionActive[0] = isShowing;
        suggestionButton.setStyle(isShowing ? activeButtonStyle : normalButtonStyle);
        

        
        // 如果顯示建議，確保AI聊天視圖關閉
        if (isShowing && aiChat.isActive()) {
            aiChat.hideChatView();
        }
        
        // 如果顯示建議，確保設定視圖關閉
        if (isShowing && isSettingsActive[0]) {
            isSettingsActive[0] = false;
            settingsButton.setStyle(normalButtonStyle);
        }
    }
    
    @Override
    public void onRestaurantNotFoundStateChanged(boolean isShowing) {
        // 當餐廳未找到視圖顯示時，確保其他視圖都被關閉
        if (isShowing) {
            if (isSuggestionActive[0]) {
                isSuggestionActive[0] = false;
                suggestionButton.setStyle(normalButtonStyle);
            }

            if (isSettingsActive[0]) {
                isSettingsActive[0] = false;
                settingsButton.setStyle(normalButtonStyle);
            }
            if (aiChat.isActive()) {
                aiChat.hideChatView();
            }
        }
    }
    
    // 實現 AIChat.ChatStateChangeListener 接口的方法
    @Override
    public void onChatStateChanged(boolean isShowing) {
        // 如果顯示AI聊天，確保其他視圖關閉
        if (isShowing) {
            // 如果建議視圖是活躍的，關閉它
            if (isSuggestionActive[0]) {
                uiManager.toggleSuggestionsView();
                isSuggestionActive[0] = false;
                suggestionButton.setStyle(normalButtonStyle);
            }
            
            // 如果設定視圖是活躍的，關閉它
            if (isSettingsActive[0]) {
                preferencesManager.toggleSettingsView();
                isSettingsActive[0] = false;
                settingsButton.setStyle(normalButtonStyle);
            }
        }
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
            
            // 如果AI聊天視圖是活躍的，關閉它
            if (aiChat.isActive()) {
                aiChat.hideChatView();
            }
        }
    }
    
    /**
     * 切換顯示AI聊天視圖
     */
    public void toggleAIChatView(String title, String initialContent, String contentType) {
        // 直接使用AIChat實例的toggleChatView方法
        aiChat.toggleChatView(title, initialContent, contentType);
    }

    // 已移除 setupInitialResponsiveLayout 方法，因為它不再被使用且引用了已刪除的方法

    /**
     * 設置固定佈局 (取消RWD功能)
     */
    private void setupResponsiveLayout(Stage primaryStage, HBox mainContentBox, 
                                      ScrollPane leftScrollPane) {
        // 🎯 移除固定最小寬度限制，讓主內容區域能完全擴展
        // mainContentBox.setMinWidth(400); // 移除這個限制
        
        // 🎯 確保主內容區域能垂直填滿
        mainContentBox.setPrefHeight(Double.MAX_VALUE);
        mainContentBox.setMaxHeight(Double.MAX_VALUE);
        mainContentBox.setMinHeight(Region.USE_COMPUTED_SIZE);
        
        // 🎯 設置 VBox.setVgrow 確保主內容區域能自動擴展
        VBox.setVgrow(mainContentBox, Priority.ALWAYS);
        
        // 🎯 不再手動設置左右面板寬度，讓6:4比例自動生效
        // 右側面板已經綁定40%寬度，左側面板會自動佔據剩餘的60%空間
        
        // 調整面板高度
        adjustPanelHeights(mainContentBox, leftScrollPane, primaryStage.getHeight());
        
        // 確保分頁欄顯示
        ensureTabBarVisible();
                
        // 主佈局進行一次調整
        mainLayout.layout();
    }
    
    /**
     * 調整面板高度 - 🎯 完全移除所有高度限制
     */
    private void adjustPanelHeights(HBox mainContentBox, ScrollPane leftScrollPane, double windowHeight) {
        // 🎯 完全移除所有高度限制，讓面板自然填滿整個可用空間
        
        // 🎯 移除直接設置，因為已經有綁定了
        // leftScrollPane.setPrefHeight(Double.MAX_VALUE); // 已有綁定，不能再設置
        leftScrollPane.setMinHeight(Region.USE_COMPUTED_SIZE); // 🎯 使用計算尺寸，不設固定限制
        leftScrollPane.setMaxHeight(Double.MAX_VALUE);
        
        // 🎯 移除右側面板的直接設置，因為已經有綁定了
        if (rightPanel != null) {
            // rightPanel.setPrefHeight(Double.MAX_VALUE); // 已有綁定，不能再設置
            rightPanel.setMinHeight(Region.USE_COMPUTED_SIZE); // 🎯 使用計算尺寸，不設固定限制
            rightPanel.setMaxHeight(Double.MAX_VALUE);
        }
    }
    
    // 分頁欄顯示狀態標記
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
                tabBar.setStyle("-fx-background-color: rgba(42, 42, 42, 0.85); -fx-border-width: 2 0 0 0; -fx-border-color: #E67649; -fx-padding: 5 0 0 10;"); // 使用半透明背景
                
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
                
                // 確保主佈局知道在底部區域發生了變化
                mainLayout.layout();
                
                // 將分頁欄置於頂層，確保不會被其他元素覆蓋
                tabBar.toFront();
            }
        } finally {
            isEnsuringTabBar = false;
        }
    }

    /**
     * 更新面板大小，替代原RWD功能
     */
    private void updatePanelSizes(HBox mainContentBox, ScrollPane leftScrollPane, double width, double height) {
        // 🎯 不再設置固定寬度，讓6:4比例自動生效
        // 左側面板會自動佔據剩餘的60%空間
        
        // 只調整面板高度
        adjustPanelHeights(mainContentBox, leftScrollPane, height);
    }
    
    /**
     * 🔍 設置高度調試監聽器，幫助診斷底部空白問題
     */
    private void setupHeightDebugging(Stage primaryStage) {
        System.out.println("🔍 開始設置高度監聽器...");
        
        // 監聽窗口高度變化
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("📏 窗口高度變化: " + oldVal + " → " + newVal);
            printAllHeights();
        });
        
        // 監聽主佈局高度變化
        mainLayout.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("📏 主佈局高度變化: " + oldVal + " → " + newVal);
        });
        
        // 監聽主容器高度變化
        mainContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("📏 主容器高度變化: " + oldVal + " → " + newVal);
        });
        
        // 監聽主內容區域高度變化
        mainContentBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("📏 主內容區域高度變化: " + oldVal + " → " + newVal);
        });
        
        // 監聽分頁欄高度變化
        tabBar.heightProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("📏 分頁欄高度變化: " + oldVal + " → " + newVal);
        });
        
        // 監聽右側面板高度變化
        if (rightPanel != null) {
            rightPanel.heightProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("📏 右側面板高度變化: " + oldVal + " → " + newVal);
            });
        }
        
        // 初始輸出所有高度
        Platform.runLater(() -> {
            System.out.println("🔍 初始高度檢查:");
            printAllHeights();
        });
    }
    
    /**
     * 🔍 輸出所有組件的當前高度
     */
    private void printAllHeights() {
        Platform.runLater(() -> {
            System.out.println("═══ 高度診斷報告 ═══");
            
            if (mainScene != null && mainScene.getWindow() != null) {
                System.out.println("🖼️  窗口高度: " + mainScene.getWindow().getHeight());
            }
            
            if (mainLayout != null) {
                System.out.println("🏠 主佈局高度: " + mainLayout.getHeight());
                System.out.println("🏠 主佈局預設高度: " + mainLayout.getPrefHeight());
                System.out.println("🏠 主佈局最小高度: " + mainLayout.getMinHeight());
            }
            
            if (mainContainer != null) {
                System.out.println("📦 主容器高度: " + mainContainer.getHeight());
                System.out.println("📦 主容器預設高度: " + mainContainer.getPrefHeight());
                System.out.println("📦 主容器最小高度: " + mainContainer.getMinHeight());
            }
            
            if (mainContentBox != null) {
                System.out.println("📋 主內容區域高度: " + mainContentBox.getHeight());
                System.out.println("📋 主內容區域預設高度: " + mainContentBox.getPrefHeight());
                System.out.println("📋 主內容區域最小高度: " + mainContentBox.getMinHeight());
            }
            
            if (tabBar != null) {
                System.out.println("🗂️  分頁欄高度: " + tabBar.getHeight());
                System.out.println("🗂️  分頁欄預設高度: " + tabBar.getPrefHeight());
                System.out.println("🗂️  分頁欄最小高度: " + tabBar.getMinHeight());
                System.out.println("🗂️  分頁欄Y位置: " + tabBar.getLayoutY());
            }
            
            if (rightPanel != null) {
                System.out.println("🟢 右側面板高度: " + rightPanel.getHeight());
                System.out.println("🟢 右側面板預設高度: " + rightPanel.getPrefHeight());
                System.out.println("🟢 右側面板最小高度: " + rightPanel.getMinHeight());
                System.out.println("🟢 右側面板Y位置: " + rightPanel.getLayoutY());
            }
            
            // 計算應該的可用空間
            double windowHeight = mainScene != null && mainScene.getWindow() != null ? 
                mainScene.getWindow().getHeight() : 0;
            double tabBarHeight = tabBar != null ? tabBar.getHeight() : 0;
            double availableHeight = windowHeight - tabBarHeight;
            
            System.out.println("🧮 計算結果:");
            System.out.println("   窗口總高度: " + windowHeight);
            System.out.println("   分頁欄高度: " + tabBarHeight);
            System.out.println("   可用內容高度: " + availableHeight);
            
            System.out.println("════════════════════");
        });
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
        photoContainer.setAlignment(Pos.CENTER); // 設置內容置中
        
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
        photoPane.setAlignment(Pos.CENTER); // 設置面板中內容置中
        
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
        
        VBox dialogVBox = new VBox(15);
        dialogVBox.setPadding(new Insets(20));
        dialogVBox.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("🍽️ 新增餐廳分析分頁");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        Label instructionLabel = new Label("要新增其他餐廳的分頁，請回到搜尋首頁：");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-alignment: center;");
        
        VBox optionsBox = new VBox(10);
        optionsBox.setAlignment(Pos.CENTER);
        
        // 主要選項：回到搜尋首頁
        Button searchHomeButton = new Button("🏠 回到搜尋首頁");
        searchHomeButton.setPrefWidth(250);
        searchHomeButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 12 20; -fx-font-size: 14px;");
        searchHomeButton.setOnAction(e -> {
            dialog.close();
            // 回到搜尋首頁
            returnToSearchHomePage(primaryStage);
        });
        
        // 取消按鈕
        Button cancelButton = new Button("取消");
        cancelButton.setPrefWidth(250);
        cancelButton.setStyle("-fx-background-color: #999999; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 12 20; -fx-font-size: 14px;");
        cancelButton.setOnAction(e -> dialog.close());
        
        // 添加懸停效果
        searchHomeButton.setOnMouseEntered(e -> searchHomeButton.setStyle("-fx-background-color: #f08a6c; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 12 20; -fx-font-size: 14px;"));
        searchHomeButton.setOnMouseExited(e -> searchHomeButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 12 20; -fx-font-size: 14px;"));
        
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #777777; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 12 20; -fx-font-size: 14px;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: #999999; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 12 20; -fx-font-size: 14px;"));
        
        optionsBox.getChildren().addAll(searchHomeButton, cancelButton);
        
        Label helpLabel = new Label("💡 提示：\n• 現在只能透過搜尋首頁來新增餐廳分頁\n• 搜尋並選擇餐廳後，系統會自動創建新的分析分頁");
        helpLabel.setWrapText(true);
        helpLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic; -fx-text-alignment: center; -fx-font-size: 12px;");
        
        dialogVBox.getChildren().addAll(titleLabel, instructionLabel, optionsBox, helpLabel);
        
        Scene dialogScene = new Scene(dialogVBox, 350, 300);
        dialog.setScene(dialogScene);
        dialog.show();
    }
    
    /**
     * 回到搜尋首頁（取代原本的聚焦搜尋欄功能）
     */
    private void focusOnSearchBar() {
        // 由於已移除搜尋欄，改為回到搜尋首頁
        Platform.runLater(() -> {
            try {
                Stage currentStage = (Stage) mainLayout.getScene().getWindow();
                returnToSearchHomePage(currentStage);
            } catch (Exception e) {
                System.err.println("無法回到搜尋首頁: " + e.getMessage());
            }
        });
    }
    
    /**
     * 回到搜尋首頁（帶動畫效果）
     */
    private void returnToSearchHomePageWithAnimation(Stage primaryStage) {
        // 創建向左滑出的動畫效果
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), mainLayout);
        slideOut.setFromX(0);
        slideOut.setToX(-primaryStage.getWidth());
        slideOut.setInterpolator(Interpolator.EASE_IN);
        
        // 淡出效果
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), mainLayout);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        // 並行執行動畫
        ParallelTransition exitTransition = new ParallelTransition(slideOut, fadeOut);
        
        exitTransition.setOnFinished(e -> {
            // 動畫完成後切換到搜尋首頁
            showSearchHomePageWithFullScreen(primaryStage);
        });
        
        exitTransition.play();
    }
    
    /**
     * 顯示全螢幕搜尋首頁（帶滑入動畫）
     */
    private void showSearchHomePageWithFullScreen(Stage primaryStage) {
        SearchHomePage searchHomePage = new SearchHomePage(primaryStage, 
            (restaurantName, restaurantId, dataSource) -> {
                // 當用戶選擇餐廳後，初始化主分析界面
                initializeMainAnalysisInterface(primaryStage, restaurantName, restaurantId, dataSource);
            }
        );
        
        // 確保搜尋首頁也是全螢幕
        Platform.runLater(() -> {
            primaryStage.setMaximized(true);
            primaryStage.setAlwaysOnTop(false);
            primaryStage.setAlwaysOnTop(true);
            primaryStage.requestFocus();
        });
        
        searchHomePage.show();
    }
    
    /**
     * 回到搜尋首頁（原方法保留作為備用）
     */
    private void returnToSearchHomePage(Stage primaryStage) {
        // 顯示搜尋首頁
        showSearchHomePage(primaryStage);
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
        
        // 🚫 移除自動載入評論 - 讓用戶手動點擊時間按鈕來載入評論
        // System.out.println("創建新分頁時自動觸發近一個月評論");
        // updateRecentReviewsDisplay(30); // 30天
        
        // 🚫 移除自動載入評論 - 讓用戶手動點擊時間按鈕來載入評論
        // Platform.runLater(() -> {
        //     System.out.println("新分頁創建後手動更新近一個月評論");
        //     updateRecentReviewsDisplay(30); // 30天
        // });
        
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
                // 移除對 AnimationManager.pulse 的呼叫
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
            
            // 先載入新的資料
            loadAndDisplayRestaurantData(tab.jsonFilePath);
            
            // 恢復原本的捲動位置
            Platform.runLater(() -> leftScrollPane.setVvalue(scrollPosition));
        } else {
            // 直接載入內容，沒有之前的分頁
            loadAndDisplayRestaurantData(tab.jsonFilePath);
            // 移除對 AnimationManager.fadeIn 的呼叫
            leftScrollPane.setOpacity(1);
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
            // 檢查文件是否存在
            if (!Files.exists(Paths.get(jsonFilePath))) {
                System.out.println("📄 JSON 文件不存在: " + jsonFilePath + "，使用預設消費範圍");
                return estimateExpenseFromRestaurantType(jsonFilePath);
            }
            
            // 讀取JSON文件
            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            
            // 嘗試解析不同格式的JSON
            JSONArray reviews = null;
            try {
                // 格式1: 直接是評論陣列
                reviews = new JSONArray(content);
            } catch (JSONException e1) {
                try {
                    // 格式2: 包含 reviews 字段的對象
                    JSONObject jsonObject = new JSONObject(content);
                    reviews = jsonObject.getJSONArray("reviews");
                } catch (JSONException e2) {
                    System.out.println("⚠️ 無法解析 JSON 格式，使用預設消費範圍");
                    return estimateExpenseFromRestaurantType(jsonFilePath);
                }
            }
            
            // 用於存儲消費範圍的列表
            List<String> expenseRanges = new ArrayList<>();
            
            // 遍歷所有評論
            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                
                // 檢查不同可能的消費字段名稱
                String[] expenseFields = {"平均每人消費", "price_level", "expense", "cost", "平均消費"};
                for (String field : expenseFields) {
                    if (review.has(field) && !review.isNull(field)) {
                        String expense = review.getString(field);
                        if (expense != null && !expense.isEmpty()) {
                            expenseRanges.add(expense);
                            break; // 找到一個字段就跳出
                        }
                    }
                }
            }
            
            // 如果沒有數據，使用預設估算
            if (expenseRanges.isEmpty()) {
                System.out.println("💰 JSON 中沒有找到消費數據，使用餐廳類型估算");
                return estimateExpenseFromRestaurantType(jsonFilePath);
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
            
            System.out.println("💰 計算出消費中位數: " + readableRange + " (共 " + expenseRanges.size() + " 條消費記錄)");
            return readableRange;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ 計算消費中位數時發生錯誤，使用預設估算");
            return estimateExpenseFromRestaurantType(jsonFilePath);
        }
    }
    
    /**
     * 根據餐廳類型估算消費範圍
     */
    private String estimateExpenseFromRestaurantType(String restaurantInfo) {
        String info = restaurantInfo.toLowerCase();
        
        // 根據餐廳名稱或類型進行估算
        if (info.contains("coffee") || info.contains("咖啡")) {
            return "NT$100-300 (估算)";
        } else if (info.contains("燒臘") || info.contains("roast")) {
            return "NT$80-200 (估算)";
        } else if (info.contains("小食堂") || info.contains("eatery")) {
            return "NT$150-400 (估算)";
        } else if (info.contains("火鍋") || info.contains("hotpot")) {
            return "NT$300-600 (估算)";
        } else if (info.contains("餐廳") || info.contains("restaurant")) {
            return "NT$200-500 (估算)";
        } else if (info.contains("快餐") || info.contains("fast food")) {
            return "NT$50-150 (估算)";
        } else {
            return "NT$150-350 (估算)";
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
     * 啟動 Firestore 特色分析
     */
    private void startFirestoreFeatureAnalysis(String restaurantId, String restaurantName) {
        // 🔍 調試：檢查傳入的參數
        System.out.println("🔍 [DEBUG] startFirestoreFeatureAnalysis 被調用");
        System.out.println("🔍 [DEBUG] 傳入的 restaurantId: " + restaurantId);
        System.out.println("🔍 [DEBUG] 傳入的 restaurantName: " + restaurantName);
        System.out.println("🔍 [DEBUG] 當前執行緒: " + Thread.currentThread().getName());
        
        if (restaurantId == null || restaurantId.isEmpty()) {
            System.out.println("❌ [ERROR] 餐廳 ID 為空或null，無法進行分析");
            Platform.runLater(() -> {
                rightPanel.getFeaturesArea().setText("❌ 無法獲取餐廳ID，無法進行特色分析\n\n" +
                    "調試信息：\n" +
                    "• 餐廳名稱：" + (restaurantName != null ? restaurantName : "null") + "\n" +
                    "• 餐廳ID：" + (restaurantId != null ? restaurantId : "null") + "\n" +
                    "• 可能原因：搜尋結果沒有包含有效的餐廳ID");
            });
            return;
        }
        
        Platform.runLater(() -> {
            rightPanel.getFeaturesArea().setText("🔄 正在分析餐廳特色...\n\n從 Firestore 載入評論資料中，請稍候...\n\n" +
                "調試信息：\n" +
                "• 餐廳名稱：" + restaurantName + "\n" +
                "• 餐廳ID：" + restaurantId + "\n" +
                "• 狀態：準備開始分析");
        });
        
        new Thread(() -> {
            try {
                System.out.println("🚀 [INFO] 開始 Firestore 特色分析: " + restaurantName + " (ID: " + restaurantId + ")");
                
                Platform.runLater(() -> {
                    rightPanel.getFeaturesArea().setText("🤖 AI 正在分析評論內容...\n\n生成特色摘要中，請稍候...\n\n" +
                        "調試信息：\n" +
                        "• 餐廳名稱：" + restaurantName + "\n" +
                        "• 餐廳ID：" + restaurantId + "\n" +
                        "• 狀態：正在調用 FirestoreRestaurantAnalyzer");
                });
                
                // 🎯 使用現有的 FirestoreRestaurantAnalyzer.main() 方法
                // 創建臨時輸出檔案來接收分析結果
                String tempOutputFile = "temp_analysis_" + restaurantId + "_" + System.currentTimeMillis() + ".json";
                String[] args = {restaurantId, tempOutputFile};
                
                System.out.println("🔍 [DEBUG] 準備調用 FirestoreRestaurantAnalyzer.main()");
                System.out.println("🔍 [DEBUG] 參數: " + Arrays.toString(args));
                System.out.println("🔍 [DEBUG] 臨時輸出檔案: " + tempOutputFile);
                
                // 直接調用現有的 main 方法
                System.out.println("📞 [INFO] 正在調用 FirestoreRestaurantAnalyzer.main(args)...");
                bigproject.ai.FirestoreRestaurantAnalyzer.main(args);
                System.out.println("✅ [INFO] FirestoreRestaurantAnalyzer.main() 調用完成");
                
                // 讀取分析結果
                File resultFile = new File(tempOutputFile);
                System.out.println("🔍 [DEBUG] 檢查結果檔案是否存在: " + resultFile.exists());
                System.out.println("🔍 [DEBUG] 結果檔案路徑: " + resultFile.getAbsolutePath());
                
                String analysisResult;
                
                if (resultFile.exists()) {
                    try {
                        System.out.println("📖 [INFO] 讀取分析結果檔案...");
                        // 讀取 JSON 結果文件
                        String jsonContent = new String(java.nio.file.Files.readAllBytes(resultFile.toPath()));
                        System.out.println("🔍 [DEBUG] JSON 內容長度: " + jsonContent.length() + " 字元");
                        System.out.println("🔍 [DEBUG] JSON 內容前 200 字元: " + 
                            (jsonContent.length() > 200 ? jsonContent.substring(0, 200) + "..." : jsonContent));
                        
                        JSONObject result = new JSONObject(jsonContent);
                        System.out.println("🔍 [DEBUG] JSON 解析成功");
                        System.out.println("🔍 [DEBUG] JSON keys: " + result.keySet());
                        
                        String summary = result.optString("summary", "分析結果不可用");
                        System.out.println("🔍 [DEBUG] Summary 長度: " + summary.length() + " 字元");
                        System.out.println("🔍 [DEBUG] Summary 前 100 字元: " + 
                            (summary.length() > 100 ? summary.substring(0, 100) + "..." : summary));
                        
                        analysisResult = "🎯 AI 特色分析結果\n" +
                                       "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                       "📍 餐廳: " + restaurantName + "\n" +
                                       "📊 分析評論數: " + result.optInt("total_reviews", 0) + " 條\n" +
                                       "✅ 有效評論數: " + result.optInt("valid_comments", 0) + " 條\n" +
                                       "⏰ 分析時間: " + result.optString("analysis_time", "未知") + "\n" +
                                       "━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                                       summary;
                        
                        System.out.println("✅ [INFO] 成功解析分析結果");
                        System.out.println("🔍 [DEBUG] 最終結果長度: " + analysisResult.length() + " 字元");
                        
                        // 清理臨時文件
                        resultFile.delete();
                        System.out.println("🗑️ [INFO] 臨時檔案已清理");
                        
                    } catch (Exception e) {
                        System.err.println("❌ [ERROR] 讀取分析結果時發生錯誤: " + e.getMessage());
                        e.printStackTrace();
                        analysisResult = "讀取分析結果時發生錯誤：" + e.getMessage() + "\n\n" +
                            "可能原因：\n" +
                            "• JSON 格式錯誤\n" +
                            "• 檔案讀取權限問題\n" +
                            "• 分析結果不完整";
                        resultFile.delete(); // 確保清理
                    }
                } else {
                    // 如果沒有輸出文件，使用備用分析
                    System.out.println("⚠️ [WARN] 無法找到分析結果檔案，使用快速分析...");
                    System.out.println("🔍 [DEBUG] 檢查當前目錄檔案:");
                    File currentDir = new File(".");
                    String[] files = currentDir.list();
                    if (files != null) {
                        for (String file : files) {
                            if (file.contains("temp_analysis")) {
                                System.out.println("  - " + file);
                            }
                        }
                    }
                    
                    analysisResult = "⚠️ 無法獲取詳細的 AI 分析結果\n\n" +
                        "📋 快速分析：\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "📍 餐廳名稱：" + restaurantName + "\n" +
                        "🆔 餐廳ID：" + restaurantId + "\n" +
                        "📄 臨時檔案：" + tempOutputFile + "\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        generateQuickAnalysis(restaurantName);
                }
                
                final String finalResult = analysisResult;
                Platform.runLater(() -> {
                    rightPanel.getFeaturesArea().setText(finalResult);
                    System.out.println("✅ [INFO] 特色分析完成並顯示: " + restaurantName);
                    
                    // 🔄 如果 AI 聊天正在活躍狀態，更新其初始內容
                    if (aiChat != null && aiChat.isActive()) {
                        System.out.println("🤖 檢測到活躍的 AI 聊天，更新初始內容");
                        aiChat.updateInitialContent(finalResult);
                    }
                });
                
            } catch (Exception e) {
                System.err.println("❌ [ERROR] Firestore 分析過程中發生錯誤: " + e.getMessage());
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    // 如果 Firestore 分析失敗，使用備用分析
                    System.out.println("⚠️ [WARN] Firestore 分析失敗，使用本地快速分析: " + e.getMessage());
                    String backupAnalysis = "❌ Firestore 分析失敗\n\n" +
                        "錯誤詳情：" + e.getMessage() + "\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "📋 使用備用分析：\n\n" +
                        generateQuickAnalysis(restaurantName);
                    rightPanel.getFeaturesArea().setText(backupAnalysis);
                });
            }
        }).start();
    }
    
    /**
     * 生成快速分析（備用方案）
     */
    private String generateQuickAnalysis(String restaurantName) {
        return "📊 " + restaurantName + " 特色分析\n" +
               "━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
               "🍽️ 餐廳特色：\n" +
               "根據顧客評論分析，這家餐廳以其獨特的料理風格和優質服務著稱。" +
               "多數顧客對餐點品質給予正面評價，特別是招牌菜品受到廣泛好評。\n\n" +
               "🏪 用餐環境：\n" +
               "餐廳營造出舒適的用餐氛圍，裝潢設計用心，為顧客提供愉快的用餐體驗。" +
               "整體環境乾淨整潔，適合各種場合的聚餐需求。\n\n" +
               "💡 經營建議：\n" +
               "建議持續保持現有的服務品質，並可考慮定期更新菜單，" +
               "增加季節性特色菜品以吸引更多回頭客。\n\n" +
               "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
               "💬 點擊此區域可與 AI 深入討論餐廳特色";
    }

    /**
     * 更新近期評論顯示
     */
    private void updateRecentReviewsDisplay(int days) {
        System.out.println("正在更新近期評論顯示，顯示近 " + days + " 天的評論...");
        
        // 直接使用 RightPanel 的方法，它會調用 LatestReviewsManager 獲取真實數據
        rightPanel.updateRecentReviewsDisplay(days);
    }
    
    /**
     * 🗑️ 已完全移除範例數據方法 - 不再使用預設評論
     * 此方法已被 LatestReviewsManager 完全替代
     * 所有評論數據現在都來自真實的 API 或 JSON 檔案
     */
    private void updateRecentReviewsWithSampleData(VBox recentReviewsBox, int days) {
        System.out.println("⚠️ 警告：updateRecentReviewsWithSampleData 方法已被完全棄用");
        System.out.println("📝 所有評論數據現在都由 LatestReviewsManager 提供真實數據");
        
        // 不再提供範例數據，直接顯示提示訊息
        recentReviewsBox.getChildren().clear();
        Label deprecatedLabel = createInfoLabel("此方法已停用\n\n所有評論數據現在都來自：\n• Google Maps API\n• 本地 JSON 檔案\n• LatestReviewsManager\n\n不再使用範例數據");
        recentReviewsBox.getChildren().add(deprecatedLabel);
    }

    /**
     * 創建評論卡片UI
     */
    public VBox createReviewCard(String date, String username, double rating, String content) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: #F8F8F8; -fx-padding: 10; -fx-background-radius: 5;");
        
        // 頂部資訊（用戶名、日期和評分）
        HBox topInfo = new HBox(10);
        topInfo.setAlignment(Pos.CENTER_LEFT);
        
        Label userLabel = new Label(username);
        userLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        userLabel.setStyle("-fx-text-fill: #333333;");
        
                Label dateLabel = new Label(date);
        dateLabel.setStyle("-fx-text-fill: #777777; -fx-font-size: 11px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 評分顯示
        HBox ratingBox = new HBox(2);
        Label ratingLabel = new Label(String.format("%.1f", rating));
        ratingLabel.setStyle("-fx-text-fill: " + RICH_MIDTONE_RED + "; -fx-font-weight: bold;");
        
        // 星星圖標（這裡用文字代替）
        Label starLabel = new Label("★");
        starLabel.setStyle("-fx-text-fill: " + RICH_MIDTONE_RED + "; -fx-font-size: 12px;");
        
        ratingBox.getChildren().addAll(ratingLabel, starLabel);
        
        topInfo.getChildren().addAll(userLabel, dateLabel, spacer, ratingBox);
        
        // 評論內容
        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: #333333;");
        
        card.getChildren().addAll(topInfo, contentLabel);
        return card;
    }

    /**
     * 載入餐廳資料
     */
    public void loadRestaurantData(String jsonFilePath) {
        loadAndDisplayRestaurantData(jsonFilePath);
    }

    /**
     * 創建載入提示標籤
     */
    public Label createLoadingLabel(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #777777; -fx-font-style: italic;");
        return label;
    }
    
    /**
     * 創建信息標籤
     */
    public Label createInfoLabel(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #555555; -fx-font-style: italic; -fx-padding: 5;");
        return label;
    }
    
    /**
     * 創建錯誤標籤
     */
    public Label createErrorLabel(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #E03C31; -fx-font-style: italic; -fx-padding: 5;");
        return label;
    }

    /**
     * 顯示 AI 初始化對話框
     */
    private void showAIInitializationDialog(Stage primaryStage) {
        // 檢查是否已經有對話框在顯示
        AIProgressDialog dialog = AIProgressDialog.show(primaryStage, "AI 功能初始化");
        
        // 開始 AI 初始化
        dialog.startAIInitialization(new AIProgressDialog.ProgressCallback() {
            @Override
            public void onProgress(double progress, String status, String detail) {
                // 進度更新會自動在對話框中顯示
            }
            
            @Override
            public void onComplete(boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        // 顯示成功訊息
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("AI 初始化完成");
                        alert.setHeaderText("AI 功能已準備就緒！");
                        alert.setContentText("現在您可以使用所有 AI 功能，包括餐廳評論分析和智能建議。");
                        alert.showAndWait();
                    } else {
                        // 顯示失敗訊息
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("AI 初始化失敗");
                        alert.setHeaderText("AI 功能初始化未完成");
                        alert.setContentText("部分 AI 功能可能無法使用。您可以稍後再次嘗試初始化。");
                        alert.showAndWait();
                    }
                    dialog.close();
                });
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("AI 初始化錯誤");
                    alert.setHeaderText("初始化過程中發生錯誤");
                    alert.setContentText("錯誤詳情：" + error);
                    alert.showAndWait();
                    dialog.close();
                });
            }
        });
    }

    // startAutoAIInitialization 方法已移除，AI 初始化現在在 AppLauncher 中進行
}


