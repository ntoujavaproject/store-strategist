package bigproject.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class RestaurantAdvisorPipeline {

    private static final String OLLAMA_URL =
        System.getenv().getOrDefault("OLLAMA_URL", "http://localhost:11434/api/generate");
    private static final String MODEL =
        System.getenv().getOrDefault("OLLAMA_MODEL", "gemma3:1b");

    private static final ObjectMapper M = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);
    private static final HttpClient C = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("用法: java -jar restaurant-advisor.jar <reviews.json> <recommendations.json>");
            System.exit(1);
        }
        Path in  = Path.of(args[0]);
        Path out = Path.of(args[1]);

        // 1️⃣ 讀取多筆評論
        JsonNode reviews = M.readTree(in.toFile());
        if (!reviews.isArray()) {
            throw new IllegalArgumentException("reviews.json 必須是一個 JSON 陣列");
        }
        List<String> comments = new ArrayList<>();
        for (JsonNode r : reviews) {
            JsonNode cmt = r.get("評論");
            if (cmt == null || cmt.isNull()) continue;
            if (cmt.isTextual()) {
                comments.add(cmt.asText());
            } else if (cmt.isArray()) {
                if (cmt.size()==1 && "GUIDED_DINING_FOOD_ASPECT".equals(cmt.get(0).asText()))
                    continue;
                for (JsonNode e : cmt) if (e.isTextual()) comments.add(e.asText());
            }
        }
        if (comments.isEmpty()) {
            throw new RuntimeException("找不到任何可用文字評論");
        }
        String joined = comments.stream().collect(Collectors.joining("\n"));

        // 2️⃣ 面向擷取（強制繁體中文）
        String aspectPrompt = """
            你是一位餐飲顧問，請用「繁體中文」閱讀下方多則顧客留言，
            列出裡面提到的「關鍵面向」及「對應關鍵詞」。
            面向範例：菜色特色、服務品質、店內氛圍、價格定位、營運效率…
            請以 JSON 格式回覆：
            {
              "菜色特色": ["…","…"],
              "服務品質": ["…","…"],
              …
            }
            顧客留言：
            """ + joined;
        JsonNode aspectJson = callOllamaJson(aspectPrompt);

        // 3️⃣ 情感與優先級分析（強制繁體中文）
        String sentimentPrompt = """
            你是一位數據分析師，請用「繁體中文」根據下方「面向擷取結果」與「原始顧客留言」：
            1. 計算每個面向的正面評論數 positive、負面評論數 negative，
               並算出優先處理指數 priority = negative/(positive+negative)。
            2. 請以 JSON 格式回覆：
            {
              "菜色特色": {"positive":10,"negative":2,"priority":0.17},
              …
            }
            面向擷取結果：
            """ + M.writeValueAsString(aspectJson) + "\n原始顧客留言：\n" + joined;
        JsonNode sentimentJson = callOllamaJson(sentimentPrompt);

        // 4️⃣ 建議生成（強制繁體中文）
        String recPrompt = """
            你是餐飲管理專家，請用「繁體中文」根據下方「面向情感分析結果」生成具體改進建議：
            - 對於 priority ≥ 0.3 的面向，每條建議 1–2 句，並標明「執行時間」與「預估成本（低/中/高）」。
            - 最後提供三個月內的優先落地計畫。
            請以 JSON 格式回覆：
            {
              "recommendations":[
                {"aspect":"…","suggestion":"…","timeline":"…","cost":"…"},
                …
              ],
              "roadmap":["…","…",…]
            }
            面向情感分析結果：
            """ + M.writeValueAsString(sentimentJson);
        JsonNode recJson = callOllamaJson(recPrompt);

        // 5️⃣ 後處理：若任何建議文字不是中文，則自動翻譯整段 JSON
        String recText = M.writeValueAsString(recJson);
        if (!looksChinese(recText)) {
            System.out.println("⚠️ 偵測到非中文字元，啟動翻譯後處理…");
            String translatePrompt = """
                請將下列 JSON 內容完整且忠實地翻譯成「繁體中文」，
                不要添加註解或省略任何句子，並保持 JSON 結構不變：
                """ + recText;
            JsonNode translated = callOllamaJson(translatePrompt);
            recJson = translated;
        }

        // 6️⃣ 輸出最終建議
        M.writeValue(out.toFile(), recJson);
        System.out.println("完成！建議已寫入： " + out.toAbsolutePath());
    }

    /** 偵測字串是否主要為中文（至少 20% 漢字） */
    private static boolean looksChinese(String text) {
        long han = text.codePoints()
                       .filter(cp -> Character.UnicodeScript.of(cp) ==
                                     Character.UnicodeScript.HAN)
                       .count();
        return han >= text.length() * 0.2;
    }

    /** 呼叫 Ollama，並解析出 JSON */
    private static JsonNode callOllamaJson(String prompt) throws IOException, InterruptedException {
        String body = M.writeValueAsString(Map.of(
            "model",  MODEL,
            "prompt", prompt,
            "stream", false
        ));
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_URL))
            .header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> resp = C.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Ollama 調用失敗: " + resp.body());
        }
        return M.readTree(resp.body()).path("response");
    }
} 