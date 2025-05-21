package bigproject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

/**
 * 處理近期評論獲取和按時間篩選功能
 */
public class LatestReviewsManager {
    
    private final String apiKey;
    private final HttpClient httpClient;
    
    // 緩存最近一次載入的評論資料，避免重複讀取JSON
    private JSONArray cachedReviews;
    private String cachedJsonFilePath;
    
    /**
     * 建構函數
     * @param apiKey Google Maps API密鑰
     */
    public LatestReviewsManager(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    
    /**
     * 從本地JSON檔案載入評論資料
     * @param jsonFilePath JSON檔案路徑
     * @return 評論資料JSON陣列
     */
    public JSONArray loadReviewsFromJson(String jsonFilePath) throws IOException, JSONException {
        // 如果已經載入過相同檔案，直接返回緩存
        if (jsonFilePath.equals(cachedJsonFilePath) && cachedReviews != null) {
            return cachedReviews;
        }
        
        // 否則從檔案讀取
        String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
        cachedReviews = new JSONArray(content);
        cachedJsonFilePath = jsonFilePath;
        return cachedReviews;
    }
    
    /**
     * 根據指定的時間範圍篩選評論
     * @param reviews 完整評論資料
     * @param days 天數範圍（1=近一天，7=近一週，30=近一月）
     * @return 篩選後的評論列表
     */
    public List<JSONObject> filterReviewsByDays(JSONArray reviews, int days) {
        if (reviews == null) {
            return new ArrayList<>();
        }
        
        List<JSONObject> filteredReviews = new ArrayList<>();
        
        // 計算截止時間 (當前時間減去指定天數)
        Instant cutoffTime = Instant.now().minus(days, ChronoUnit.DAYS);
        long cutoffTimestamp = cutoffTime.getEpochSecond();
        
        System.out.println("篩選評論: 截止時間戳 " + cutoffTimestamp + " (" + 
                           LocalDateTime.ofInstant(cutoffTime, ZoneId.systemDefault()) + ")");
        
        for (int i = 0; i < reviews.length(); i++) {
            try {
                JSONObject review = reviews.getJSONObject(i);
                
                // 獲取評論時間戳 (Unix時間戳，以秒為單位)
                if (review.has("time") && !review.isNull("time")) {
                    long reviewTime = review.getLong("time");
                    
                    // 只保留時間戳大於截止時間的評論 (即近期評論)
                    if (reviewTime >= cutoffTimestamp) {
                        filteredReviews.add(review);
                        System.out.println("保留評論: 時間戳 " + reviewTime + " (" + 
                                          LocalDateTime.ofInstant(Instant.ofEpochSecond(reviewTime), 
                                                               ZoneId.systemDefault()) + ")");
                    } else {
                        System.out.println("過濾評論: 時間戳 " + reviewTime + " (" + 
                                          LocalDateTime.ofInstant(Instant.ofEpochSecond(reviewTime), 
                                                               ZoneId.systemDefault()) + ")");
                    }
                }
            } catch (JSONException e) {
                System.err.println("處理評論時出錯: " + e.getMessage());
            }
        }
        
        return filteredReviews;
    }
    
    /**
     * 更新近期評論顯示
     * @param jsonFilePath JSON檔案路徑
     * @param days 天數範圍
     * @param container 評論容器
     * @param parentComponent 父元件，用於創建評論卡片
     */
    public void updateRecentReviewsDisplay(String jsonFilePath, int days, VBox container, compare parentComponent) {
        // 清空容器
        Platform.runLater(() -> {
            container.getChildren().clear();
            container.getChildren().add(parentComponent.createLoadingLabel("正在載入近 " + days + " 天的評論..."));
        });
        
        // 確保 JSON 檔案路徑有效
        if (jsonFilePath == null || jsonFilePath.isEmpty()) {
            Platform.runLater(() -> {
                container.getChildren().clear();
                container.getChildren().add(
                    parentComponent.createErrorLabel("JSON 檔案路徑無效")
                );
            });
            System.err.println("錯誤: JSON 檔案路徑為空");
            return;
        }
        
        // 確保 JSON 檔案存在
        if (!new java.io.File(jsonFilePath).exists()) {
            Platform.runLater(() -> {
                container.getChildren().clear();
                container.getChildren().add(
                    parentComponent.createErrorLabel("找不到 JSON 檔案: " + jsonFilePath)
                );
            });
            System.err.println("錯誤: 找不到 JSON 檔案: " + jsonFilePath);
            return;
        }
        
        System.out.println("嘗試從檔案載入評論: " + jsonFilePath);
        
        // 異步載入和處理評論
        CompletableFuture.runAsync(() -> {
            try {
                // 載入評論資料
                JSONArray allReviews = loadReviewsFromJson(jsonFilePath);
                System.out.println("成功載入 JSON 檔案，共找到 " + allReviews.length() + " 條評論");
                
                // 按時間範圍篩選
                List<JSONObject> filteredReviews = filterReviewsByDays(allReviews, days);
                System.out.println("篩選結果: 近 " + days + " 天內共有 " + filteredReviews.size() + " 條評論");
                
                if (filteredReviews.isEmpty()) {
                    Platform.runLater(() -> {
                        container.getChildren().clear();
                        container.getChildren().add(
                            parentComponent.createInfoLabel("在過去 " + days + " 天內沒有評論，顯示最近的評論：")
                        );
                        
                        // 沒有近期評論時，顯示最近的5條評論
                        int showCount = Math.min(allReviews.length(), 5);
                        if (showCount > 0) {
                            System.out.println("沒有符合條件的近期評論，顯示最近的 " + showCount + " 條評論");
                            
                            for (int i = 0; i < showCount; i++) {
                                try {
                                    JSONObject review = allReviews.getJSONObject(i);
                                    
                                    // 獲取評論資料
                                    String reviewer = review.optString("author_name", "匿名");
                                    String commentText = review.optString("text", "無評論內容");
                                    double rating = review.optDouble("rating", 0.0);
                                    
                                    // 獲取相對時間描述或轉換時間戳
                                    String timeDescription;
                                    if (review.has("relative_time_description")) {
                                        timeDescription = review.optString("relative_time_description", "未知時間");
                                    } else if (review.has("time")) {
                                        long timestamp = review.getLong("time");
                                        // 簡化日期顯示
                                        timeDescription = formatRelativeTime(timestamp);
                                    } else {
                                        timeDescription = "未知時間";
                                    }
                                    
                                    // 創建並添加評論卡片
                                    container.getChildren().add(
                                        parentComponent.createReviewCard(timeDescription, reviewer, rating, commentText)
                                    );
                                } catch (Exception e) {
                                    System.err.println("處理評論 #" + i + " 時出錯: " + e.getMessage());
                                }
                            }
                        }
                    });
                    return;
                }
                
                // 在UI線程中更新顯示
                Platform.runLater(() -> {
                    container.getChildren().clear();
                    
                    // 最多顯示10條評論
                    int displayCount = Math.min(filteredReviews.size(), 10);
                    
                    for (int i = 0; i < displayCount; i++) {
                        JSONObject review = filteredReviews.get(i);
                        
                        // 獲取評論資料
                        String reviewer = review.optString("author_name", "匿名");
                        String commentText = review.optString("text", "無評論內容");
                        double rating = review.optDouble("rating", 0.0);
                        
                        // 獲取相對時間描述或轉換時間戳
                        String timeDescription;
                        if (review.has("relative_time_description")) {
                            timeDescription = review.optString("relative_time_description", "未知時間");
                        } else if (review.has("time")) {
                            long timestamp = review.getLong("time");
                            // 簡化日期顯示
                            timeDescription = formatRelativeTime(timestamp);
                        } else {
                            timeDescription = "未知時間";
                        }
                        
                        // 顯示日誌，確認卡片創建過程
                        System.out.println("創建評論卡片 #" + (i+1) + ": " + reviewer + " - " + timeDescription);
                        
                        // 創建並添加評論卡片
                        container.getChildren().add(
                            parentComponent.createReviewCard(timeDescription, reviewer, rating, commentText)
                        );
                    }
                    
                    // 如果篩選出的評論超過顯示數量，添加提示
                    if (filteredReviews.size() > displayCount) {
                        container.getChildren().add(
                            parentComponent.createInfoLabel("還有 " + (filteredReviews.size() - displayCount) + " 條評論未顯示")
                        );
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    container.getChildren().clear();
                    container.getChildren().add(
                        parentComponent.createErrorLabel("載入評論時發生錯誤: " + e.getMessage())
                    );
                    // 添加詳細錯誤信息
                    Label detailsLabel = new Label("詳細錯誤: " + e.toString());
                    detailsLabel.setStyle("-fx-text-fill: #777777; -fx-font-style: italic; -fx-wrap-text: true;");
                    detailsLabel.setWrapText(true);
                    container.getChildren().add(detailsLabel);
                    
                    // 如果錯誤堆疊深度大於0，顯示第一個堆疊元素
                    if (e.getStackTrace().length > 0) {
                        Label stackLabel = new Label("位置: " + e.getStackTrace()[0].toString());
                        stackLabel.setStyle("-fx-text-fill: #777777; -fx-font-style: italic; -fx-wrap-text: true;");
                        stackLabel.setWrapText(true);
                        container.getChildren().add(stackLabel);
                    }
                });
                System.err.println("載入評論時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 格式化相對時間
     * @param timestamp Unix時間戳
     * @return 格式化的相對時間字符串
     */
    private String formatRelativeTime(long timestamp) {
        Instant reviewTime = Instant.ofEpochSecond(timestamp);
        Instant now = Instant.now();
        
        long diffSeconds = ChronoUnit.SECONDS.between(reviewTime, now);
        
        if (diffSeconds < 60) {
            return "剛剛";
        } else if (diffSeconds < 3600) {
            return (diffSeconds / 60) + " 分鐘前";
        } else if (diffSeconds < 86400) {
            return (diffSeconds / 3600) + " 小時前";
        } else if (diffSeconds < 604800) {
            return (diffSeconds / 86400) + " 天前";
        } else if (diffSeconds < 2592000) {
            return (diffSeconds / 604800) + " 週前";
        } else {
            return (diffSeconds / 2592000) + " 個月前";
        }
    }
    
    /**
     * 嘗試使用Google Places API獲取最新評論
     * 這個方法可以用於更新本地JSON檔案
     * @param placeId 地點ID
     * @param jsonFilePath 要更新的JSON檔案路徑
     * @return 成功或失敗
     */
    public CompletableFuture<Boolean> fetchLatestReviews(String placeId, String jsonFilePath) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("API Key未設置");
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            String url = String.format(
                "https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&fields=name,rating,reviews,user_ratings_total&language=zh-TW&reviews_sort=newest&key=%s",
                URLEncoder.encode(placeId, StandardCharsets.UTF_8.toString()),
                apiKey
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response.body());
                            if ("OK".equals(jsonResponse.optString("status")) && jsonResponse.has("result")) {
                                JSONObject result = jsonResponse.getJSONObject("result");
                                
                                // 讀取現有JSON文件
                                JSONObject existingData;
                                try {
                                    String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
                                    existingData = new JSONObject(content);
                                } catch (IOException | JSONException e) {
                                    // 如果檔案不存在或內容不是有效的JSON，創建新的對象
                                    existingData = new JSONObject();
                                    existingData.put("name", result.optString("name"));
                                    existingData.put("total_ratings", 0);
                                    existingData.put("rating", 0);
                                    existingData.put("reviews", new JSONArray());
                                }
                                
                                // 更新基本信息
                                existingData.put("total_ratings", result.optInt("user_ratings_total", existingData.optInt("total_ratings")));
                                existingData.put("rating", result.optDouble("rating", existingData.optDouble("rating")));
                                
                                // 獲取新的評論
                                if (result.has("reviews")) {
                                    JSONArray newReviews = result.getJSONArray("reviews");
                                    
                                    // 獲取現有評論
                                    JSONArray existingReviews;
                                    if (existingData.has("reviews")) {
                                        existingReviews = existingData.getJSONArray("reviews");
                                    } else {
                                        existingReviews = new JSONArray();
                                        existingData.put("reviews", existingReviews);
                                    }
                                    
                                    // 合併評論 (避免重複)
                                    mergeReviews(existingReviews, newReviews);
                                    
                                    // 保存更新的數據
                                    Files.write(Paths.get(jsonFilePath), 
                                               existingData.toString(2).getBytes(StandardCharsets.UTF_8));
                                    
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("處理API響應時出錯: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("API請求失敗: 狀態碼 " + response.statusCode());
                        System.err.println("響應內容: " + response.body());
                    }
                    return false;
                })
                .exceptionally(e -> {
                    System.err.println("執行API請求時出錯: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                });
        } catch (Exception e) {
            System.err.println("創建API請求時出錯: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 合併新舊評論，避免重複
     * @param existingReviews 現有評論
     * @param newReviews 新評論
     */
    private void mergeReviews(JSONArray existingReviews, JSONArray newReviews) throws JSONException {
        // 創建一個集合來存儲已有評論的唯一標識符
        List<String> existingIds = new ArrayList<>();
        
        // 從現有評論中提取唯一標識符
        for (int i = 0; i < existingReviews.length(); i++) {
            JSONObject review = existingReviews.getJSONObject(i);
            // 使用作者名稱+時間作為唯一標識符
            String authorName = review.optString("author_name", "");
            long time = review.optLong("time", 0);
            existingIds.add(authorName + "_" + time);
        }
        
        // 添加新評論 (如果它們不存在)
        for (int i = 0; i < newReviews.length(); i++) {
            JSONObject newReview = newReviews.getJSONObject(i);
            String authorName = newReview.optString("author_name", "");
            long time = newReview.optLong("time", 0);
            String id = authorName + "_" + time;
            
            if (!existingIds.contains(id)) {
                existingReviews.put(newReview);
                System.out.println("添加新評論: " + authorName + " (" + 
                               LocalDateTime.ofInstant(Instant.ofEpochSecond(time), 
                                                    ZoneId.systemDefault()) + ")");
            }
        }
    }
    
    /**
     * 直接從 Google Maps API 獲取最新評論並顯示
     * @param placeId 地點ID
     * @param days 天數範圍
     * @param container 評論容器
     * @param parentComponent 父元件
     */
    public void fetchAndDisplayReviews(String placeId, int days, VBox container, compare parentComponent) {
        // 清空容器
        Platform.runLater(() -> {
            container.getChildren().clear();
            container.getChildren().add(parentComponent.createLoadingLabel("正在從 Google Maps 載入近 " + days + " 天的評論..."));
        });
        
        // 確保 API 金鑰有效
        if (apiKey == null || apiKey.isEmpty()) {
            Platform.runLater(() -> {
                container.getChildren().clear();
                container.getChildren().add(parentComponent.createErrorLabel("API 金鑰未設置"));
            });
            System.err.println("錯誤: API 金鑰為空");
            return;
        }
        
        // 確保地點ID有效
        if (placeId == null || placeId.isEmpty()) {
            Platform.runLater(() -> {
                container.getChildren().clear();
                container.getChildren().add(parentComponent.createErrorLabel("地點 ID 無效"));
            });
            System.err.println("錯誤: 地點 ID 為空");
            return;
        }
        
        System.out.println("嘗試從 Google Maps API 獲取評論，地點 ID: " + placeId);
        
        try {
            String url = String.format(
                "https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&fields=name,rating,reviews,user_ratings_total&language=zh-TW&reviews_sort=newest&key=%s",
                URLEncoder.encode(placeId, StandardCharsets.UTF_8.toString()),
                apiKey
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();
            
            // 異步發送請求
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            System.out.println("API 回應狀態碼: 200 (成功)");
                            System.out.println("API 回應內容: " + response.body());
                            
                            JSONObject jsonResponse = new JSONObject(response.body());
                            if ("OK".equals(jsonResponse.optString("status")) && jsonResponse.has("result")) {
                                JSONObject result = jsonResponse.getJSONObject("result");
                                
                                // 獲取評論
                                if (result.has("reviews")) {
                                    JSONArray reviews = result.getJSONArray("reviews");
                                    System.out.println("從 API 成功獲取 " + reviews.length() + " 條評論");
                                    
                                    // 按時間範圍篩選
                                    List<JSONObject> filteredReviews = new ArrayList<>();
                                    Instant cutoffTime = Instant.now().minus(days, ChronoUnit.DAYS);
                                    long cutoffTimestamp = cutoffTime.getEpochSecond();
                                    
                                    for (int i = 0; i < reviews.length(); i++) {
                                        JSONObject review = reviews.getJSONObject(i);
                                        System.out.println("評論 #" + (i+1) + ": " + review.toString());
                                        
                                        if (review.has("time")) {
                                            long reviewTime = review.getLong("time");
                                            if (reviewTime >= cutoffTimestamp) {
                                                filteredReviews.add(review);
                                                System.out.println("  > 符合時間條件，加入篩選結果");
                                            } else {
                                                System.out.println("  > 不符合時間條件，評論時間太早");
                                            }
                                        } else {
                                            System.out.println("  > 評論沒有時間戳");
                                        }
                                    }
                                    
                                    System.out.println("篩選後有 " + filteredReviews.size() + " 條評論在近 " + days + " 天內");
                                    
                                    // 在UI線程中更新顯示
                                    Platform.runLater(() -> {
                                        container.getChildren().clear();
                                        
                                        if (filteredReviews.isEmpty()) {
                                            container.getChildren().add(
                                                parentComponent.createInfoLabel("在過去 " + days + " 天內沒有評論，顯示最近的評論：")
                                            );
                                            
                                            // 顯示所有評論
                                            if (reviews.length() > 0) {
                                                System.out.println("顯示所有評論（不限時間）");
                                                displayReviews(reviews, Math.min(reviews.length(), 5), container, parentComponent);
                                            } else {
                                                container.getChildren().add(
                                                    parentComponent.createInfoLabel("該地點沒有任何評論記錄")
                                                );
                                            }
                                        } else {
                                            // 顯示篩選後的評論
                                            System.out.println("顯示篩選後的評論");
                                            displayReviews(filteredReviews, Math.min(filteredReviews.size(), 10), container, parentComponent);
                                            
                                            // 如果篩選出的評論超過顯示數量，添加提示
                                            if (filteredReviews.size() > 10) {
                                                container.getChildren().add(
                                                    parentComponent.createInfoLabel("還有 " + (filteredReviews.size() - 10) + " 條評論未顯示")
                                                );
                                            }
                                        }
                                    });
                                } else {
                                    System.out.println("API 回應中沒有評論");
                                    Platform.runLater(() -> {
                                        container.getChildren().clear();
                                        container.getChildren().add(
                                            parentComponent.createInfoLabel("該地點沒有評論資料")
                                        );
                                    });
                                }
                            } else {
                                System.out.println("API 回應狀態不是 OK: " + jsonResponse.optString("status"));
                                Platform.runLater(() -> {
                                    container.getChildren().clear();
                                    container.getChildren().add(
                                        parentComponent.createErrorLabel("API 響應錯誤: " + jsonResponse.optString("status"))
                                    );
                                });
                            }
                        } catch (Exception e) {
                            System.err.println("解析 API 回應時出錯: " + e.getMessage());
                            handleApiError(e, container, parentComponent);
                        }
                    } else {
                        System.out.println("API 回應狀態碼: " + response.statusCode() + " (失敗)");
                        System.out.println("API 回應內容: " + response.body());
                        
                        Platform.runLater(() -> {
                            container.getChildren().clear();
                            container.getChildren().add(
                                parentComponent.createErrorLabel("API 請求失敗: 狀態碼 " + response.statusCode())
                            );
                            container.getChildren().add(
                                parentComponent.createInfoLabel("響應內容: " + response.body())
                            );
                        });
                    }
                    return null;
                })
                .exceptionally(e -> {
                    System.err.println("發送 API 請求時出錯: " + e.getMessage());
                    handleApiError(e, container, parentComponent);
                    return null;
                });
        } catch (Exception e) {
            handleApiError(e, container, parentComponent);
        }
    }
    
    /**
     * 顯示評論列表
     * @param reviews 評論列表
     * @param limit 最大顯示數量
     * @param container 容器
     * @param parentComponent 父元件
     */
    private void displayReviews(List<JSONObject> reviews, int limit, VBox container, compare parentComponent) {
        for (int i = 0; i < limit; i++) {
            JSONObject review = reviews.get(i);
            displayReview(review, container, parentComponent);
        }
    }
    
    /**
     * 顯示評論列表 (JSONArray 版本)
     * @param reviews 評論列表
     * @param limit 最大顯示數量
     * @param container 容器
     * @param parentComponent 父元件
     */
    private void displayReviews(JSONArray reviews, int limit, VBox container, compare parentComponent) {
        for (int i = 0; i < limit; i++) {
            try {
                JSONObject review = reviews.getJSONObject(i);
                displayReview(review, container, parentComponent);
            } catch (JSONException e) {
                System.err.println("處理評論時出錯: " + e.getMessage());
            }
        }
    }
    
    /**
     * 顯示單個評論
     * @param review 評論對象
     * @param container 容器
     * @param parentComponent 父元件
     */
    private void displayReview(JSONObject review, VBox container, compare parentComponent) {
        // 獲取評論資料
        String reviewer = review.optString("author_name", "匿名");
        String commentText = review.optString("text", "無評論內容");
        double rating = review.optDouble("rating", 0.0);
        
        // 獲取相對時間描述或轉換時間戳
        String timeDescription;
        if (review.has("relative_time_description")) {
            timeDescription = review.optString("relative_time_description", "未知時間");
        } else if (review.has("time")) {
            long timestamp = review.getLong("time");
            // 簡化日期顯示
            timeDescription = formatRelativeTime(timestamp);
        } else {
            timeDescription = "未知時間";
        }
        
        // 創建並添加評論卡片
        container.getChildren().add(
            parentComponent.createReviewCard(timeDescription, reviewer, rating, commentText)
        );
    }
    
    /**
     * 處理 API 錯誤
     * @param e 異常
     * @param container 容器
     * @param parentComponent 父元件
     */
    private void handleApiError(Throwable e, VBox container, compare parentComponent) {
        System.err.println("API 錯誤: " + e.getMessage());
        e.printStackTrace();
        
        Platform.runLater(() -> {
            container.getChildren().clear();
            container.getChildren().add(
                parentComponent.createErrorLabel("載入評論時發生錯誤: " + e.getMessage())
            );
            
            // 添加詳細錯誤信息
            Label detailsLabel = new Label("詳細錯誤: " + e.toString());
            detailsLabel.setStyle("-fx-text-fill: #777777; -fx-font-style: italic; -fx-wrap-text: true;");
            detailsLabel.setWrapText(true);
            container.getChildren().add(detailsLabel);
            
            // 如果錯誤堆疊深度大於0，顯示第一個堆疊元素
            if (e.getStackTrace().length > 0) {
                Label stackLabel = new Label("位置: " + e.getStackTrace()[0].toString());
                stackLabel.setStyle("-fx-text-fill: #777777; -fx-font-style: italic; -fx-wrap-text: true;");
                stackLabel.setWrapText(true);
                container.getChildren().add(stackLabel);
            }
        });
    }

    /**
     * 從 Google Maps API 獲取評論資料
     * @param placeId 地點ID
     * @return 評論 JSONArray 或 null
     */
    private JSONArray fetchReviewsFromGoogleMaps(String placeId) {
        if (placeId == null || placeId.isEmpty()) {
            System.out.println("錯誤：無效的地點ID");
            return null;
        }
        
        System.out.println("開始從 Google Maps API 獲取評論，地點 ID: " + placeId);
        System.out.println("地點 ID 格式檢查: " + (placeId.matches("ChIJ[0-9A-Za-z_-]+") ? "有效" : "可能無效"));
        
        try {
            // 構建 URL
            String encodedPlaceId = URLEncoder.encode(placeId, StandardCharsets.UTF_8.toString());
            String urlStr = "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=" + encodedPlaceId +
                    "&fields=reviews" +
                    "&key=" + apiKey;
            
            System.out.println("API 請求 URL: " + urlStr);
            
            // 創建 HTTP 請求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .GET()
                    .build();
            
            // 發送請求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("API 回應狀態碼: " + response.statusCode());
            System.out.println("API 回應內容摘要: " + response.body().substring(0, Math.min(100, response.body().length())) + "...");
            
            // 解析回應
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                
                // 檢查回應狀態
                String status = jsonResponse.getString("status");
                System.out.println("API 回應狀態: " + status);
                
                if ("OK".equals(status)) {
                    JSONObject result = jsonResponse.getJSONObject("result");
                    
                    if (result.has("reviews")) {
                        JSONArray reviews = result.getJSONArray("reviews");
                        System.out.println("成功獲取 " + reviews.length() + " 條評論");
                        return reviews;
                    } else {
                        System.out.println("該地點沒有評論");
                    }
                } else {
                    System.out.println("API 回應狀態不是 OK: " + status);
                    if (jsonResponse.has("error_message")) {
                        System.out.println("錯誤訊息: " + jsonResponse.getString("error_message"));
                    }
                }
            } else {
                System.out.println("API 請求失敗，狀態碼: " + response.statusCode());
            }
            
        } catch (Exception e) {
            System.out.println("獲取評論時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
} 