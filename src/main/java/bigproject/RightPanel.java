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
    private VBox competitorListVBox;
    private VBox recentReviewsBox;
    
    // 分析區塊的 TextArea
    private TextArea featuresArea;
    private TextArea prosArea;
    private TextArea consArea;
    
    // 父視窗參考
    private compare parentComponent;
    
    // 新增最新評論管理器
    private LatestReviewsManager reviewsManager;
    
    // 當前顯示的餐廳JSON檔案
    private String currentJsonFilePath;
    
    /**
     * 建構函數
     * @param parentComponent 父元件參考，用於獲取AIChat等功能
     */
    public RightPanel(compare parentComponent) {
        super(15);  // 使用 15 像素的垂直間距
        this.parentComponent = parentComponent;
        
        // 初始化最新評論管理器，使用 API Key
        this.reviewsManager = new LatestReviewsManager("AIzaSyAfssp2jChrVBpRPFuAhBE6f6kXYDQaV0I");
        
        // 設置面板樣式
        setStyle("-fx-background-color: " + RICH_LIGHT_GREEN + "; -fx-background-radius: 0;");
        setPadding(new Insets(15, 0, 300, 15));  // 右側邊距為0，確保貼緊
        setPrefWidth(450);  // 固定寬度 450
        setMinWidth(450);   // 固定最小寬度
        setMaxWidth(450);   // 固定最大寬度
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
        
        // 資料來源區域
        initializeSourcesSection();
        
        // 近期評論區域
        initializeRecentReviewsSection();
        
        // 餐廳分析區塊
        initializeAnalysisSection();
        
        // 增加底部空間，確保滾動時可以顯示所有內容
        Region spacer = new Region();
        spacer.setMinHeight(200);
        spacer.setPrefHeight(200);
        getChildren().add(spacer);
    }
    
    /**
     * 初始化評分區域
     */
    private void initializeRatingsSection() {
        ratingsHeader = new Label("綜合評分");
        ratingsHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        ratingsHeader.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        ratingsBox = new VBox(5);
        ratingsBox.setPadding(new Insets(5, 0, 15, 0));
        ratingsBox.setStyle("-fx-background-color: transparent;");
        
        // 初始化評分條
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
        
        getChildren().addAll(ratingsHeader, ratingsBox);
    }
    
    /**
     * 初始化資料來源區域
     */
    private void initializeSourcesSection() {
        Label sourcesLabel = new Label("資料來源");
        sourcesLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        sourcesLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        competitorListVBox = new VBox(5);
        competitorListVBox.setPadding(new Insets(5, 0, 0, 0));
        competitorListVBox.getChildren().add(createCompetitorEntry("Haidai Roast Shop", "Haidai Roast Shop.json"));
        competitorListVBox.getChildren().add(createCompetitorEntry("Sea Side Eatery", "Sea Side Eatery Info.json"));
        
        getChildren().addAll(sourcesLabel, competitorListVBox);
    }
    
    /**
     * 初始化近期評論區域
     */
    private void initializeRecentReviewsSection() {
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
        weekButton.setStyle(timeButtonStyle);
        monthButton.setStyle(timeButtonActiveStyle); // 預設選中一個月
        
        // 設置按鈕點擊事件
        dayButton.setOnAction(e -> {
            // 更改按鈕樣式
            dayButton.setStyle(timeButtonActiveStyle);
            weekButton.setStyle(timeButtonStyle);
            monthButton.setStyle(timeButtonStyle);
            
            // 強制更新顯示近一天的評論
            updateRecentReviewsDisplay(1); // 1天
        });
        
        weekButton.setOnAction(e -> {
            // 更改按鈕樣式
            dayButton.setStyle(timeButtonStyle);
            weekButton.setStyle(timeButtonActiveStyle);
            monthButton.setStyle(timeButtonStyle);
            
            // 強制更新顯示近一週的評論
            updateRecentReviewsDisplay(7); // 7天
        });
        
        monthButton.setOnAction(e -> {
            // 更改按鈕樣式
            dayButton.setStyle(timeButtonStyle);
            weekButton.setStyle(timeButtonStyle);
            monthButton.setStyle(timeButtonActiveStyle);
            
            // 強制更新顯示近一個月的評論
            updateRecentReviewsDisplay(30); // 30天
        });
        
        // 添加懸停效果
        addHoverEffect(dayButton, timeButtonStyle, RICH_MIDTONE_RED);
        addHoverEffect(weekButton, timeButtonStyle, RICH_MIDTONE_RED);
        addHoverEffect(monthButton, timeButtonStyle, RICH_MIDTONE_RED);
        
        timeRangeButtonsBox.getChildren().addAll(dayButton, weekButton, monthButton);
        
        // 在標題和時間範圍按鈕之間添加間距
        HBox reviewHeaderBox = new HBox(10);
        reviewHeaderBox.setAlignment(Pos.CENTER_LEFT);
        reviewHeaderBox.getChildren().addAll(recentReviewsLabel, timeRangeButtonsBox);

        // 創建近期評論列表容器
        recentReviewsBox = new VBox(10);
        recentReviewsBox.getStyleClass().add("recent-reviews-container");
        recentReviewsBox.setPadding(new Insets(5, 0, 15, 0));
        recentReviewsBox.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-padding: 10;");

        getChildren().addAll(reviewHeaderBox, recentReviewsBox);
        
        // 確保載入時就顯示近一個月的評論
        Platform.runLater(() -> updateRecentReviewsDisplay(30)); // 30天
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
        
        // 優點分析
        Label prosLabel = new Label("優點");
        prosLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        prosLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        prosArea = new TextArea();
        prosArea.setPromptText("載入中...");
        prosArea.setEditable(false);
        prosArea.setWrapText(true);
        prosArea.setPrefHeight(120);
        prosArea.setMinHeight(120);
        VBox.setVgrow(prosArea, Priority.SOMETIMES);
        prosArea.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-cursor: hand;");
        
        // 缺點分析
        Label consLabel = new Label("缺點");
        consLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        consLabel.setStyle("-fx-text-fill: " + PALE_DARK_YELLOW + ";");
        
        consArea = new TextArea();
        consArea.setPromptText("載入中...");
        consArea.setEditable(false);
        consArea.setWrapText(true);
        consArea.setPrefHeight(120);
        consArea.setMinHeight(120);
        VBox.setVgrow(consArea, Priority.SOMETIMES);
        consArea.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-cursor: hand;");
        
        // 為每個區域添加點擊事件
        featuresArea.setOnMouseClicked(e -> {
            parentComponent.toggleAIChatView("特色討論", featuresArea.getText(), "餐廳特色");
        });
        
        prosArea.setOnMouseClicked(e -> {
            parentComponent.toggleAIChatView("優點討論", prosArea.getText(), "餐廳優點");
        });
        
        consArea.setOnMouseClicked(e -> {
            parentComponent.toggleAIChatView("缺點討論", consArea.getText(), "餐廳缺點");
        });
        
        // 添加懸停效果
        for (TextArea area : new TextArea[]{featuresArea, prosArea, consArea}) {
            area.setOnMouseEntered(e -> {
                area.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: " + RICH_MIDTONE_RED + "; -fx-border-width: 1.5; -fx-cursor: hand;");
            });
            
            area.setOnMouseExited(e -> {
                area.setStyle("-fx-background-color: white; -fx-border-color: " + PALE_DARK_YELLOW + "; -fx-border-width: 1; -fx-cursor: hand;");
            });
        }
        
        getChildren().addAll(featuresLabel, featuresArea, prosLabel, prosArea, consLabel, consArea);
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
            if ("Haidai Roast Shop".equals(displayName)) mapQuery = "海大燒臘";
            else if ("Sea Side Eatery".equals(displayName)) mapQuery = "海那邊小食堂 基隆";
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
     * 更新近期評論顯示
     */
    public void updateRecentReviewsDisplay(int days) {
        System.out.println("右側面板更新近期評論顯示，顯示近 " + days + " 天的評論...");
        
        // 清空現有內容
        recentReviewsBox.getChildren().clear();
        
        // 添加載入指示
        Label loadingLabel = new Label("正在載入近 " + days + " 天的評論...");
        loadingLabel.setStyle("-fx-text-fill: #777777; -fx-font-style: italic;");
        recentReviewsBox.getChildren().add(loadingLabel);
        
        // 檢查是否有設置當前JSON檔案
        if (currentJsonFilePath == null || currentJsonFilePath.isEmpty()) {
            recentReviewsBox.getChildren().clear();
            Label errorLabel = new Label("尚未載入餐廳資料");
            errorLabel.setStyle("-fx-text-fill: #E03C31; -fx-font-style: italic;");
            recentReviewsBox.getChildren().add(errorLabel);
            return;
        }
        
        // 直接使用 Google Maps API 獲取評論
        String placeId = extractPlaceIdFromFilename(currentJsonFilePath);
        if (placeId != null && !placeId.isEmpty()) {
            // 使用地點ID直接從API獲取評論
            System.out.println("使用地點ID從API直接獲取評論: " + placeId);
            reviewsManager.fetchAndDisplayReviews(placeId, days, recentReviewsBox, parentComponent);
        } else {
            // 如果無法從檔名提取地點ID，則使用JSON檔案中的評論
            System.out.println("無法從檔名提取地點ID，使用JSON檔案中的評論: " + currentJsonFilePath);
            reviewsManager.updateRecentReviewsDisplay(currentJsonFilePath, days, recentReviewsBox, parentComponent);
        }
        
        // 注意：系統不再使用範例資料，而是從API或JSON獲取真實評論
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
     * 使用示例數據填充近期評論容器
     * 保留為備用方法，以防API或JSON數據出問題時使用
     * 注意：此方法已不再主動調用，僅作為備用方案保留
     */
    private void updateRecentReviewsWithSampleData(int days) {
        System.out.println("警告：正在使用範例資料代替真實資料！");
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
        if (ratingsBox != null && !medianExpense.equals("未知")) {
            // 檢查是否已經有消費標籤
            boolean hasExpenseLabel = false;
            HBox expenseBox = null;
            
            // 使用索引遍歷而不是迭代器，避免 ConcurrentModificationException
            for (int i = 0; i < ratingsBox.getChildren().size(); i++) {
                Node node = ratingsBox.getChildren().get(i);
                if (node instanceof HBox && ((HBox) node).getId() != null && ((HBox) node).getId().equals("expenseBox")) {
                    hasExpenseLabel = true;
                    // 更新現有標籤
                    expenseBox = (HBox) node;
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
                expenseBox = new HBox(10);
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
    
    /**
     * 獲取評分欄位
     */
    public Map<String, ProgressBar> getRatingBars() {
        return ratingBars;
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
     * 獲取優點分析文本區域
     */
    public TextArea getProsArea() {
        return prosArea;
    }
    
    /**
     * 獲取缺點分析文本區域
     */
    public TextArea getConsArea() {
        return consArea;
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
        prosArea.setText(message);
        consArea.setText(message);
        
        // 清空評論區域
        recentReviewsBox.getChildren().clear();
        recentReviewsBox.getChildren().add(new Label(message));
    }
    
    /**
     * 設置當前顯示的餐廳JSON檔案
     */
    public void setCurrentJsonFilePath(String jsonFilePath) {
        this.currentJsonFilePath = jsonFilePath;
    }
} 