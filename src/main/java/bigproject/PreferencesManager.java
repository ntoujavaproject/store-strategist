package bigproject;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * 管理應用程式偏好設定並提供設定界面視圖
 */
public class PreferencesManager {
    
    // 保存的設定鍵
    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_FONT_SIZE = "fontSize";
    private static final String KEY_DARK_MODE = "darkMode";
    
    // 預設值
    private static final String DEFAULT_FONT_FAMILY = "System";
    private static final double DEFAULT_FONT_SIZE = 12.0;
    private static final boolean DEFAULT_DARK_MODE = true;

    // 可用字體列表
    private static final List<String> AVAILABLE_FONTS = Arrays.asList(
        "System", "Serif", "SansSerif", "Monospaced", "Dialog"
    );
    
    // 可用字體大小
    private static final List<Double> AVAILABLE_FONT_SIZES = Arrays.asList(
        10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0
    );
    
    // 界面元素
    private final Stage primaryStage;
    private final BorderPane mainLayout;
    private final Preferences prefs;
    private final UIManager uiManager;
    
    // 設定視圖元素
    private VBox settingsContainer;
    private ScrollPane settingsScrollPane;
    private boolean isSettingsViewShowing = false;
    
    // 設定狀態監聽器接口
    public interface SettingsStateChangeListener {
        void onSettingsStateChanged(boolean isShowing);
    }
    
    private SettingsStateChangeListener stateChangeListener;
    
    /**
     * 建構函數
     */
    public PreferencesManager(Stage primaryStage, BorderPane mainLayout, UIManager uiManager) {
        this.primaryStage = primaryStage;
        this.mainLayout = mainLayout;
        this.uiManager = uiManager;
        this.prefs = Preferences.userNodeForPackage(PreferencesManager.class);
    }
    
    /**
     * 設置狀態變更監聽器
     */
    public void setStateChangeListener(SettingsStateChangeListener listener) {
        this.stateChangeListener = listener;
    }
    
    /**
     * 切換顯示設定視圖
     */
    public void toggleSettingsView() {
        if (isSettingsViewShowing) {
            // 目前正在顯示設定，切換回主視圖
            uiManager.showMainView();
            isSettingsViewShowing = false;
        } else {
            // 目前顯示主視圖，切換到設定視圖
            showSettingsView();
            isSettingsViewShowing = true;
        }
        
        // 通知監聽器狀態變化
        if (stateChangeListener != null) {
            stateChangeListener.onSettingsStateChanged(isSettingsViewShowing);
        }
    }
    
    /**
     * 顯示設定視圖
     */
    private void showSettingsView() {
        // 創建設定容器
        settingsContainer = new VBox(20);
        settingsContainer.setId("settings-container");
        settingsContainer.setPadding(new Insets(30));
        settingsContainer.setAlignment(Pos.TOP_CENTER);
        settingsContainer.setStyle("-fx-background-color: #2C2C2C;");
        
        // 標題
        Label titleLabel = new Label("應用程式設定");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: white;");
        
        // 添加各個設定區塊
        VBox fontSection = createFontSection();
        VBox appearanceSection = createAppearanceSection();
        
        // 建立底部按鈕區
        HBox buttonBar = new HBox(15);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(20, 0, 0, 0));
        
        Button saveButton = new Button("套用");
        saveButton.setStyle("-fx-background-color: #E67649; -fx-text-fill: white; -fx-padding: 8 20 8 20;");
        saveButton.setOnAction(e -> saveSettings());
        
        Button cancelButton = new Button("取消");
        cancelButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-padding: 8 20 8 20;");
        cancelButton.setOnAction(e -> cancelSettings());
        
        buttonBar.getChildren().addAll(cancelButton, saveButton);
        
        // 組裝設定界面
        settingsContainer.getChildren().addAll(
            titleLabel,
            createSeparator(),
            fontSection,
            createSeparator(),
            appearanceSection,
            createSeparator(),
            buttonBar
        );
        
        // 創建滾動面板
        settingsScrollPane = new ScrollPane(settingsContainer);
        settingsScrollPane.setFitToWidth(true);
        settingsScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        settingsScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        // 顯示設定視圖
        mainLayout.setCenter(settingsScrollPane);
        System.out.println("切換到設定視圖");
    }
    
    /**
     * 創建字體設定區塊
     */
    private VBox createFontSection() {
        VBox section = new VBox(15);
        section.setAlignment(Pos.TOP_LEFT);
        
        Label sectionTitle = new Label("字體設定");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        sectionTitle.setStyle("-fx-text-fill: #E67649;");
        
        // 字體選擇
        HBox fontFamilyBox = new HBox(10);
        fontFamilyBox.setAlignment(Pos.CENTER_LEFT);
        
        Label fontFamilyLabel = new Label("字型：");
        fontFamilyLabel.setMinWidth(100);
        fontFamilyLabel.setStyle("-fx-text-fill: white;");
        
        ComboBox<String> fontFamilyCombo = new ComboBox<>();
        fontFamilyCombo.getItems().addAll(AVAILABLE_FONTS);
        fontFamilyCombo.setValue(prefs.get(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY));
        fontFamilyCombo.setId("fontFamilyCombo");
        fontFamilyCombo.setPrefWidth(250);
        
        fontFamilyBox.getChildren().addAll(fontFamilyLabel, fontFamilyCombo);
        
        // 字體大小
        HBox fontSizeBox = new HBox(10);
        fontSizeBox.setAlignment(Pos.CENTER_LEFT);
        
        Label fontSizeLabel = new Label("大小：");
        fontSizeLabel.setMinWidth(100);
        fontSizeLabel.setStyle("-fx-text-fill: white;");
        
        ComboBox<Double> fontSizeCombo = new ComboBox<>();
        fontSizeCombo.getItems().addAll(AVAILABLE_FONT_SIZES);
        fontSizeCombo.setValue(prefs.getDouble(KEY_FONT_SIZE, DEFAULT_FONT_SIZE));
        fontSizeCombo.setId("fontSizeCombo");
        fontSizeCombo.setPrefWidth(250);
        
        fontSizeBox.getChildren().addAll(fontSizeLabel, fontSizeCombo);
        
        // 預覽
        Label previewLabel = new Label("預覽：");
        previewLabel.setMinWidth(100);
        previewLabel.setStyle("-fx-text-fill: white;");
        
        Label previewText = new Label("這是字體預覽文字 This is preview text 123");
        previewText.setStyle("-fx-text-fill: white; -fx-background-color: #3A3A3A; -fx-padding: 10; -fx-background-radius: 5;");
        previewText.setFont(Font.font(
            fontFamilyCombo.getValue(),
            fontSizeCombo.getValue()
        ));
        previewText.setMinHeight(60);
        previewText.setPrefWidth(400);
        
        // 當字體設定變化時更新預覽
        fontFamilyCombo.setOnAction(e -> 
            previewText.setFont(Font.font(
                fontFamilyCombo.getValue(),
                fontSizeCombo.getValue()
            ))
        );
        
        fontSizeCombo.setOnAction(e -> 
            previewText.setFont(Font.font(
                fontFamilyCombo.getValue(),
                fontSizeCombo.getValue()
            ))
        );
        
        HBox previewBox = new HBox(10);
        previewBox.setAlignment(Pos.TOP_LEFT);
        previewBox.getChildren().addAll(previewLabel, previewText);
        
        section.getChildren().addAll(sectionTitle, fontFamilyBox, fontSizeBox, previewBox);
        return section;
    }
    
    /**
     * 創建外觀設定區塊
     */
    private VBox createAppearanceSection() {
        VBox section = new VBox(15);
        section.setAlignment(Pos.TOP_LEFT);
        
        Label sectionTitle = new Label("外觀設定");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        sectionTitle.setStyle("-fx-text-fill: #E67649;");
        
        // 深色模式切換
        HBox darkModeBox = new HBox(10);
        darkModeBox.setAlignment(Pos.CENTER_LEFT);
        
        Label darkModeLabel = new Label("深色模式：");
        darkModeLabel.setMinWidth(100);
        darkModeLabel.setStyle("-fx-text-fill: white;");
        
        ToggleButton darkModeToggle = new ToggleButton();
        darkModeToggle.setSelected(prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE));
        darkModeToggle.setText(darkModeToggle.isSelected() ? "開啟" : "關閉");
        darkModeToggle.setId("darkModeToggle");
        
        darkModeToggle.setOnAction(e -> 
            darkModeToggle.setText(darkModeToggle.isSelected() ? "開啟" : "關閉")
        );
        
        darkModeBox.getChildren().addAll(darkModeLabel, darkModeToggle);
        
        section.getChildren().addAll(sectionTitle, darkModeBox);
        return section;
    }
    
    /**
     * 創建分隔線
     */
    private HBox createSeparator() {
        HBox separator = new HBox();
        separator.setStyle("-fx-background-color: #444444; -fx-min-height: 1px; -fx-max-height: 1px;");
        separator.setPrefHeight(1);
        separator.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(separator, Priority.ALWAYS);
        separator.setPadding(new Insets(10, 0, 10, 0));
        return separator;
    }
    
    /**
     * 儲存設定
     */
    private void saveSettings() {
        // 獲取設定值
        ComboBox<String> fontFamilyCombo = (ComboBox<String>) settingsContainer.lookup("#fontFamilyCombo");
        ComboBox<Double> fontSizeCombo = (ComboBox<Double>) settingsContainer.lookup("#fontSizeCombo");
        ToggleButton darkModeToggle = (ToggleButton) settingsContainer.lookup("#darkModeToggle");
        
        if (fontFamilyCombo != null && fontSizeCombo != null && darkModeToggle != null) {
            // 保存設定
            String fontFamily = fontFamilyCombo.getValue();
            double fontSize = fontSizeCombo.getValue();
            boolean darkMode = darkModeToggle.isSelected();
            
            // 儲存到 Preferences
            prefs.put(KEY_FONT_FAMILY, fontFamily);
            prefs.putDouble(KEY_FONT_SIZE, fontSize);
            prefs.putBoolean(KEY_DARK_MODE, darkMode);
            
            // 更新UI
            uiManager.updateFontStyle(fontFamily, fontSize);
            uiManager.updateTheme(darkMode);
            
            System.out.println("設定已儲存: 字體=" + fontFamily + ", 大小=" + fontSize + ", 深色模式=" + darkMode);
        }
        
        // 回到主視圖
        toggleSettingsView();
    }
    
    /**
     * 取消設定
     */
    private void cancelSettings() {
        // 直接回到主視圖，不儲存設定
        toggleSettingsView();
    }
    
    /**
     * 返回設定是否正在顯示
     */
    public boolean isSettingsViewShowing() {
        return isSettingsViewShowing;
    }
    
    /**
     * 獲取當前字體
     */
    public String getFontFamily() {
        return prefs.get(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY);
    }
    
    /**
     * 獲取當前字體大小
     */
    public double getFontSize() {
        return prefs.getDouble(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    
    /**
     * 獲取當前主題模式
     */
    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE);
    }
} 