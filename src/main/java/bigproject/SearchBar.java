package bigproject;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 搜尋欄元件，包含自動完成功能
 */
public class SearchBar extends HBox {
    // UI 元件
    private TextField searchField;
    private Button searchButton;
    private VBox suggestionsBox;
    private ScrollPane suggestionsScroll;
    private StackPane searchStackPane;
    
    // 回調函數
    private Consumer<String> onSearchHandler;
    
    /**
     * 建立搜尋欄
     * @param onSearchHandler 搜尋處理回調函數
     */
    public SearchBar(Consumer<String> onSearchHandler) {
        super(10);
        this.onSearchHandler = onSearchHandler;
        
        // 基本設置
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10, 20, 10, 20));
        setMaxWidth(Double.MAX_VALUE);
        setPrefWidth(Double.MAX_VALUE);
        setMinHeight(60);
        setStyle("-fx-background-color: rgba(58, 58, 58, 0.7); " +
                "-fx-background-radius: 30; " +
                "-fx-border-color: #E67649; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 30; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        setVisible(true);
        setManaged(true);
        
        initializeUI();
        setupEventHandlers();
    }
    
    /**
     * 初始化UI元件
     */
    private void initializeUI() {
        // 添加一個標籤，指示這是搜尋區域
        Label searchLabel = new Label("搜尋：");
        searchLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        // 創建圓角搜索框
        searchField = new TextField();
        searchField.setPromptText("請輸入關鍵字搜尋餐廳...");
        searchField.setPrefHeight(45);
        searchField.getStyleClass().add("search-history-field");
        searchField.setStyle("-fx-background-radius: 20; -fx-padding: 8 15 8 15; -fx-font-size: 18px; " +
                           "-fx-background-color: #1E1E1E; -fx-text-fill: white; " + 
                           "-fx-prompt-text-fill: #BBBBBB; -fx-border-color: #E67649; " + 
                           "-fx-border-width: 1.5px; -fx-border-radius: 20; " + 
                           "-fx-focus-color: #F08159; -fx-faint-focus-color: #F0815944;");
        
        // 強制固定搜索框寬度 - 增加寬度
        searchField.setPrefWidth(600);
        searchField.setMinWidth(600);
        searchField.setMaxWidth(600);
        searchField.setVisible(true);
        searchField.setManaged(true);
        searchField.setEditable(true);
        
        // 創建搜尋建議下拉選單容器和搜索框的組合
        searchStackPane = new StackPane();
        searchStackPane.setAlignment(Pos.TOP_LEFT);
        searchStackPane.setMaxWidth(600);
        searchStackPane.setPrefWidth(600);
        searchStackPane.setMinWidth(600);
        // 🔧 移除高度限制，讓建議選單能夠完整顯示
        // searchStackPane.setMaxHeight(45);
        // searchStackPane.setPrefHeight(45);
        searchStackPane.setVisible(true);
        searchStackPane.setManaged(true);
        
        // 建立搜尋建議下拉選單
        suggestionsBox = new VBox(4);
        suggestionsBox.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #E67649; " +
                              "-fx-border-width: 0 1.5 1.5 1.5; " +
                              "-fx-border-radius: 0 0 15 15; -fx-background-radius: 0 0 15 15; " + 
                              "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 5);");
        suggestionsBox.setVisible(false);
        suggestionsBox.setPrefWidth(600);
        suggestionsBox.setMinWidth(600);
        suggestionsBox.setMaxWidth(600);
        // 🔧 設定建議選單的偏好高度和最大高度
        suggestionsBox.setPrefHeight(500);
        suggestionsBox.setMaxHeight(500);
        
        // 創建滾動面板來包裹建議選單
        suggestionsScroll = new ScrollPane(suggestionsBox);
        suggestionsScroll.setFitToWidth(true);
        suggestionsScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        suggestionsScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        suggestionsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        // 🔧 設定滾動面板的偏好高度和最大高度
        suggestionsScroll.setPrefHeight(500);
        suggestionsScroll.setMaxHeight(500);
        suggestionsScroll.setVisible(false);
        suggestionsScroll.setPrefWidth(600);
        suggestionsScroll.setMaxWidth(600);
        suggestionsScroll.setMinWidth(600);
        
        suggestionsBox.setPadding(new Insets(5, 0, 5, 0));
        
        StackPane.setAlignment(suggestionsBox, Pos.TOP_LEFT);
        
        // 🔧 只將搜尋框添加到 StackPane，建議選單將作為浮動層添加到主容器
        searchStackPane.getChildren().add(searchField);
        searchStackPane.setPrefWidth(Double.MAX_VALUE);
        VBox.setMargin(searchStackPane, new Insets(0, 0, 5, 0));
        
        // 創建搜索按鈕
        searchButton = new Button("搜尋");
        searchButton.setPrefHeight(45);
        searchButton.setPrefWidth(100);
        searchButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; " + 
                             "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 18px; " +
                             "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);");
        
        // 添加左側間隔，確保搜索欄在中間顯示
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        
        // 添加右側間隔
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        
        // 將元件添加到容器中
        getChildren().add(leftSpacer);
        getChildren().add(searchLabel);
        getChildren().add(searchStackPane);
        getChildren().add(searchButton);
        getChildren().add(rightSpacer);
        
        // 🔧 將建議選單作為浮動層添加到 SearchBar 容器中
        getChildren().add(suggestionsScroll);
        
        // 🔧 關鍵設置：讓建議選單不參與佈局計算，成為真正的浮動元件
        suggestionsScroll.setManaged(false); // 不參與父容器的佈局計算
        suggestionsScroll.setViewOrder(-1); // 將建議選單置於最前方（z-index較高）
        suggestionsScroll.setMouseTransparent(false); // 確保可以接收滑鼠事件
        
        // 🔧 設置建議選單的絕對位置，讓它浮動在搜尋框下方
        // 計算搜尋框的起始位置（左間距 + 標籤 + 一些間距）
        suggestionsScroll.setLayoutX(120); // 大約搜尋框的起始位置
        suggestionsScroll.setLayoutY(70);  // 搜尋欄下方
        
        // 🔧 添加動態定位監聽器，當搜尋框位置改變時自動調整建議選單位置
        searchStackPane.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (suggestionsScroll.isVisible()) {
                // 計算搜尋框在搜尋欄中的實際位置
                double stackPaneX = searchStackPane.getLayoutX();
                double stackPaneY = searchStackPane.getLayoutY() + searchStackPane.getHeight();
                
                // 設置建議選單的位置對齊搜尋框
                suggestionsScroll.setLayoutX(stackPaneX);
                suggestionsScroll.setLayoutY(stackPaneY);
            }
        });
    }
    
    /**
     * 設置事件處理器
     */
    private void setupEventHandlers() {
        // 搜索按鈕鼠標懸停效果
        searchButton.setOnMouseEntered(e -> {
            searchButton.setStyle("-fx-background-color: #F08159; -fx-text-fill: white; " + 
                                 "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 18px; " + 
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 2);");
            searchButton.setCursor(Cursor.HAND);
        });
        
        searchButton.setOnMouseExited(e -> {
            searchButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; " + 
                                 "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 18px; " +
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);");
            searchButton.setCursor(Cursor.DEFAULT);
        });
        
        // 添加按下效果
        searchButton.setOnMousePressed(e -> {
            searchButton.setStyle("-fx-background-color: #D45E3A; -fx-text-fill: white; " + 
                                 "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 18px; " +
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 0);");
        });
        
        searchButton.setOnMouseReleased(e -> {
            searchButton.setStyle("-fx-background-color: #F08159; -fx-text-fill: white; " + 
                                 "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 18px; " + 
                                 "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        });
        
        // 搜索按鈕點擊事件
        searchButton.setOnAction(e -> {
            if (onSearchHandler != null) {
                onSearchHandler.accept(searchField.getText());
            }
        });
        
        // 搜索框按Enter鍵也觸發搜索
        searchField.setOnAction(e -> {
            if (onSearchHandler != null) {
                onSearchHandler.accept(searchField.getText());
            }
        });
        
        // 當聚焦時改變樣式
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) { // 獲得焦點
                searchField.setStyle("-fx-background-radius: 20; -fx-padding: 8 15 8 15; -fx-font-size: 18px; " +
                                   "-fx-background-color: #252525; -fx-text-fill: white; " + 
                                   "-fx-prompt-text-fill: #BBBBBB; -fx-border-color: #F08159; " + 
                                   "-fx-border-width: 2px; -fx-border-radius: 20; " + 
                                   "-fx-effect: dropshadow(gaussian, rgba(240,129,89,0.3), 10, 0, 0, 0); " + 
                                   "-fx-focus-color: #F08159; -fx-faint-focus-color: #F0815944;");
            } else { // 失去焦點
                searchField.setStyle("-fx-background-radius: 20; -fx-padding: 8 15 8 15; -fx-font-size: 18px; " +
                                   "-fx-background-color: #1E1E1E; -fx-text-fill: white; " + 
                                   "-fx-prompt-text-fill: #BBBBBB; -fx-border-color: #E67649; " + 
                                   "-fx-border-width: 1.5px; -fx-border-radius: 20; " + 
                                   "-fx-focus-color: #F08159; -fx-faint-focus-color: #F0815944;");
            }
        });
        
        // 自動搜尋的防抖動設計
        final java.util.Timer[] searchTimer = {null};
        final int DEBOUNCE_DELAY = 300;
        
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
                            JSONObject searchResult = new bigproject.search.AlgoliaRestaurantSearch().performSearch(newValue.trim(), true);
                            int hitsCount = searchResult.getInt("nbHits");
                            
                            if (hitsCount > 0) {
                                // 取得搜尋結果
                                JSONArray hits = searchResult.getJSONArray("hits");
                                int limit = Math.min(hits.length(), 10);
                                
                                // 🔧 根據實際項目數量動態調整建議選單高度
                                double itemHeight = 70.0;
                                double containerPadding = 10.0;
                                double calculatedHeight = Math.min(limit * itemHeight + containerPadding, 500.0);
                                
                                // 設定動態高度
                                suggestionsBox.setPrefHeight(calculatedHeight);
                                suggestionsBox.setMaxHeight(calculatedHeight);
                                suggestionsScroll.setPrefHeight(calculatedHeight);
                                suggestionsScroll.setMaxHeight(calculatedHeight);
                                
                                // 創建建議項目
                                for (int i = 0; i < limit; i++) {
                                    JSONObject hit = hits.getJSONObject(i);
                                    String restaurantName = hit.getString("name");
                                    String address = hit.optString("address", "");
                                    
                                    // 創建建議項
                                    HBox suggestionItem = createSuggestionItem(restaurantName, address);
                                    
                                    // 設置點擊事件
                                    suggestionItem.setOnMouseClicked(event -> {
                                        // 設置文字並立即執行搜尋
                                        searchField.setText(restaurantName);
                                        suggestionsScroll.setVisible(false);
                                        suggestionsBox.setVisible(false);
                                        
                                        // 先聚焦到搜尋框上，再執行搜尋
                                        searchField.requestFocus();
                                        Platform.runLater(() -> {
                                            if (onSearchHandler != null) {
                                                onSearchHandler.accept(restaurantName);
                                            }
                                        });
                                        
                                        event.consume();
                                    });
                                    
                                    suggestionsBox.getChildren().add(suggestionItem);
                                }
                                
                                // 顯示建議選單和滾動面板
                                suggestionsScroll.setVisible(true);
                                suggestionsBox.setVisible(true);
                                
                                // 🔧 確保建議選單位置正確
                                double stackPaneX = searchStackPane.getLayoutX();
                                double stackPaneY = searchStackPane.getLayoutY() + searchStackPane.getHeight();
                                suggestionsScroll.setLayoutX(stackPaneX);
                                suggestionsScroll.setLayoutY(stackPaneY);
                            } else {
                                suggestionsScroll.setVisible(false);
                                suggestionsBox.setVisible(false);
                            }
                        } catch (Exception e) {
                            System.err.println("獲取搜尋建議時發生錯誤: " + e.getMessage());
                            suggestionsScroll.setVisible(false);
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
                                JSONObject searchResult = new bigproject.search.AlgoliaRestaurantSearch().performSearch(searchField.getText().trim(), true);
                                showSuggestions(searchResult);
                            } catch (Exception e) {
                                System.err.println("獲取焦點時的搜尋建議錯誤: " + e.getMessage());
                            }
                        });
                    }
                }, 100);
            } else if (!newVal) {
                // 延遲隱藏，避免在點擊選項前隱藏選單
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            // 如果焦點不在搜索框上，隱藏建議
                            if (!searchField.isFocused()) {
                                suggestionsScroll.setVisible(false);
                                suggestionsBox.setVisible(false);
                            }
                        });
                    }
                }, 200);
            }
        });
    }
    
    /**
     * 創建一個搜尋建議項元素
     * @param name 餐廳名稱
     * @param address 餐廳地址
     * @return 建議項的HBox容器
     */
    private HBox createSuggestionItem(String name, String address) {
        HBox item = new HBox(12);
        item.setPadding(new Insets(18, 20, 18, 20));
        item.setStyle("-fx-background-color: #1E1E1E; -fx-cursor: hand; -fx-border-radius: 12; -fx-background-radius: 12;");
        
        item.setPrefWidth(560);
        item.setMinWidth(560);
        item.setMaxWidth(560);
        
        // 建立餐廳名稱標籤
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-font-size: 18px;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(500);
        
        // 建立地址標籤（如果有）
        VBox contentBox = new VBox(6);
        contentBox.setMaxWidth(500);
        contentBox.setPrefWidth(500);
        contentBox.getChildren().add(nameLabel);
        
        if (address != null && !address.isEmpty()) {
            Label addressLabel = new Label(address);
            addressLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #AAAAAA;");
            addressLabel.setWrapText(true);
            addressLabel.setMaxWidth(500);
            contentBox.getChildren().add(addressLabel);
        }
        
        // 左側圖標 - 使用餐廳圖標
        Label iconLabel = new Label("🍽️");
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #F08159;");
        iconLabel.setPrefWidth(38);
        
        item.getChildren().addAll(iconLabel, contentBox);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        
        // 懸停效果
        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: #2A2A2A; -fx-cursor: hand; " + 
                         "-fx-effect: dropshadow(gaussian, rgba(240,129,89,0.3), 8, 0, 0, 2); " + 
                         "-fx-border-radius: 12; -fx-background-radius: 12;");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F08159; -fx-font-size: 18px;");
        });
        
        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: #1E1E1E; -fx-cursor: hand; -fx-border-radius: 12; -fx-background-radius: 12;");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-font-size: 18px;");
        });
        
        // 點擊效果
        item.setOnMousePressed(e -> {
            item.setStyle("-fx-background-color: #333333; -fx-cursor: hand; " + 
                         "-fx-border-radius: 12; -fx-background-radius: 12;");
        });
        
        item.setOnMouseReleased(e -> {
            item.setStyle("-fx-background-color: #2A2A2A; -fx-cursor: hand; " + 
                         "-fx-effect: dropshadow(gaussian, rgba(240,129,89,0.3), 8, 0, 0, 2); " + 
                         "-fx-border-radius: 12; -fx-background-radius: 12;");
        });
        
        return item;
    }
    
    /**
     * 顯示搜尋建議
     * @param searchResult Algolia 搜尋結果
     */
    private void showSuggestions(JSONObject searchResult) {
        try {
            int hitsCount = searchResult.getInt("nbHits");
            
            if (hitsCount > 0) {
                // 取得搜尋結果
                JSONArray hits = searchResult.getJSONArray("hits");
                int limit = Math.min(hits.length(), 10);
                
                // 🔧 根據實際項目數量動態調整建議選單高度
                // 每個建議項約 70px 高度（包含 padding），加上容器的 padding
                double itemHeight = 70.0;
                double containerPadding = 10.0;
                double calculatedHeight = Math.min(limit * itemHeight + containerPadding, 500.0);
                
                // 設定動態高度
                suggestionsBox.setPrefHeight(calculatedHeight);
                suggestionsBox.setMaxHeight(calculatedHeight);
                suggestionsScroll.setPrefHeight(calculatedHeight);
                suggestionsScroll.setMaxHeight(calculatedHeight);
                
                // 創建建議項目
                for (int i = 0; i < limit; i++) {
                    JSONObject hit = hits.getJSONObject(i);
                    String restaurantName = hit.getString("name");
                    String address = hit.optString("address", "");
                    
                    // 創建建議項
                    HBox suggestionItem = createSuggestionItem(restaurantName, address);
                    
                    // 設置點擊事件
                    suggestionItem.setOnMouseClicked(event -> {
                        searchField.setText(restaurantName);
                        suggestionsScroll.setVisible(false);
                        suggestionsBox.setVisible(false);
                        if (onSearchHandler != null) {
                            onSearchHandler.accept(restaurantName);
                        }
                        event.consume(); // 確保事件不會傳播
                    });
                    
                    suggestionsBox.getChildren().add(suggestionItem);
                }
                
                // 顯示建議選單和滾動面板
                suggestionsScroll.setVisible(true);
                suggestionsBox.setVisible(true);
                
                // 🔧 確保建議選單位置正確
                double stackPaneX = searchStackPane.getLayoutX();
                double stackPaneY = searchStackPane.getLayoutY() + searchStackPane.getHeight();
                suggestionsScroll.setLayoutX(stackPaneX);
                suggestionsScroll.setLayoutY(stackPaneY);
                
                // 確保滾動條回到頂部
                suggestionsScroll.setVvalue(0);
            } else {
                suggestionsScroll.setVisible(false);
                suggestionsBox.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("顯示搜尋建議時發生錯誤: " + e.getMessage());
            suggestionsScroll.setVisible(false);
            suggestionsBox.setVisible(false);
        }
    }
    
    /**
     * 設置搜尋按鈕文字
     * @param text 按鈕文字
     */
    public void setSearchButtonText(String text) {
        searchButton.setText(text);
    }
    
    /**
     * 設置搜尋按鈕寬度
     * @param width 按鈕寬度
     */
    public void setSearchButtonWidth(double width) {
        searchButton.setPrefWidth(width);
    }
    
    /**
     * 獲取當前搜尋文字
     * @return 搜尋文字
     */
    public String getSearchText() {
        return searchField.getText();
    }
    
    /**
     * 設置搜尋文字
     * @param text 搜尋文字
     */
    public void setSearchText(String text) {
        searchField.setText(text);
    }
    
    /**
     * 獲取搜尋按鈕元件
     * @return 搜尋按鈕
     */
    public Button getSearchButton() {
        return searchButton;
    }
    
    /**
     * 獲取搜尋框元件
     * @return 搜尋框
     */
    public TextField getSearchField() {
        return searchField;
    }
    
    /**
     * 在瀏覽器中打開地圖搜尋
     * @param query 搜尋關鍵詞
     */
    public static void openMapInBrowser(String query) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String url = "https://www.google.com/maps/search/?api=1&query=" + encodedQuery;
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                System.err.println("無法開啟瀏覽器顯示地圖: " + ex.getMessage());
            }
        } else {
            System.err.println("此平台不支援開啟瀏覽器");
        }
    }
} 