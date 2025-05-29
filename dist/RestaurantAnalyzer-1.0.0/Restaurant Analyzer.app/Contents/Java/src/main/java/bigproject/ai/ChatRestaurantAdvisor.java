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
            conversationHistory.add("營業者：" + question);
            
            // 組建 prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是餐飲經營顧問，根據以下餐廳特色與對話，給出具體經營建議。\n");
            prompt.append("餐廳特色：\n").append(restaurantFeatures).append("\n");
            prompt.append("對話記錄：\n");
            for (String turn : conversationHistory) {
                prompt.append(turn).append("\n");
            }
            prompt.append("AI：");
            
            String reply = callOllama(prompt.toString());
            
            // 若回英文，再翻譯
            if (!looksChinese(reply)) {
                reply = callOllama("請把下列內容完整翻成「繁體中文」，不要加任何註解：\n" + reply);
            }
            
            conversationHistory.add("AI：" + reply.trim());
            return reply.trim();
            
        } catch (Exception e) {
            System.err.println("❌ AI 對話失敗: " + e.getMessage());
            return "抱歉，AI 服務暫時無法使用，請稍後再試。\n錯誤詳情：" + e.getMessage();
        }
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
        List<String> history = new ArrayList<>();

        System.out.println("── 已載入餐廳特色 ──");
        System.out.println(features);
        System.out.println("輸入你的問題，輸入 exit 離開。");

        while (true) {
            System.out.print("> ");
            String question = scanner.nextLine().trim();
            if (question.isEmpty()) continue;
            if ("exit".equalsIgnoreCase(question)) break;

            history.add("營業者：" + question);

            // 組 prompt：system + features + 歷史對話 + 本次提問
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是餐飲經營顧問，根據以下餐廳特色與對話，給出具體經營建議。\n");
            prompt.append("餐廳特色：\n").append(features).append("\n");
            prompt.append("對話記錄：\n");
            for (String turn : history) {
                prompt.append(turn).append("\n");
            }
            prompt.append("AI：");

            String reply = callOllama(prompt.toString());
            // 若回英文，再翻譯
            if (!looksChinese(reply)) {
                reply = callOllama("請把下列內容完整翻成「繁體中文」，不要加任何註解：\n" + reply);
            }

            System.out.println("AI: " + reply.trim());
            history.add("AI：" + reply.trim());
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