package bigproject;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 搜尋首頁 - 應用程式的初始界面
 * 用戶必須先搜尋餐廳才能進入主分析界面
 */
public class SearchHomePage {
    
    private Stage primaryStage;
    private SearchResultCallback callback;
    private VBox mainContainer;
    private TextField searchField;
    private Button searchButton;
    private VBox suggestionsContainer;
    private ProgressIndicator loadingIndicator;
    private Label statusLabel;
    
    // 顏色配置
    private static final String PALE_DARK_YELLOW = "#6F6732";
    private static final String RICH_MIDTONE_RED = "#E67649";
    private static final String DARK_BACKGROUND = "#2C2C2C";
    private static final String LIGHT_TEXT = "#F5F5F5";
    
    /**
     * 搜尋結果回調接口
     */
    public interface SearchResultCallback {
        void onRestaurantSelected(String restaurantName, String restaurantId, String dataSource);
    }
    
    /**
     * 建構函數
     */
    public SearchHomePage(Stage primaryStage, SearchResultCallback callback) {
        this.primaryStage = primaryStage;
        this.callback = callback;
        initializeUI();
    }
    
    /**
     * 初始化用戶界面
     */
    private void initializeUI() {
        // 主容器
        mainContainer = new VBox(30);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(50));
        
        // 使用背景圖片
        String bgImagePath = "file:" + System.getProperty("user.dir").replace(" ", "%20") + "/應用程式背景.png";
        mainContainer.setStyle(
            "-fx-background-image: url('" + bgImagePath + "'); " +
            "-fx-background-size: cover; " +
            "-fx-background-position: center center;"
        );
        
        // 創建標題區域
        createTitleSection();
        
        // 創建搜尋區域
        createSearchSection();
        
        // 創建建議區域
        createSuggestionsSection();
        
        // 創建狀態指示區域
        createStatusSection();
    }
    
    /**
     * 創建標題區域
     */
    private void createTitleSection() {
        VBox titleBox = new VBox(15);
        titleBox.setAlignment(Pos.CENTER);
        
        // 主標題
        Label titleLabel = new Label("🍽️ 餐廳市場分析系統");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        titleLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 3, 0, 1, 1);");
        
        // 副標題
        Label subtitleLabel = new Label("探索餐廳的真實評價與市場洞察");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        subtitleLabel.setStyle("-fx-text-fill: " + LIGHT_TEXT + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 1, 1);");
        
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        mainContainer.getChildren().add(titleBox);
    }
    
    /**
     * 創建搜尋區域
     */
    private void createSearchSection() {
        VBox searchBox = new VBox(20);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setMaxWidth(600);
        
        // 搜尋提示
        Label promptLabel = new Label("請輸入您想要分析的餐廳名稱");
        promptLabel.setFont(Font.font("System", FontWeight.NORMAL, 18));
        promptLabel.setStyle("-fx-text-fill: " + LIGHT_TEXT + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 1, 1);");
        
        // 搜尋輸入框和按鈕
        HBox searchInputBox = new HBox(10);
        searchInputBox.setAlignment(Pos.CENTER);
        
        searchField = new TextField();
        searchField.setPromptText("例如：鼎泰豐、麥當勞、星巴克...");
        searchField.setPrefHeight(50);
        searchField.setStyle(
            "-fx-font-size: 16px; " +
            "-fx-background-color: rgba(255, 255, 255, 0.9); " +
            "-fx-background-radius: 25; " +
            "-fx-border-radius: 25; " +
            "-fx-border-color: " + RICH_MIDTONE_RED + "; " +
            "-fx-border-width: 2; " +
            "-fx-padding: 10 20; " +
            "-fx-text-fill: #2C2C2C; " +
            "-fx-prompt-text-fill: #666666;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        searchButton = new Button("🔍 搜尋");
        searchButton.setPrefHeight(50);
        searchButton.setPrefWidth(120);
        searchButton.setStyle(
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: " + RICH_MIDTONE_RED + "; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 25; " +
            "-fx-border-radius: 25; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
        );
        
        // 添加搜尋按鈕懸停效果
        searchButton.setOnMouseEntered(e -> {
            searchButton.setStyle(
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-color: #f08a6c; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 25; " +
                "-fx-border-radius: 25; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);"
            );
        });
        
        searchButton.setOnMouseExited(e -> {
            searchButton.setStyle(
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-color: " + RICH_MIDTONE_RED + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 25; " +
                "-fx-border-radius: 25; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
            );
        });
        
        searchInputBox.getChildren().addAll(searchField, searchButton);
        
        // 設置搜尋事件
        searchButton.setOnAction(e -> performSearch());
        searchField.setOnAction(e -> performSearch());
        
        // 添加實時搜尋建議功能 - 在用戶輸入時立即顯示建議
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.trim().length() >= 1) { // 1個字符就開始搜尋
                performSuggestionSearch(newValue.trim());
            } else {
                clearSuggestions();
            }
        });
        
        searchBox.getChildren().addAll(promptLabel, searchInputBox);
        mainContainer.getChildren().add(searchBox);
    }
    
    /**
     * 創建建議區域
     */
    private void createSuggestionsSection() {
        suggestionsContainer = new VBox(10);
        suggestionsContainer.setAlignment(Pos.CENTER);
        suggestionsContainer.setMaxWidth(600);
        suggestionsContainer.setVisible(false);
        suggestionsContainer.setStyle(
            "-fx-background-color: rgba(30, 30, 30, 0.95); " +
            "-fx-background-radius: 15 15 15 15; " +
            "-fx-border-radius: 15 15 15 15; " +
            "-fx-border-color: " + RICH_MIDTONE_RED + "; " +
            "-fx-border-width: 2 2 2 2; " +
            "-fx-padding: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 5);"
        );
        
        mainContainer.getChildren().add(suggestionsContainer);
    }
    
    /**
     * 創建狀態指示區域
     */
    private void createStatusSection() {
        VBox statusBox = new VBox(10);
        statusBox.setAlignment(Pos.CENTER);
        
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(40, 40);
        loadingIndicator.setVisible(false);
        
        statusLabel = new Label("");
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        statusLabel.setStyle("-fx-text-fill: " + LIGHT_TEXT + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 1, 1);");
        
        statusBox.getChildren().addAll(loadingIndicator, statusLabel);
        mainContainer.getChildren().add(statusBox);
    }
    
    /**
     * 執行實時搜尋建議
     */
    private void performSuggestionSearch(String query) {
        if (query.isEmpty()) {
            clearSuggestions();
            return;
        }
        
        // 在背景執行建議搜尋
        new Thread(() -> {
            try {
                // 使用 AlgoliaRestaurantSearch 進行建議搜尋（限制較少結果）
                JSONObject searchResult = new bigproject.search.AlgoliaRestaurantSearch().performSearch(query, true);
                int hitsCount = searchResult.getInt("nbHits");
                
                Platform.runLater(() -> {
                    if (hitsCount > 0) {
                        showSuggestionResults(searchResult, Math.min(hitsCount, 5)); // 最多顯示5個建議
                    } else {
                        clearSuggestions();
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    clearSuggestions();
                });
            }
        }).start();
    }
    
    /**
     * 顯示搜尋建議結果
     */
    private void showSuggestionResults(JSONObject searchResult, int maxResults) {
        try {
            JSONArray hits = searchResult.getJSONArray("hits");
            
            // 清空建議容器
            suggestionsContainer.getChildren().clear();
            
            // 添加建議結果
            for (int i = 0; i < Math.min(hits.length(), maxResults); i++) {
                JSONObject hit = hits.getJSONObject(i);
                String restaurantName = hit.getString("name");
                String address = hit.optString("address", "");
                String restaurantId = hit.optString("objectID", "");
                
                VBox suggestionItem = createSuggestionItem(restaurantName, address, restaurantId);
                suggestionsContainer.getChildren().add(suggestionItem);
            }
            
            suggestionsContainer.setVisible(true);
            
        } catch (Exception e) {
            clearSuggestions();
        }
    }
    
    /**
     * 創建建議項目（簡化版本）
     */
    private VBox createSuggestionItem(String restaurantName, String address, String restaurantId) {
        VBox item = new VBox(3);
        item.setPadding(new Insets(8));
        item.setStyle(
            "-fx-background-color: rgba(64, 64, 64, 0.8); " +
            "-fx-background-radius: 5; " +
            "-fx-border-radius: 5; " +
            "-fx-cursor: hand;"
        );
        
        Label nameLabel = new Label(restaurantName);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setStyle("-fx-text-fill: white;");
        
        if (!address.isEmpty()) {
            Label addressLabel = new Label(address);
            addressLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
            addressLabel.setStyle("-fx-text-fill: #666666;");
            addressLabel.setWrapText(true);
            item.getChildren().addAll(nameLabel, addressLabel);
        } else {
            item.getChildren().add(nameLabel);
        }
        
        // 添加點擊事件
        item.setOnMouseClicked(e -> {
            searchField.setText(restaurantName);
            clearSuggestions();
            selectRestaurant(restaurantName, restaurantId, "algolia");
        });
        
        // 添加懸停效果
        item.setOnMouseEntered(e -> {
            item.setStyle(
                "-fx-background-color: rgba(230, 118, 73, 0.8); " +
                "-fx-background-radius: 5; " +
                "-fx-border-radius: 5; " +
                "-fx-border-color: " + RICH_MIDTONE_RED + "; " +
                "-fx-border-width: 1; " +
                "-fx-cursor: hand;"
            );
        });
        
        item.setOnMouseExited(e -> {
            item.setStyle(
                "-fx-background-color: rgba(64, 64, 64, 0.8); " +
                "-fx-background-radius: 5; " +
                "-fx-border-radius: 5; " +
                "-fx-cursor: hand;"
            );
        });
        
        return item;
    }

    /**
     * 執行搜尋
     */
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showStatus("請輸入餐廳名稱", false);
            return;
        }
        
        showStatus("正在搜尋「" + query + "」...", true);
        clearSuggestions();
        
        // 在背景執行搜尋
        new Thread(() -> {
            try {
                // 使用 AlgoliaRestaurantSearch 進行搜尋
                JSONObject searchResult = new bigproject.search.AlgoliaRestaurantSearch().performSearch(query, true);
                int hitsCount = searchResult.getInt("nbHits");
                
                Platform.runLater(() -> {
                    if (hitsCount > 0) {
                        showSearchResults(searchResult, query);
                    } else {
                        showNoResultsFound(query);
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showStatus("搜尋時發生錯誤：" + e.getMessage(), false);
                });
            }
        }).start();
    }
    
    /**
     * 顯示搜尋結果
     */
    private void showSearchResults(JSONObject searchResult, String originalQuery) {
        try {
            JSONArray hits = searchResult.getJSONArray("hits");
            int resultsToShow = Math.min(hits.length(), 8); // 最多顯示8個結果
            
            showStatus("找到 " + searchResult.getInt("nbHits") + " 家相關餐廳", false);
            
            // 清空建議容器
            suggestionsContainer.getChildren().clear();
            
            // 添加結果標題
            Label resultsTitle = new Label("搜尋結果：");
            resultsTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
            resultsTitle.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
            suggestionsContainer.getChildren().add(resultsTitle);
            
            // 添加搜尋結果
            for (int i = 0; i < resultsToShow; i++) {
                JSONObject hit = hits.getJSONObject(i);
                String restaurantName = hit.getString("name");
                String address = hit.optString("address", "");
                String restaurantId = hit.optString("objectID", "");
                
                VBox resultItem = createResultItem(restaurantName, address, restaurantId);
                suggestionsContainer.getChildren().add(resultItem);
            }
            
            // 如果沒有找到結果，提供收集選項
            if (resultsToShow == 0) {
                VBox noResultsOption = createCollectionOption(originalQuery);
                suggestionsContainer.getChildren().add(noResultsOption);
            } else {
                // 添加"找不到想要的餐廳"選項
                VBox collectOption = createCollectionOption(originalQuery);
                suggestionsContainer.getChildren().add(collectOption);
            }
            
            suggestionsContainer.setVisible(true);
            
        } catch (Exception e) {
            showStatus("解析搜尋結果時發生錯誤", false);
        }
    }
    
    /**
     * 創建結果項目
     */
    private VBox createResultItem(String restaurantName, String address, String restaurantId) {
        VBox item = new VBox(5);
        item.setPadding(new Insets(10));
        item.setStyle(
            "-fx-background-color: rgba(240, 240, 240, 0.8); " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-cursor: hand;"
        );
        
        Label nameLabel = new Label(restaurantName);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        Label addressLabel = new Label(address.isEmpty() ? "地址資訊不完整" : address);
        addressLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        addressLabel.setStyle("-fx-text-fill: #666666;");
        addressLabel.setWrapText(true);
        
        item.getChildren().addAll(nameLabel, addressLabel);
        
        // 添加點擊事件
        item.setOnMouseClicked(e -> {
            selectRestaurant(restaurantName, restaurantId, "algolia");
        });
        
        // 添加懸停效果
        item.setOnMouseEntered(e -> {
            item.setStyle(
                "-fx-background-color: rgba(230, 118, 73, 0.1); " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: " + RICH_MIDTONE_RED + "; " +
                "-fx-border-width: 1; " +
                "-fx-cursor: hand;"
            );
        });
        
        item.setOnMouseExited(e -> {
            item.setStyle(
                "-fx-background-color: rgba(240, 240, 240, 0.8); " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: hand;"
            );
        });
        
        return item;
    }
    
    /**
     * 創建資料收集選項
     */
    private VBox createCollectionOption(String originalQuery) {
        VBox item = new VBox(8);
        item.setPadding(new Insets(12));
        item.setStyle(
            "-fx-background-color: rgba(230, 118, 73, 0.1); " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: " + RICH_MIDTONE_RED + "; " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand;"
        );
        
        Label titleLabel = new Label("🔍 找不到想要的餐廳？");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: " + RICH_MIDTONE_RED + ";");
        
        Label descLabel = new Label("點擊這裡從 Google Maps 收集「" + originalQuery + "」的最新資料");
        descLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        descLabel.setStyle("-fx-text-fill: #666666;");
        descLabel.setWrapText(true);
        
        item.getChildren().addAll(titleLabel, descLabel);
        
        // 添加點擊事件
        item.setOnMouseClicked(e -> {
            selectRestaurant(originalQuery, "", "collection");
        });
        
        // 添加懸停效果
        item.setOnMouseEntered(e -> {
            item.setStyle(
                "-fx-background-color: rgba(230, 118, 73, 0.2); " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: " + RICH_MIDTONE_RED + "; " +
                "-fx-border-width: 2; " +
                "-fx-cursor: hand;"
            );
        });
        
        item.setOnMouseExited(e -> {
            item.setStyle(
                "-fx-background-color: rgba(230, 118, 73, 0.1); " +
                "-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: " + RICH_MIDTONE_RED + "; " +
                "-fx-border-width: 1; " +
                "-fx-cursor: hand;"
            );
        });
        
        return item;
    }
    
    /**
     * 顯示沒有結果找到
     */
    private void showNoResultsFound(String query) {
        showStatus("在資料庫中找不到「" + query + "」", false);
        
        // 清空建議容器
        suggestionsContainer.getChildren().clear();
        
        // 顯示收集選項
        VBox collectOption = createCollectionOption(query);
        suggestionsContainer.getChildren().add(collectOption);
        
        suggestionsContainer.setVisible(true);
    }
    
    /**
     * 選擇餐廳
     */
    private void selectRestaurant(String restaurantName, String restaurantId, String dataSource) {
        showStatus("正在載入「" + restaurantName + "」...", true);
        
        // 延遲一下讓用戶看到載入狀態
        Platform.runLater(() -> {
            if (callback != null) {
                callback.onRestaurantSelected(restaurantName, restaurantId, dataSource);
            }
        });
    }
    
    /**
     * 顯示狀態訊息
     */
    private void showStatus(String message, boolean showLoading) {
        statusLabel.setText(message);
        loadingIndicator.setVisible(showLoading);
    }
    
    /**
     * 清空建議
     */
    private void clearSuggestions() {
        suggestionsContainer.getChildren().clear();
        suggestionsContainer.setVisible(false);
    }
    
    /**
     * 顯示搜尋首頁
     */
    public void show() {
        Scene scene = new Scene(mainContainer, 1024, 768);
        
        // 載入CSS樣式
        try {
            scene.getStylesheets().add(getClass().getResource("/bigproject/modern_dark_theme.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("無法載入CSS樣式: " + e.getMessage());
        }
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("餐廳分析系統 - 搜尋");
        primaryStage.show();
        
        // 聚焦到搜尋欄
        Platform.runLater(() -> {
            searchField.requestFocus();
        });
    }
} 