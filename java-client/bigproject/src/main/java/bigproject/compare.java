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
public class compare extends Application implements UIManager.StateChangeListener {

    private VBox competitorListVBox;
    private BorderPane mainLayout;
    private Scene mainScene;
    private HBox mainContentBox; // 將mainContentBox升級為類成員變數
    private VBox mainContainer; // 將mainContainer也升級為類成員變數
    private ScrollPane leftScrollPane; // 將leftScrollPane也升級為類成員變數
    private VBox rightPanel; // 將rightPanel也升級為類成員變數

    private Preferences prefs = Preferences.userNodeForPackage(compare.class);

    // API Key is used by GooglePlacesService instance
    private static final String API_KEY = System.getenv("GOOGLE_MAPS_API_KEY");

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
    
    // 按鈕引用
    private Button suggestionButton;
    private Button reportButton;

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

        primaryStage.setTitle("餐廳分析");
        
        // 設置窗口總是在最前面顯示
        primaryStage.setAlwaysOnTop(true);
        
        // 添加監聽器確保窗口始終保持在最上層
        primaryStage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // 如果窗口失去焦點
                primaryStage.setAlwaysOnTop(false); // 暫時取消最上層
                primaryStage.setAlwaysOnTop(true); // 再次設為最上層
            }
        });
        
        // 使用ResourceManager設置應用程式圖標
        ResourceManager.setAppIcon(primaryStage);
        
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(15));
        mainLayout.setStyle("-fx-background-color: #2C2C2C;"); // 使用深色背景統一風格
        mainLayout.setPrefHeight(Double.MAX_VALUE); // 確保主佈局填滿整個高度
        mainLayout.setPrefWidth(Double.MAX_VALUE); // 確保主佈局填滿整個寬度
        
        // 設置主佈局初始不可見，用於後續動畫
        mainLayout.setOpacity(0);
        
        // 創建分頁欄
        tabBar = new HBox(5);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(5, 10, 5, 10));
        tabBar.setStyle("-fx-background-color: #1E1E1E; -fx-border-width: 0 0 1 0; -fx-border-color: #444444;");
        tabBar.setMinHeight(40); // 設置最小高度確保可見
        tabBar.setPrefHeight(40); // 設置首選高度
        tabBar.setMaxWidth(Double.MAX_VALUE); // 確保橫向填滿
        
        // 創建新增分頁按鈕
        Button addTabButton = new Button("+");
        addTabButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCCCCC; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 5 0 5;");
        addTabButton.setOnAction(e -> showAddTabDialog(primaryStage));
        
        // 添加懸停效果
        addTabButton.setOnMouseEntered(e -> addTabButton.setStyle("-fx-background-color: #444444; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 5 0 5;"));
        addTabButton.setOnMouseExited(e -> addTabButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCCCCC; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 5 0 5;"));
        
        tabBar.getChildren().add(addTabButton);

        // Initialize Services first
        googlePlacesService = new GooglePlacesService(API_KEY);
        dataManager = new DataManager();

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
        Button settingsButton = new Button("⚙️");
        
        // 設置具體的樣式而不是使用CSS類
        normalButtonStyle = "-fx-text-fill: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 15 8 15;";
        activeButtonStyle = "-fx-background-color: #8B4513; " + normalButtonStyle + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 4, 0, 0, 1);";
        hoverButtonStyle = "-fx-background-color: #f08a6c; " + normalButtonStyle;
        normalButtonStyle = "-fx-background-color: #E67649; " + normalButtonStyle;
        
        suggestionButton.setStyle(normalButtonStyle);
        reportButton.setStyle(normalButtonStyle);
        
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
        mainContentBox = new HBox(15); // Spacing between left and right
        mainContentBox.setPadding(new Insets(10, 0, 0, 0)); // 移除底部邊距，讓內容延伸到底
        mainContentBox.setPrefHeight(Double.MAX_VALUE); // 確保內容區域填滿整個高度
        mainContentBox.setMinHeight(600); // 設置最小高度，避免內容區域過小
        mainContentBox.setStyle("-fx-background-color: transparent;"); // 透明背景讓子元素背景顯示

        // --- Left Panel (Reviews, Details) ---
        VBox leftPanel = new VBox(20);
        leftPanel.setPadding(new Insets(20, 20, 50, 20)); // 增加底部邊距
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setPrefHeight(Double.MAX_VALUE); // 確保預設高度撐滿
        leftPanel.setStyle("-fx-background-color: #F7E8DD;"); // 使用統一的膚色背景
        leftPanel.getStyleClass().add("content-panel");

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
        
        // 啟用滑鼠滾動和拖動
        photosScroll.setPannable(true);
        photosScroll.setFitToWidth(true);
        
        // 設置更合適的樣式，確保滾動條可見並增加邊距，使外觀更好
        photosScroll.setStyle("-fx-background-color: #222222; " +
                              "-fx-border-color: transparent; " +
                              "-fx-padding: 0; " +
                              "-fx-background-insets: 0; " +
                              "-fx-border-width: 0;");
        
        // 強制啟用滾動功能，無論內容大小
        photosScroll.setVvalue(0);
        
        // 添加拖拽滾動功能
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
            double deltaY = event.getDeltaY() * 0.25; // 減少滾動速度
            photosScroll.setVvalue(photosScroll.getVvalue() - deltaY / photosContainer.getHeight());
            event.consume(); // 防止事件傳播
        });

        // --- Right Panel (Ratings, Data Sources) ---
        rightPanel = new VBox(15); // Increased spacing
        rightPanel.setPadding(new Insets(15, 15, 50, 15)); // 增加底部邊距
        rightPanel.setMinWidth(250); // Min width for right panel
        rightPanel.setMaxWidth(400); // Optional: Max width
        rightPanel.setPrefHeight(Double.MAX_VALUE); // 確保填滿整個高度
        rightPanel.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + ";"); // 使用統一的淺綠色背景
        rightPanel.getStyleClass().add("right-panel");

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
        
        // 特色、優點和缺點分析區塊 - 移到右側面板
        Label featuresLabel = new Label("特色");
        featuresLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        featuresLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        Label prosLabel = new Label("優點");
        prosLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        prosLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        Label consLabel = new Label("缺點");
        consLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        consLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        // 先添加評分和資料來源部分
        rightPanel.getChildren().addAll(ratingsHeader, ratingsBox, sourcesLabel, competitorListVBox);

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
        
        // 確保滾動面板正確處理內容的高度變化
        leftPanel.heightProperty().addListener((obs, oldVal, newVal) -> {
            leftScrollPane.layout();
        });
        
        // 添加面板到主內容區域
        mainContentBox.getChildren().addAll(leftScrollPane, rightPanel);
        HBox.setHgrow(leftScrollPane, Priority.ALWAYS); // Let left panel grow
        
        // 將主內容區域加入主容器
        mainContainer.getChildren().add(mainContentBox);
        VBox.setVgrow(mainContentBox, Priority.ALWAYS); // 讓內容區域自動擴展
        
        // --- Setup Scene and UIManager --- 
        mainLayout.setCenter(mainContainer); // 使用包含搜索欄的容器作為主要內容
        mainLayout.setTop(topBar); // 設置頂部欄
        
        // 明確設置底部的分頁欄，並確保它有足夠的空間
        mainLayout.setBottom(tabBar);
        BorderPane.setMargin(tabBar, new Insets(10, 0, 0, 0)); // 為分頁欄增加頂部邊距
        
        mainLayout.setStyle("-fx-background-color: #2C2C2C; -fx-min-height: 100%;"); // 確保填滿整個空間
        
        // 初始化場景，並設置最小尺寸防止過小導致UI變形
        mainScene = new Scene(mainLayout, 1024, 768);
        mainScene.getRoot().setStyle("-fx-min-height: 100%;");

        // --- Initialize UIManager NOW --- 
        uiManager = new UIManager(prefs, primaryStage, mainScene, mainLayout, mainContentBox); // Pass main HBox instead
        
        // Set this class as the state change listener
        uiManager.setStateChangeListener(this);

        // --- Update font style using UIManager ---
        uiManager.updateFontStyle(currentFontFamily, currentFontSize);

        // --- Set Actions requiring uiManager ---
        settingsButton.setOnAction(e -> uiManager.showSettingsDialog());
        
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
            area.setMinHeight(100);
            area.setPrefHeight(120);
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

        // 添加評論區和照片區到左側面板
        leftPanel.getChildren().addAll(reviewsLabel, reviewsArea, photosLabel, photosScroll);
        
        // --- Add Left and Right Panels to HBox --- 
        // 這段代碼現在被移到了上面，leftScrollPane在上面已完成初始化
        
        // 建立響應式設計的內容調整器
        // 強制使用小視窗模式 - 即只顯示青蘋果綠欄位，隱藏膚色欄位
        // 建立響應式設計的內容調整器
        // 強制使用小視窗模式 - 即只顯示青蘋果綠欄位，隱藏膚色欄位
        setupInitialResponsiveLayout(mainContentBox, leftScrollPane, rightPanel, true);
        setupResponsiveLayout(primaryStage, mainContentBox, leftScrollPane, rightPanel, searchButton);
        
        // --- Apply Theme and Show Stage ---
        uiManager.updateTheme(true); // 強制使用深色模式
        
        // 確保樣式更新被應用
        String cssUrl = getClass().getResource("/bigproject/modern_dark_theme.css").toExternalForm();
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(cssUrl);
        
        primaryStage.setScene(mainScene);
        
        // 確保在應用程式完全啟動後設置正確的佈局模式
        Platform.runLater(() -> {
            // 根據視窗大小決定佈局模式
            if (primaryStage.getWidth() >= 800) {
                // 大視窗模式：水平佈局（膚色欄在左，青蘋果欄在右）
                switchToHorizontalLayout(mainContentBox, leftScrollPane, rightPanel);
                isHorizontalLayout = true;
            } else {
                // 小視窗模式：垂直佈局（青蘋果欄在上）
                switchToVerticalLayout(mainContentBox, leftScrollPane, rightPanel);
                isHorizontalLayout = false;
            }
            
            // 同時設置搜尋按鈕的顯示模式
            if (primaryStage.getWidth() < 800) {
                searchButton.setText("+");
                searchButton.setPrefWidth(40);
            } else {
                searchButton.setText("搜尋");
                searchButton.setPrefWidth(75);
            }
            
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
            mainLayout.requestLayout();
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
        
        // 為主舞台添加淡入和彈跳效果
        addFadeInWithBounceEffect(mainLayout, primaryStage);
        
        // 在窗口顯示後再次確保它在最上層和最大化狀態
        Platform.runLater(() -> {
            primaryStage.setAlwaysOnTop(false);
            primaryStage.setAlwaysOnTop(true);
            primaryStage.requestFocus(); // 請求焦點
            primaryStage.setMaximized(true); // 再次確保最大化
            
            // 確保分頁欄顯示在全螢幕模式
            ensureTabBarVisible();
            tabBar.toFront(); // 確保分頁欄在最前層
            System.out.println("全螢幕模式下強制顯示分頁欄");
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
    }

     private void clearRestaurantDataDisplay(String message) {
         dataManager.clearRestaurantDataDisplay(message, ratingsHeader, ratingsBox, ratingBars, reviewsArea, photosContainer, featuresArea, prosArea, consArea);
    }

    /**
     * 添加淡入效果和彈跳動畫到主窗口
     */
    private void addFadeInWithBounceEffect(Node node, Stage stage) {
        // 創建淡入效果 - 延長持續時間，使其更慢
        FadeTransition fadeIn = new FadeTransition(Duration.millis(2000), node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_IN); // 使用緩入插值器讓透明度變化更自然
        
        // 創建彈跳效果 - 窗口從下方彈入，也放慢速度
        TranslateTransition bounce = new TranslateTransition(Duration.millis(1800), node);
        bounce.setFromY(50);  // 從下方50像素處開始
        bounce.setToY(0);     // 移動到原始位置
        bounce.setInterpolator(Interpolator.SPLINE(0.1, 0.8, 0.2, 1.0));  // 彈道效果
        
        // 組合兩個動畫一起播放
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(fadeIn, bounce);
        
        // 播放動畫
        parallelTransition.play();
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
    }
    
    @Override
    public void onSuggestionsStateChanged(boolean isShowing) {
        isSuggestionActive[0] = isShowing;
        suggestionButton.setStyle(isShowing ? activeButtonStyle : normalButtonStyle);
        
        // 如果顯示建議，確保AI聊天視圖被隱藏
        if (isShowing && isAIChatActive[0]) {
            hideAIChatView();
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

    /**
     * 在應用程式啟動時設置初始響應式佈局
     * @param useSmallWindowMode 是否使用小視窗模式（青蘋果綠欄在上，膚色欄在下）
     */
    private void setupInitialResponsiveLayout(HBox mainContentBox, ScrollPane leftScrollPane, 
                                             VBox rightPanel, boolean useSmallWindowMode) {
        // 直接使用傳入的參數決定初始佈局
        if (useSmallWindowMode) {
            // 使用垂直布局：只顯示青蘋果欄(右面板)，隱藏膚色欄(左面板)
            switchToVerticalLayout(mainContentBox, leftScrollPane, rightPanel);
            isHorizontalLayout = false;
        } else {
            // 使用水平布局：膚色欄(左面板)在左，青蘋果欄(右面板)在右
            switchToHorizontalLayout(mainContentBox, leftScrollPane, rightPanel);
            isHorizontalLayout = true;
        }
    }

    /**
     * 建立響應式設計的版面配置
     * 針對不同螢幕寬度調整元素位置和大小
     */
    private void setupResponsiveLayout(Stage primaryStage, HBox mainContentBox, 
                                      ScrollPane leftScrollPane, VBox rightPanel,
                                      Button searchButton) {
        // 設置最小寬度以確保UI不會被壓縮
        mainContentBox.setMinWidth(400);
        rightPanel.setMinWidth(250);
        leftScrollPane.setMinWidth(300);
        
        // 保存最後一次檢測到的寬度，用於減少不必要的布局切換
        final double[] lastWidth = {primaryStage.getWidth()};
        final double[] lastHeight = {primaryStage.getHeight()};
        
        // 更改視窗大小監聽器，使其能夠即時更新面板大小
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue();
            double oldWidth = oldVal.doubleValue();
            
            // 即時更新面板大小，不需等待顯著變化
            updatePanelSizes(mainContentBox, leftScrollPane, rightPanel, width, primaryStage.getHeight());
            
            // 搜尋按鈕大小調整
            if (width < 800) {
                searchButton.setText("+");
                searchButton.setPrefWidth(40);
            } else {
                searchButton.setText("搜尋");
                searchButton.setPrefWidth(75);
            }
            
            // 檢測顯著的寬度變化或跨越閾值
            boolean significantChange = Math.abs(width - lastWidth[0]) > LAYOUT_CHANGE_THRESHOLD;
            boolean crossedThreshold = (oldWidth < 800 && width >= 800) || (oldWidth >= 800 && width < 800);
            
            // 只有在顯著變化時才調整布局模式
            if (significantChange || crossedThreshold) {
                lastWidth[0] = width; // 更新最後檢測到的寬度
                
                // 在主UI線程上執行佈局變更
                Platform.runLater(() -> {
                    if (width < 800 && isHorizontalLayout) {
                        // 切換到垂直布局：只顯示青蘋果欄(右面板)，隱藏膚色欄(左面板)
                        switchToVerticalLayout(mainContentBox, leftScrollPane, rightPanel);
                        isHorizontalLayout = false;
                    } else if (width >= 800 && !isHorizontalLayout) {
                        // 切換到水平布局：膚色欄(左面板)在左，青蘋果欄(右面板)在右
                        switchToHorizontalLayout(mainContentBox, leftScrollPane, rightPanel);
                        isHorizontalLayout = true;
                    }
                });
            }
            
            // 無論是否有顯著變化，都立即調整分頁欄
            ensureTabBarVisible();
        });
        
        // 增加對高度變化的監聽，立即更新面板大小和分頁欄
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            double height = newVal.doubleValue();
            double oldHeight = oldVal.doubleValue();
            
            // 每次高度變化都更新面板大小
            updatePanelSizes(mainContentBox, leftScrollPane, rightPanel, primaryStage.getWidth(), height);
            
            // 無論變化大小，都確保分頁欄可見
            ensureTabBarVisible();
            
            System.out.println("視窗高度變化: " + oldHeight + " -> " + height + " - 強制檢查分頁欄可見性");
        });
        
        // 確保在初始設置時也調用一次，設置適當的分頁欄大小
        Platform.runLater(() -> {
            ensureTabBarVisible();
        });
    }
    
    /**
     * 調整面板高度以適應視窗高度變化
     */
    private void adjustPanelHeights(HBox mainContentBox, ScrollPane leftScrollPane, VBox rightPanel, double windowHeight) {
        // 計算可用高度（減去頂部工具欄和搜索欄的高度，以及分頁欄的高度）
        double topBarHeight = 60; // 頂部工具欄高度
        double tabBarHeight = 50; // 分頁欄高度
        double availableHeight = windowHeight - topBarHeight - tabBarHeight - 30; // 額外減去30像素作為邊距
        
        // 確保可用高度不小於最小值
        availableHeight = Math.max(availableHeight, 500);
        
        // 調整主內容區域高度
        mainContentBox.setPrefHeight(availableHeight);
        mainContentBox.setMinHeight(availableHeight);
        
        // 根據佈局模式調整面板高度
        if (isHorizontalLayout) {
            // 水平佈局：左右兩側面板
            leftScrollPane.setPrefHeight(availableHeight);
            leftScrollPane.setMinHeight(availableHeight);
            rightPanel.setPrefHeight(availableHeight);
        } else {
            // 垂直佈局：只有右側面板
            if (!mainContentBox.getChildren().isEmpty()) {
                ScrollPane rightScrollPane = (ScrollPane) mainContentBox.getChildren().get(0);
                rightScrollPane.setPrefHeight(availableHeight);
                rightScrollPane.setMinHeight(availableHeight);
            }
        }
        
        // 強制佈局重新計算
        mainContentBox.requestLayout();
        System.out.println("調整面板高度完成 - 可用高度: " + availableHeight + "px");
    }
    
    /**
     * 更新面板大小，確保它們隨視窗大小變化
     */
    private void updatePanelSizes(HBox mainContentBox, ScrollPane leftScrollPane, VBox rightPanel, double width, double height) {
        // 考慮分頁欄高度，確保留出足夠空間
        double tabBarHeight = 50; // 分頁欄的估計高度，略微增加以確保足夠空間
        double topBarHeight = 60; // 頂部工具欄和搜索欄的估計高度
        double availableHeight = height - tabBarHeight - topBarHeight - 20; // 減去20像素作為額外邊距，確保分頁欄有足夠空間
        
        // 確保可用高度不小於最小值
        availableHeight = Math.max(availableHeight, 500);
        
        // 設置主內容區域高度
        mainContentBox.setPrefHeight(availableHeight); // 減去頂部工具欄、搜索欄和分頁欄的高度
        mainContentBox.setMinHeight(availableHeight); // 確保最小高度
        
        if (isHorizontalLayout) {
            // 大視窗模式 - 水平佈局
            leftScrollPane.setPrefHeight(availableHeight);
            leftScrollPane.setMinHeight(availableHeight);
            
            // 設置左側面板寬度比例 (約70%)
            double leftWidth = width * 0.7;
            leftScrollPane.setPrefWidth(leftWidth);
            
            // 右側面板寬度 (約30%)，但不小於300px且不大於350px
            double rightWidth = Math.min(350, Math.max(300, width * 0.3 - 20));
            rightPanel.setPrefWidth(rightWidth);
            
            // 強制佈局更新
            Platform.runLater(() -> {
                mainContentBox.requestLayout();
                leftScrollPane.requestLayout();
                rightPanel.requestLayout();
                
                // 確保分頁欄可見
                ensureTabBarVisible();
            });
        } else {
            // 小視窗模式 - 垂直佈局
            // 確保右側面板填滿可用寬度
            if (!mainContentBox.getChildren().isEmpty()) {
                // 是否為ScrollPane (在垂直佈局中應該是)
                if (mainContentBox.getChildren().get(0) instanceof ScrollPane) {
                    ScrollPane rightScrollPane = (ScrollPane) mainContentBox.getChildren().get(0);
                    
                    // 調整垂直佈局中青蘋果欄的大小
                    rightScrollPane.setPrefWidth(width - 20); // 留一點邊距，更好地填滿空間
                    rightScrollPane.setMinWidth(Math.min(width - 20, 300)); // 確保即使視窗縮小也保持合理寬度
                    rightScrollPane.setPrefHeight(availableHeight);
                    rightScrollPane.setMinHeight(availableHeight);
                    
                    // 讓右面板能夠擴展填滿ScrollPane
                    rightPanel.setPrefWidth(width - 30); // 比ScrollPane稍窄
                    rightPanel.setMaxWidth(width - 30); // 限制最大寬度，確保跟隨視窗變化
                    
                    // 確保背景色擴展到整個可視區域
                    Platform.runLater(() -> {
                        // 更新ScrollPane樣式
                        rightScrollPane.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + "; -fx-background: " + RICH_LIGHT_GREEN + ";");
                        
                        // 更新Viewport樣式
                        Node viewport = rightScrollPane.lookup(".viewport");
                        if (viewport != null) {
                            viewport.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + ";");
                        }
                        
                        // 更新內容面板樣式
                        rightPanel.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + ";");
                        
                        // 強制佈局更新
                        mainContentBox.requestLayout();
                        rightScrollPane.requestLayout();
                        rightPanel.requestLayout();
                        
                        // 確保分頁欄可見
                        ensureTabBarVisible();
                    });
                }
            }
        }
        
        // 記錄調整
        System.out.println("更新面板大小 - 視窗尺寸: " + width + "x" + height + ", 可用高度: " + availableHeight);
    }
    
    /**
     * 切換到垂直布局：只顯示青蘋果欄(右面板)，隱藏膚色欄(左面板)
     */
    private void switchToVerticalLayout(HBox mainContentBox, ScrollPane leftScrollPane, VBox rightPanel) {
        // 清除現有子元素
        mainContentBox.getChildren().clear();
        
        // 設置布局屬性
        mainContentBox.setAlignment(Pos.TOP_CENTER);
        mainContentBox.setSpacing(10);
        mainContentBox.setPrefHeight(Double.MAX_VALUE);
        mainContentBox.setMinHeight(600);
        
        // 重設右面板屬性以擴展填滿可用空間
        rightPanel.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + ";");
        rightPanel.setPrefWidth(Double.MAX_VALUE); // 允許填滿可用空間
        rightPanel.setMaxWidth(Double.MAX_VALUE);
        rightPanel.setMinWidth(300); // 保留最小寬度限制，避免過窄
        rightPanel.setPrefHeight(Double.MAX_VALUE); // 讓它填滿可用高度
        
        // 確保右面板有足夠的底部邊距
        rightPanel.setPadding(new Insets(15, 15, 70, 15)); // 增加底部邊距，留出分頁欄的空間
        
        // 創建一個新的ScrollPane包裹右面板，以便於垂直滾動
        ScrollPane rightScrollPane = new ScrollPane(rightPanel);
        rightScrollPane.setFitToWidth(true);
        rightScrollPane.setFitToHeight(false); // 關鍵：設為false，讓內容可以超出視窗高度並顯示滾動條
        rightScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        rightScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); 
        rightScrollPane.setPannable(true);
        rightScrollPane.setPrefViewportHeight(Double.MAX_VALUE); // 使視口高度自適應
        rightScrollPane.setVvalue(0); // 重置滾動位置到頂部
        
        // 設置右側滾動面板的背景顏色
        rightScrollPane.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + "; -fx-background: " + RICH_LIGHT_GREEN + ";");
        
        // 添加拖拽滾動功能
        final double[] dragStartY = {0};
        final double[] initialVvalue = {0};
        
        rightScrollPane.setOnMousePressed(event -> {
            dragStartY[0] = event.getY();
            initialVvalue[0] = rightScrollPane.getVvalue();
            event.consume();
        });
        
        rightScrollPane.setOnMouseDragged(event -> {
            double deltaY = dragStartY[0] - event.getY();
            double contentHeight = rightPanel.getHeight();
            double viewportHeight = rightScrollPane.getHeight();
            
            // 調整滾動量，使滾動更流暢
            if (contentHeight > viewportHeight) {
                double scrollableRange = contentHeight - viewportHeight;
                double newVvalue = initialVvalue[0] + (deltaY / scrollableRange);
                rightScrollPane.setVvalue(Math.min(Math.max(newVvalue, 0), 1.0));
            }
            event.consume();
        });
        
        // 優化滾輪事件處理，以便更精確控制滾動速度
        rightScrollPane.setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            
            // 根據內容高度調整滾動量
            double contentHeight = rightPanel.getHeight();
            double viewportHeight = rightScrollPane.getHeight();
            
            if (contentHeight > viewportHeight) {
                double scrollFactor = 0.005; // 較小的因子讓滾動更平滑
                double newVvalue = rightScrollPane.getVvalue() - (deltaY * scrollFactor);
                rightScrollPane.setVvalue(Math.min(Math.max(newVvalue, 0), 1.0));
                event.consume();
            }
        });
                                
        // 確保子控件能夠垂直擴展
        VBox.setVgrow(rightPanel, Priority.ALWAYS);
        
        // 使用Platform.runLater確保viewport節點可用後再設置其背景
        Platform.runLater(() -> {
            Node viewport = rightScrollPane.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + ";");
            }
            
            // 確保滾動條區域和內容區域背景也正確設置
            rightScrollPane.lookup(".corner").setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + ";");
            
            // 確保父容器設置正確
            VBox mainContainer = (VBox) mainContentBox.getParent();
            if (mainContainer != null) {
                mainContainer.setPrefHeight(Double.MAX_VALUE);
                VBox.setVgrow(mainContentBox, Priority.ALWAYS);
            }
            
            // 強制重新計算內容佈局
            mainContentBox.requestLayout();
            rightScrollPane.requestLayout();
            rightPanel.requestLayout();
            
            // 確保分頁欄在底部顯示，使用明亮的背景色使其更加明顯
            if (mainLayout != null && tabBar != null) {
                mainLayout.setBottom(tabBar);
                tabBar.setStyle("-fx-background-color: #2A2A2A; -fx-border-width: 1 0 0 0; -fx-border-color: #666666; -fx-padding: 5 10 5 10;");
                tabBar.setVisible(true);
                tabBar.setManaged(true);
                tabBar.toFront(); // 確保分頁欄在最前面
                System.out.println("垂直佈局模式下設置分頁欄");
            }
        });
        
        // 設置滾動面板填滿可用空間
        HBox.setHgrow(rightScrollPane, Priority.ALWAYS);
        rightScrollPane.setMaxWidth(Double.MAX_VALUE);
        rightScrollPane.setMinWidth(300); // 確保最小寬度
        rightScrollPane.setPrefWidth(Double.MAX_VALUE); // 填滿可用寬度
        rightScrollPane.setMaxHeight(Double.MAX_VALUE);
        rightScrollPane.setMinHeight(600); // 確保最小高度
        rightScrollPane.setPrefHeight(Double.MAX_VALUE); // 填滿可用高度
        
        // 添加到主內容區域
        mainContentBox.getChildren().add(rightScrollPane);
        
        // 更新布局標誌
        isHorizontalLayout = false;
    }
    
    /**
     * 切換到水平布局：膚色欄(左面板)在左，青蘋果欄(右面板)在右
     */
    private void switchToHorizontalLayout(HBox mainContentBox, ScrollPane leftScrollPane, VBox rightPanel) {
        // 清除現有子元素
        mainContentBox.getChildren().clear();
        
        // 改變 HBox 布局屬性以水平排列
        mainContentBox.setAlignment(Pos.CENTER_LEFT);
        mainContentBox.setSpacing(15);
        mainContentBox.setMinHeight(600); // 確保內容區域有足夠高度
        mainContentBox.setPrefHeight(Double.MAX_VALUE); // 填滿可用高度
        
        // 重設左側面板屬性
        leftScrollPane.setMinHeight(600);
        leftScrollPane.setPrefHeight(Double.MAX_VALUE);
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setFitToHeight(false); // 改為false，允許內容超出視窗高度
        leftScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); 
        leftScrollPane.setPannable(true);
        leftScrollPane.setVvalue(0); // 重置滾動位置到頂部
        
        // 獲取左面板並確保底部有足夠空間
        VBox leftPanel = (VBox) leftScrollPane.getContent();
        if (leftPanel != null) {
            leftPanel.setPadding(new Insets(20, 20, 70, 20)); // 增加底部邊距
        }
        
        // 確保滾動條可見並且可以操作
        leftScrollPane.setStyle("-fx-background-color: #F7E8DD; -fx-border-color: transparent; " +
                               "-fx-padding: 0; -fx-background-insets: 0;");
        
        // 添加拖拽滾動功能
        final double[] dragStartY = {0};
        final double[] initialVvalue = {0};
        
        leftScrollPane.setOnMousePressed(event -> {
            dragStartY[0] = event.getY();
            initialVvalue[0] = leftScrollPane.getVvalue();
            event.consume();
        });
        
        leftScrollPane.setOnMouseDragged(event -> {
            double deltaY = dragStartY[0] - event.getY();
            double viewportHeight = leftScrollPane.getHeight();
            double contentHeight = viewportHeight * 1.5; // 估算內容高度
            
            // 調整滾動量
            if (contentHeight > viewportHeight) {
                double scrollableRange = contentHeight - viewportHeight;
                double newVvalue = initialVvalue[0] + (deltaY / scrollableRange);
                leftScrollPane.setVvalue(Math.min(Math.max(newVvalue, 0), 1.0));
            }
            event.consume();
        });
        
        // 優化滾輪事件處理
        leftScrollPane.setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            // 根據內容高度調整滾動量
            double viewportHeight = leftScrollPane.getHeight();
            double contentHeight = viewportHeight * 1.5; // 估算內容高度
            
            if (contentHeight > viewportHeight) {
                double scrollFactor = 0.005; // 較小的因子讓滾動更平滑
                double newVvalue = leftScrollPane.getVvalue() - (deltaY * scrollFactor);
                leftScrollPane.setVvalue(Math.min(Math.max(newVvalue, 0), 1.0));
                event.consume();
            }
        });
        
        // 重設右面板屬性，確保其可以正確顯示
        rightPanel.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + ";");
        rightPanel.setMinWidth(300); // 確保有足夠的最小寬度
        rightPanel.setPrefWidth(350); // 設置預設寬度
        rightPanel.setPrefHeight(Double.MAX_VALUE); // 填滿可用高度
        rightPanel.setMinHeight(600); // 確保最小高度足夠顯示內容
        
        // 確保右面板有足夠的底部邊距
        rightPanel.setPadding(new Insets(15, 15, 70, 15)); // 增加底部邊距
        
        // 創建一個新的ScrollPane包裹右面板，以便於垂直滾動
        ScrollPane rightScrollPane = new ScrollPane(rightPanel);
        rightScrollPane.setFitToWidth(true);
        rightScrollPane.setFitToHeight(false); // 改為false，使內容可以滾動
        rightScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        rightScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS); 
        rightScrollPane.setPannable(true);
        rightScrollPane.setVvalue(0); // 重置滾動位置到頂部
        rightScrollPane.setMinHeight(600); // 設置最小高度
        
        // 設置寬度比例 - 優化比例分配
        HBox.setHgrow(leftScrollPane, Priority.ALWAYS);
        leftScrollPane.setMaxWidth(Double.MAX_VALUE);
        leftScrollPane.setMinWidth(500); // 增加左面板的最小寬度，確保足夠顯示內容
        
        HBox.setHgrow(rightScrollPane, Priority.NEVER);
        rightScrollPane.setMaxWidth(350); // 保持右側面板寬度合理
        rightScrollPane.setMinWidth(300); // 確保右側面板有最小寬度
        
        // 使用Platform.runLater確保viewport節點可用後再設置其背景
        Platform.runLater(() -> {
            // 設置視口背景
            Node viewport = rightScrollPane.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + ";");
            }
            
            // 強制重新計算內容佈局
            rightScrollPane.requestLayout();
            
            // 確保分頁欄仍然在底部顯示
            if (mainLayout != null && tabBar != null) {
                mainLayout.setBottom(tabBar);
                tabBar.setStyle("-fx-background-color: #2A2A2A; -fx-border-width: 1 0 0 0; -fx-border-color: #666666; -fx-padding: 5 10 5 10;");
                tabBar.setVisible(true);
                tabBar.setManaged(true);
                tabBar.toFront(); // 確保分頁欄在最前面
                System.out.println("水平佈局模式下設置分頁欄");
            }
        });
        
        // 將面板添加到主內容區域
        mainContentBox.getChildren().addAll(leftScrollPane, rightScrollPane);
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
        content.medianExpense = medianExpense; // 保存平均消費信息
        tabContents.put(tabId, content);
        
        // 選中新分頁
        selectTab(tabId);
        
        // 確保分頁欄可見
        mainLayout.setBottom(tabBar);
        tabBar.setVisible(true);
        tabBar.setManaged(true);
        
        // 調整UI以確保分頁欄可見
        Platform.runLater(() -> {
            mainLayout.requestLayout();
            tabBar.toFront();
            System.out.println("創建了新分頁: " + displayName + " (平均消費: " + medianExpense + ")");
        });
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
        
        // 更新所有分頁樣式
        for (TabContent t : tabContents.values()) {
            if (t.id.equals(tabId)) {
                t.tabBox.setStyle("-fx-background-color: #4D4D4D; -fx-background-radius: 5 5 0 0; -fx-cursor: hand;");
                ((Label)t.tabBox.getChildren().get(0)).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            } else {
                t.tabBox.setStyle("-fx-background-color: #333333; -fx-background-radius: 5 5 0 0; -fx-cursor: hand;");
                ((Label)t.tabBox.getChildren().get(0)).setStyle("-fx-text-fill: #CCCCCC;");
            }
        }
        
        // 切換內容
        currentTabId = tabId;
        loadAndDisplayRestaurantData(tab.jsonFilePath);
        
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

    // 新增輔助方法，確保分頁欄顯示
    private void ensureTabBarVisible() {
        if (mainLayout != null && tabBar != null) {
            // 重新將分頁欄設置到主布局的底部
            mainLayout.setBottom(tabBar);
            
            // 確保分頁欄可見且管理
            tabBar.setVisible(true);
            tabBar.setManaged(true);
            
            // 設置更明顯的樣式，增加邊框厚度和顏色
            tabBar.setStyle("-fx-background-color: #2A2A2A; -fx-border-width: 2 0 0 0; -fx-border-color: #E67649; -fx-padding: 5 10 5 10;");
            
            // 確保最小高度足夠
            tabBar.setMinHeight(45); // 增加高度使其更容易看到
            tabBar.setPrefHeight(45);
            tabBar.setMaxHeight(45); // 固定最大高度，避免在視窗高度變化時被拉伸
            
            // 確保寬度填滿整個視窗寬度
            tabBar.setMaxWidth(Double.MAX_VALUE);
            tabBar.setPrefWidth(Double.MAX_VALUE);
            
            // 在全螢幕模式下，確保分頁欄始終置於底部
            BorderPane.setMargin(tabBar, new Insets(10, 0, 5, 0)); // 增加底部邊距
            
            // 將分頁欄提升到前台，避免被其他元素覆蓋
            tabBar.toFront();
            
            // 使用Platform.runLater確保UI更新
            Platform.runLater(() -> {
                // 重新觸發布局計算
                mainLayout.requestLayout();
                
                // 再次確保分頁欄在最前顯示
                tabBar.toFront();
                
                System.out.println("重新設置分頁欄可見性 - 全螢幕模式強化版本");
            });
        }
    }
}


