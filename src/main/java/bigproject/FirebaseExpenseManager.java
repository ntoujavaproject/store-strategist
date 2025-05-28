package bigproject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Firebase 消費管理器
 * 專門用於從 Firebase Firestore 獲取餐廳評論中的真實消費數據
 * 支援區間數據處理和null值容錯
 */
public class FirebaseExpenseManager {
    
    // Firebase 專案 ID
    private static final String PROJECT_ID = "java2025-91d74";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * 消費區間類別 - 用於表示消費範圍
     */
    public static class ExpenseRange {
        public final int min;
        public final int max;
        public final double midpoint;
        
        public ExpenseRange(int min, int max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
            this.midpoint = (this.min + this.max) / 2.0;
        }
        
        public ExpenseRange(int singleValue) {
            this.min = singleValue;
            this.max = singleValue;
            this.midpoint = singleValue;
        }
        
        public boolean isRange() {
            return min != max;
        }
        
        @Override
        public String toString() {
            if (isRange()) {
                return String.format("%d-%d", min, max);
            } else {
                return String.valueOf(min);
            }
        }
    }
    
    /**
     * 從 Firebase 獲取餐廳的消費中位數
     * @param restaurantId 餐廳 ID
     * @return 消費中位數的文字描述，如果無數據則返回 null
     */
    public static String getMedianExpenseFromFirebase(String restaurantId) {
        try {
            System.out.println("🔍 從 Firebase 獲取餐廳消費數據: " + restaurantId);
            
            // 1. 從 Firebase 獲取評論數據
            JsonNode reviews = fetchReviewsFromFirebase(restaurantId);
            if (reviews == null || !reviews.isArray() || reviews.size() == 0) {
                System.out.println("❌ 找不到 Firebase 評論數據");
                return null;
            }
            
            // 2. 提取消費區間數據
            List<ExpenseRange> expenseRanges = extractExpenseRanges(reviews);
            if (expenseRanges.isEmpty()) {
                System.out.println("❌ 沒有有效的消費數據");
                return null;
            }
            
            // 3. 計算區間中位數
            String medianExpense = calculateRangeBasedMedian(expenseRanges);
            System.out.println("✅ 成功從 Firebase 獲取消費中位數: " + medianExpense);
            return medianExpense;
            
        } catch (Exception e) {
            System.err.println("❌ 從 Firebase 獲取消費數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 從 Firebase Firestore 獲取餐廳評論
     */
    private static JsonNode fetchReviewsFromFirebase(String restaurantId) throws Exception {
        String url = String.format(
            "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/" +
            "restaurants/%s/reviews",
            PROJECT_ID, restaurantId);
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000); // 10 秒連接超時
        conn.setReadTimeout(30000);    // 30 秒讀取超時
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.err.println("Firebase 讀取失敗，HTTP " + responseCode);
            return null;
        }
        
        try (InputStream is = conn.getInputStream()) {
            JsonNode root = mapper.readTree(is);
            return root.get("documents");
        }
    }
    
    /**
     * 從評論數據中提取消費區間
     */
    private static List<ExpenseRange> extractExpenseRanges(JsonNode reviews) {
        List<ExpenseRange> ranges = new ArrayList<>();
        int totalReviews = 0;
        int nullCount = 0;
        int validCount = 0;
        
        for (JsonNode doc : reviews) {
            totalReviews++;
            JsonNode fields = doc.get("fields");
            if (fields == null) {
                nullCount++;
                continue;
            }
            
            // 獲取 spend 欄位
            JsonNode spendField = fields.get("spend");
            if (spendField == null || spendField.isNull()) {
                nullCount++;
                continue;
            }
            
            String spendValue = null;
            if (spendField.has("stringValue")) {
                spendValue = spendField.get("stringValue").asText();
            } else if (spendField.has("integerValue")) {
                spendValue = spendField.get("integerValue").asText();
            }
            
            if (spendValue != null && !spendValue.trim().isEmpty()) {
                ExpenseRange range = parseExpenseRange(spendValue);
                if (range != null) {
                    ranges.add(range);
                    validCount++;
                    System.out.println("💰 找到消費區間: " + spendValue + " → " + range);
                }
            } else {
                nullCount++;
            }
        }
        
        System.out.println("📊 評論統計:");
        System.out.println("   📝 總評論數: " + totalReviews);
        System.out.println("   ❌ 無消費數據: " + nullCount + " (" + String.format("%.1f", (nullCount * 100.0 / totalReviews)) + "%)");
        System.out.println("   ✅ 有效消費數據: " + validCount + " (" + String.format("%.1f", (validCount * 100.0 / totalReviews)) + "%)");
        
        return ranges;
    }
    
    /**
     * 解析消費金額字串並轉換為區間對象
     * 支援Firebase格式：E:TWD_200_TO_400、E:TWD_1_TO_200
     * 也支援一般格式：NT$100、$150、100元、100-200等
     */
    private static ExpenseRange parseExpenseRange(String spendValue) {
        if (spendValue == null || spendValue.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 🔧 處理 Firebase 特殊格式：E:TWD_200_TO_400
            if (spendValue.startsWith("E:TWD_") && spendValue.contains("_TO_")) {
                String[] parts = spendValue.replace("E:TWD_", "").split("_TO_");
                if (parts.length == 2) {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    System.out.println("🎯 Firebase格式解析: " + spendValue + " → " + min + "-" + max);
                    return new ExpenseRange(min, max);
                }
            }
            
            // 移除常見的前後綴和符號，但保留區間分隔符
            String cleaned = spendValue.toLowerCase()
                    .replaceAll("nt\\$|\\$|元|dollar|每人|per person", "")
                    .replaceAll("[^0-9\\-~]", "")
                    .trim();
            
            if (cleaned.isEmpty()) {
                return null;
            }
            
            // 處理一般區間格式（如 100-200 或 100~200）
            if (cleaned.contains("-")) {
                String[] parts = cleaned.split("-");
                if (parts.length == 2) {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    return new ExpenseRange(min, max);
                }
            } else if (cleaned.contains("~")) {
                String[] parts = cleaned.split("~");
                if (parts.length == 2) {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    return new ExpenseRange(min, max);
                }
            }
            
            // 直接解析單一數字
            int value = Integer.parseInt(cleaned);
            return new ExpenseRange(value);
            
        } catch (NumberFormatException e) {
            System.out.println("⚠️ 無法解析消費金額: " + spendValue);
            return null;
        }
    }
    
    /**
     * 簡化版計算 - 只返回一個合理的消費區間
     */
    private static String calculateRangeBasedMedian(List<ExpenseRange> ranges) {
        if (ranges.isEmpty()) {
            return "暫無資料";
        }
        
        // 過濾掉異常值（超過10000的數據可能是錯誤的）
        List<ExpenseRange> validRanges = ranges.stream()
                .filter(r -> r.min <= 10000 && r.max <= 10000)
                .collect(Collectors.toList());
        
        if (validRanges.isEmpty()) {
            return "暫無資料";
        }
        
        System.out.println("📊 有效數據: " + validRanges.size() + " 筆 (過濾後)");
        
        // 使用中點值進行排序
        List<Double> midpoints = validRanges.stream()
                .map(r -> r.midpoint)
                .sorted()
                .collect(Collectors.toList());
        
        // 計算合理的區間範圍 (25%-75%)
        int size = midpoints.size();
        int q1Index = Math.max(0, size / 4);
        int q3Index = Math.min(size - 1, 3 * size / 4);
        
        double q1 = midpoints.get(q1Index);
        double q3 = midpoints.get(q3Index);
        
        // 四捨五入到最接近的10
        int rangeMin = (int) (Math.round(q1 / 10.0) * 10);
        int rangeMax = (int) (Math.round(q3 / 10.0) * 10);
        
        // 確保最小區間為50
        if (rangeMax - rangeMin < 50) {
            rangeMax = rangeMin + 50;
        }
        
        String result = String.format("NT$%d-%d", rangeMin, rangeMax);
        System.out.println("✅ 計算結果: " + result);
        
        return result;
    }
    
    /**
     * 測試方法：直接從 Firebase 獲取茹絲咖啡的消費數據
     */
    public static void main(String[] args) {
        // 測試茹絲咖啡的餐廳 ID
        String ruthCoffeeId = "0x345d4e15a208c94d:0xc83f833b1c9b3bf6";
        
        System.out.println("🧪 測試從 Firebase 獲取茹絲咖啡消費數據...");
        String result = getMedianExpenseFromFirebase(ruthCoffeeId);
        
        if (result != null) {
            System.out.println("✅ 測試成功！茹絲咖啡的消費中位數: " + result);
        } else {
            System.out.println("❌ 測試失敗：無法獲取消費數據");
        }
    }
} 