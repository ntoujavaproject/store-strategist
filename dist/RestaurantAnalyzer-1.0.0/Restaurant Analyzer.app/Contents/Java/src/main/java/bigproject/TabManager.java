package bigproject;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.HashMap;
import java.util.Map;

/**
 * 分頁管理器
 * 負責處理應用程式中所有分頁相關的邏輯
 */
public class TabManager {
    
    // 分頁相關成員變數
    private Map<String, TabContent> tabContents = new HashMap<>();
    private String currentTabId = null;
    private HBox tabBar;
    private Stage parentStage;
    
    // 回調接口
    private TabChangeListener tabChangeListener;
    
    /**
     * 分頁變更監聽器接口
     */
    public interface TabChangeListener {
        /**
         * 當分頁被選中時調用
         * @param tabId 分頁ID
         * @param tabContent 分頁內容
         */
        void onTabSelected(String tabId, TabContent tabContent);
        
        /**
         * 當分頁被關閉時調用
         * @param tabId 分頁ID
         */
        void onTabClosed(String tabId);
        
        /**
         * 當新分頁被創建時調用
         * @param tabId 分頁ID
         * @param tabContent 分頁內容
         */
        void onTabCreated(String tabId, TabContent tabContent);
    }
    
    /**
     * 分頁內容類
     */
    public static class TabContent {
        public String id;
        public String displayName;
        public String jsonFilePath;
        public HBox tabBox;
        public String medianExpense;
        
        public TabContent(String id, String displayName, String jsonFilePath, HBox tabBox) {
            this.id = id;
            this.displayName = displayName;
            this.jsonFilePath = jsonFilePath;
            this.tabBox = tabBox;
            this.medianExpense = "未知";
        }
    }
    
    /**
     * 建構子
     * @param parentStage 父視窗
     * @param tabChangeListener 分頁變更監聽器
     */
    public TabManager(Stage parentStage, TabChangeListener tabChangeListener) {
        this.parentStage = parentStage;
        this.tabChangeListener = tabChangeListener;
        initializeTabBar();
    }
    
    /**
     * 初始化分頁欄
     */
    private void initializeTabBar() {
        tabBar = new HBox(5);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(5, 10, 0, 10));
        tabBar.setStyle("-fx-background-color: rgba(42, 42, 42, 0.85); -fx-border-width: 2 0 0 0; -fx-border-color: #E67649; -fx-padding: 5 10 0 10;");
        tabBar.setMinHeight(45);
        tabBar.setPrefHeight(45);
        tabBar.setMaxHeight(45);
        tabBar.setPrefWidth(Double.MAX_VALUE);
        tabBar.setMaxWidth(Double.MAX_VALUE);
        tabBar.setSnapToPixel(true);
        
        // 添加 "+" 按鈕用於創建新分頁
        Button addTabButton = new Button("+");
        addTabButton.setStyle("-fx-background-color: #444444; -fx-text-fill: white;");
        addTabButton.setOnAction(e -> showAddTabDialog());
        
        tabBar.getChildren().add(addTabButton);
    }
    
    /**
     * 獲取分頁欄
     * @return 分頁欄 HBox
     */
    public HBox getTabBar() {
        return tabBar;
    }
    
    /**
     * 獲取當前選中的分頁ID
     * @return 當前分頁ID
     */
    public String getCurrentTabId() {
        return currentTabId;
    }
    
    /**
     * 獲取當前選中的分頁內容
     * @return 當前分頁內容
     */
    public TabContent getCurrentTabContent() {
        return currentTabId != null ? tabContents.get(currentTabId) : null;
    }
    
    /**
     * 獲取所有分頁內容
     * @return 分頁內容映射
     */
    public Map<String, TabContent> getAllTabContents() {
        return new HashMap<>(tabContents);
    }
    
    /**
     * 創建歡迎分頁
     */
    public void createWelcomeTab() {
        String tabId = "welcome_tab";
        HBox tabBox = createTabButton(tabId, "歡迎", null);
        
        // 添加到分頁欄（在 + 按鈕之前）
        tabBar.getChildren().add(tabBar.getChildren().size() - 1, tabBox);
        
        // 創建並存儲分頁內容
        TabContent content = new TabContent(tabId, "歡迎", null, tabBox);
        content.medianExpense = "未知";
        tabContents.put(tabId, content);
        
        // 選擇新創建的分頁
        selectTab(tabId);
        
        System.out.println("創建了歡迎分頁");
    }
    
    /**
     * 顯示添加新分頁的對話框
     */
    private void showAddTabDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("新增分頁");
        
        VBox dialogVBox = new VBox(10);
        dialogVBox.setPadding(new Insets(15));
        dialogVBox.setAlignment(Pos.CENTER);
        
        Label nameLabel = new Label("請先搜尋餐廳，然後點擊「新增分頁」來添加分析標籤");
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-text-alignment: center; -fx-font-size: 14px;");
        
        // 🚫 移除硬編碼的餐廳清單 - 讓用戶通過搜尋功能來新增分頁
        Label instructionLabel = new Label("💡 使用方法：\n1. 在搜尋欄輸入餐廳名稱\n2. 點擊搜尋結果\n3. 系統會自動為您創建新的分析分頁");
        instructionLabel.setWrapText(true);
        instructionLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic; -fx-text-alignment: center; -fx-padding: 20;");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button closeButton = new Button("知道了");
        closeButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 8 20;");
        
        closeButton.setOnAction(e -> dialog.close());
        
        buttonBox.getChildren().add(closeButton);
        dialogVBox.getChildren().addAll(nameLabel, instructionLabel, buttonBox);
        
        Scene dialogScene = new Scene(dialogVBox, 300, 250);
        dialog.setScene(dialogScene);
        dialog.show();
    }
    
    /**
     * 創建新的分頁
     * @param displayName 顯示名稱
     * @param jsonFilePath JSON檔案路徑
     */
    public void createNewTab(String displayName, String jsonFilePath) {
        // 檢查是否已存在相同標題的分頁
        for (TabContent tab : tabContents.values()) {
            if (tab.displayName.equals(displayName)) {
                selectTab(tab.id);
                return;
            }
        }
        
        // 生成唯一ID
        String tabId = "tab_" + System.currentTimeMillis();
        
        // 創建分頁按鈕
        HBox tabBox = createTabButton(tabId, displayName, jsonFilePath);
        
        // 添加到分頁欄（在 + 按鈕之前）
        tabBar.getChildren().add(tabBar.getChildren().size() - 1, tabBox);
        
        // 創建並存儲分頁內容
        TabContent content = new TabContent(tabId, displayName, jsonFilePath, tabBox);
        tabContents.put(tabId, content);
        
        // 通知監聽器
        if (tabChangeListener != null) {
            tabChangeListener.onTabCreated(tabId, content);
        }
        
        // 選擇新創建的分頁
        selectTab(tabId);
        
        System.out.println("創建了新分頁: " + displayName);
    }
    
    /**
     * 創建分頁按鈕
     * @param tabId 分頁ID
     * @param displayName 顯示名稱
     * @param jsonFilePath JSON檔案路徑
     * @return 分頁按鈕容器
     */
    private HBox createTabButton(String tabId, String displayName, String jsonFilePath) {
        HBox tabBox = new HBox(5);
        tabBox.setAlignment(Pos.CENTER_LEFT);
        tabBox.setPadding(new Insets(8, 15, 8, 15));
        tabBox.setStyle("-fx-background-color: #333333; -fx-background-radius: 5 5 0 0; -fx-cursor: hand;");
        tabBox.setMinHeight(30);
        tabBox.setId(tabId);
        
        Label tabLabel = new Label(displayName);
        tabLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
        
        Button closeButton = new Button("×");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCCCCC; -fx-padding: 0 0 0 5; -fx-font-size: 16px;");
        closeButton.setOnAction(e -> closeTab(tabId));
        
        // 添加懸停效果
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-padding: 0 0 0 5;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCCCCC; -fx-padding: 0 0 0 5;"));
        
        tabBox.getChildren().addAll(tabLabel, closeButton);
        
        // 點擊分頁切換
        tabBox.setOnMouseClicked(e -> selectTab(tabId));
        
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
     * @param tabId 要選擇的分頁ID
     */
    public void selectTab(String tabId) {
        TabContent tab = tabContents.get(tabId);
        if (tab == null) return;
        
        String previousTabId = currentTabId;
        
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
        
        // 更新當前分頁ID
        currentTabId = tabId;
        
        // 通知監聽器
        if (tabChangeListener != null) {
            tabChangeListener.onTabSelected(tabId, tab);
        }
        
        System.out.println("選擇分頁: " + tab.displayName);
    }
    
    /**
     * 關閉分頁
     * @param tabId 要關閉的分頁ID
     */
    public void closeTab(String tabId) {
        TabContent tab = tabContents.get(tabId);
        if (tab == null) return;
        
        // 不允許關閉最後一個分頁
        if (tabContents.size() <= 1) {
            return;
        }
        
        // 從UI移除
        tabBar.getChildren().remove(tab.tabBox);
        
        // 通知監聽器
        if (tabChangeListener != null) {
            tabChangeListener.onTabClosed(tabId);
        }
        
        // 從存儲中移除
        tabContents.remove(tabId);
        
        // 如果關閉的是當前分頁，則選擇另一個分頁
        if (tabId.equals(currentTabId)) {
            String nextTabId = tabContents.keySet().iterator().next();
            selectTab(nextTabId);
        }
        
        System.out.println("關閉分頁: " + tab.displayName);
    }
    
    /**
     * 獲取分頁數量
     * @return 分頁數量
     */
    public int getTabCount() {
        return tabContents.size();
    }
    
    /**
     * 檢查是否有指定的分頁
     * @param tabId 分頁ID
     * @return 是否存在
     */
    public boolean hasTab(String tabId) {
        return tabContents.containsKey(tabId);
    }
    
    /**
     * 清除所有分頁
     */
    public void clearAllTabs() {
        // 清除UI
        tabBar.getChildren().clear();
        
        // 重新添加 + 按鈕
        Button addTabButton = new Button("+");
        addTabButton.setStyle("-fx-background-color: #444444; -fx-text-fill: white;");
        addTabButton.setOnAction(e -> showAddTabDialog());
        tabBar.getChildren().add(addTabButton);
        
        // 清除數據
        tabContents.clear();
        currentTabId = null;
        
        System.out.println("已清除所有分頁");
    }
    
    /**
     * 更新分頁的平均消費資訊
     * @param tabId 分頁ID
     * @param medianExpense 平均消費
     */
    public void updateTabMedianExpense(String tabId, String medianExpense) {
        TabContent tab = tabContents.get(tabId);
        if (tab != null) {
            tab.medianExpense = medianExpense;
            System.out.println("更新分頁 " + tab.displayName + " 的平均消費: " + medianExpense);
        }
    }
} 