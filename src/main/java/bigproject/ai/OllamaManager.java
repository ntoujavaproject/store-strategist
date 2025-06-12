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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

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
                
                                    // 等待服務啟動 - 增加等待時間和更好的檢查邏輯
                int maxRetries = 30; // 增加到30秒
                System.out.println("⏳ 等待 Ollama 服務啟動...");
                for (int i = 0; i < maxRetries; i++) {
                    Thread.sleep(1000); // 等待1秒
                    
                    // 每5秒顯示一次進度
                    if (i % 5 == 0 && i > 0) {
                        System.out.println("⏳ 已等待 " + i + " 秒，繼續等待...");
                    }
                    
                    if (isOllamaServiceRunning()) {
                        System.out.println("✅ Ollama 服務啟動成功 (等待了 " + (i + 1) + " 秒)");
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
     * 智能清理端口衝突 - 只在必要時終止有問題的進程
     */
    private void cleanupPortConflicts() {
        try {
            System.out.println("🔍 檢查端口 11434 狀態...");
            
            // 首先檢查服務是否已經正常運行
            if (isOllamaServiceRunning()) {
                System.out.println("✅ 檢測到 Ollama 服務正常運行，無需清理");
                return;
            }
            
            // 如果端口被佔用但服務不正常，才進行清理
            if (!isPortAvailable(11434)) {
                System.out.println("⚠️ 端口 11434 被佔用但服務不可用，開始清理...");
                
                // 使用 lsof 找出佔用端口的進程
                ProcessBuilder pb = new ProcessBuilder("lsof", "-ti", ":11434");
                Process process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    boolean foundOllamaProcess = false;
                    while ((line = reader.readLine()) != null) {
                        String pid = line.trim();
                        if (!pid.isEmpty()) {
                            System.out.println("🔧 發現佔用端口的進程 PID: " + pid);
                            
                            // 檢查這個進程是否是 Ollama
                            if (isOllamaProcess(pid)) {
                                System.out.println("💀 終止有問題的 Ollama 進程: " + pid);
                                killProcess(pid);
                                foundOllamaProcess = true;
                            } else {
                                System.out.println("⚠️ 發現非 Ollama 進程佔用端口: " + pid + "，跳過終止");
                            }
                        }
                    }
                    
                    if (foundOllamaProcess) {
                        // 如果終止了 Ollama 進程，等待端口釋放
                        waitForPortRelease(11434, 10);
                    }
                }
                
                process.waitFor(5, TimeUnit.SECONDS);
            } else {
                System.out.println("✅ 端口 11434 可用");
            }
            
        } catch (Exception e) {
            System.err.println("清理端口衝突時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 檢查指定端口是否可用
     * @param port 要檢查的端口
     * @return true 如果端口可用，false 如果被佔用
     */
    private boolean isPortAvailable(int port) {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 等待端口釋放
     * @param port 要等待的端口
     * @param maxWaitSeconds 最大等待秒數
     * @return true 如果端口已釋放，false 如果超時
     */
    private boolean waitForPortRelease(int port, int maxWaitSeconds) {
        System.out.println("⏳ 等待端口 " + port + " 釋放...");
        
        for (int i = 0; i < maxWaitSeconds; i++) {
            if (isPortAvailable(port)) {
                System.out.println("✅ 端口 " + port + " 已釋放");
                return true;
            }
            
            try {
                Thread.sleep(1000);
                System.out.print(".");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        System.out.println("\n⚠️ 等待端口釋放超時");
        return false;
    }
    
    /**
     * 檢查與 Ollama 服務的連接狀態
     * @return 連接狀態和詳細信息
     */
    private ConnectionStatus checkOllamaConnection() {
        // 1. 檢查端口是否可連接
        if (!isPortReachable("localhost", 11434, 3000)) {
            return new ConnectionStatus(false, "端口 11434 無法連接");
        }
        
        // 2. 檢查 HTTP 服務是否回應
        try {
            java.net.URL url = new java.net.URL("http://localhost:11434/api/tags");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return new ConnectionStatus(true, "Ollama 服務正常運行");
            } else {
                return new ConnectionStatus(false, "Ollama 服務回應異常: HTTP " + responseCode);
            }
        } catch (Exception e) {
            return new ConnectionStatus(false, "無法連接到 Ollama 服務: " + e.getMessage());
        }
    }
    
    /**
     * 檢查指定主機和端口是否可達
     * @param host 主機名
     * @param port 端口號
     * @param timeout 超時時間（毫秒）
     * @return true 如果可達，false 否則
     */
    private boolean isPortReachable(String host, int port, int timeout) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 連接狀態類
     */
    private static class ConnectionStatus {
        public final boolean isConnected;
        public final String message;
        
        public ConnectionStatus(boolean isConnected, String message) {
            this.isConnected = isConnected;
            this.message = message;
        }
    }
    
    /**
     * 增強的服務運行檢測
     * @return true 如果服務正在運行
     */
    private boolean isOllamaServiceRunning() {
        // 使用新的連接檢查方法
        ConnectionStatus status = checkOllamaConnection();
        System.out.println("🔗 Ollama 連接狀態: " + status.message);
        return status.isConnected;
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
     * 智能清理所有 Ollama 進程（僅在確實需要時）
     */
    private void cleanupAllOllamaProcesses() {
        try {
            System.out.println("🔍 搜索並清理所有 Ollama 進程...");
            
            // 檢查是否有其他應用正在使用 ollama
            if (isOllamaBeingUsedByOtherApps()) {
                System.out.println("⚠️ 檢測到其他應用正在使用 Ollama，不進行清理");
                return;
            }
            
            ProcessBuilder pb = new ProcessBuilder("ps", "aux");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int cleanedCount = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ollama") && !line.contains("grep")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length > 1) {
                            String pid = parts[1];
                            System.out.println("🔍 發現 Ollama 進程: PID " + pid + " - " + line);
                            
                            // 只終止由我們啟動的進程
                            if (shouldTerminateProcess(pid, line)) {
                                System.out.println("🔧 正在終止 Ollama 進程: " + pid);
                                killProcess(pid);
                                cleanedCount++;
                            } else {
                                System.out.println("💼 保留系統或其他應用的 Ollama 進程: " + pid);
                            }
                        }
                    }
                }
                System.out.println("✅ 已清理 " + cleanedCount + " 個 Ollama 進程");
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("清理 Ollama 進程時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 檢查是否有其他應用正在使用 Ollama
     * @return true 如果有其他應用在使用
     */
    private boolean isOllamaBeingUsedByOtherApps() {
        try {
            // 檢查是否有活躍的 API 連接
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/ps"))  // 檢查正在運行的模型
                    .timeout(java.time.Duration.ofSeconds(2))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // 檢查回應內容，看是否有其他模型在運行
                String responseBody = response.body();
                return responseBody.contains("\"models\"") && !responseBody.contains("\"models\":[]");
            }
        } catch (Exception e) {
            // 如果檢查失敗，保守做法是假設有其他應用在使用
            System.out.println("⚠️ 無法檢查 Ollama 使用狀態，採取保守策略");
            return true;
        }
        return false;
    }
    
    /**
     * 判斷是否應該終止指定進程
     * @param pid 進程ID
     * @param processLine 完整的進程信息行
     * @return true 如果應該終止
     */
    private boolean shouldTerminateProcess(String pid, String processLine) {
        try {
            // 檢查進程的詳細信息
            ProcessBuilder pb = new ProcessBuilder("ps", "-p", pid, "-o", "command=");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String command = reader.readLine();
                if (command != null) {
                    // 只終止我們能確定是由我們管理的進程
                    // 例如：通過特定的啟動參數或路徑來識別
                    if (command.contains(ollamaPath.toString()) || 
                        (ollamaProcess != null && pid.equals(String.valueOf(ollamaProcess.pid())))) {
                        return true;
                    }
                    
                    // 保留系統安裝的或其他應用管理的 ollama
                    if (command.contains("/opt/homebrew") || 
                        command.contains("/usr/local") ||
                        command.contains("homebrew")) {
                        return false;
                    }
                }
            }
            
            process.waitFor(2, TimeUnit.SECONDS);
            return false;
            
        } catch (Exception e) {
            // 如果無法確定，則不終止
            return false;
        }
    }
    
    /**
     * 安全關閉管理器，現在會更徹底地清理 ollama 進程
     */
    public void shutdown() {
        try {
            System.out.println("🔧 開始清理 Ollama 相關資源...");
            
            // 停止我們直接啟動的進程
            if (ollamaProcess != null && ollamaProcess.isAlive()) {
                System.out.println("🔧 停止我們啟動的 Ollama 進程...");
                stopOllamaService();
            }
            
            // 檢查並清理孤立的 ollama runner 進程
            System.out.println("🔍 檢查孤立的 Ollama 進程...");
            cleanupOrphanedOllamaProcesses();
            
            // 等待線程池完成
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            System.out.println("✅ Ollama 資源清理完成");
        } catch (Exception e) {
            System.err.println("關閉 OllamaManager 時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 清理孤立的 Ollama 進程（主要是 ollama runner）
     */
    private void cleanupOrphanedOllamaProcesses() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ps", "aux");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int cleanedCount = 0;
                while ((line = reader.readLine()) != null) {
                    // 只清理 ollama runner 進程，保留 ollama serve
                    if (line.contains("ollama runner") && !line.contains("grep")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length > 1) {
                            String pid = parts[1];
                            System.out.println("🔧 清理孤立的 Ollama runner: PID " + pid);
                            
                            try {
                                ProcessBuilder killPb = new ProcessBuilder("kill", "-9", pid);
                                Process killProcess = killPb.start();
                                killProcess.waitFor(2, TimeUnit.SECONDS);
                                cleanedCount++;
                            } catch (Exception e) {
                                System.err.println("無法終止 runner 進程 " + pid + ": " + e.getMessage());
                            }
                        }
                    }
                }
                if (cleanedCount > 0) {
                    System.out.println("✅ 已清理 " + cleanedCount + " 個孤立的 Ollama runner 進程");
                } else {
                    System.out.println("✅ 沒有發現孤立的 Ollama 進程");
                }
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            System.err.println("清理孤立進程時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 強制關閉管理器，確保 Ollama 進程被完全終止
     * 用於 JVM shutdown hook 或強制清理場景
     */
    public void forceShutdown() {
        try {
            System.out.println("🔧 強制關閉 Ollama 相關資源...");
            
            // 強制停止我們啟動的進程
            if (ollamaProcess != null && ollamaProcess.isAlive()) {
                System.out.println("🔧 強制停止我們啟動的 Ollama 進程...");
                ollamaProcess.destroyForcibly();
                try {
                    ollamaProcess.waitFor(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // 激進清理：清理所有 ollama 相關進程（包括 runner）
            System.out.println("🧹 清理所有 Ollama 相關進程...");
            cleanupAllOllamaProcessesForce();
            
            // 強制關閉線程池
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.out.println("⚠️ 線程池未能在指定時間內關閉");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            isOllamaRunning = false;
            System.out.println("✅ Ollama 資源強制清理完成");
        } catch (Exception e) {
            System.err.println("強制關閉 OllamaManager 時發生錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 激進的進程清理 - 清理所有 ollama 相關進程
     */
    private void cleanupAllOllamaProcessesForce() {
        try {
            System.out.println("🔍 搜索所有 Ollama 進程進行強制清理...");
            
            ProcessBuilder pb = new ProcessBuilder("ps", "aux");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int cleanedCount = 0;
                while ((line = reader.readLine()) != null) {
                    if ((line.contains("ollama") || line.contains(".ollama")) && !line.contains("grep")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length > 1) {
                            String pid = parts[1];
                            String processInfo = line;
                            
                            // 跳過當前進程
                            if (pid.equals(String.valueOf(ProcessHandle.current().pid()))) {
                                continue;
                            }
                            
                            System.out.println("🔧 強制終止 Ollama 進程: PID " + pid);
                            System.out.println("   詳情: " + processInfo.substring(0, Math.min(processInfo.length(), 100)) + "...");
                            
                            try {
                                // 使用 kill -9 強制終止
                                ProcessBuilder killPb = new ProcessBuilder("kill", "-9", pid);
                                Process killProcess = killPb.start();
                                killProcess.waitFor(2, TimeUnit.SECONDS);
                                cleanedCount++;
                            } catch (Exception e) {
                                System.err.println("無法終止進程 " + pid + ": " + e.getMessage());
                            }
                        }
                    }
                }
                System.out.println("✅ 已強制清理 " + cleanedCount + " 個 Ollama 進程");
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            
            // 等待一下讓進程完全終止
            Thread.sleep(2000);
            
        } catch (Exception e) {
            System.err.println("強制清理 Ollama 進程時發生錯誤: " + e.getMessage());
        }
    }
} 