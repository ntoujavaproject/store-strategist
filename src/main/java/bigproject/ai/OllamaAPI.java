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
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OllamaAPI類，用於與本地Ollama服務通信
 */
public class OllamaAPI {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "gemma3:4b";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(60))
            .build();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    private static OllamaManager ollamaManager;
    
    static {
        // 初始化OllamaManager
        ollamaManager = new OllamaManager();
        
        // 添加 JVM shutdown hook 確保在任何情況下都會清理資源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔧 JVM Shutdown Hook: 開始清理 Ollama 資源...");
            try {
                if (ollamaManager != null) {
                    ollamaManager.forceShutdown(); // 使用強制關閉
                }
                executor.shutdown();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                System.out.println("✅ JVM Shutdown Hook: Ollama 資源清理完成");
            } catch (Exception e) {
                System.err.println("⚠️ JVM Shutdown Hook: 清理資源時發生錯誤: " + e.getMessage());
            }
        }, "OllamaCleanupThread"));
    }
    
    /**
     * 檢查Ollama服務是否可用
     * @return 服務是否可用
     */
    public static boolean isServiceAvailable() {
        return isServiceAvailable(1); // 預設只檢查一次
    }
    
    /**
     * 檢查Ollama服務是否可用（帶重試）
     * @param maxRetries 最大重試次數
     * @return 服務是否可用
     */
    public static boolean isServiceAvailable(int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 首先檢查端口是否可達
                if (!isPortReachable("localhost", 11434, 2000)) {
                    System.out.println("🔌 第 " + attempt + " 次嘗試: 端口 11434 無法連接");
                    if (attempt < maxRetries) {
                        Thread.sleep(1000);
                        continue;
                    }
                    return false;
                }
                
                // 檢查 health 端點
                HttpRequest healthRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:11434/api/health"))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                
                HttpResponse<String> healthResponse = client.send(healthRequest, HttpResponse.BodyHandlers.ofString());
                if (healthResponse.statusCode() == 200) {
                    System.out.println("✅ Ollama health check 成功");
                    return true;
                }
                
                // 如果 health 失败，尝试 tags 端點
                HttpRequest tagsRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:11434/api/tags"))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                
                HttpResponse<String> tagsResponse = client.send(tagsRequest, HttpResponse.BodyHandlers.ofString());
                if (tagsResponse.statusCode() == 200) {
                    System.out.println("✅ Ollama tags API 回應正常");
                    return true;
                }
                
                System.out.println("⚠️ 第 " + attempt + " 次嘗試: Ollama API 回應異常 (HTTP " + tagsResponse.statusCode() + ")");
                
            } catch (Exception e) {
                System.out.println("❌ 第 " + attempt + " 次嘗試失敗: " + e.getMessage());
            }
            
            // 如果不是最後一次嘗試，等待後重試
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(2000); // 等待2秒後重試
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 檢查指定主機和端口是否可達
     * @param host 主機名
     * @param port 端口號
     * @param timeout 超時時間（毫秒）
     * @return true 如果可達，false 否則
     */
    private static boolean isPortReachable(String host, int port, int timeout) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), timeout);
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /**
     * 確保Ollama服務可用，如果不可用則啟動服務（帶重試）
     * @return 服務是否可用的Future
     */
    public static CompletableFuture<Boolean> ensureServiceAvailable() {
        return ensureServiceAvailable(3); // 預設重試3次
    }
    
    /**
     * 確保Ollama服務可用，如果不可用則啟動服務（帶重試）
     * @param maxRetries 最大重試次數
     * @return 服務是否可用的Future
     */
    public static CompletableFuture<Boolean> ensureServiceAvailable(int maxRetries) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("🚀 開始確保 Ollama 服務可用...");
            
            // 首先檢查服務是否已經可用
            if (isServiceAvailable(2)) {
                System.out.println("✅ Ollama 服務已經可用");
                return true;
            }
            
            System.out.println("⚠️ Ollama 服務不可用，嘗試啟動...");
            
            // 嘗試啟動服務
            try {
                boolean serviceStarted = ollamaManager.ensureOllamaRunning().get();
                if (!serviceStarted) {
                    System.err.println("❌ 無法啟動 Ollama 服務");
                    return false;
                }
                
                // 等待服務完全啟動並進行重試檢查
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    System.out.println("🔄 第 " + attempt + " 次檢查服務可用性...");
                    
                    if (isServiceAvailable(2)) {
                        System.out.println("🎉 Ollama 服務已成功啟動並可用");
                        return true;
                    }
                    
                    if (attempt < maxRetries) {
                        System.out.println("⏳ 等待 3 秒後重試...");
                        Thread.sleep(3000);
                    }
                }
                
                System.err.println("❌ 服務啟動後仍然無法連接");
                return false;
                
            } catch (Exception e) {
                System.err.println("❌ 啟動 Ollama 服務時發生錯誤: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
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