package bigproject.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OllamaAPI類，用於與本地Ollama服務通信
 */
public class OllamaAPI {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "gemma3:1b";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    private static OllamaManager ollamaManager;
    
    static {
        // 初始化OllamaManager
        ollamaManager = new OllamaManager();
    }
    
    /**
     * 檢查Ollama服務是否可用
     * @return 服務是否可用
     */
    public static boolean isServiceAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/health"))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 確保Ollama服務可用，如果不可用則啟動服務
     * @return 服務是否可用的Future
     */
    public static CompletableFuture<Boolean> ensureServiceAvailable() {
        return ollamaManager.ensureOllamaRunning();
    }
    
    /**
     * 確保指定模型已下載並可用
     * @param modelName 模型名稱
     * @return 模型是否可用的Future
     */
    public static CompletableFuture<Boolean> ensureModelAvailable(String modelName) {
        return ollamaManager.ensureModelReady(modelName);
    }
    
    /**
     * 同步生成文本補全
     * @param prompt 提示詞
     * @return 生成的文本
     */
    public static String generateCompletion(String prompt) {
        return generateCompletion(prompt, DEFAULT_MODEL);
    }
    
    /**
     * 同步生成文本補全
     * @param prompt 提示詞
     * @param model 使用的模型
     * @return 生成的文本
     */
    public static String generateCompletion(String prompt, String model) {
        try {
            // 確保服務可用並且模型已下載
            boolean serviceReady = ensureServiceAvailable().get();
            if (!serviceReady) {
                return "無法啟動Ollama服務，使用備用回應。";
            }
            
            boolean modelReady = ensureModelAvailable(model).get();
            if (!modelReady) {
                return "模型 " + model + " 不可用，使用備用回應。";
            }
            
            // 構建請求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("API請求失敗: " + response.statusCode());
            }
            
            JsonNode responseJson = mapper.readTree(response.body());
            return responseJson.path("response").asText();
        } catch (Exception e) {
            System.err.println("生成文本時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return "生成文本時發生錯誤，請稍後再試。";
        }
    }
    
    /**
     * 異步生成文本補全
     * @param prompt 提示詞
     * @return 包含生成文本的Future
     */
    public static CompletableFuture<String> generateCompletionAsync(String prompt) {
        return generateCompletionAsync(prompt, DEFAULT_MODEL);
    }
    
    /**
     * 異步生成文本補全
     * @param prompt 提示詞
     * @param model 使用的模型
     * @return 包含生成文本的Future
     */
    public static CompletableFuture<String> generateCompletionAsync(String prompt, String model) {
        return CompletableFuture.supplyAsync(() -> generateCompletion(prompt, model), executor);
    }
    
    /**
     * 在應用程序關閉時釋放資源
     */
    public static void shutdown() {
        executor.shutdown();
        if (ollamaManager != null) {
            ollamaManager.shutdown();
        }
    }
} 