package bigproject.ai;

import java.io.File;
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
 * AI分析整合類，用於從主應用調用AI分析功能
 */
public class AIAnalysisIntegration {
    // 使用線程池處理異步任務
    private static final Executor executor = Executors.newFixedThreadPool(2);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * 分析餐廳評論並生成摘要
     * 
     * @param reviewsJsonPath 評論JSON文件路徑
     * @return 完成後的Future，包含生成的摘要文本
     */
    public static CompletableFuture<String> analyzeRestaurantReviews(String reviewsJsonPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 創建臨時輸出文件
                Path tempOutputPath = Files.createTempFile("analysis_", ".json");
                
                // 調用分析器
                RestaurantAnalyzer.main(new String[]{reviewsJsonPath, tempOutputPath.toString()});
                
                // 讀取結果
                JsonNode result = mapper.readTree(tempOutputPath.toFile());
                String summary = result.path("summary").asText();
                
                // 清理臨時文件
                Files.deleteIfExists(tempOutputPath);
                
                return summary;
            } catch (Exception e) {
                e.printStackTrace();
                return "分析過程發生錯誤: " + e.getMessage();
            }
        }, executor);
    }
    
    /**
     * 生成餐廳改進建議
     * 
     * @param reviewsJsonPath 評論JSON文件路徑
     * @return 完成後的Future，包含改進建議的JsonNode
     */
    public static CompletableFuture<JsonNode> generateImprovementRecommendations(String reviewsJsonPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 創建臨時輸出文件
                Path tempOutputPath = Files.createTempFile("recommendations_", ".json");
                
                // 調用分析器
                RestaurantAdvisorPipeline.main(new String[]{reviewsJsonPath, tempOutputPath.toString()});
                
                // 讀取結果
                JsonNode recommendations = mapper.readTree(tempOutputPath.toFile());
                
                // 清理臨時文件
                Files.deleteIfExists(tempOutputPath);
                
                return recommendations;
            } catch (Exception e) {
                e.printStackTrace();
                
                // 創建錯誤回應
                ObjectNode errorNode = mapper.createObjectNode();
                ArrayNode recArray = mapper.createArrayNode();
                ObjectNode rec = mapper.createObjectNode();
                rec.put("aspect", "錯誤");
                rec.put("suggestion", "處理過程中發生錯誤: " + e.getMessage());
                rec.put("timeline", "無");
                rec.put("cost", "無");
                recArray.add(rec);
                errorNode.set("recommendations", recArray);
                
                return errorNode;
            }
        }, executor);
    }
    
    /**
     * 產生假的分析結果（當無法連接AI服務時使用）
     * 
     * @return 預先編寫的分析摘要
     */
    public static String generateFallbackAnalysis() {
        return "餐廳主打海鮮料理與燒烤，以新鮮食材的呈現和特色烹調方式獲得顧客認可。招牌菜包括烤魚、海鮮拼盤以及特製醬料系列。服務方面，大部分員工態度親切且專業，但在尖峰時段存在上菜速度不一致的問題，偶有漏單情況。店內氛圍優雅舒適，裝潢融合現代與海洋元素，音樂選擇恰到好處。建議改善點為強化服務人員的培訓計劃，特別是在忙碌時段的工作協調與訂單跟進，同時可考慮實施餐點準備時間預估系統，讓顧客對等待時間有更明確的預期。";
    }
    
    /**
     * 產生假的改進建議（當無法連接AI服務時使用）
     * 
     * @return 預先編寫的改進建議JSON
     */
    public static JsonNode generateFallbackRecommendations() {
        try {
            String json = """
            {
                "recommendations": [
                    {
                        "aspect": "服務效率",
                        "suggestion": "建立標準化的服務流程，針對高峰期增加人手配置",
                        "timeline": "1個月內",
                        "cost": "中"
                    },
                    {
                        "aspect": "菜品口味",
                        "suggestion": "優化招牌菜品的醬料配方，根據顧客反饋調整口味",
                        "timeline": "2週內",
                        "cost": "低"
                    },
                    {
                        "aspect": "用餐環境",
                        "suggestion": "改善用餐區照明和背景音樂，提升整體用餐體驗",
                        "timeline": "3週內",
                        "cost": "中"
                    }
                ],
                "roadmap": [
                    "第1週：收集詳細顧客反饋，進行菜品口味調整實驗",
                    "第2-3週：設計並實施標準化服務流程，培訓員工",
                    "第4-6週：評估初步改進成效，調整用餐環境，引入新的背景音樂系統"
                ]
            }
            """;
            return mapper.readTree(json);
        } catch (IOException e) {
            e.printStackTrace();
            return mapper.createObjectNode();
        }
    }
} 