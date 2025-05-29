package bigproject;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 青蘋果欄 - 右側面板組件
 * 包含評分、資料來源、近期評論和餐廳分析等區塊
 */
public class RightPanel extends VBox {
    
    // 顏色設定
    private static final String PALE_DARK_YELLOW = "#6F6732";
    private static final String RICH_MIDTONE_RED = "#E67649";
    private static final String RICH_LIGHT_GREEN = "#DCF2CC";  // 青蘋果色
    
    // UI 元件
    private Label ratingsHeader;
    private VBox ratingsBox;
    private Map<String, ProgressBar> ratingBars;


    
    // 分析區塊的 TextArea
    private TextArea featuresArea;
    
    // 🤖 AI 對話相關元件（添加在特色區域下方）
    private bigproject.ai.ChatRestaurantAdvisor chatAdvisor;
    
    // 父視窗參考
    private compare parentComponent;
    
    // 新增最新評論管理器
    private LatestReviewsManager reviewsManager;
    
    // 當前顯示的餐廳JSON檔案
    private String currentJsonFilePath;
    
    // 當前搜尋到的餐廳資訊
    private String currentRestaurantName;
    private String currentRestaurantId;  
    private String currentPlaceId;
    
    // 近期評論詳細視圖相關
    private boolean isReviewDetailMode = false;
    private String originalFeaturesContent = "";
    private Button monthButton, weekButton, dayButton;
    private int currentSelectedDays = 30; // 預設為近一個月
    
    /**
     * 建構函數
     * @param parentComponent 父元件參考，用於獲取AIChat等功能
     */
    public RightPanel(compare parentComponent) {
        super(15);  // 使用 15 像素的垂直間距
        this.parentComponent = parentComponent;
        
        // 初始化最新評論管理器，使用 API Key
        this.reviewsManager = new LatestReviewsManager("AIzaSyAfssp2jChrVBpRPFuAhBE6f6kXYDQaV0I");
        
        // 🎯 設置深色主題面板樣式
        setStyle("-fx-background-color: linear-gradient(to bottom, #1A1A1A 0%, #2C2C2C 100%); -fx-background-radius: 0;");
        setPadding(new Insets(15, 0, 0, 15));  // 🎯 上15px、右0px、底0px、左15px
        // 🎯 移除固定寬度設置，改為響應式寬度（將由父容器控制）
        // setPrefWidth(450);  // 移除固定寬度
        // setMinWidth(450);   // 移除固定最小寬度  
        // setMaxWidth(450);   // 移除固定最大寬度
        setMinHeight(3000); // 確保所有內容可滾動
        setPrefHeight(3500);
        
        // 初始化面板元素
        initializeComponents();
    }
    
    /**
     * 初始化所有面板元素
     */
    private void initializeComponents() {
        // 評分區域
        initializeRatingsSection();
        
        // 餐廳分析區塊
        initializeAnalysisSection();
        
        // 移除底部空間，讓內容直接貼到底部
        // Region spacer = new Region();
        // spacer.setMinHeight(200);
        // spacer.setPrefHeight(200);
        // getChildren().add(spacer);
    }
    
    // 添加評分數值標籤的映射
    private Map<String, Label> ratingValueLabels;
    
    // 消費中位數相關元件
    private Label medianExpenseValueLabel;
    
    /**
     * 初始化評分區域
     */
    private void initializeRatingsSection() {
        ratingsHeader = new Label("綜合評分");
        ratingsHeader.setFont(Font.font("System", FontWeight.BOLD, 18));
        ratingsHeader.setStyle("-fx-text-fill: #FFFFFF; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 4, 0, 0, 2);");
        
        // 🎯 深色主題容器
        ratingsBox = new VBox(12); // 增加間距
        ratingsBox.setPadding(new Insets(20, 15, 20, 15));
        ratingsBox.setStyle("-fx-background-color: linear-gradient(to bottom, #1E1E1E 0%, #2A2A2A 100%); " +
                           "-fx-background-radius: 12; " +
                           "-fx-border-color: linear-gradient(to right, #FF6B6B, #4ECDC4, #45B7D1, #96CEB4); " +
                           "-fx-border-width: 3; " +
                           "-fx-border-radius: 12; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 3);");
        
        // 初始化評分條和數值標籤
        ratingBars = new HashMap<>();
        ratingValueLabels = new HashMap<>();
        String[] categories = {"餐點", "服務", "環境", "價格"};
        String[] icons = {"🍽️", "👥", "🏪", "💰"}; 
        // 🎨 繽紛的漸層顏色
        String[] barColors = {
            "linear-gradient(to right, #FF6B6B 0%, #FF8E8E 100%)", // 紅色漸層
            "linear-gradient(to right, #4ECDC4 0%, #7FDBDA 100%)", // 青綠色漸層
            "linear-gradient(to right, #45B7D1 0%, #74C0FC 100%)", // 藍色漸層
            "linear-gradient(to right, #96CEB4 0%, #B8E6C1 100%)"  // 綠色漸層
        };
        String[] shadowColors = {"rgba(255, 107, 107, 0.5)", "rgba(78, 205, 196, 0.5)", "rgba(69, 183, 209, 0.5)", "rgba(150, 206, 180, 0.5)"};
        
        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];
            String icon = icons[i];
            String barColor = barColors[i];
            String shadowColor = shadowColors[i];
            
            // 🎯 創建每個評分項目的容器 - 深色主題
            VBox ratingItemBox = new VBox(8);
            ratingItemBox.setPadding(new Insets(15, 20, 15, 20));
            ratingItemBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2D2D2D 0%, #3A3A3A 100%); " +
                                  "-fx-background-radius: 8; " +
                                  "-fx-border-color: rgba(255,255,255,0.1); " +
                                  "-fx-border-width: 1; " +
                                  "-fx-border-radius: 8;");
            
            // 頂部：類別名稱和評分數值
            HBox topRow = new HBox();
            topRow.setAlignment(Pos.CENTER_LEFT);
            
            // 類別標籤（包含圖標）- 深色主題
            Label catLabel = new Label(icon + " " + category);
            catLabel.setMinWidth(80);
            catLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            catLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 2, 0, 0, 1);");
            
            // 佔位空間
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            // 🎯 評分數值標籤 - 深色主題繽紛設計
            Label valueLabel = new Label("0.0");
            valueLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
            valueLabel.setStyle("-fx-text-fill: #FFFFFF; " +
                              "-fx-background-color: " + barColor + "; " +
                              "-fx-background-radius: 18; " +
                              "-fx-padding: 6 15 6 15; " +
                              "-fx-border-color: rgba(255,255,255,0.3); " +
                              "-fx-border-width: 1.5; " +
                              "-fx-border-radius: 18; " +
                              "-fx-effect: dropshadow(three-pass-box, " + shadowColor + ", 5, 0, 0, 2);");
            ratingValueLabels.put(category, valueLabel);
            
            topRow.getChildren().addAll(catLabel, spacer, valueLabel);
            
            // 🎯 底部：柱狀圖替代進度條
            VBox barContainer = new VBox();
            barContainer.setAlignment(Pos.BOTTOM_LEFT);
            barContainer.setMinHeight(40);
            barContainer.setMaxHeight(40);
            barContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); " +
                                 "-fx-background-radius: 6; " +
                                 "-fx-border-color: rgba(255,255,255,0.1); " +
                                 "-fx-border-width: 1; " +
                                 "-fx-border-radius: 6;");
            
            // 創建一個柱狀區域
            Region barFill = new Region();
            barFill.setMinHeight(0); // 初始高度為0
            barFill.setMaxWidth(Double.MAX_VALUE);
            barFill.setStyle("-fx-background-color: " + barColor + "; " +
                           "-fx-background-radius: 5; " +
                           "-fx-effect: dropshadow(three-pass-box, " + shadowColor + ", 4, 0, 0, 1);");
            
            // 將柱狀條放在容器底部
            VBox.setVgrow(barFill, Priority.NEVER);
            barContainer.getChildren().add(barFill);
            
            // 儲存進度條參考（這裡用 Region 模擬）
            ProgressBar fakeProgressBar = new ProgressBar(0.0);
            fakeProgressBar.setVisible(false); // 隱藏原始進度條
            ratingBars.put(category, fakeProgressBar);
            
            // 🎯 添加豪華的 hover 效果 - 深色主題
            ratingItemBox.setOnMouseEntered(e -> {
                ratingItemBox.setStyle("-fx-background-color: linear-gradient(to bottom, #3A3A3A 0%, #4A4A4A 100%); " +
                                     "-fx-background-radius: 8; " +
                                     "-fx-border-color: rgba(255,255,255,0.3); " +
                                     "-fx-border-width: 2; " +
                                     "-fx-border-radius: 8; " +
                                     "-fx-effect: dropshadow(three-pass-box, " + shadowColor + ", 12, 0, 0, 3);");
                // 柱狀圖容器也要有 hover 效果
                barContainer.setStyle("-fx-background-color: rgba(0,0,0,0.5); " +
                                    "-fx-background-radius: 6; " +
                                    "-fx-border-color: rgba(255,255,255,0.2); " +
                                    "-fx-border-width: 1; " +
                                    "-fx-border-radius: 6;");
            });
            
            ratingItemBox.setOnMouseExited(e -> {
                ratingItemBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2D2D2D 0%, #3A3A3A 100%); " +
                                     "-fx-background-radius: 8; " +
                                     "-fx-border-color: rgba(255,255,255,0.1); " +
                                     "-fx-border-width: 1; " +
                                     "-fx-border-radius: 8;");
                // 恢復原始的柱狀圖容器樣式
                barContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); " +
                                    "-fx-background-radius: 6; " +
                                    "-fx-border-color: rgba(255,255,255,0.1); " +
                                    "-fx-border-width: 1; " +
                                    "-fx-border-radius: 6;");
            });
            
            ratingItemBox.getChildren().addAll(topRow, barContainer);
            ratingsBox.getChildren().add(ratingItemBox);
            
            // 🎯 在項目之間添加繽紛分隔線（除了最後一個）
            if (i < categories.length - 1) {
                Separator separator = new Separator();
                separator.setStyle("-fx-background-color: linear-gradient(to right, " + 
                                 "#FF6B6B 0%, #4ECDC4 25%, #45B7D1 50%, #96CEB4 75%, #FF6B6B 100%); " +
                                 "-fx-opacity: 0.6; " +
                                 "-fx-padding: 2 0 2 0;");
                ratingsBox.getChildren().add(separator);
            }
            
            // 🎯 儲存柱狀條參考以便後續更新
            barFill.setUserData(category + "_bar");
        }
        
        // 💰 在評分區域前添加消費中位數區域
        VBox medianExpenseSection = createMedianExpenseSection();
        
        getChildren().addAll(ratingsHeader, medianExpenseSection, ratingsBox);
    }
    
    /**
     * 創建消費中位數區域
     */
    private VBox createMedianExpenseSection() {
        VBox medianExpenseBox = new VBox(8);
        medianExpenseBox.setPadding(new Insets(15, 15, 15, 15));
        medianExpenseBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2A2A2A 0%, #3A3A3A 100%); " +
                                 "-fx-background-radius: 10; " +
                                 "-fx-border-color: linear-gradient(to right, #FFD700, #FFA500); " +
                                 "-fx-border-width: 2; " +
                                 "-fx-border-radius: 10; " +
                                 "-fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.3), 8, 0, 0, 2);");
        
        // 消費中位數標題
        Label titleLabel = new Label("💰 平均消費中位數");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: #FFD700; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 3, 0, 0, 1);");
        
        // 消費數值容器
        HBox valueContainer = new HBox(10);
        valueContainer.setAlignment(Pos.CENTER);
        
        // 金錢圖標
        Label iconLabel = new Label("💵");
        iconLabel.setFont(Font.font(20));
        iconLabel.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 2, 0, 0, 1);");
        
        // 消費數值標籤
        medianExpenseValueLabel = new Label("載入中...");
        medianExpenseValueLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        medianExpenseValueLabel.setStyle("-fx-text-fill: #FFFFFF; " +
                                        "-fx-background-color: linear-gradient(to right, #FFD700 0%, #FFA500 100%); " +
                                        "-fx-background-radius: 15; " +
                                        "-fx-padding: 8 20 8 20; " +
                                        "-fx-border-color: rgba(255,255,255,0.3); " +
                                        "-fx-border-width: 1; " +
                                        "-fx-border-radius: 15; " +
                                        "-fx-effect: dropshadow(three-pass-box, rgba(255,165,0,0.4), 5, 0, 0, 2);");
        
        valueContainer.getChildren().addAll(iconLabel, medianExpenseValueLabel);
        medianExpenseBox.getChildren().addAll(titleLabel, valueContainer);
        
        // 添加懸停效果
        medianExpenseBox.setOnMouseEntered(e -> {
            medianExpenseBox.setStyle("-fx-background-color: linear-gradient(to bottom, #3A3A3A 0%, #4A4A4A 100%); " +
                                     "-fx-background-radius: 10; " +
                                     "-fx-border-color: linear-gradient(to right, #FFD700, #FFA500); " +
                                     "-fx-border-width: 3; " +
                                     "-fx-border-radius: 10; " +
                                     "-fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.5), 12, 0, 0, 3);");
        });
        
        medianExpenseBox.setOnMouseExited(e -> {
            medianExpenseBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2A2A2A 0%, #3A3A3A 100%); " +
                                     "-fx-background-radius: 10; " +
                                     "-fx-border-color: linear-gradient(to right, #FFD700, #FFA500); " +
                                     "-fx-border-width: 2; " +
                                     "-fx-border-radius: 10; " +
                                     "-fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.3), 8, 0, 0, 2);");
        });
        
        return medianExpenseBox;
    }

    
    /**
     * 初始化餐廳分析區塊
     */
    private void initializeAnalysisSection() {
        // 特色分析
        Label featuresLabel = new Label("特色");
        featuresLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        featuresLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        featuresArea = new TextArea();
        featuresArea.setPromptText("載入中...");
        featuresArea.setEditable(false);
        featuresArea.setWrapText(true);
        featuresArea.setPrefHeight(120);
        featuresArea.setMinHeight(120);
        VBox.setVgrow(featuresArea, Priority.SOMETIMES);
        featuresArea.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-cursor: hand;");
        
        // 為每個區域添加點擊事件
        featuresArea.setOnMouseClicked(e -> {
            if (isReviewDetailMode) {
                // 如果在詳細模式，點擊返回
                exitReviewDetailMode();
            } else {
                // 正常的 AI 聊天功能
                parentComponent.toggleAIChatView("特色討論", featuresArea.getText(), "餐廳特色");
            }
        });
        
        // 添加懸停效果
        featuresArea.setOnMouseEntered(e -> {
            if (isReviewDetailMode) {
                featuresArea.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: " + RICH_MIDTONE_RED + "; -fx-border-width: 1.5; -fx-cursor: hand;");
            } else {
                featuresArea.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: " + RICH_MIDTONE_RED + "; -fx-border-width: 1.5; -fx-cursor: hand;");
            }
        });
        
        featuresArea.setOnMouseExited(e -> {
            if (isReviewDetailMode) {
                featuresArea.setStyle("-fx-background-color: white; -fx-border-color: " + RICH_MIDTONE_RED + "; -fx-border-width: 1; -fx-cursor: hand;");
            } else {
                featuresArea.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-cursor: hand;");
            }
        });
        
        getChildren().addAll(featuresLabel, featuresArea);
    }
    
    /**
     * 進入近期評論詳細模式
     */
    private void enterReviewDetailMode() {
        System.out.println("🎯 進入近期評論詳細模式");
        isReviewDetailMode = true;
        
        // 保存原始特色內容
        originalFeaturesContent = featuresArea.getText();
        
        // 創建詳細評論視圖
        VBox detailView = createDetailedReviewView();
        
        // 將詳細視圖內容設置到特色區域
        featuresArea.clear();
        featuresArea.setStyle("-fx-background-color: white; -fx-border-color: " + RICH_MIDTONE_RED + "; -fx-border-width: 2; -fx-cursor: hand;");
        
        StringBuilder detailContent = new StringBuilder();
        detailContent.append("📊 近期評論詳細分析模式\n");
        detailContent.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        detailContent.append("📅 當前顯示範圍: 近 ").append(currentSelectedDays).append(" 天\n");
        detailContent.append("🔍 餐廳: ").append(currentRestaurantName != null ? currentRestaurantName : "未選擇").append("\n");
        detailContent.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        detailContent.append("📋 使用下方的時間軸來調整顯示範圍\n");
        detailContent.append("🎚️ 拖動滑桿選擇要查看的天數範圍\n");
        detailContent.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        detailContent.append("💡 點擊此區域可返回原來的特色分析");
        
        featuresArea.setText(detailContent.toString());
    }
    
    /**
     * 退出近期評論詳細模式
     */
    private void exitReviewDetailMode() {
        System.out.println("🏠 退出近期評論詳細模式");
        isReviewDetailMode = false;
        
        // 恢復原始特色內容
        featuresArea.setText(originalFeaturesContent);
        featuresArea.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-cursor: hand;");
    }
    
    /**
     * 創建詳細評論視圖
     */
    private VBox createDetailedReviewView() {
        VBox detailView = new VBox(10);
        detailView.setPadding(new Insets(15));
        detailView.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
        
        // 標題
        Label titleLabel = new Label("📊 近期評論詳細分析");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        // 時間軸控制
        Label sliderLabel = new Label("📅 選擇時間範圍：");
        sliderLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + "; -fx-font-weight: bold;");
        
        Slider timeSlider = new Slider(1, 90, currentSelectedDays);
        timeSlider.setShowTickLabels(true);
        timeSlider.setShowTickMarks(true);
        timeSlider.setMajorTickUnit(15);
        timeSlider.setMinorTickCount(2);
        timeSlider.setBlockIncrement(1);
        timeSlider.setStyle("-fx-control-inner-background: " + RICH_LIGHT_GREEN + ";");
        
        Label daysLabel = new Label("近 " + currentSelectedDays + " 天");
        daysLabel.setStyle("-fx-text-fill: " + RICH_MIDTONE_RED + "; -fx-font-weight: bold;");
        
        // 時間軸變更事件
        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int days = newVal.intValue();
            currentSelectedDays = days;
            daysLabel.setText("近 " + days + " 天");
            
            // 更新按鈕選中狀態
            String normalStyle = "-fx-background-color: #E67649; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 10 5 10; -fx-font-size: 11px;";
            String activeStyle = "-fx-background-color: #8B4513; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 10 5 10; -fx-font-size: 11px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 4, 0, 0, 1);";
            
            monthButton.setStyle(normalStyle);
            weekButton.setStyle(normalStyle);
            dayButton.setStyle(normalStyle);
            
            if (days <= 1) {
                dayButton.setStyle(activeStyle);
            } else if (days <= 7) {
                weekButton.setStyle(activeStyle);
            } else if (days >= 28) {
                monthButton.setStyle(activeStyle);
            }
            
            // 實時更新評論顯示
            updateRecentReviewsDisplay(days);
        });
        
        // 快速選擇按鈕
        HBox quickSelectBox = new HBox(10);
        quickSelectBox.setAlignment(Pos.CENTER);
        
        Button day1Btn = new Button("今天");
        Button day3Btn = new Button("3天");
        Button week1Btn = new Button("1週");
        Button week2Btn = new Button("2週");
        Button month1Btn = new Button("1月");
        Button month3Btn = new Button("3月");
        
        String quickBtnStyle = "-fx-background-color: #F0F0F0; -fx-text-fill: #333; -fx-background-radius: 12; -fx-padding: 3 8 3 8; -fx-font-size: 10px;";
        
        day1Btn.setStyle(quickBtnStyle);
        day3Btn.setStyle(quickBtnStyle);
        week1Btn.setStyle(quickBtnStyle);
        week2Btn.setStyle(quickBtnStyle);
        month1Btn.setStyle(quickBtnStyle);
        month3Btn.setStyle(quickBtnStyle);
        
        day1Btn.setOnAction(e -> timeSlider.setValue(1));
        day3Btn.setOnAction(e -> timeSlider.setValue(3));
        week1Btn.setOnAction(e -> timeSlider.setValue(7));
        week2Btn.setOnAction(e -> timeSlider.setValue(14));
        month1Btn.setOnAction(e -> timeSlider.setValue(30));
        month3Btn.setOnAction(e -> timeSlider.setValue(90));
        
        quickSelectBox.getChildren().addAll(day1Btn, day3Btn, week1Btn, week2Btn, month1Btn, month3Btn);
        
        // 返回按鈕
        Button backButton = new Button("🏠 返回特色分析");
        backButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15 8 15;");
        backButton.setOnAction(e -> exitReviewDetailMode());
        
        detailView.getChildren().addAll(
            titleLabel,
            new Separator(),
            sliderLabel,
            timeSlider,
            daysLabel,
            new Label("🎯 快速選擇："),
            quickSelectBox,
            new Separator(),
            backButton
        );
        
        return detailView;
    }
    
    /**
     * 創建競爭對手條目
     */
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
            parentComponent.loadRestaurantData(jsonFilePath);
        });

        Button showOnMapButton = new Button("在地圖上顯示");
        showOnMapButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white;");
        showOnMapButton.setOnAction(e -> {
            System.out.println("Map Button clicked for: " + displayName);
            String mapQuery = displayName;
            if ("海大燒臘".equals(displayName)) mapQuery = "海大燒臘";
            else if ("海那邊小食堂".equals(displayName)) mapQuery = "海那邊小食堂 基隆";
            parentComponent.openMapInBrowser(mapQuery);
        });

        HBox buttonBox = new HBox(5, loadDataButton, showOnMapButton);
        entryBox.getChildren().addAll(nameLabel, buttonBox);
        return entryBox;
    }
    
    /**
     * 添加懸停效果
     */
    private void addHoverEffect(Button button, String normalStyle, String activeColor) {
        button.setOnMouseEntered(e -> {
            if (!button.getStyle().contains(activeColor)) {
                button.setStyle("-fx-background-color: #EEEEEE; -fx-text-fill: #333333; -fx-font-size: 11px; -fx-background-radius: 12; -fx-padding: 3 8 3 8;");
            }
        });
        
        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains(activeColor)) {
                button.setStyle(normalStyle);
            }
        });
    }
    
    /**
     * 更新近期評論顯示 (已移至側欄)
     * 此方法保留以維持向後相容性，但功能已移至 RecentReviewsSidebar
     */
    @Deprecated
    public void updateRecentReviewsDisplay(int days) {
        System.out.println("⚠️ updateRecentReviewsDisplay 已棄用，功能已移至 RecentReviewsSidebar");
    }
    
    /**
     * 從檔案名稱中提取 Google Maps 地點ID
     * @param jsonFilePath JSON檔案路徑
     * @return 地點ID或null
     */
    private String extractPlaceIdFromFilename(String jsonFilePath) {
        // 海大燒臘的地點ID
        if (jsonFilePath.contains("Haidai") || jsonFilePath.contains("海大")) {
            return "ChIJN1t_tDeuEmsRUsoyG83frY4"; // 更新為示例 ID，因為原始 ID 已無效
        }
        // 海那邊小食堂的地點ID
        else if (jsonFilePath.contains("Sea Side") || jsonFilePath.contains("海那邊")) {
            return "ChIJ2cYvYAauEmsREyXgAjpN1uI"; // 更新為示例 ID，因為原始 ID 已無效
        }
        
        // 其他情況無法獲取地點ID
        return null;
    }
    
    /**
     * 🗑️ 已移除範例數據方法 - 功能已移至側欄
     */
    @Deprecated
    private void updateRecentReviewsWithSampleData(int days) {
        System.out.println("⚠️ updateRecentReviewsWithSampleData 已棄用，功能已移至 RecentReviewsSidebar");
    }
    
    /**
     * 創建評論卡片UI
     */
    private VBox createReviewCard(String date, String username, double rating, String content) {
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
     * 更新平均消費中位數顯示
     */
    public void updateMedianExpense(String medianExpense) {
        if (medianExpenseValueLabel != null) {
            if (!medianExpense.equals("未知") && !medianExpense.isEmpty()) {
                medianExpenseValueLabel.setText(medianExpense);
                System.out.println("✅ 已更新消費中位數顯示: " + medianExpense);
            } else {
                medianExpenseValueLabel.setText("暫無資料");
                System.out.println("⚠️ 消費中位數資料不可用");
            }
        } else {
            System.out.println("❌ 消費中位數標籤尚未初始化");
        }
    }
    
    /**
     * 獲取評分欄位
     */
    public Map<String, ProgressBar> getRatingBars() {
        return ratingBars;
    }
    
    /**
     * 獲取評分數值標籤
     */
    public Map<String, Label> getRatingValueLabels() {
        return ratingValueLabels;
    }
    
    /**
     * 更新評分顯示（同時更新柱狀圖和數值）
     * @param category 評分類別
     * @param rating 評分值 (0.0 - 5.0)
     */
    public void updateRatingDisplay(String category, double rating) {
        System.out.println("🎯 更新評分顯示: " + category + " = " + rating);
        
        // 更新數值標籤
        Label valueLabel = ratingValueLabels.get(category);
        if (valueLabel != null) {
            valueLabel.setText(String.format("%.1f", rating));
            System.out.println("✅ 已更新數值標籤: " + category + " = " + String.format("%.1f", rating));
        } else {
            System.out.println("❌ 找不到數值標籤: " + category);
        }
        
        // 🎯 更新柱狀圖高度
        updateBarHeight(category, rating);
    }
    
    /**
     * 更新柱狀圖高度
     * @param category 評分類別
     * @param rating 評分值 (0.0 - 5.0)
     */
    private void updateBarHeight(String category, double rating) {
        // 在 ratingsBox 中尋找對應的柱狀條
        for (javafx.scene.Node node : ratingsBox.getChildren()) {
            if (node instanceof VBox) {
                VBox itemBox = (VBox) node;
                // 尋找柱狀圖容器（第二個子元素）
                if (itemBox.getChildren().size() >= 2 && itemBox.getChildren().get(1) instanceof VBox) {
                    VBox barContainer = (VBox) itemBox.getChildren().get(1);
                    // 尋找柱狀條（barContainer 的子元素）
                    for (javafx.scene.Node barNode : barContainer.getChildren()) {
                        if (barNode instanceof Region && barNode.getUserData() != null) {
                            String userData = (String) barNode.getUserData();
                            if (userData.equals(category + "_bar")) {
                                Region barFill = (Region) barNode;
                                // 🎯 根據評分計算柱狀圖高度（0-5分對應0-35px）
                                double targetHeight = (rating / 5.0) * 35;
                                barFill.setMinHeight(targetHeight);
                                barFill.setPrefHeight(targetHeight);
                                barFill.setMaxHeight(targetHeight);
                                
                                System.out.println("✅ 已更新柱狀圖: " + category + " 高度 = " + targetHeight + "px");
                                return;
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("❌ 找不到柱狀圖: " + category);
    }
    
    /**
     * 獲取評分標題
     */
    public Label getRatingsHeader() {
        return ratingsHeader;
    }
    
    /**
     * 獲取評分區域
     */
    public VBox getRatingsBox() {
        return ratingsBox;
    }
    
    /**
     * 獲取特色分析文本區域
     */
    public TextArea getFeaturesArea() {
        return featuresArea;
    }
    
    /**
     * 清空所有數據顯示
     */
    public void clearDataDisplay(String message) {
        // 清空評分
        for (ProgressBar bar : ratingBars.values()) {
            bar.setProgress(0);
        }
        
        // 更新文字區域顯示
        featuresArea.setText(message);
        
        // 評論區域已移至側欄，不再需要清空
        System.out.println("📝 評論區域功能已移至側欄");
    }
    
    /**
     * 設置當前JSON檔案路徑
     */
    public void setCurrentJsonFilePath(String jsonFilePath) {
        this.currentJsonFilePath = jsonFilePath;
    }
    
    /**
     * 將本地 JSON 檔案內容更新到經營建議功能
     * @param jsonFilePath JSON 檔案路徑
     * @param totalReviews 總評論數
     * @param validReviews 有效評論數
     * @param allComments 所有評論內容
     */
    public void updateSuggestionsFromJsonData(String jsonFilePath, int totalReviews, int validReviews, String allComments) {
        // 更新特色區域，顯示本地 JSON 資料統計
        StringBuilder analysisInfo = new StringBuilder();
        analysisInfo.append("📁 本地資料分析\n");
        analysisInfo.append("━━━━━━━━━━━━━━━━\n");
        analysisInfo.append("📊 資料來源: ").append(jsonFilePath).append("\n");
        analysisInfo.append("📈 總評論數: ").append(totalReviews).append(" 條\n");
        analysisInfo.append("✅ 有效評論: ").append(validReviews).append(" 條\n");
        analysisInfo.append("━━━━━━━━━━━━━━━━\n");
        analysisInfo.append("💡 點擊此區域可進行 AI 互動分析\n");
        analysisInfo.append("🎯 經營建議將基於這些本地評論資料生成");
        
        Platform.runLater(() -> {
            featuresArea.setText(analysisInfo.toString());
            
            // 同時通知父元件可以使用這些資料進行經營建議
            System.out.println("✅ 本地 JSON 資料已載入至經營建議系統");
            System.out.println("📝 可用於 AI 分析的評論字數: " + allComments.length() + " 字元");
        });
    }
    
    /**
     * 設置當前搜尋到的餐廳資訊
     */
    public void setCurrentRestaurantInfo(String name, String id, String placeId) {
        this.currentRestaurantName = name;
        this.currentRestaurantId = id;
        this.currentPlaceId = placeId;
        
        // 🚫 移除自動載入評論的邏輯 - 讓用戶手動點擊時間按鈕來載入評論
        // 只設置餐廳資訊，不自動載入評論
        System.out.println("✅ 已設置餐廳資訊: " + name + " (ID: " + id + ", PlaceID: " + placeId + ")");
        System.out.println("💡 用戶可點擊時間按鈕來載入對應時間範圍的評論");
    }
    
    /**
     * 獲取當前餐廳 ID
     * @return 當前餐廳的 ID，如果未設置則返回 null
     */
    public String getCurrentRestaurantId() {
        return currentRestaurantId;
    }
    
    /**
     * 獲取當前餐廳名稱
     * @return 當前餐廳的名稱，如果未設置則返回 null
     */
    public String getCurrentRestaurantName() {
        return currentRestaurantName;
    }
    
    /**
     * 獲取當前餐廳 Place ID
     * @return 當前餐廳的 Place ID，如果未設置則返回 null
     */
    public String getCurrentPlaceId() {
        return currentPlaceId;
    }
    
    /**
     * 更新分析區域（優點和缺點）
     * 這個方法用於相容性，實際上我們只更新特色區域
     */
    public void updateAnalysisAreas(String pros, String cons) {
        // 將優點和缺點資訊整合到特色區域中
        String combinedText = featuresArea.getText();
        if (!combinedText.contains("優點") && !combinedText.contains("注意")) {
            combinedText += "\n\n" + pros + "\n\n" + cons;
            featuresArea.setText(combinedText);
        }
    }
    
    /**
     * 獲取更深的顏色（用於漸層效果）
     */
    private String getDarkerColor(String color) {
        switch (color) {
            case "#2E7D32": return "#1B5E20"; // 深綠
            case "#D32F2F": return "#B71C1C"; // 深紅
            default: return "#D4532A"; // 深橘色（默認）
        }
    }
    
    /**
     * 獲取帶透明度的顏色（用於陰影效果）
     */
    private String getColorWithAlpha(String color, double alpha) {
        switch (color) {
            case "#2E7D32": return "rgba(46, 125, 50, " + alpha + ")"; // 綠色
            case "#D32F2F": return "rgba(211, 47, 47, " + alpha + ")"; // 紅色
            default: return "rgba(230, 118, 73, " + alpha + ")"; // 橘色（默認）
        }
    }
    
    /**
     * 更新 AI 對話的餐廳特色資訊
     */
    public void updateAIChatFeatures(String featuresText) {
        if (chatAdvisor == null) {
            chatAdvisor = new bigproject.ai.ChatRestaurantAdvisor();
        }
        chatAdvisor.setRestaurantFeatures(featuresText);
        System.out.println("✅ 已更新 AI 對話的餐廳特色資訊");
    }
} 