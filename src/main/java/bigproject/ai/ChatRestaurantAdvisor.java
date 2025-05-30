package bigproject.ai;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.*;

public class ChatRestaurantAdvisor {
    // Ollama API 設定，可用環境變數覆寫
    private static final String OLLAMA_URL =
        System.getenv().getOrDefault("OLLAMA_URL", "http://localhost:11434/api/generate");
    private static final String DEFAULT_MODEL =
        System.getenv().getOrDefault("OLLAMA_MODEL", "gemma3:4b");
    
    // 對話歷史
    private List<String> conversationHistory = new ArrayList<>();
    private String restaurantFeatures = "";
    
    /**
     * 設置餐廳特色資訊
     */
    public void setRestaurantFeatures(String features) {
        this.restaurantFeatures = features;
        System.out.println("🍽️ 已設置餐廳特色資訊，長度: " + features.length() + " 字元");
    }
    
    /**
     * 與 AI 對話
     */
    public String chatWithAI(String question) {
        try {
            // 記錄用戶輸入
            System.out.println("🗣️ 用戶問題: " + question);
            conversationHistory.add("營業者：" + question);
            
            // 改進的prompt構建，確保AI能夠回應用戶的具體問題
            StringBuilder prompt = new StringBuilder();
            
            // 系統角色定義
            prompt.append("你是一位專業的餐飲經營顧問AI助手。請根據以下資訊回答問題：\n\n");
            
            // 餐廳特色資訊（如果有的話）
            if (restaurantFeatures != null && !restaurantFeatures.trim().isEmpty()) {
                prompt.append("餐廳特色分析結果：\n");
                prompt.append(restaurantFeatures).append("\n\n");
            }
            
            // 添加最近的對話歷史（只保留最近5輪對話以避免prompt過長）
            if (conversationHistory.size() > 10) {
                prompt.append("最近的對話：\n");
                List<String> recentHistory = conversationHistory.subList(
                    Math.max(0, conversationHistory.size() - 10), 
                    conversationHistory.size()
                );
                for (String turn : recentHistory) {
                    prompt.append(turn).append("\n");
                }
            } else {
                prompt.append("對話記錄：\n");
                for (String turn : conversationHistory) {
                    prompt.append(turn).append("\n");
                }
            }
            
            // 明確的指示
            prompt.append("\n請根據用戶的問題提供具體、實用的建議。");
            prompt.append("如果問題與餐廳經營相關，請結合餐廳特色資訊來回答。");
            prompt.append("請直接回答問題，不要重複餐廳特色資訊。\n\n");
            prompt.append("AI：");
            
            System.out.println("🤖 發送prompt到Ollama: " + prompt.toString().substring(0, Math.min(200, prompt.length())) + "...");
            
            String reply = callOllama(prompt.toString());
            
            // 清理回應，移除可能的重複內容
            reply = cleanResponse(reply);
            
            // 若回應是英文，再翻譯成繁體中文
            if (!looksChinese(reply)) {
                System.out.println("🔄 偵測到英文回應，正在翻譯...");
                reply = callOllama("請把下列內容完整翻譯成「繁體中文」，保持原意但使用自然的中文表達：\n" + reply);
            }
            
            conversationHistory.add("AI：" + reply.trim());
            System.out.println("✅ AI回應: " + reply.trim());
            return reply.trim();
            
        } catch (Exception e) {
            System.err.println("❌ AI 對話失敗: " + e.getMessage());
            e.printStackTrace();
            return "抱歉，AI 服務暫時無法使用，請稍後再試。\n錯誤詳情：" + e.getMessage();
        }
    }
    
    /**
     * 清理AI回應，移除重複或無關的內容
     */
    private String cleanResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "抱歉，我無法理解您的問題，請換個方式提問。";
        }
        
        // 移除可能的前綴標記
        response = response.replaceAll("^AI：", "").trim();
        response = response.replaceAll("^AI:", "").trim();
        
        // 如果回應過短，提示用戶
        if (response.length() < 10) {
            return "我需要更多資訊才能給您詳細的建議，請提供更具體的問題。";
        }
        
        return response;
    }
    
    /**
     * 清空對話歷史
     */
    public void clearHistory() {
        conversationHistory.clear();
        System.out.println("🗑️ 已清空對話歷史");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("用法: java ChatRestaurantAdvisor \"<餐廳特色描述>\"");
            System.exit(1);
        }
        String features = args[0];
        Scanner scanner = new Scanner(System.in);
        
        // 創建ChatRestaurantAdvisor實例
        ChatRestaurantAdvisor advisor = new ChatRestaurantAdvisor();
        advisor.setRestaurantFeatures(features);

        System.out.println("── 已載入餐廳特色 ──");
        System.out.println(features);
        System.out.println("輸入你的問題，輸入 exit 離開。");

        while (true) {
            System.out.print("> ");
            String question = scanner.nextLine().trim();
            if (question.isEmpty()) continue;
            if ("exit".equalsIgnoreCase(question)) break;

            String reply = advisor.chatWithAI(question);
            System.out.println("AI: " + reply);
        }

        System.out.println("結束對話。");
        scanner.close();
    }

    private static String callOllama(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        // 手動構建 JSON，不依賴 Jackson
        String jsonBody = String.format(
            "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
            DEFAULT_MODEL,
            prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        );
        
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
            
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Ollama 呼叫失敗：" + resp.statusCode() + " / " + resp.body());
        }
        
        // 簡單解析 JSON 回應
        String responseBody = resp.body();
        int responseStart = responseBody.indexOf("\"response\":\"") + 12;
        int responseEnd = responseBody.lastIndexOf("\",\"done\"");
        if (responseStart > 11 && responseEnd > responseStart) {
            return responseBody.substring(responseStart, responseEnd)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\r", "\r");
        } else {
            throw new RuntimeException("無法解析 Ollama 回應: " + responseBody);
        }
    }

    private static boolean looksChinese(String text) {
        long hanCount = text.codePoints()
            .filter(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN)
            .count();
        return hanCount >= text.length() * 0.3;
    }
}