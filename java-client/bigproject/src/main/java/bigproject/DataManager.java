package bigproject;

import com.vdurmont.emoji.EmojiParser;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.effect.BoxBlur;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.scene.layout.Region;

public class DataManager {

    // --- Load and Display Data from JSON ---
    public void loadAndDisplayRestaurantData(String jsonFilePath, Label ratingsHeader, VBox ratingsBox,
                                           Map<String, ProgressBar> ratingBars,
                                           TextArea reviewsArea, 
                                           FlowPane photosContainer,
                                           TextArea featuresArea, TextArea prosArea, TextArea consArea) {
        System.out.println("Loading data from: " + jsonFilePath);
        try {
            JSONArray reviews = loadReviewsFromJson(jsonFilePath);
            ratingsBox.getChildren().removeIf(node -> node.getId() != null && node.getId().equals("message-label"));
            photosContainer.getChildren().clear(); // 清空照片容器

            if (reviews != null && !reviews.isEmpty()) {
                Map<String, Double> averageScores = calculateAverageRatings(reviews);
                Platform.runLater(() -> {
                    updateRatingBars(averageScores, ratingBars);
                    updateReviewsArea(reviews, reviewsArea);
                    updatePhotosContainer(reviews, photosContainer); // 更新照片
                    featuresArea.setText("特色分析 (待實作)...");
                    prosArea.setText("優點分析 (待實作)...");
                    consArea.setText("缺點分析 (待實作)...");
                    String restaurantName = jsonFilePath.replace(".json", "").replace(" Info", "");
                    ratingsHeader.setText(restaurantName + " - 綜合評分");
                });
            } else {
                Platform.runLater(() -> clearRestaurantDataDisplay("無法從 " + jsonFilePath + " 載入評論資料",
                                                                  ratingsHeader, ratingsBox, ratingBars, reviewsArea, photosContainer, featuresArea, prosArea, consArea));
            }
        } catch (IOException | JSONException e) {
            Platform.runLater(() -> clearRestaurantDataDisplay("讀取檔案時發生錯誤: " + jsonFilePath,
                                                                  ratingsHeader, ratingsBox, ratingBars, reviewsArea, photosContainer, featuresArea, prosArea, consArea));
        }
    }

    // --- Clear Data Display ---
    public void clearRestaurantDataDisplay(String message, Label ratingsHeader, VBox ratingsBox,
                                         Map<String, ProgressBar> ratingBars,
                                         TextArea reviewsArea,
                                         FlowPane photosContainer,
                                         TextArea featuresArea, TextArea prosArea, TextArea consArea) {
        ratingsHeader.setText("綜合評分");
        ratingBars.values().forEach(bar -> bar.setProgress(0.0));
        ratingsBox.getChildren().removeIf(node -> node.getId() != null && node.getId().equals("message-label"));
        Label messageLabel = new Label(message);
        messageLabel.setId("message-label");
        messageLabel.setStyle("-fx-padding: 10 0 0 0;");
        ratingsBox.getChildren().add(messageLabel);

        reviewsArea.setText("");
        photosContainer.getChildren().clear(); // 清空照片容器
        featuresArea.setText("");
        prosArea.setText("");
        consArea.setText("");
    }

    // --- Load JSON Array from File ---
    private JSONArray loadReviewsFromJson(String filePath) throws IOException, JSONException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return new JSONArray(content);
    }

    // --- Calculate Average Ratings ---
    private Map<String, Double> calculateAverageRatings(JSONArray reviews) {
        Map<String, Double> averageScores = new HashMap<>();
        if (reviews == null || reviews.isEmpty()) {
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
        averageScores.put("價格", estimatePriceRatingValue(priceLevels));

        return averageScores;
    }

    // --- Update Rating Bars ---
    private void updateRatingBars(Map<String, Double> averageScores, Map<String, ProgressBar> ratingBars) {
        ratingBars.forEach((category, bar) -> {
            double score = averageScores.getOrDefault(category, 0.0);
            bar.setProgress(score / 5.0);
        });
    }

    // --- Estimate Price Rating Value (Returns 0-5) ---
    private double estimatePriceRatingValue(List<String> priceLevels) {
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

    // --- Update Reviews Text Area ---
    private void updateReviewsArea(JSONArray reviews, TextArea reviewsArea) {
        if (reviews == null || reviews.isEmpty()) {
            reviewsArea.setText("無評論資料");
            return;
        }
        StringBuilder sb = new StringBuilder();
        int reviewsToShow = Math.min(10, reviews.length());

        for (int i = 0; i < reviewsToShow; i++) {
            try {
                JSONObject review = reviews.getJSONObject(i);
                String reviewer = review.optString("評論者", "匿名");
                String commentCode = review.optString("評論", "無評論內容");
                String commentText = commentCode != null ? commentCode : "無評論內容"; // 簡化處理
                double rating = review.optDouble("評論分數", 0.0);
                String date = review.optString("留言日期", review.optString("留言時間", "未知時間"));

                sb.append(String.format("[%s] %s (%s)\n", formatStars(rating), reviewer, date));
                sb.append(commentText).append("\n");
                
                sb.append("\n");
            } catch (Exception e) {
                // Skip review display on error
                System.err.println("Error displaying review: " + e.getMessage());
            }
        }
        reviewsArea.setText(sb.toString());
        reviewsArea.positionCaret(0);
    }

    // --- Format Stars (Still used for reviews text area) ---
    private String formatStars(double rating) {
        if (rating <= 0) return "☆☆☆☆☆";
        int fullStars = (int) rating;
        boolean halfStar = (rating - fullStars) >= 0.25 && (rating - fullStars) < 0.75;
        boolean fullPlus = (rating - fullStars) >= 0.75;

        StringBuilder stars = new StringBuilder();
        int calculatedFullStars = fullStars + (fullPlus ? 1 : 0);
        calculatedFullStars = Math.min(calculatedFullStars, 5);

        for (int i = 0; i < calculatedFullStars ; i++) {
            stars.append("★");
        }
        if (halfStar && calculatedFullStars < 5) {
            stars.append("½");
        }

        int symbolsCount = calculatedFullStars + (halfStar && calculatedFullStars < 5 ? 1 : 0);
        int emptyStars = 5 - symbolsCount;
        emptyStars = Math.max(0, emptyStars);

        for (int i = 0; i < emptyStars; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }

    // --- 更新照片顯示 - 改善版本 ---
    private void updatePhotosContainer(JSONArray reviews, FlowPane photosContainer) {
        if (reviews == null || reviews.isEmpty()) {
            Label noPhotosLabel = new Label("無照片資料");
            noPhotosLabel.setStyle("-fx-text-fill: #555; -fx-font-style: italic;");
            photosContainer.getChildren().add(noPhotosLabel);
            return;
        }
        
        // 使用FlowPane直接添加照片，無需手動管理行
        int maxPhotos = 20; // 最多顯示的照片數量
        final int[] photoAdded = {0}; // 使用數組包裝以滿足effectively final要求
        
        // 確保容器可以自由滾動
        photosContainer.setMaxHeight(Double.MAX_VALUE);
        photosContainer.setMinHeight(Region.USE_COMPUTED_SIZE);
        
        // 設置合適的間距，改善照片排列
        photosContainer.setHgap(15);
        photosContainer.setVgap(15);
        photosContainer.setPadding(new Insets(10));
        
        // 清除現有內容
        photosContainer.getChildren().clear();
        
        // 用於追蹤已顯示過的評論者
        Set<String> displayedReviewers = new HashSet<>();
        
        // 遍歷所有評論，尋找照片
        for (int i = 0; i < reviews.length(); i++) {
            try {
                JSONObject review = reviews.getJSONObject(i);
                
                // 檢查評論者是否為null
                String reviewer = review.optString("評論者", null);
                if (reviewer == null || reviewer.trim().isEmpty() || reviewer.equals("null")) {
                    continue; // 跳過沒有評論者的評論
                }
                
                double rating = review.optDouble("評論分數", 0.0);
                
                // 嚴格檢查：避免同一評論者的多張照片
                if (displayedReviewers.contains(reviewer)) {
                    continue;
                }
                
                // 檢查是否有照片資訊，以及照片是否有效
                if (review.has("照片") && !review.isNull("照片")) {
                    Object photoObj = review.opt("照片");
                    String photoUrl = null;
                    
                    // 嘗試從不同的資料結構中獲取照片URL
                    if (photoObj instanceof JSONArray) {
                        JSONArray photos = (JSONArray) photoObj;
                        if (photos.length() > 0) {
                            // 只處理第一張有效照片
                            for (int j = 0; j < photos.length() && photoUrl == null; j++) {
                                try {
                                    if (photos.optJSONArray(j) != null) {
                                        JSONArray photoDetails = photos.getJSONArray(j);
                                        if (photoDetails.length() > 1) {
                                            String tempUrl = photoDetails.optString(1);
                                            if (tempUrl != null && !tempUrl.trim().isEmpty() && !tempUrl.equals("null")) {
                                                photoUrl = tempUrl;
                                            }
                                        }
                                    } else if (photos.optString(j) != null) {
                                        String tempUrl = photos.optString(j);
                                        if (tempUrl != null && !tempUrl.trim().isEmpty() && !tempUrl.equals("null")) {
                                            photoUrl = tempUrl;
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error processing photo detail: " + e.getMessage());
                                }
                            }
                        }
                    } else if (photoObj instanceof String) {
                        String tempUrl = (String) photoObj;
                        if (tempUrl != null && !tempUrl.trim().isEmpty() && !tempUrl.equals("null")) {
                            photoUrl = tempUrl;
                        }
                    }
                    
                    // 只有找到有效的照片URL才添加
                    if (photoUrl != null && !photoUrl.trim().isEmpty() && !photoUrl.equals("null")) {
                        if (photoAdded[0] >= maxPhotos) {
                            // 如果超過最大照片數，則跳過
                            continue;
                        }
                        
                        addPhotoToContainer(photosContainer, photoUrl, reviewer, rating);
                        photoAdded[0]++;
                        
                        // 標記此評論者已顯示過照片
                        displayedReviewers.add(reviewer);
                    }
                }
            } catch (JSONException e) {
                // 跳過出問題的評論
                System.err.println("Error processing review photos: " + e.getMessage());
            }
        }
        
        // 如果沒有照片被添加
        if (photosContainer.getChildren().isEmpty()) {
            Label noPhotosLabel = new Label("無照片資料或無法載入照片");
            noPhotosLabel.setStyle("-fx-text-fill: #555; -fx-font-style: italic;");
            photosContainer.getChildren().add(noPhotosLabel);
        }
        
        // 確保所有更新完成後觸發一次佈局重繪
        Platform.runLater(() -> {
            // 設置足夠的空間讓照片排列並允許滾動
            if (photoAdded[0] > 6) {  // 如果照片數量足夠多需要滾動
                // 計算一個合理的高度，使得照片區域需要滾動
                double estimatedHeight = Math.ceil(photoAdded[0] / 3.0) * 240;  // 假設每行大約3張照片，每張高約240
                photosContainer.setMinHeight(estimatedHeight);
            }
            
            photosContainer.requestLayout();
        });
    }
    
    // --- 添加單張照片到容器 - 改善版本 ---
    private void addPhotoToContainer(FlowPane container, String photoUrl, String reviewer, double rating) {
        try {
            VBox photoBox = new VBox(5);
            photoBox.setAlignment(Pos.CENTER);
            // 使用更柔和的風格，並增加外邊距讓排列更美觀
            photoBox.setStyle("-fx-border-color: rgba(111, 103, 50, 0.6); " +
                              "-fx-border-width: 1; " +
                              "-fx-padding: 10; " +
                              "-fx-background-color: rgba(255, 255, 255, 0.6); " +  // 背景半透明
                              "-fx-background-radius: 10; " +  // 圓角
                              "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1), " +
                              "boxblur(10, 10, 3);");  // 添加模糊效果
            photoBox.setMaxWidth(200);
            photoBox.setMinWidth(200);
            photoBox.setMaxHeight(240); // 減小高度，因為我們不再顯示評論者信息
            photoBox.setMinHeight(200); // 確保有足夠的最小高度但不要太大
            
            ImageView imageView = new ImageView();
            imageView.setFitWidth(180);
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-background-color: #f0f0f0;");
            
            // 設定滑鼠懸停效果
            imageView.setCursor(Cursor.HAND); // 更改游標為手形
            
            // 創建照片載入提示
            Label loadingLabel = new Label("正在載入照片...");
            loadingLabel.setStyle("-fx-text-fill: #6F6732;");
            photoBox.getChildren().add(loadingLabel);
            
            // 添加到佈局中 - 先添加再載入圖片，確保界面響應
            container.getChildren().add(photoBox);
            
            // 嘗試異步載入圖片
            CompletableFuture.runAsync(() -> {
                try {
                    // 修正URL格式問題
                    String cleanUrl = photoUrl.trim().replace("[", "").replace("]", "").replace("\"", "");
                    if (!cleanUrl.startsWith("http")) {
                        cleanUrl = "https:" + cleanUrl;
                    }
                    
                    // 保存最終的URL以便在Lambda表達式中使用
                    final String finalUrl = cleanUrl;
                    
                    Image image = new Image(cleanUrl, true); // true表示異步載入
                    
                    Platform.runLater(() -> {
                        if (!image.isError()) {
                            imageView.setImage(image);
                            photoBox.getChildren().clear();
                            photoBox.getChildren().addAll(imageView);
                            
                            // 移除評論者信息，不再在縮圖顯示名稱和星級
                            // 但仍保留點擊事件，以便在全尺寸視圖中顯示信息
                            
                            // 添加點擊事件 - 點擊放大查看
                            imageView.setOnMouseClicked(event -> showFullSizeImage(finalUrl, reviewer, rating));
                            
                            // 觸發容器重新佈局
                            container.requestLayout();
                        } else {
                            loadingLabel.setText("無法載入圖片");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> loadingLabel.setText("圖片載入錯誤"));
                    System.err.println("Error loading image: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error adding photo to container: " + e.getMessage());
        }
    }
    
    // 顯示全尺寸圖片的方法
    private void showFullSizeImage(String imageUrl, String reviewer, double rating) {
        try {
            // 創建一個新的彈出窗口
            Stage imageStage = new Stage();
            imageStage.initModality(Modality.APPLICATION_MODAL); // 設為模態窗口
            imageStage.setTitle(reviewer + " 的照片");
            
            // 創建一個大尺寸的ImageView
            ImageView largeImageView = new ImageView();
            largeImageView.setPreserveRatio(true);
            
            // 使用原始圖片的尺寸，但設定最大尺寸
            largeImageView.setFitWidth(800);
            largeImageView.setFitHeight(600);
            
            // 載入更高質量的圖片
            Image largeImage = new Image(imageUrl, 1200, 0, true, true);
            largeImageView.setImage(largeImage);
            
            // 創建關閉按鈕
            Button closeButton = new Button("關閉");
            closeButton.setOnAction(e -> imageStage.close());
            closeButton.setStyle("-fx-background-color: #6F6732; -fx-text-fill: white;");
            
            // 創建評論者信息標籤
            Label infoLabel = new Label(reviewer + " " + formatStars(rating));
            infoLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            // 將元素添加到佈局中
            VBox vbox = new VBox(10);
            vbox.setAlignment(Pos.CENTER);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");
            vbox.getChildren().addAll(largeImageView, infoLabel, closeButton);
            
            // 創建場景
            Scene scene = new Scene(vbox, 850, 700);
            
            // 設置場景並顯示窗口
            imageStage.setScene(scene);
            imageStage.show();
            
        } catch (Exception e) {
            System.err.println("Error showing full size image: " + e.getMessage());
        }
    }
} 