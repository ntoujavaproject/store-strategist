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
    // Ollama官方下載URL - 更新為最新版本
    private static final String OLLAMA_MAC_URL = "https://github.com/ollama/ollama/releases/latest/download/ollama-darwin.tgz";
    private static final String OLLAMA_WINDOWS_URL = "https://github.com/ollama/ollama/releases/latest/download/ollama-windows-amd64.zip";
    
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
                // 首先檢查服務是否已經在運行
                if (isOllamaServiceRunning()) {
                    System.out.println("檢測到 Ollama 服務已在運行");
                    isOllamaRunning = true;
                    return true;
                }
                
                // 檢查是否已安裝Ollama
                if (!isOllamaInstalled()) {
                    System.out.println("Ollama 未安裝，開始下載安裝...");
                    boolean installed = downloadAndInstallOllama();
                    if (!installed) {
                        System.err.println("無法安裝Ollama");
                        return false;
                    }
                }
                
                // 清理可能存在的端口衝突
                cleanupPortConflicts();
                
                // 如果服務沒有在運行，嘗試啟動
                try {
                    startOllamaService();
                
                    // 等待服務啟動
                    int maxRetries = 10;
                for (int i = 0; i < maxRetries; i++) {
                        Thread.sleep(1000); // 等待1秒
                    if (isOllamaServiceRunning()) {
                            System.out.println("Ollama 服務啟動成功");
                            isOllamaRunning = true;
                            return true;
                        }
                    }
                    
                    System.err.println("Ollama 服務啟動超時");
                    return false;
                    
                } catch (IOException e) {
                    // 如果啟動失敗，檢查是否是因為服務已經在運行
                    if (e.getMessage().contains("address already in use") || 
                        e.getMessage().contains("bind") ||
                        isOllamaServiceRunning()) {
                        System.out.println("檢測到 Ollama 服務已在其他進程中運行，將使用現有服務");
                        isOllamaRunning = true;
                        return true;
                    } else {
                        System.err.println("啟動 Ollama 服務時發生錯誤: " + e.getMessage());
                        return false;
                    }
                }
                
            } catch (Exception e) {
                System.err.println("確保Ollama運行時發生錯誤: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * 清理端口衝突 - 終止佔用 11434 端口的舊進程
     */
    private void cleanupPortConflicts() {
        try {
            System.out.println("🔍 檢查端口 11434 是否被佔用...");
            
            // 使用 lsof 找出佔用端口的進程
            ProcessBuilder pb = new ProcessBuilder("lsof", "-ti", ":11434");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String pid = line.trim();
                    if (!pid.isEmpty()) {
                        System.out.println("🔧 發現佔用端口的進程 PID: " + pid);
                        
                        // 檢查這個進程是否是 Ollama
                        if (isOllamaProcess(pid)) {
                            System.out.println("💀 終止舊的 Ollama 進程: " + pid);
                            killProcess(pid);
                        } else {
                            System.out.println("⚠️ 發現非 Ollama 進程佔用端口: " + pid + "，跳過終止");
                        }
                    }
                }
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            
            // 等待一下讓端口釋放
            Thread.sleep(2000);
            
        } catch (Exception e) {
            System.err.println("清理端口衝突時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 檢查指定 PID 是否是 Ollama 進程
     */
    private boolean isOllamaProcess(String pid) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ps", "-p", pid, "-o", "command=");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String command = reader.readLine();
                if (command != null) {
                    return command.contains("ollama") || command.contains(".ollama");
                }
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            return false;
            
        } catch (Exception e) {
            System.err.println("檢查進程時發生錯誤: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 終止指定 PID 的進程
     */
    private void killProcess(String pid) {
        try {
            // 先嘗試優雅終止 (SIGTERM)
            ProcessBuilder pb = new ProcessBuilder("kill", pid);
            Process process = pb.start();
            process.waitFor(3, TimeUnit.SECONDS);
            
            // 等待一下看進程是否已終止
            Thread.sleep(1000);
            
            // 檢查進程是否還在運行
            ProcessBuilder checkPb = new ProcessBuilder("ps", "-p", pid);
            Process checkProcess = checkPb.start();
            int exitCode = checkProcess.waitFor();
            
            if (exitCode == 0) {
                // 進程還在運行，使用強制終止 (SIGKILL)
                System.out.println("🔨 優雅終止失敗，使用強制終止");
                ProcessBuilder killPb = new ProcessBuilder("kill", "-9", pid);
                Process killProcess = killPb.start();
                killProcess.waitFor(3, TimeUnit.SECONDS);
            }
            
        } catch (Exception e) {
            System.err.println("終止進程時發生錯誤: " + e.getMessage());
        }
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
            
            // 根據操作系統選擇下載URL和檔案名
            String downloadUrl;
            String fileName;
            if (isWindows()) {
                downloadUrl = OLLAMA_WINDOWS_URL;
                fileName = "ollama.zip";
            } else {
                downloadUrl = OLLAMA_MAC_URL;
                fileName = "ollama.tgz";
            }
            
            Path downloadPath = tempDir.resolve(fileName);
            
            // 下載Ollama
            System.out.println("從 " + downloadUrl + " 下載Ollama...");
            downloadFile(new URL(downloadUrl), downloadPath);
            
            // 解壓並安裝
            if (isWindows()) {
                System.out.println("解壓Ollama...");
                unzipFile(downloadPath, tempDir);
                // 複製可執行文件到安裝目錄
                Files.createDirectories(ollamaPath.getParent());
                Files.copy(tempDir.resolve("ollama.exe"), ollamaPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Mac: 解壓 .tgz 檔案
                System.out.println("解壓Ollama...");
                untarGzFile(downloadPath, tempDir);
                // 複製可執行文件到安裝目錄
                Files.createDirectories(ollamaPath.getParent());
                Path extractedOllama = tempDir.resolve("ollama");
                if (!Files.exists(extractedOllama)) {
                    // 可能在子目錄中
                    extractedOllama = findOllamaExecutable(tempDir);
                }
                if (extractedOllama != null && Files.exists(extractedOllama)) {
                    Files.copy(extractedOllama, ollamaPath, StandardCopyOption.REPLACE_EXISTING);
                setExecutablePermission(ollamaPath);
                } else {
                    throw new IOException("找不到 Ollama 可執行檔案");
                }
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
     * 解壓 .tar.gz 檔案 (Mac)
     */
    private void untarGzFile(Path tarGzFile, Path destDir) throws IOException, InterruptedException {
        // 使用系統的 tar 命令解壓
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tarGzFile.toString(), "-C", destDir.toString());
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("解壓失敗，退出碼: " + exitCode);
        }
    }
    
    /**
     * 在目錄中尋找 Ollama 可執行檔案
     */
    private Path findOllamaExecutable(Path dir) throws IOException {
        return Files.walk(dir)
                .filter(path -> path.getFileName().toString().equals("ollama"))
                .filter(Files::isExecutable)
                .findFirst()
                .orElse(null);
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
            // 使用 /api/tags 端點，這是Ollama實際提供的端點
            URL url = new URL("http://localhost:11434/api/tags");
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
    public boolean isOllamaInstalled() {
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
                    // 智能分析日誌級別
                    String formattedMessage = formatOllamaLog(line, logPrefix);
                    System.out.println(formattedMessage);
                }
            } catch (IOException e) {
                System.err.println(logPrefix + "讀取錯誤: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * 智能格式化 Ollama 日誌輸出
     */
    private String formatOllamaLog(String line, String originalPrefix) {
        if (line == null || line.trim().isEmpty()) {
            return originalPrefix + ": " + line;
        }
        
        // 檢查是否包含日誌級別
        if (line.contains("level=")) {
            if (line.contains("level=ERROR") || line.contains("level=FATAL")) {
                return "Ollama錯誤: " + line;
            } else if (line.contains("level=WARN")) {
                return "Ollama警告: " + line;
            } else if (line.contains("level=INFO")) {
                return "Ollama資訊: " + line;
            } else if (line.contains("level=DEBUG")) {
                return "Ollama調試: " + line;
            }
        }
        
        // 檢查 GIN 框架的 HTTP 請求日誌（這些是正常的）
        if (line.contains("[GIN]") && line.contains("200")) {
            return "Ollama HTTP: " + line;
        }
        
        // 檢查常見的錯誤關鍵字
        String lowerLine = line.toLowerCase();
        if (lowerLine.contains("error") || lowerLine.contains("failed") || 
            lowerLine.contains("exception") || lowerLine.contains("panic")) {
            return "Ollama錯誤: " + line;
        }
        
        // 檢查警告關鍵字
        if (lowerLine.contains("warning") || lowerLine.contains("warn")) {
            return "Ollama警告: " + line;
        }
        
        // 檢查成功和正常運行的關鍵字
        if (lowerLine.contains("listening") || lowerLine.contains("server") ||
            lowerLine.contains("starting") || lowerLine.contains("ready") ||
            lowerLine.contains("success")) {
            return "Ollama資訊: " + line;
        }
        
        // 如果原始前綴是錯誤流但沒有錯誤標識，可能是正常的運行日誌
        if ("Ollama錯誤".equals(originalPrefix)) {
            return "Ollama輸出: " + line;
        }
        
        // 其他情況保持原始前綴
        return originalPrefix + ": " + line;
    }
    
    /**
     * 格式化模型下載日誌
     */
    private String formatModelDownloadLog(String line) {
        if (line == null || line.trim().isEmpty()) {
            return "模型下載: " + line;
        }
        
        String lowerLine = line.toLowerCase();
        
        // 檢查真正的錯誤
        if (lowerLine.contains("error") || lowerLine.contains("failed") || 
            lowerLine.contains("exception") || lowerLine.contains("panic") ||
            lowerLine.contains("fatal")) {
            return "模型下載錯誤: " + line;
        }
        
        // 檢查警告
        if (lowerLine.contains("warning") || lowerLine.contains("warn")) {
            return "模型下載警告: " + line;
        }
        
        // 檢查進度和正常信息
        if (lowerLine.contains("pulling") || lowerLine.contains("downloading") ||
            lowerLine.contains("verifying") || lowerLine.contains("success") ||
            lowerLine.contains("complete") || lowerLine.contains("%")) {
            return "模型下載進度: " + line;
        }
        
        // 其他情況視為正常日誌
        return "模型下載日誌: " + line;
    }
    
    /**
     * 判斷是否為真正的錯誤
     */
    private boolean isActualError(String line) {
        if (line == null) {
            return false;
        }
        
        String lowerLine = line.toLowerCase();
        return lowerLine.contains("error") || lowerLine.contains("failed") || 
               lowerLine.contains("exception") || lowerLine.contains("panic") ||
               lowerLine.contains("fatal");
    }
    
    /**
     * 確保特定模型下載完成
     * @param modelName 模型名稱
     * @return 模型是否可用
     */
    public CompletableFuture<Boolean> ensureModelReady(String modelName) {
        return ensureModelReady(modelName, null);
    }
    
    /**
     * 確保特定模型下載完成（帶輸出回調）
     * @param modelName 模型名稱
     * @param outputCallback 實時輸出回調
     * @return 模型是否可用
     */
    public CompletableFuture<Boolean> ensureModelReady(String modelName, OutputCallback outputCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 首先確保Ollama正在運行
                if (!isOllamaServiceRunning()) {
                    return false;
                }
                
                // 檢查模型是否已下載
                if (!isModelDownloaded(modelName)) {
                    // 下載模型
                    downloadModel(modelName, outputCallback);
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
     * 輸出回調接口
     */
    public interface OutputCallback {
        void onOutput(String output);
    }
    
    /**
     * 檢查模型是否已下載
     */
    public boolean isModelDownloaded(String modelName) {
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
        downloadModel(modelName, null);
    }
    
    /**
     * 下載模型（帶輸出回調）
     */
    private void downloadModel(String modelName, OutputCallback outputCallback) throws IOException, InterruptedException {
        System.out.println("開始下載模型: " + modelName);
        
        if (outputCallback != null) {
            outputCallback.onOutput("pulling manifest");
        }
        
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (isWindows()) {
            processBuilder.command(ollamaPath.toString(), "pull", modelName);
        } else {
            processBuilder.command(ollamaPath.toString(), "pull", modelName);
        }
        
        Process process = processBuilder.start();
        
        // 實時讀取輸出
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("模型下載輸出: " + line);
                    if (outputCallback != null) {
                        outputCallback.onOutput(line);
                    }
                }
            } catch (IOException e) {
                System.err.println("讀取模型下載輸出時發生錯誤: " + e.getMessage());
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();
        
        // 實時讀取錯誤輸出（智能分析）
        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 使用智能日誌格式化
                    String formattedMessage = formatModelDownloadLog(line);
                    System.out.println(formattedMessage);
                    
                    if (outputCallback != null) {
                        // 對於回調，只在真正的錯誤時添加 ERROR 前綴
                        if (isActualError(line)) {
                        outputCallback.onOutput("ERROR: " + line);
                        } else {
                            outputCallback.onOutput(line);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("讀取模型下載錯誤輸出時發生錯誤: " + e.getMessage());
            }
        });
        errorThread.setDaemon(true);
        errorThread.start();
        
        // 等待下載完成
        int exitCode = process.waitFor();
        
        if (outputCallback != null) {
            if (exitCode == 0) {
                outputCallback.onOutput("success");
            } else {
                outputCallback.onOutput("下載失敗，退出碼: " + exitCode);
            }
        }
        
        System.out.println("模型下載完成: " + modelName + "，退出碼: " + exitCode);
    }
    
    /**
     * 關閉OllamaManager並釋放資源
     */
    public void shutdown() {
        System.out.println("🔧 開始清理 Ollama 相關資源...");
        
        // 首先停止我們啟動的 Ollama 服務
        stopOllamaService();
        
        // 然後清理所有 Ollama 進程
        cleanupAllOllamaProcesses();
        
        // 關閉執行器
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ Ollama 資源清理完成");
    }
    
    /**
     * 清理所有 Ollama 進程
     */
    private void cleanupAllOllamaProcesses() {
        try {
            System.out.println("🔍 搜索並清理所有 Ollama 進程...");
            
            // 使用 ps 命令找出所有 Ollama 進程
            ProcessBuilder pb = new ProcessBuilder("ps", "aux");
            Process process = pb.start();
            
            List<String> ollamaPids = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 檢查是否是 Ollama 相關進程
                    if ((line.contains("ollama") || line.contains(".ollama")) && !line.contains("grep")) {
                        // 提取 PID (通常是第二列)
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            String pid = parts[1];
                            ollamaPids.add(pid);
                            System.out.println("🔍 發現 Ollama 進程: PID " + pid + " - " + line);
                        }
                    }
                }
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            
            // 優雅地終止所有 Ollama 進程
            for (String pid : ollamaPids) {
                try {
                    System.out.println("🔧 正在終止 Ollama 進程: " + pid);
                    killProcess(pid);
                    Thread.sleep(500); // 給進程一些時間來清理
                } catch (Exception e) {
                    System.err.println("⚠️ 終止進程 " + pid + " 時發生錯誤: " + e.getMessage());
                }
            }
            
            if (ollamaPids.isEmpty()) {
                System.out.println("✅ 沒有發現 Ollama 進程需要清理");
            } else {
                System.out.println("✅ 已清理 " + ollamaPids.size() + " 個 Ollama 進程");
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ 清理 Ollama 進程時發生錯誤: " + e.getMessage());
        }
    }
} 