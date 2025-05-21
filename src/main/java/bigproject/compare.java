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
import javafx.scene.Parent;
import javafx.event.EventHandler;
import javafx.scene.input.ScrollEvent;

import bigproject.RightPanel;

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
    
    // 添加 AIChat 實例
    private AIChat aiChat;

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

    @Override
    public void start(Stage primaryStage) {
        // 設置關閉窗口的處理器
        primaryStage.setOnCloseRequest(event -> {
            // 清理資源
            System.exit(0);
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
        
        // 創建主佈局
        mainLayout = new BorderPane();
        // 調整主布局邊距，上、左、右有邊距，底部無邊距，確保分頁欄不留空白
        mainLayout.setPadding(new Insets(15, 15, 0, 0)); // 完全移除底部邊距，右邊也移除以避免對分頁欄位置的影響
        mainLayout.setStyle("-fx-background-color: #2C2C2C;"); // 使用深色背景統一風格
        mainLayout.setPrefHeight(Double.MAX_VALUE); // 確保主佈局填滿整個高度
        
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
        searchContainer.setPadding(new Insets(15, 20, 15, 20));
        searchContainer.setMaxWidth(Double.MAX_VALUE); // 允許最大寬度
        searchContainer.setPrefWidth(Double.MAX_VALUE); // 填滿可用寬度
        searchContainer.setMinHeight(60); // 增加高度
        searchContainer.setStyle("-fx-background-color: #3A3A3A; -fx-background-radius: 10; -fx-border-color: #E67649; -fx-border-width: 1px; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        searchContainer.setVisible(true); // 確保可見
        searchContainer.setManaged(true); // 確保被佈局管理
        
        // 添加一個標籤，指示這是搜尋區域
        Label searchLabel = new Label("餐廳搜尋：");
        searchLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        searchContainer.getChildren().add(searchLabel);
        
        // 創建圓角搜索框
        TextField searchField = new TextField();
        searchField.setPromptText("請輸入關鍵字搜尋餐廳...");
        searchField.setPrefHeight(40);  // 增加高度
        searchField.getStyleClass().add("search-history-field");
        searchField.setStyle("-fx-background-radius: 20; -fx-padding: 5 15 5 15; -fx-font-size: 16px; " + 
                             "-fx-background-color: #1E1E1E; -fx-text-fill: white; " + 
                             "-fx-prompt-text-fill: #BBBBBB; -fx-border-color: #E67649; " + 
                             "-fx-border-width: 1.5px; -fx-border-radius: 20; " + 
                             "-fx-focus-color: #F08159; -fx-faint-focus-color: #F0815944;");
        searchField.setPrefWidth(Double.MAX_VALUE);
        searchField.setVisible(true);  // 確保可見
        searchField.setManaged(true);  // 確保被佈局管理
        searchField.setEditable(true); // 確保可編輯
        HBox.setHgrow(searchField, Priority.ALWAYS); // 讓搜索框占用所有可用空間
        
        // 當聚焦時改變樣式
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) { // 獲得焦點
                searchField.setStyle("-fx-background-radius: 20; -fx-padding: 5 15 5 15; -fx-font-size: 16px; " + 
                                   "-fx-background-color: #252525; -fx-text-fill: white; " + 
                                   "-fx-prompt-text-fill: #BBBBBB; -fx-border-color: #F08159; " + 
                                   "-fx-border-width: 2px; -fx-border-radius: 20; " + 
                                   "-fx-effect: dropshadow(gaussian, rgba(240,129,89,0.3), 10, 0, 0, 0); " + 
                                   "-fx-focus-color: #F08159; -fx-faint-focus-color: #F0815944;");
            } else { // 失去焦點
                searchField.setStyle("-fx-background-radius: 20; -fx-padding: 5 15 5 15; -fx-font-size: 16px; " + 
                                   "-fx-background-color: #1E1E1E; -fx-text-fill: white; " + 
                                   "-fx-prompt-text-fill: #BBBBBB; -fx-border-color: #E67649; " + 
                                   "-fx-border-width: 1.5px; -fx-border-radius: 20; " + 
                                   "-fx-focus-color: #F08159; -fx-faint-focus-color: #F0815944;");
            }
            
            // ... existing code ...
        });
        
        // 創建搜尋建議下拉選單容器和搜索框的組合
        StackPane searchStackPane = new StackPane();
        searchStackPane.setAlignment(Pos.TOP_LEFT);
        searchStackPane.setMaxWidth(Double.MAX_VALUE);
        searchStackPane.setPrefWidth(Double.MAX_VALUE);
        searchStackPane.setMinWidth(400); // 設置最小寬度
        searchStackPane.setVisible(true); // 確保可見
        searchStackPane.setManaged(true); // 確保被佈局管理
        HBox.setHgrow(searchStackPane, Priority.ALWAYS);

        // 建立搜尋建議下拉選單
        VBox suggestionsBox = new VBox(2); // 減少間距，更緊湊
        suggestionsBox.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #E67649; -fx-border-width: 0 1.5 1.5 1.5; " +
                                "-fx-border-radius: 0 0 15 15; -fx-background-radius: 0 0 15 15; " + 
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 5);");
        suggestionsBox.setVisible(false);
        suggestionsBox.setPrefWidth(searchField.getWidth());
        suggestionsBox.setMaxWidth(Double.MAX_VALUE);
        suggestionsBox.setMaxHeight(350);  // 增加建議選單高度
        suggestionsBox.setPadding(new Insets(5, 0, 5, 0));  // 添加內部邊距
        
        // 設置較高層級，確保顯示在其他元素上方
        StackPane.setAlignment(suggestionsBox, Pos.TOP_LEFT);
        
        // 當搜索框寬度變化時，同步更新建議框寬度和位置
        searchField.widthProperty().addListener((obs, oldVal, newVal) -> {
            suggestionsBox.setPrefWidth(newVal.doubleValue());
        });
        
        // 確保suggestionsBox的寬度與searchField一致，並設置正確的位置
        searchField.heightProperty().addListener((obs, oldVal, newVal) -> {
            suggestionsBox.setTranslateY(newVal.doubleValue());
        });
        
        // 添加到堆疊面板中
        searchStackPane.getChildren().addAll(searchField, suggestionsBox);
        searchStackPane.setPrefWidth(Double.MAX_VALUE);
        VBox.setMargin(searchStackPane, new Insets(0, 0, 5, 0)); // 添加底部間距
        
        // 搜尋組件將在下面添加到容器中
        
        // 設置建議框的位置 (在搜索框下方)
        suggestionsBox.setTranslateY(searchField.getHeight());
        
        // 確保 StackPane 能夠擴展填充可用空間
        HBox.setHgrow(searchStackPane, Priority.ALWAYS);
        
        // 自動搜尋的防抖動設計
        final java.util.Timer[] searchTimer = {null};
        final int DEBOUNCE_DELAY = 300; // 降低延遲以提高反應速度
        
        // 監聽文字變更，顯示搜尋建議
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // 取消上一個計時器
            if (searchTimer[0] != null) {
                searchTimer[0].cancel();
            }
            
            // 如果輸入為空，隱藏建議選單
            if (newValue == null || newValue.trim().isEmpty()) {
                suggestionsBox.setVisible(false);
                suggestionsBox.getChildren().clear();
                return;
            }
            
            // 創建新計時器，延遲執行搜尋
            searchTimer[0] = new java.util.Timer();
            searchTimer[0].schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    // 確保在JavaFX主執行緒中執行UI操作
                    Platform.runLater(() -> {
                        // 獲取搜尋建議
                        try {
                            suggestionsBox.getChildren().clear();
                            org.json.JSONObject searchResult = new bigproject.search.AlgoliaRestaurantSearch().performSearch(newValue.trim(), true);
                            int hitsCount = searchResult.getInt("nbHits");
                            
                            if (hitsCount > 0) {
                                // 顯示建議選單
                                suggestionsBox.setVisible(true);
                                
                                // 取得搜尋結果
                                org.json.JSONArray hits = searchResult.getJSONArray("hits");
                                int limit = Math.min(hits.length(), 8); // 最多顯示8個建議
                                
                                // 創建建議項目
                                for (int i = 0; i < limit; i++) {
                                    org.json.JSONObject hit = hits.getJSONObject(i);
                                    String restaurantName = hit.getString("name");
                                    String address = hit.optString("address", "");
                                    
                                    // 創建建議項
                                    HBox suggestionItem = createSuggestionItem(restaurantName, address);
                                    
                                    // 設置點擊事件
                                    suggestionItem.setOnMouseClicked(event -> {
                                        // 設置文字並立即執行搜尋
                                        searchField.setText(restaurantName);
                                        suggestionsBox.setVisible(false);
                                        
                                        // 先聚焦到搜尋框上，再執行搜尋
                                        searchField.requestFocus();
                                        Platform.runLater(() -> {
                                            handleSearch(restaurantName);
                                        });
                                        
                                        // 阻止事件傳播
                                        event.consume();
                                    });
                                    
                                    suggestionsBox.getChildren().add(suggestionItem);
                                }
                            } else {
                                suggestionsBox.setVisible(false);
                            }
                        } catch (Exception e) {
                            System.err.println("獲取搜尋建議時發生錯誤: " + e.getMessage());
                            suggestionsBox.setVisible(false);
                        }
                    });
                }
            }, DEBOUNCE_DELAY);
        });
        
        // 當搜索框獲得焦點時，如果有內容則顯示建議選單
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && !searchField.getText().trim().isEmpty()) {
                // 在搜索框獲得焦點且有內容時，觸發一次搜尋建議
                if (searchTimer[0] != null) {
                    searchTimer[0].cancel();
                }
                searchTimer[0] = new java.util.Timer();
                searchTimer[0].schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            try {
                                suggestionsBox.getChildren().clear();
                                org.json.JSONObject searchResult = new bigproject.search.AlgoliaRestaurantSearch().performSearch(searchField.getText().trim(), true);
                                showSuggestions(searchResult, suggestionsBox, searchField);
                            } catch (Exception e) {
                                System.err.println("獲取焦點時的搜尋建議錯誤: " + e.getMessage());
                            }
                        });
                    }
                }, 100); // 較短的延遲
            } else if (!newVal) {
                // 延遲隱藏，避免在點擊選項前隱藏選單
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            // 如果焦點不在搜索框上，隱藏建議
                            if (!searchField.isFocused()) {
                                suggestionsBox.setVisible(false);
                            }
                        });
                    }
                }, 200);
            }
        });
        
        Button searchButton = new Button("搜尋");
        searchButton.setPrefHeight(40);  // 與搜尋欄同高
        searchButton.setPrefWidth(90);   // 增加寬度
        searchButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; " + 
                             "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 15px; " +
                             "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);");
        
        // 添加鼠標懸停效果
        searchButton.setOnMouseEntered(e -> {
            searchButton.setStyle("-fx-background-color: #F08159; -fx-text-fill: white; " + 
                                 "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 15px; " + 
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 2);");
            searchButton.setCursor(javafx.scene.Cursor.HAND);
        });
        
        searchButton.setOnMouseExited(e -> {
            searchButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; " + 
                                 "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 15px; " +
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);");
            searchButton.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        // 添加按下效果
        searchButton.setOnMousePressed(e -> {
            searchButton.setStyle("-fx-background-color: #D45E3A; -fx-text-fill: white; " + 
                                 "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 15px; " +
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 0);");
        });
        
        searchButton.setOnMouseReleased(e -> {
            searchButton.setStyle("-fx-background-color: #F08159; -fx-text-fill: white; " + 
                                 "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 15px; " + 
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        });
        
        // 清空搜尋容器，重新添加元素
        searchContainer.getChildren().clear();
        
        // 先將搜尋框容器(stackPane)添加到搜尋容器中
        searchContainer.getChildren().add(searchStackPane);
        
        // 再將搜尋按鈕添加到搜尋容器中
        searchContainer.getChildren().add(searchButton);
        
        // 設置搜尋按鈕點擊事件
        searchButton.setOnAction(e -> {
            handleSearch(searchField.getText());
        });
        
        // --- 創建主布局 --- 
        mainContainer = new VBox(5); // 使用VBox包含主要內容區域
        mainContainer.setPrefHeight(Double.MAX_VALUE); // 確保填滿整個高度
        VBox.setVgrow(mainContainer, Priority.ALWAYS); // 確保主容器能擴展填滿
        
        // 初始化 AIChat 實例
        aiChat = new AIChat(mainLayout, mainContainer, this);

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
        rightPanel = new RightPanel(this);
        
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
        // 創建頂部組合容器，同時包含頂部按鈕和搜尋欄
        VBox topContainer = new VBox(10);
        topContainer.setPadding(new Insets(10, 15, 5, 15));
        topContainer.getChildren().addAll(topBar, searchContainer);
        
        mainLayout.setCenter(mainContainer); // 使用主容器作為主要內容
        mainLayout.setTop(topContainer); // 設置頂部組合容器
        
        // 明確設置底部的分頁欄，固定在視窗底部
        mainLayout.setBottom(tabBar);
        BorderPane.setMargin(tabBar, new Insets(0, 0, 0, 0)); // 移除底部邊距，確保完全貼緊視窗底部
        
        mainLayout.setStyle("-fx-background-color: #2C2C2C; -fx-min-height: 100%;"); // 確保填滿整個空間
        
        // 初始化場景，並設置最小尺寸防止過小導致UI變形
        mainScene = new Scene(mainLayout, 1024, 768);
        mainScene.getRoot().setStyle("-fx-min-height: 100%;");
        
        // 當點擊搜索框以外的地方時，隱藏建議選單
        mainScene.setOnMouseClicked(event -> {
            // 檢查點擊是否在搜索框或建議選單上
            if (!suggestionsBox.getBoundsInParent().contains(event.getSceneX(), event.getSceneY()) &&
                !searchField.getBoundsInParent().contains(event.getSceneX(), event.getSceneY())) {
                suggestionsBox.setVisible(false);
            }
        });

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
        
            // 為每個區域添加特定的點擊事件 - 改成直接呼叫AIChat介面
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
            String query = searchField.getText();
            if (query != null && !query.trim().isEmpty()) {
                handleSearch(query.trim());
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

        // --- 刪除Cmd+T/Ctrl+T快捷鍵功能 ---

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

    /**
     * 開啟地圖顯示
     */
    public void openMapInBrowser(String query) {
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
        dataManager.loadAndDisplayRestaurantData(jsonFilePath, 
            rightPanel.getRatingsHeader(), 
            rightPanel.getRatingsBox(), 
            rightPanel.getRatingBars(), 
            reviewsArea, 
            photosContainer, 
            rightPanel.getFeaturesArea(), 
            rightPanel.getProsArea(), 
            rightPanel.getConsArea());
        
        // 計算平均消費中位數
        String medianExpense = calculateMedianExpense(jsonFilePath);
        
        // 更新右側面板的平均消費中位數
        rightPanel.updateMedianExpense(medianExpense);
        
        // 設置當前JSON檔案路徑，供近期評論功能使用
        rightPanel.setCurrentJsonFilePath(jsonFilePath);
        
        // 更新近期評論顯示 - 預設選中近一個月按鈕
        rightPanel.updateRecentReviewsDisplay(30); // 30天
    }

    private void clearRestaurantDataDisplay(String message) {
        dataManager.clearRestaurantDataDisplay(message, 
            rightPanel.getRatingsHeader(), 
            rightPanel.getRatingsBox(), 
            rightPanel.getRatingBars(), 
            reviewsArea, 
            photosContainer, 
            rightPanel.getFeaturesArea(), 
            rightPanel.getProsArea(), 
            rightPanel.getConsArea());
            
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
                            
                            // 根據餐廳名稱載入對應的 JSON 檔案
                            if (restaurantName.contains("海大") || restaurantName.contains("Haidai")) {
                                loadAndDisplayRestaurantData("Haidai Roast Shop.json");
                            } else if (restaurantName.contains("海那邊") || restaurantName.contains("Sea Side")) {
                                loadAndDisplayRestaurantData("Sea Side Eatery Info.json");
                            } else {
                                // 如果找不到對應的 JSON 檔案，顯示錯誤訊息
                                clearRestaurantDataDisplay("找到餐廳：" + restaurantName + "，但無法載入詳細資料");
                            }
                            
                            // 在控制台顯示搜尋結果摘要
                            System.out.println("找到 " + hitsCount + " 家與「" + trimmedQuery + "」相關的餐廳");
                            for (int i = 0; i < Math.min(hits.length(), 3); i++) {
                                org.json.JSONObject hit = hits.getJSONObject(i);
                                System.out.println((i + 1) + ". " + hit.getString("name") + " - " + hit.optString("address", "無地址資訊"));
                            }
                        } else {
                            // 如果 Algolia 沒有結果，嘗試使用 Google Maps
                            openMapInBrowser(trimmedQuery);
                            clearRestaurantDataDisplay("在餐廳資料庫中找不到「" + trimmedQuery + "」，已在 Google Maps 中開啟搜尋");
                        }
                    });
                    
                } catch (Exception e) {
                    // 如果搜尋過程出錯，顯示錯誤訊息
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        clearRestaurantDataDisplay("搜尋時發生錯誤：" + e.getMessage());
                        openMapInBrowser(trimmedQuery);
                    });
                }
            }).start();
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
        
        // 如果顯示報告，確保建議視圖關閉
        if (isShowing && isSuggestionActive[0]) {
            isSuggestionActive[0] = false;
            suggestionButton.setStyle(normalButtonStyle);
        }
        
        // 如果顯示報告，確保AI聊天視圖關閉
        if (isShowing && aiChat.isActive()) {
            aiChat.hideChatView();
        }
    }
    
    @Override
    public void onSuggestionsStateChanged(boolean isShowing) {
        isSuggestionActive[0] = isShowing;
        suggestionButton.setStyle(isShowing ? activeButtonStyle : normalButtonStyle);
        
        // 如果顯示建議，確保報告視圖關閉
        if (isShowing && isReportActive[0]) {
            isReportActive[0] = false;
            reportButton.setStyle(normalButtonStyle);
        }
        
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
            
            // 如果月報視圖是活躍的，關閉它
        if (isReportActive[0]) {
            uiManager.toggleMonthlyReport();
            isReportActive[0] = false;
            reportButton.setStyle(normalButtonStyle);
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
        
            // 如果月報視圖是活躍的，關閉它
            if (isReportActive[0]) {
                isReportActive[0] = false;
                reportButton.setStyle(normalButtonStyle);
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
                                      ScrollPane leftScrollPane,
                                      Button searchButton) {
        // 設置固定最小寬度
        mainContentBox.setMinWidth(400);
        
        // 調整左右面板的固定寬度
        double rightPanelWidth = 450; // 固定青蘋果欄寬度
        double availableWidth = primaryStage.getWidth() - 20; // 考慮邊距
        double leftPanelWidth = availableWidth - rightPanelWidth;
        
        // 設置固定大小
        leftScrollPane.setPrefWidth(leftPanelWidth);
        leftScrollPane.setMaxWidth(leftPanelWidth);
        
        // 調整面板高度
        adjustPanelHeights(mainContentBox, leftScrollPane, primaryStage.getHeight());
        
        // 確保分頁欄顯示
                ensureTabBarVisible();
                
        // 搜尋按鈕統一使用文字
                searchButton.setText("搜尋");
                searchButton.setPrefWidth(75);
        
        // 主佈局進行一次調整
                mainLayout.layout();
    }
    
    /**
     * 調整面板高度
     */
    private void adjustPanelHeights(HBox mainContentBox, ScrollPane leftScrollPane, double windowHeight) {
        // 固定值設定
        double topBarHeight = 60; // 頂部工具欄高度
        double searchBarHeight = 50; // 搜索欄高度
        double tabBarHeight = 45; // 分頁欄高度
        
        // 計算實際可用高度
        double availableHeight = windowHeight - topBarHeight - searchBarHeight - tabBarHeight - 10;
        
        // 確保可用高度不小於最小值
        availableHeight = Math.max(availableHeight, 400);
        
        // 設置左側滾動面板高度
        leftScrollPane.setPrefHeight(availableHeight);
        leftScrollPane.setMinHeight(availableHeight);
        
        // 設置右側面板高度
        if (rightPanel != null) {
            rightPanel.setPrefHeight(availableHeight);
            rightPanel.setMinHeight(availableHeight);
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
        // 固定佈局設置
        double rightPanelWidth = 450; // 固定青蘋果欄寬度
        double availableWidth = width - 20; // 考慮邊距
        double leftPanelWidth = Math.max(availableWidth - rightPanelWidth, 300); // 確保最小寬度
        
        // 設置固定大小
        leftScrollPane.setPrefWidth(leftPanelWidth);
        leftScrollPane.setMaxWidth(leftPanelWidth);
        
        // 調整面板高度
        adjustPanelHeights(mainContentBox, leftScrollPane, height);
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
        // 避免呼叫 button.fire()，直接調用更新方法
        System.out.println("創建新分頁時自動觸發近一個月評論");
        updateRecentReviewsDisplay(30); // 30天
        
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
     * 更新近期評論顯示
     */
    private void updateRecentReviewsDisplay(int days) {
        System.out.println("正在更新近期評論顯示，顯示近 " + days + " 天的評論...");
        
        // 直接使用 RightPanel 的方法，它會調用 LatestReviewsManager 獲取真實數據
        rightPanel.updateRecentReviewsDisplay(days);
    }
    
    /**
     * 使用示例數據填充近期評論容器
     * 注意：此方法已不再使用，已由 LatestReviewsManager 替代
     * 僅作為程式備份保留
     */
    private void updateRecentReviewsWithSampleData(VBox recentReviewsBox, int days) {
        System.out.println("警告：使用範例數據代替真實數據！此功能已棄用。");
        
        // 清空現有內容
        recentReviewsBox.getChildren().clear();
        
        // 示例評論數據
        String[][] reviewData;
        if (days <= 1) {
            // 今天的評論
            reviewData = new String[][] {
                {"今天", "李小姐", "4.5", "服務態度很好，餐點美味！老闆親切有禮，會再來。"},
                {"今天", "張先生", "4.0", "食物好吃，但環境有點擁擠。"}
            };
        } else if (days <= 7) {
            // 一週內的評論
            reviewData = new String[][] {
                {"今天", "李小姐", "4.5", "服務態度很好，餐點美味！老闆親切有禮，會再來。"},
                {"今天", "張先生", "4.0", "食物好吃，但環境有點擁擠。"},
                {"昨天", "王太太", "5.0", "這家店的特色料理實在太棒了，强烈推薦！"},
                {"3天前", "林先生", "3.5", "價格有點貴，但口味不錯。"},
                {"5天前", "陳太太", "4.0", "乾淨舒適的環境，餐點也相當美味。"}
            };
                } else {
            // 一個月內的評論
            reviewData = new String[][] {
                {"今天", "李小姐", "4.5", "服務態度很好，餐點美味！老闆親切有禮，會再來。"},
                {"今天", "張先生", "4.0", "食物好吃，但環境有點擁擠。"},
                {"昨天", "王太太", "5.0", "這家店的特色料理實在太棒了，强烈推薦！"},
                {"3天前", "林先生", "3.5", "價格有點貴，但口味不錯。"},
                {"5天前", "陳太太", "4.0", "乾淨舒適的環境，餐點也相當美味。"},
                {"1週前", "黃小姐", "4.5", "服務生態度友善，餐點份量十足。"},
                {"10天前", "吳先生", "3.0", "等待時間有點長，但食物品質還不錯。"},
                {"2週前", "謝太太", "4.0", "適合家庭聚餐，菜單選擇多樣。"},
                {"3週前", "鄭先生", "4.5", "食材新鮮，價格合理，推薦！"},
                {"1個月前", "劉小姐", "5.0", "絕對是我吃過最好吃的餐廳之一，每道菜都很用心。"}
            };
        }
        
        // 為每條評論創建UI元素
        for (String[] review : reviewData) {
            VBox reviewCard = createReviewCard(review[0], review[1], Double.parseDouble(review[2]), review[3]);
            recentReviewsBox.getChildren().add(reviewCard);
        }
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
     * 創建一個搜尋建議項元素
     * @param name 餐廳名稱
     * @param address 餐廳地址
     * @return 建議項的 HBox 容器
     */
    private HBox createSuggestionItem(String name, String address) {
        HBox item = new HBox(12);  // 增加間距
        item.setPadding(new Insets(12, 15, 12, 15));  // 增加上下內邊距
        item.setStyle("-fx-background-color: #1E1E1E; -fx-cursor: hand; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        // 建立餐廳名稱標籤
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-font-size: 14px;");  // 白色文字
        
        // 建立地址標籤（如果有）
        VBox contentBox = new VBox(4);  // 增加標籤間距
        contentBox.getChildren().add(nameLabel);
        
        if (address != null && !address.isEmpty()) {
            Label addressLabel = new Label(address);
            addressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #AAAAAA;");  // 淺灰色
            contentBox.getChildren().add(addressLabel);
        }
        
        // 左側圖標 - 使用餐廳圖標
        Label iconLabel = new Label("🍽️");  // 使用餐廳相關表情符號
        iconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #F08159;");  // 增大圖標並改為主題色
        iconLabel.setPrefWidth(30);  // 增加寬度
        
        item.getChildren().addAll(iconLabel, contentBox);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        
        // 懸停效果 - 使用更明顯的色彩和陰影
        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: #2A2A2A; -fx-cursor: hand; " + 
                         "-fx-effect: dropshadow(gaussian, rgba(240,129,89,0.3), 8, 0, 0, 2); " + 
                         "-fx-border-radius: 8; -fx-background-radius: 8;");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F08159; -fx-font-size: 14px;");  // 變色突顯
        });
        
        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: #1E1E1E; -fx-cursor: hand; -fx-border-radius: 8; -fx-background-radius: 8;");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-font-size: 14px;");  // 恢復原色
        });
        
        // 點擊效果
        item.setOnMousePressed(e -> {
            item.setStyle("-fx-background-color: #333333; -fx-cursor: hand; " + 
                         "-fx-border-radius: 8; -fx-background-radius: 8;");
        });
        
        item.setOnMouseReleased(e -> {
            item.setStyle("-fx-background-color: #2A2A2A; -fx-cursor: hand; " + 
                         "-fx-effect: dropshadow(gaussian, rgba(240,129,89,0.3), 8, 0, 0, 2); " + 
                         "-fx-border-radius: 8; -fx-background-radius: 8;");
        });
        
        return item;
    }

    /**
     * 顯示搜尋建議
     * @param searchResult Algolia 搜尋結果
     * @param suggestionsBox 建議選單容器
     * @param searchField 搜尋欄位
     */
    private void showSuggestions(org.json.JSONObject searchResult, VBox suggestionsBox, TextField searchField) {
        try {
            int hitsCount = searchResult.getInt("nbHits");
            
            if (hitsCount > 0) {
                // 顯示建議選單
                suggestionsBox.setVisible(true);
                
                // 取得搜尋結果
                org.json.JSONArray hits = searchResult.getJSONArray("hits");
                int limit = Math.min(hits.length(), 8); // 最多顯示8個建議
                
                // 創建建議項目
                for (int i = 0; i < limit; i++) {
                    org.json.JSONObject hit = hits.getJSONObject(i);
                    String restaurantName = hit.getString("name");
                    String address = hit.optString("address", "");
                    
                    // 創建建議項
                    HBox suggestionItem = createSuggestionItem(restaurantName, address);
                    
                    // 設置點擊事件
                    suggestionItem.setOnMouseClicked(event -> {
                        searchField.setText(restaurantName);
                        suggestionsBox.setVisible(false);
                        handleSearch(restaurantName);
                    });
                    
                    suggestionsBox.getChildren().add(suggestionItem);
                }
            } else {
                suggestionsBox.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("顯示搜尋建議時發生錯誤: " + e.getMessage());
            suggestionsBox.setVisible(false);
        }
    }
}


