package bigproject;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * 餐廳評分數據分析器
 * 調用 Python 腳本從 Firestore 獲取評論數據並計算評分統計
 */
public class RatingDataAnalyzer {
    
    private static final String PYTHON_SCRIPT_PATH = "data-collector/rating_analyzer.py";
    private static final String PYTHON_EXECUTABLE = ".venv/bin/python";
    private static final int TIMEOUT_SECONDS = 60;
    
    private RightPanel rightPanel;
    private compare parentComponent;
    
    /**
     * 構造函數
     * @param rightPanel 右側面板組件
     * @param parentComponent 父組件
     */
    public RatingDataAnalyzer(RightPanel rightPanel, compare parentComponent) {
        this.rightPanel = rightPanel;
        this.parentComponent = parentComponent;
    }
    
    /**
     * 異步分析餐廳評分數據
     * @param restaurantId 餐廳 ID
     * @param restaurantName 餐廳名稱
     */
    public void analyzeRestaurantRatingsAsync(String restaurantId, String restaurantName) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔍 開始分析餐廳評分: " + restaurantName + " (ID: " + restaurantId + ")");
                
                // 在UI線程中顯示加載狀態
                Platform.runLater(() -> {
                    updateRatingDisplayToLoading();
                });
                
                // 調用 Python 腳本分析評分
                Map<String, Double> ratings = analyzePythonRatings(restaurantId, restaurantName);
                
                // 在UI線程中更新評分顯示
                Platform.runLater(() -> {
                    updateRatingDisplayFromResults(ratings);
                });
                
            } catch (Exception e) {
                System.err.println("❌ 評分分析失敗: " + e.getMessage());
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    updateRatingDisplayToError();
                });
            }
        });
    }
    
    /**
     * 調用 Python 腳本分析評分數據
     * @param restaurantId 餐廳 ID
     * @param restaurantName 餐廳名稱
     * @return 評分數據映射
     */
    private Map<String, Double> analyzePythonRatings(String restaurantId, String restaurantName) throws Exception {
        // 構建 Python 命令
        List<String> command = new ArrayList<>();
        command.add(PYTHON_EXECUTABLE);
        command.add(PYTHON_SCRIPT_PATH);
        command.add(restaurantId);
        if (restaurantName != null && !restaurantName.trim().isEmpty()) {
            command.add(restaurantName);
        }
        
        System.out.println("🐍 執行 Python 命令: " + String.join(" ", command));
        
        // 執行 Python 腳本
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File("."));
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // 讀取輸出
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            boolean inJsonSection = false;
            
            while ((line = reader.readLine()) != null) {
                System.out.println("Python輸出: " + line);
                
                // 檢測JSON輸出區段
                if (line.contains("評分分析結果:")) {
                    inJsonSection = true;
                    continue;
                }
                
                if (inJsonSection) {
                    output.append(line).append("\n");
                }
            }
        }
        
        // 等待進程完成
        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python 腳本執行超時");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Python 腳本執行失敗，退出碼: " + exitCode);
        }
        
        // 解析 JSON 結果
        return parseRatingResults(output.toString().trim());
    }
    
    /**
     * 解析 Python 腳本返回的 JSON 結果
     * @param jsonString JSON 字符串
     * @return 評分數據映射
     */
    private Map<String, Double> parseRatingResults(String jsonString) throws JSONException {
        Map<String, Double> ratings = new HashMap<>();
        
        try {
            System.out.println("📊 解析JSON結果:\n" + jsonString);
            
            JSONObject result = new JSONObject(jsonString);
            
            if (!result.getBoolean("success")) {
                String message = result.optString("message", "未知錯誤");
                throw new RuntimeException("Python分析失敗: " + message);
            }
            
            JSONObject ratingsData = result.getJSONObject("ratings");
            
            // 提取各項評分的平均值
            String[] categories = {"餐點", "服務", "環境", "價格"};
            for (String category : categories) {
                if (ratingsData.has(category)) {
                    JSONObject categoryData = ratingsData.getJSONObject(category);
                    double average = categoryData.getDouble("average");
                    ratings.put(category, average);
                    
                    System.out.println("✅ " + category + "評分: " + average + 
                                     " (基於 " + categoryData.getInt("count") + " 筆評論)");
                } else {
                    ratings.put(category, 0.0);
                    System.out.println("⚠️  " + category + "評分: 無數據");
                }
            }
            
            // 記錄統計信息
            int totalReviews = result.getInt("total_reviews");
            System.out.println("📈 總評論數: " + totalReviews);
            
        } catch (JSONException e) {
            System.err.println("❌ JSON解析錯誤: " + e.getMessage());
            System.err.println("原始JSON: " + jsonString);
            
            // 返回默認值
            ratings.put("餐點", 0.0);
            ratings.put("服務", 0.0);
            ratings.put("環境", 0.0);
            ratings.put("價格", 0.0);
        }
        
        return ratings;
    }
    
    /**
     * 將評分顯示更新為加載狀態
     */
    private void updateRatingDisplayToLoading() {
        if (rightPanel != null) {
            System.out.println("🔄 設置評分顯示為加載狀態");
            
            // 更新所有評分為 0.0 並顯示加載狀態
            Map<String, Label> valueLabels = rightPanel.getRatingValueLabels();
            if (valueLabels != null) {
                for (Map.Entry<String, Label> entry : valueLabels.entrySet()) {
                    Label label = entry.getValue();
                    if (label != null) {
                        label.setText("...");
                        // 添加加載動畫效果
                        label.setStyle(label.getStyle() + "; -fx-text-fill: #FFD700;");
                    }
                }
            }
            
            // 重置柱狀圖高度
            String[] categories = {"餐點", "服務", "環境", "價格"};
            for (String category : categories) {
                rightPanel.updateRatingDisplay(category, 0.0);
            }
        }
    }
    
    /**
     * 從分析結果更新評分顯示
     * @param ratings 評分數據映射
     */
    private void updateRatingDisplayFromResults(Map<String, Double> ratings) {
        if (rightPanel != null && ratings != null) {
            System.out.println("📊 更新評分顯示");
            
            // 更新各項評分顯示
            for (Map.Entry<String, Double> entry : ratings.entrySet()) {
                String category = entry.getKey();
                Double rating = entry.getValue();
                
                if (rating != null) {
                    rightPanel.updateRatingDisplay(category, rating);
                    System.out.println("✅ 已更新 " + category + " 評分: " + rating);
                }
            }
            
            // 恢復數值標籤的正常樣式
            Map<String, Label> valueLabels = rightPanel.getRatingValueLabels();
            if (valueLabels != null) {
                for (Map.Entry<String, Label> entry : valueLabels.entrySet()) {
                    Label label = entry.getValue();
                    if (label != null) {
                        // 移除加載狀態的樣式
                        String style = label.getStyle().replace("; -fx-text-fill: #FFD700;", "");
                        label.setStyle(style);
                    }
                }
            }
        }
    }
    
    /**
     * 將評分顯示更新為錯誤狀態
     */
    private void updateRatingDisplayToError() {
        if (rightPanel != null) {
            System.out.println("❌ 設置評分顯示為錯誤狀態");
            
            // 更新所有評分為 0.0
            String[] categories = {"餐點", "服務", "環境", "價格"};
            for (String category : categories) {
                rightPanel.updateRatingDisplay(category, 0.0);
            }
            
            // 更新數值標籤顯示錯誤狀態
            Map<String, Label> valueLabels = rightPanel.getRatingValueLabels();
            if (valueLabels != null) {
                for (Map.Entry<String, Label> entry : valueLabels.entrySet()) {
                    Label label = entry.getValue();
                    if (label != null) {
                        label.setText("N/A");
                        label.setStyle(label.getStyle() + "; -fx-text-fill: #FF6B6B;");
                    }
                }
            }
        }
    }
    
    /**
     * 檢查 Python 環境和腳本是否可用
     * @return 如果可用返回 true
     */
    public static boolean checkPythonEnvironment() {
        try {
            // 檢查 Python 是否可用
            ProcessBuilder pythonCheck = new ProcessBuilder(PYTHON_EXECUTABLE, "--version");
            Process process = pythonCheck.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (!finished || process.exitValue() != 0) {
                System.err.println("❌ Python3 不可用");
                return false;
            }
            
            // 檢查腳本文件是否存在
            Path scriptPath = Paths.get(PYTHON_SCRIPT_PATH);
            if (!Files.exists(scriptPath)) {
                System.err.println("❌ Python 腳本不存在: " + PYTHON_SCRIPT_PATH);
                return false;
            }
            
            System.out.println("✅ Python 環境檢查通過");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Python 環境檢查失敗: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 手動觸發評分分析（用於調試）
     * @param restaurantId 餐廳 ID
     * @param restaurantName 餐廳名稱
     */
    public void manualAnalyze(String restaurantId, String restaurantName) {
        System.out.println("🔧 手動觸發評分分析");
        analyzeRestaurantRatingsAsync(restaurantId, restaurantName);
    }
} 