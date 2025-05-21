package bigproject.ai;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ollama管理類，負責下載、安裝和管理Ollama
 */
public class OllamaManager {
    // Ollama官方下載URL
    private static final String OLLAMA_MAC_URL = "https://github.com/ollama/ollama/releases/latest/download/ollama-darwin";
    private static final String OLLAMA_WINDOWS_URL = "https://github.com/ollama/ollama/releases/latest/download/ollama-windows.zip";
    
    // 用於執行Ollama命令的線程池
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // Ollama本地安裝路徑
    private final Path ollamaPath;
    private Process ollamaProcess;
    private boolean isOllamaRunning = false;
    
    /**
     * 創建OllamaManager實例
     */
    public OllamaManager() {
        // 確定Ollama的安裝路徑
        ollamaPath = getOllamaInstallPath();
    }
    
    /**
     * 確保Ollama已安裝並啟動
     * @return 完成後的Future
     */
    public CompletableFuture<Boolean> ensureOllamaRunning() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 如果已經在運行，直接返回
                if (isOllamaRunning && ollamaProcess != null && ollamaProcess.isAlive()) {
                    return true;
                }
                
                // 首先檢查是否已安裝Ollama
                if (!isOllamaInstalled()) {
                    boolean installed = downloadAndInstallOllama();
                    if (!installed) {
                        System.err.println("無法安裝Ollama");
                        return false;
                    }
                }
                
                // 檢查Ollama服務是否已在運行
                if (!isOllamaServiceRunning()) {
                    // 啟動Ollama服務
                    startOllamaService();
                }
                
                // 檢查服務是否成功啟動
                int maxRetries = 5;
                for (int i = 0; i < maxRetries; i++) {
                    if (isOllamaServiceRunning()) {
                        isOllamaRunning = true;
                        break;
                    }
                    Thread.sleep(1000); // 等待1秒再次檢查
                }
                
                return isOllamaRunning;
            } catch (Exception e) {
                System.err.println("確保Ollama運行時發生錯誤: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * 下載並安裝Ollama
     * @return 是否成功安裝
     */
    private boolean downloadAndInstallOllama() {
        try {
            System.out.println("開始下載並安裝Ollama...");
            
            // 創建臨時目錄來存放下載的文件
            Path tempDir = Files.createTempDirectory("ollama_installer");
            
            // 根據操作系統選擇下載URL
            String downloadUrl = isWindows() ? OLLAMA_WINDOWS_URL : OLLAMA_MAC_URL;
            Path downloadPath = tempDir.resolve(isWindows() ? "ollama.zip" : "ollama");
            
            // 下載Ollama
            System.out.println("從 " + downloadUrl + " 下載Ollama...");
            downloadFile(new URL(downloadUrl), downloadPath);
            
            // 在Windows上解壓文件，在Mac上設置執行權限
            if (isWindows()) {
                System.out.println("解壓Ollama...");
                unzipFile(downloadPath, tempDir);
                // 複製可執行文件到安裝目錄
                Files.createDirectories(ollamaPath.getParent());
                Files.copy(tempDir.resolve("ollama.exe"), ollamaPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // 在Mac上設置執行權限
                System.out.println("設置Ollama執行權限...");
                Files.createDirectories(ollamaPath.getParent());
                Files.copy(downloadPath, ollamaPath, StandardCopyOption.REPLACE_EXISTING);
                setExecutablePermission(ollamaPath);
            }
            
            // 安裝完成後清理臨時文件
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("無法刪除臨時文件: " + path);
                    }
                });
            
            System.out.println("Ollama安裝完成!");
            return true;
        } catch (Exception e) {
            System.err.println("安裝Ollama時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 啟動Ollama服務
     */
    private void startOllamaService() throws IOException {
        System.out.println("啟動Ollama服務...");
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        if (isWindows()) {
            // Windows啟動命令
            processBuilder.command(ollamaPath.toString(), "serve");
        } else {
            // Mac/Linux啟動命令
            processBuilder.command(ollamaPath.toString(), "serve");
        }
        
        // 重定向輸出
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);
        
        // 啟動進程
        ollamaProcess = processBuilder.start();
        
        // 非阻塞地讀取輸出
        startOutputReader(ollamaProcess.getInputStream(), "Ollama輸出");
        startOutputReader(ollamaProcess.getErrorStream(), "Ollama錯誤");
        
        System.out.println("Ollama服務已啟動");
    }
    
    /**
     * 停止Ollama服務
     */
    public void stopOllamaService() {
        if (ollamaProcess != null && ollamaProcess.isAlive()) {
            System.out.println("停止Ollama服務...");
            ollamaProcess.destroy();
            try {
                if (!ollamaProcess.waitFor(5, TimeUnit.SECONDS)) {
                    // 如果5秒後進程還沒有終止，強制終止
                    ollamaProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            isOllamaRunning = false;
            System.out.println("Ollama服務已停止");
        }
    }
    
    /**
     * 檢查Ollama服務是否正在運行
     */
    private boolean isOllamaServiceRunning() {
        try {
            // 嘗試連接Ollama API端點來檢查服務是否運行
            URL url = new URL("http://localhost:11434/api/health");
            InputStream in = url.openStream();
            in.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 檢查Ollama是否已安裝
     */
    private boolean isOllamaInstalled() {
        return Files.exists(ollamaPath) && Files.isExecutable(ollamaPath);
    }
    
    /**
     * 獲取Ollama安裝路徑
     */
    private Path getOllamaInstallPath() {
        String userHome = System.getProperty("user.home");
        if (isWindows()) {
            return Paths.get(userHome, "AppData", "Local", "Ollama", "ollama.exe");
        } else {
            return Paths.get(userHome, ".ollama", "bin", "ollama");
        }
    }
    
    /**
     * 下載文件
     */
    private void downloadFile(URL url, Path destination) throws IOException {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile());
             FileChannel fileChannel = fileOutputStream.getChannel()) {
            
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }
    
    /**
     * 解壓文件(Windows)
     */
    private void unzipFile(Path zipFile, Path destDir) throws IOException {
        // 使用Java ZipInputStream解壓文件
        try (InputStream is = Files.newInputStream(zipFile);
             BufferedInputStream bis = new BufferedInputStream(is);
             java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(bis)) {
            
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = destDir.resolve(entry.getName());
                
                // 創建父目錄
                Files.createDirectories(entryPath.getParent());
                
                if (!entry.isDirectory()) {
                    // 複製文件
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                zis.closeEntry();
            }
        }
    }
    
    /**
     * 設置文件的執行權限(Mac/Linux)
     */
    private void setExecutablePermission(Path file) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        
        try {
            Files.setPosixFilePermissions(file, perms);
        } catch (UnsupportedOperationException e) {
            // 在不支持POSIX權限的系統上，嘗試使用命令
            try {
                Runtime.getRuntime().exec("chmod +x " + file.toString()).waitFor();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 判斷當前系統是否是Windows
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    
    /**
     * 非阻塞地讀取進程輸出
     */
    private void startOutputReader(InputStream inputStream, String logPrefix) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(logPrefix + ": " + line);
                }
            } catch (IOException e) {
                System.err.println(logPrefix + "讀取錯誤: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * 確保特定模型下載完成
     * @param modelName 模型名稱
     * @return 模型是否可用
     */
    public CompletableFuture<Boolean> ensureModelReady(String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 首先確保Ollama正在運行
                if (!isOllamaServiceRunning()) {
                    return false;
                }
                
                // 檢查模型是否已下載
                if (!isModelDownloaded(modelName)) {
                    // 下載模型
                    downloadModel(modelName);
                }
                
                // 再次檢查模型是否已下載
                return isModelDownloaded(modelName);
            } catch (Exception e) {
                System.err.println("確保模型可用時發生錯誤: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * 檢查模型是否已下載
     */
    private boolean isModelDownloaded(String modelName) {
        try {
            // 運行命令檢查模型列表
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (isWindows()) {
                processBuilder.command(ollamaPath.toString(), "list");
            } else {
                processBuilder.command(ollamaPath.toString(), "list");
            }
            
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(modelName)) {
                        return true;
                    }
                }
            }
            
            process.waitFor(10, TimeUnit.SECONDS);
            return false;
        } catch (Exception e) {
            System.err.println("檢查模型時發生錯誤: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 下載模型
     */
    private void downloadModel(String modelName) throws IOException, InterruptedException {
        System.out.println("開始下載模型: " + modelName);
        
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (isWindows()) {
            processBuilder.command(ollamaPath.toString(), "pull", modelName);
        } else {
            processBuilder.command(ollamaPath.toString(), "pull", modelName);
        }
        
        Process process = processBuilder.start();
        
        // 讀取輸出
        startOutputReader(process.getInputStream(), "模型下載輸出");
        startOutputReader(process.getErrorStream(), "模型下載錯誤");
        
        // 等待下載完成
        process.waitFor();
        System.out.println("模型下載完成: " + modelName);
    }
    
    /**
     * 關閉OllamaManager並釋放資源
     */
    public void shutdown() {
        stopOllamaService();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 