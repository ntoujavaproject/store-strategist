package bigproject.ai;

// 檔名：FirestoreRestaurantAnalyzer.java
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 從 Firestore 取出餐廳 reviews → 呼叫 Ollama 產生 300–350 字繁中摘要。
 * 使用自動下載的 Ollama 系統，用戶無需手動安裝。
 * args:
 *   0 = restaurantId   (必填)
 *   1 = output.json    (選填, 若給定則把結果寫檔)
 */
public class FirestoreRestaurantAnalyzer {

    // === 請改成你的 Firebase 專案 ID ===
    private static final String PROJECT_ID = "java2025-91d74";

    // ---- 預設模型 ----
    private static final String DEFAULT_MODEL = System.getenv()
            .getOrDefault("OLLAMA_MODEL", "gemma3:4b");

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("用法: java FirestoreRestaurantAnalyzer <restaurantId> [output.json]");
            System.exit(1);
        }
        String restaurantId = args[0];
        Path outFile = args.length >= 2 ? Path.of(args[1]) : null;

        System.out.println("🚀 開始分析餐廳評論...");
        System.out.println("📍 餐廳ID: " + restaurantId);

        // 1️⃣ 讀取 Firestore reviews 子集合
        System.out.println("📖 從 Firestore 讀取評論資料...");
        JsonNode docs = fetchReviews(restaurantId);
        if (docs == null || !docs.isArray() || docs.size() == 0) {
            System.err.println("❌ 找不到任何評論，無法分析。");
            return;
        }
        System.out.println("✅ 成功讀取 " + docs.size() + " 條評論");

        // 2️⃣ 萃取「評論」欄位文字，同步排除由 Guided Dining 標籤產生的假評論
        StringJoiner sj = new StringJoiner("\n");
        int validComments = 0;
        int maxCommentsToProcess = 10; // 限制只處理10條精選評論，提高速度
        
        // 收集所有有效評論並存儲評分
        List<Map.Entry<String, Double>> ratedComments = new ArrayList<>();
        
        for (JsonNode doc : docs) {
            JsonNode fields = doc.get("fields");
            if (fields == null) continue;
            
            // 獲取評論內容
            JsonNode comment = fields.get("comment");
            if (comment == null || comment.isNull()) continue;
            
            // 獲取評分
            JsonNode rating = fields.get("rating");
            double ratingValue = 0.0;
            if (rating != null && !rating.isNull()) {
                if (rating.has("doubleValue")) {
                    ratingValue = rating.get("doubleValue").asDouble();
                } else if (rating.has("integerValue")) {
                    ratingValue = rating.get("integerValue").asDouble();
                }
            }
            
            // 處理評論文本
            if (comment.has("stringValue")) {
                String commentText = comment.get("stringValue").asText();
                if (!commentText.startsWith("GUIDED_DINING_")) {
                    ratedComments.add(new AbstractMap.SimpleEntry<>(commentText, ratingValue));
                }
            } else if (comment.has("arrayValue")) {
                JsonNode arr = comment.get("arrayValue").get("values");
                if (arr == null || !arr.isArray()) continue;
                // 跳過僅含 GUIDED_DINING_* 標籤的陣列
                if (arr.size() == 1) {
                    String v = arr.get(0).path("stringValue").asText();
                    if (v.startsWith("GUIDED_DINING_")) continue;
                }
                for (JsonNode n : arr) {
                    if (n.has("stringValue")) {
                        String commentText = n.get("stringValue").asText();
                        if (!commentText.startsWith("GUIDED_DINING_")) {
                            ratedComments.add(new AbstractMap.SimpleEntry<>(commentText, ratingValue));
                        }
                    }
                }
            }
        }
        
        // 按評分從高到低排序評論
        ratedComments.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        
        // 只取前 maxCommentsToProcess 條評論
        for (int i = 0; i < Math.min(maxCommentsToProcess, ratedComments.size()); i++) {
            sj.add(ratedComments.get(i).getKey());
            validComments++;
        }

        String allComments = sj.toString().trim();
        if (allComments.isEmpty()) {
            System.err.println("❌ 沒有可用的文字評論，無法分析。");
            return;
        }
        System.out.println("✅ 處理了 " + validComments + " 條精選評論 (按評分排序，最多處理 " + maxCommentsToProcess + " 條)");

        // 3️⃣ 建立 Prompt 與呼叫 Ollama（使用自動下載系統）
        System.out.println("🤖 準備 AI 分析...");
        String prompt = "你是專業的餐飲評論分析師，請根據下方多則顧客留言，" +
            "用「繁體中文」寫一份詳細的分析報告，包含：\n\n" +
            "**菜色飲品特色：** 分析顧客對餐點、飲品的評價，包括味道、品質、特色菜品等。\n\n" +
            "**服務優缺點：** 詳細說明服務人員的態度、專業度、服務速度等優缺點。\n\n" +
            "**店內氛圍：** 描述用餐環境、裝潢風格、舒適度、適合的場合等。\n\n" +
            "**經營改善建議：** 基於顧客反饋，提供具體可行的經營改善建議。\n\n" +
            "請提供完整詳細的分析，每個部分都要充分說明，" +
            "文字要自然流暢，不要使用條列符號或標題格式。\n\n" +
            "顧客留言：\n" + allComments;

        String summary = callOllamaWithAutoSetup(prompt);

        // 若模型誤回英文，再翻譯一次
        if (!looksChinese(summary)) {
            System.out.println("⚠️ 偵測到非中文回應，進行翻譯...");
            summary = callOllamaWithAutoSetup("""
                請把下列內容完整翻成「繁體中文」，不要加任何註解：
                """ + summary);
        }

        // 4️⃣ 輸出
        System.out.println("====== 特色文字摘要 ======\n");
        System.out.println(summary.trim());

        if (outFile != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("analysis_time", OffsetDateTime.now().toString());
            result.put("summary", summary.trim());
            result.put("restaurant_id", restaurantId);
            result.put("total_reviews", docs.size());
            result.put("valid_comments", validComments);
            mapper.writeValue(outFile.toFile(), result);
            System.out.println("\n✓ 已寫入 " + outFile.toAbsolutePath());
        }
        
        System.out.println("🎉 分析完成！");
    }

    // ------------ Firestore 讀取 ------------
    private static JsonNode fetchReviews(String restaurantId) throws Exception {
        String url = String.format(
            "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/"
          + "restaurants/%s/reviews",
            PROJECT_ID, restaurantId);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Firestore 讀取失敗，HTTP "
                    + conn.getResponseCode());
        }

        try (InputStream is = conn.getInputStream()) {
            JsonNode root = mapper.readTree(is);
            return root.get("documents");
        }
    }

    // ------------ 使用自動下載系統的 Ollama 呼叫 ------------
    private static String callOllamaWithAutoSetup(String prompt) {
        try {
            System.out.println("🔄 檢查並準備 AI 模型...");
            
            // 使用現有的自動下載系統
            String response = OllamaAPI.generateCompletion(prompt, DEFAULT_MODEL);
            
            // 檢查是否是備用回應（表示 Ollama 不可用）
            if (response.contains("無法啟動Ollama") || response.contains("不可用") || response.contains("備用回應")) {
                System.out.println("⚠️ AI 模型暫時不可用，使用內建分析...");
                return generateFallbackAnalysis(prompt);
            }
            
            System.out.println("✅ AI 分析完成");
            return response.trim();
            
        } catch (Exception e) {
            System.err.println("⚠️ AI 分析發生錯誤，使用內建分析: " + e.getMessage());
            return generateFallbackAnalysis(prompt);
        }
    }

    // ------------ 備用分析功能 ------------
    private static String generateFallbackAnalysis(String prompt) {
        // 從 prompt 中提取評論內容
        String comments = prompt.substring(prompt.indexOf("顧客留言：") + 5);
        
        // 簡單的關鍵詞分析
        Map<String, Integer> positiveWords = new HashMap<>();
        Map<String, Integer> negativeWords = new HashMap<>();
        
        // 正面詞彙
        String[] positive = {"好吃", "美味", "推薦", "不錯", "滿意", "喜歡", "棒", "優秀", "新鮮", "香", "甜", "服務好", "親切", "快速"};
        // 負面詞彙  
        String[] negative = {"難吃", "不好吃", "失望", "貴", "慢", "冷", "鹹", "油膩", "服務差", "態度不好", "等很久", "小份"};
        
        for (String word : positive) {
            int count = comments.split(word).length - 1;
            if (count > 0) positiveWords.put(word, count);
        }
        
        for (String word : negative) {
            int count = comments.split(word).length - 1;
            if (count > 0) negativeWords.put(word, count);
        }
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("根據顧客評論分析，");
        
        if (!positiveWords.isEmpty()) {
            analysis.append("餐廳的主要優點包括");
            positiveWords.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> analysis.append(entry.getKey()).append("、"));
            analysis.setLength(analysis.length() - 1); // 移除最後的頓號
            analysis.append("。");
        }
        
        if (!negativeWords.isEmpty()) {
            analysis.append("需要改進的方面包括");
            negativeWords.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(2)
                .forEach(entry -> analysis.append(entry.getKey()).append("、"));
            analysis.setLength(analysis.length() - 1);
            analysis.append("。");
        }
        
        analysis.append("建議餐廳持續關注顧客反饋，針對主要問題進行改善，同時保持現有優勢。");
        
        return analysis.toString();
    }

    // ------------ 中文檢查：至少 30% 漢字 ------------
    private static boolean looksChinese(String text) {
        long han = text.codePoints()
                .filter(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN)
                .count();
        return han >= text.length() * 0.3;
    }
}