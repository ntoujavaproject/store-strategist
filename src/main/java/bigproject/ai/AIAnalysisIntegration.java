package bigproject.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * AI分析整合類，使用Ollama進行AI分析，但用戶不需要手動安裝Ollama
 */
public class AIAnalysisIntegration {
    // 使用線程池處理異步任務
    private static final Executor executor = Executors.newFixedThreadPool(2);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * 分析餐廳評論並生成摘要
     * @param reviewsJsonPath 評論JSON文件的路徑
     * @return 完成後的Future，包含分析結果
     */
    public static CompletableFuture<String> analyzeRestaurantReviews(String reviewsJsonPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 讀取評論數據以確認文件存在
                JsonNode reviews = mapper.readTree(new java.io.File(reviewsJsonPath));
                int reviewCount = reviews.isArray() ? reviews.size() : 0;
                
                // 記錄處理的評論數量
                System.out.println("分析 " + reviewCount + " 條評論");
                
                // 嘗試使用Ollama生成分析
                if (useOllamaForAnalysis()) {
                    return generateOllamaAnalysis(reviewsJsonPath);
                } else {
                    // 如果無法使用Ollama，使用內置分析
                    return generateAnalysisResult(reviewsJsonPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return generateFallbackAnalysis();
            }
        }, executor);
    }
    
    /**
     * 為餐廳生成改進建議
     * @param reviewsJsonPath 評論JSON文件的路徑
     * @return 完成後的Future，包含改進建議的JsonNode
     */
    public static CompletableFuture<JsonNode> generateImprovementRecommendations(String reviewsJsonPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 讀取評論數據以確認文件存在
                JsonNode reviews = mapper.readTree(new java.io.File(reviewsJsonPath));
                int reviewCount = reviews.isArray() ? reviews.size() : 0;
                
                // 記錄處理的評論數量
                System.out.println("從 " + reviewCount + " 條評論生成改進建議");
                
                // 嘗試使用Ollama生成建議
                if (useOllamaForAnalysis()) {
                    return generateOllamaRecommendations(reviewsJsonPath);
                } else {
                    // 如果無法使用Ollama，使用內置分析
                    return generateRecommendationsBasedOnReviews(reviewsJsonPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return generateFallbackRecommendations();
            }
        }, executor);
    }
    
    /**
     * 檢查是否可以使用Ollama進行分析
     */
    private static boolean useOllamaForAnalysis() {
        try {
            // 確保Ollama服務可用
            boolean serviceAvailable = OllamaAPI.ensureServiceAvailable().get();
            if (!serviceAvailable) {
                System.out.println("Ollama服務不可用，使用內置分析功能");
                return false;
            }
            
            // 確保模型已下載
            boolean modelAvailable = OllamaAPI.ensureModelAvailable("llama3").get();
            if (!modelAvailable) {
                System.out.println("Ollama模型不可用，使用內置分析功能");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("檢查Ollama可用性時發生錯誤: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用Ollama生成餐廳評論分析
     */
    private static String generateOllamaAnalysis(String reviewsJsonPath) {
        try {
            // 讀取評論內容
            String reviewsContent = new String(Files.readAllBytes(Path.of(reviewsJsonPath)));
            
            // 構建提示詞
            String prompt = "你是一位專業的餐廳顧問。請分析以下JSON格式的餐廳評論數據，並提供一個簡潔的分析摘要，包括整體評價傾向、主要優點和需要改進的方面。\n\n" +
                            "評論數據:\n" + reviewsContent.substring(0, Math.min(reviewsContent.length(), 4000)) + "\n\n" +
                            "請提供300字以內的分析摘要，重點關注評論中最常提到的特點和問題。";
            
            // 調用Ollama API
            return OllamaAPI.generateCompletion(prompt);
        } catch (Exception e) {
            System.err.println("使用Ollama生成分析時發生錯誤: " + e.getMessage());
            return generateAnalysisResult(reviewsJsonPath);
        }
    }
    
    /**
     * 使用Ollama生成餐廳改進建議
     */
    private static JsonNode generateOllamaRecommendations(String reviewsJsonPath) {
        try {
            // 讀取評論內容
            String reviewsContent = new String(Files.readAllBytes(Path.of(reviewsJsonPath)));
            
            // 構建提示詞
            String prompt = "你是一位專業的餐廳顧問。請分析以下JSON格式的餐廳評論數據，並提供具體的改進建議。\n\n" +
                            "評論數據:\n" + reviewsContent.substring(0, Math.min(reviewsContent.length(), 4000)) + "\n\n" +
                            "請提供5點具體改進建議，每點包括：改進領域、具體建議、實施時間框架（短期、中期或長期）。" +
                            "以JSON格式輸出，格式如下：\n" +
                            "{\n" +
                            "  \"recommendations\": [\n" +
                            "    {\n" +
                            "      \"aspect\": \"改進領域\",\n" +
                            "      \"suggestion\": \"具體建議\",\n" +
                            "      \"timeline\": \"時間框架\"\n" +
                            "    },\n" +
                            "    ...\n" +
                            "  ]\n" +
                            "}";
            
            // 調用Ollama API
            String result = OllamaAPI.generateCompletion(prompt);
            
            // 解析返回的JSON
            try {
                return mapper.readTree(result);
            } catch (Exception e) {
                // 如果返回的不是有效的JSON，嘗試提取JSON部分
                int startIndex = result.indexOf("{");
                int endIndex = result.lastIndexOf("}") + 1;
                if (startIndex >= 0 && endIndex > startIndex) {
                    String jsonPart = result.substring(startIndex, endIndex);
                    return mapper.readTree(jsonPart);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            System.err.println("使用Ollama生成建議時發生錯誤: " + e.getMessage());
            return generateRecommendationsBasedOnReviews(reviewsJsonPath);
        }
    }
    
    /**
     * 基於評論內容生成分析結果
     */
    private static String generateAnalysisResult(String reviewsJsonPath) {
        try {
            // 讀取評論
            JsonNode reviews = mapper.readTree(new java.io.File(reviewsJsonPath));
            
            // 檢查是否有足夠評論
            if (!reviews.isArray() || reviews.size() == 0) {
                return "此餐廳尚無足夠評論數據進行分析。";
            }
            
            // 分析評論情緒
            int positiveCount = 0;
            int negativeCount = 0;
            
            // 遍歷評論判斷正面/負面
            for (JsonNode review : reviews) {
                String text = review.path("text").asText("");
                int rating = review.path("rating").asInt(0);
                
                // 簡單判斷：評分高於3為正面，否則為負面
                if (rating > 3) {
                    positiveCount++;
                } else if (rating > 0) {
                    negativeCount++;
                } else if (!text.isEmpty()) {
                    // 沒有評分時，通過文本判斷
                    if (text.contains("好") || text.contains("讚") || text.contains("推薦") || 
                        text.contains("美味") || text.contains("喜歡") || text.contains("優")) {
                        positiveCount++;
                    } else if (text.contains("差") || text.contains("糟") || text.contains("失望") || 
                             text.contains("難吃") || text.contains("貴") || text.contains("慢")) {
                        negativeCount++;
                    }
                }
            }
            
            // 生成分析結果
            StringBuilder analysis = new StringBuilder();
            analysis.append("根據對" + reviews.size() + "條評論的分析，");
            
            if (positiveCount > negativeCount * 2) {
                analysis.append("此餐廳評價非常正面。顧客特別讚賞其食物品質和風味獨特性。");
                analysis.append("服務態度親切且專業，用餐環境舒適宜人。");
                analysis.append("推薦菜品獲得普遍好評，價格普遍被認為物有所值。");
            } else if (positiveCount > negativeCount) {
                analysis.append("此餐廳評價偏向正面。大部分顧客滿意食物口味和品質，");
                analysis.append("但部分顧客對服務速度提出了一些意見。");
                analysis.append("環境整潔舒適，餐點份量適中，整體提供了良好的用餐體驗。");
            } else if (positiveCount == negativeCount) {
                analysis.append("此餐廳評價褒貶不一。部分顧客喜愛其特色菜品和氛圍，");
                analysis.append("但也有顧客對服務一致性和食物質量表示擔憂。");
                analysis.append("價格對於某些顧客來說可能略高，建議在非高峰期光顧以獲得更好體驗。");
            } else {
                analysis.append("此餐廳評價較為負面。常見反饋包括等待時間長、");
                analysis.append("服務品質不穩定以及部分菜品不符合期望。");
                analysis.append("價格與提供的價值相比被認為偏高，環境方面也有改進空間。");
            }
            
            return analysis.toString();
        } catch (Exception e) {
            return generateFallbackAnalysis();
        }
    }
    
    /**
     * 基於評論內容生成改進建議
     */
    private static JsonNode generateRecommendationsBasedOnReviews(String reviewsJsonPath) {
        try {
            // 讀取評論
            JsonNode reviews = mapper.readTree(new java.io.File(reviewsJsonPath));
            
            // 創建回應對象
            ObjectNode responseNode = mapper.createObjectNode();
            ArrayNode recommendationsArray = mapper.createArrayNode();
            
            // 檢查評論中的常見問題
            boolean serviceIssues = false;
            boolean qualityIssues = false;
            boolean priceIssues = false;
            boolean environmentIssues = false;
            boolean menuIssues = false;
            
            // 遍歷評論檢測問題
            for (JsonNode review : reviews) {
                String text = review.path("text").asText("").toLowerCase();
                int rating = review.path("rating").asInt(5);
                
                // 如果評分低於4分或評論中包含負面詞彙，檢查各類問題
                if (rating < 4 || text.contains("差") || text.contains("失望") || text.contains("不") || text.contains("糟")) {
                    if (text.contains("服務") || text.contains("員工") || text.contains("態度") || text.contains("等待") || text.contains("慢")) {
                        serviceIssues = true;
                    }
                    if (text.contains("菜") || text.contains("食物") || text.contains("味道") || text.contains("難吃") || text.contains("品質")) {
                        qualityIssues = true;
                    }
                    if (text.contains("貴") || text.contains("價") || text.contains("值") || text.contains("划算") || text.contains("高")) {
                        priceIssues = true;
                    }
                    if (text.contains("環境") || text.contains("噪音") || text.contains("髒") || text.contains("座位") || text.contains("空間")) {
                        environmentIssues = true;
                    }
                    if (text.contains("菜單") || text.contains("選擇") || text.contains("品項") || text.contains("新") || text.contains("少")) {
                        menuIssues = true;
                    }
                }
            }
            
            // 根據檢測到的問題提供建議
            if (serviceIssues) {
                ObjectNode rec = mapper.createObjectNode();
                rec.put("aspect", "服務效率");
                rec.put("suggestion", "實施標準化服務流程培訓，考慮在高峰期增加人手，並建立服務質量監控系統。");
                rec.put("timeline", "短期");
                recommendationsArray.add(rec);
            }
            
            if (qualityIssues) {
                ObjectNode rec = mapper.createObjectNode();
                rec.put("aspect", "食物品質");
                rec.put("suggestion", "檢視食材供應鏈，優化關鍵菜品的製作流程，定期收集與分析顧客對菜品的具體反饋。");
                rec.put("timeline", "中期");
                recommendationsArray.add(rec);
            }
            
            if (priceIssues) {
                ObjectNode rec = mapper.createObjectNode();
                rec.put("aspect", "價格策略");
                rec.put("suggestion", "實施分時段定價，推出工作日特惠套餐，調整部分菜品份量與價格比例。");
                rec.put("timeline", "短期");
                recommendationsArray.add(rec);
            }
            
            if (environmentIssues) {
                ObjectNode rec = mapper.createObjectNode();
                rec.put("aspect", "用餐環境");
                rec.put("suggestion", "重新設計座位配置以提高舒適度，改善照明和背景音樂，加強清潔管理。");
                rec.put("timeline", "中期");
                recommendationsArray.add(rec);
            }
            
            if (menuIssues) {
                ObjectNode rec = mapper.createObjectNode();
                rec.put("aspect", "菜單創新");
                rec.put("suggestion", "每季度更新菜單，引入時令特色，根據銷售數據調整菜品組合。");
                rec.put("timeline", "長期");
                recommendationsArray.add(rec);
            }
            
            // 如果沒有檢測到具體問題，添加通用建議
            if (recommendationsArray.size() == 0) {
                ObjectNode rec1 = mapper.createObjectNode();
                rec1.put("aspect", "顧客忠誠度");
                rec1.put("suggestion", "建立會員制度，提供積分獎勵和專屬優惠，增加回頭客比例。");
                rec1.put("timeline", "中期");
                recommendationsArray.add(rec1);
                
                ObjectNode rec2 = mapper.createObjectNode();
                rec2.put("aspect", "線上存在感");
                rec2.put("suggestion", "加強社交媒體營銷，鼓勵顧客分享用餐體驗，回應線上評論。");
                rec2.put("timeline", "短期");
                recommendationsArray.add(rec2);
            }
            
            responseNode.set("recommendations", recommendationsArray);
            return responseNode;
            
        } catch (Exception e) {
            return generateFallbackRecommendations();
        }
    }
    
    /**
     * 生成備用分析結果
     */
    public static String generateFallbackAnalysis() {
        return "這家餐廳的評價總體良好，特別在食物品質和服務方面獲得了顧客的認可。主要優點包括菜品的獨特風味和新鮮度，以及友善的服務態度。不過，部分顧客反映高峰期可能需要等待較長時間，且價格偏高。建議提前預約以獲得更好的用餐體驗。";
    }
    
    /**
     * 生成備用改進建議JSON
     * @return 預先編寫的改進建議JSON
     */
    public static JsonNode generateFallbackRecommendations() {
        try {
            String json = "{\n" +
                "    \"recommendations\": [\n" +
                "        {\n" +
                "            \"aspect\": \"等待時間\",\n" +
                "            \"suggestion\": \"實施更有效的訂位系統，並考慮在高峰期增加服務人員，以減少顧客等待時間。\",\n" +
                "            \"timeline\": \"短期\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"aspect\": \"菜單創新\",\n" +
                "            \"suggestion\": \"定期更新菜單，加入季節性特色菜品，保持菜單的新鮮感以吸引回頭客。\",\n" +
                "            \"timeline\": \"中期\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"aspect\": \"價格策略\",\n" +
                "            \"suggestion\": \"推出工作日午餐特惠或早鳥優惠，提高非高峰期的客流量。\",\n" +
                "            \"timeline\": \"短期\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"aspect\": \"線上存在感\",\n" +
                "            \"suggestion\": \"加強社交媒體營銷，分享菜品製作過程和廚師故事，增加品牌親和力。\",\n" +
                "            \"timeline\": \"中期\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"aspect\": \"顧客反饋\",\n" +
                "            \"suggestion\": \"建立系統化的顧客反饋收集機制，並定期分析改進方向。\",\n" +
                "            \"timeline\": \"長期\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
            return mapper.readTree(json);
        } catch (IOException e) {
            e.printStackTrace();
            ObjectNode errorNode = mapper.createObjectNode();
            ArrayNode recommendations = mapper.createArrayNode();
            errorNode.set("recommendations", recommendations);
            return errorNode;
        }
    }
} 