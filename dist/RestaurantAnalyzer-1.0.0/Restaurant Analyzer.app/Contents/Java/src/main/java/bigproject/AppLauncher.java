package bigproject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * 應用程式啟動器 - 直接啟動餐廳分析應用程式
 * 在啟動時自動檢測和初始化 AI 功能
 */
public class AppLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🚀 JavaFX Platform 已啟動");
            
            // 設置關閉行為：不要在隱藏所有窗口時退出
            Platform.setImplicitExit(false);
            
            // 確保Platform已完全初始化後再進行AI檢查
            Platform.runLater(() -> {
                System.out.println("✅ Platform.runLater 可用，開始AI檢查");
                performAIInitializationCheck(primaryStage);
            });
            
        } catch (Exception ex) {
            System.err.println("無法啟動應用程式: " + ex.getMessage());
            ex.printStackTrace();
            // 如果出錯，直接嘗試啟動主程式
            try {
                compare compareApp = new compare();
                compareApp.start(primaryStage);
            } catch (Exception e2) {
                System.err.println("備用啟動也失敗: " + e2.getMessage());
                e2.printStackTrace();
            }
        }
    }
    
    /**
     * 執行 AI 初始化檢查
     */
    private void performAIInitializationCheck(Stage primaryStage) {
        System.out.println("🔍 開始檢查 AI 功能狀態...");
        
        // 在背景執行緒檢查 AI 是否需要初始化
        new Thread(() -> {
            boolean aiCheckSuccessful = false;
            boolean needsInitialization = false;
            
            try {
                System.out.println("🔧 正在載入 AI 模組...");
                
                // 檢查 OllamaManager 是否可用
                try {
                    // 嘗試創建 OllamaManager 實例來檢查 AI 狀態
                    System.out.println("🔍 嘗試載入 OllamaManager 類...");
                    Class<?> ollamaManagerClass = Class.forName("bigproject.ai.OllamaManager");
                    System.out.println("✅ OllamaManager 類載入成功");
                    
                    System.out.println("🔍 嘗試創建 OllamaManager 實例...");
                    Object manager = ollamaManagerClass.getDeclaredConstructor().newInstance();
                    System.out.println("✅ OllamaManager 實例創建成功");
                    
                    // 使用反射調用方法檢查安裝狀態
                    System.out.println("🔍 檢查 Ollama 安裝狀態...");
                    java.lang.reflect.Method isInstalledMethod = ollamaManagerClass.getMethod("isOllamaInstalled");
                    boolean isInstalled = (Boolean) isInstalledMethod.invoke(manager);
                    System.out.println("📋 Ollama 安裝狀態: " + (isInstalled ? "已安裝" : "未安裝"));
                    
                    if (isInstalled) {
                        // 先確保 Ollama 服務運行，再檢查模型
                        System.out.println("🔍 確保 Ollama 服務運行...");
                        try {
                            java.lang.reflect.Method ensureRunningMethod = ollamaManagerClass.getMethod("ensureOllamaRunning");
                            Object serviceReady = ensureRunningMethod.invoke(manager);
                            System.out.println("📋 Ollama 服務啟動完成");
                            
                            // 等待一秒讓服務完全啟動
                            Thread.sleep(1000);
                            
                            System.out.println("🔍 檢查 AI 模型下載狀態...");
                            java.lang.reflect.Method isModelDownloadedMethod = ollamaManagerClass.getMethod("isModelDownloaded", String.class);
                            boolean isModelDownloaded = (Boolean) isModelDownloadedMethod.invoke(manager, "gemma3:4b");
                            System.out.println("📋 AI 模型下載狀態: " + (isModelDownloaded ? "已下載" : "未下載"));
                            
                            needsInitialization = !isModelDownloaded;
                        } catch (Exception serviceException) {
                            System.err.println("⚠️ 無法啟動 Ollama 服務: " + serviceException.getMessage());
                            // 如果服務無法啟動，仍然需要初始化
                            needsInitialization = true;
                        }
                    } else {
                        // Ollama 未安裝，需要初始化
                        needsInitialization = true;
                    }
                    aiCheckSuccessful = true;
                    
                    System.out.println("📊 AI 檢查結果: " + (needsInitialization ? "需要初始化" : "已就緒"));
                    
                } catch (ClassNotFoundException e) {
                    System.err.println("❌ AI 模組類別未找到: " + e.getMessage());
                    System.err.println("💡 這可能表示 AI 功能尚未完全安裝");
                } catch (NoSuchMethodException e) {
                    System.err.println("❌ AI 模組方法未找到: " + e.getMessage());
                    System.err.println("💡 AI 模組版本可能不兼容");
                } catch (Exception e) {
                    System.err.println("❌ AI 模組檢查失敗: " + e.getMessage());
                    e.printStackTrace();
                }
                
            } catch (Exception e) {
                System.err.println("❌ AI 初始化檢查發生嚴重錯誤: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 在主執行緒中處理結果
            final boolean finalNeedsInitialization = needsInitialization;
            final boolean finalAiCheckSuccessful = aiCheckSuccessful;
            
                            Platform.runLater(() -> {
                    try {
                        if (finalAiCheckSuccessful) {
                            // 無論是否需要初始化，都顯示 AI 狀態檢查對話框
                            System.out.println("🔧 顯示 AI 狀態檢查對話框...");
                            
                            // 增加5秒超時保護機制
                            final boolean[] dialogStarted = {false};
                            
                            // 啟動一個超時檢查
                            new Thread(() -> {
                                try {
                                    Thread.sleep(5000); // 等待5秒
                                    if (!dialogStarted[0]) {
                                        System.err.println("⏰ AI對話框啟動超時，強制啟動主程式...");
                                        Platform.runLater(() -> startMainApplication(primaryStage));
                                    }
                                } catch (InterruptedException e) {
                                    // 正常情況，被中斷表示對話框成功啟動
                                }
                            }).start();
                            
                            try {
                                // 傳遞是否需要初始化的信息
                                showAIStatusCheckWithProgress(primaryStage, finalNeedsInitialization);
                                dialogStarted[0] = true;
                            } catch (Exception dialogError) {
                                System.err.println("❌ AI對話框啟動失敗: " + dialogError.getMessage());
                                dialogError.printStackTrace();
                                // 對話框失敗，直接啟動主程式
                                startMainApplication(primaryStage);
                            }
                        } else {
                            // AI 檢查失敗，直接啟動主應用程式
                            System.out.println("⚠️ AI 檢查失敗，但仍啟動主程式...");
                            startMainApplication(primaryStage);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ 處理 AI 檢查結果時發生錯誤: " + e.getMessage());
                        e.printStackTrace();
                        // 確保無論如何都要啟動主程式
                        System.out.println("🚀 強制啟動主程式...");
                        startMainApplication(primaryStage);
                    }
                });
            
        }).start();
    }
    
    /**
     * 顯示 AI 狀態檢查進度對話框
     */
    private void showAIStatusCheckWithProgress(Stage primaryStage, boolean needsInitialization) {
        try {
            System.out.println("🔧 嘗試顯示 AI 初始化進度對話框...");
            
            // 使用反射來調用 AIProgressDialog，避免直接依賴
            System.out.println("🔍 載入 AIProgressDialog 類...");
            Class<?> dialogClass = Class.forName("bigproject.ai.AIProgressDialog");
            System.out.println("✅ AIProgressDialog 類載入成功");
            
            System.out.println("🔍 查找 show 方法...");
            java.lang.reflect.Method showMethod = dialogClass.getMethod("show", Stage.class, String.class);
            System.out.println("✅ show 方法找到");
            
            System.out.println("🔧 顯示對話框...");
            String dialogTitle = needsInitialization ? "AI 功能初始化" : "AI 功能狀態檢查";
            Object dialog = showMethod.invoke(null, primaryStage, dialogTitle);
            System.out.println("✅ 對話框顯示成功");
            
            // 創建回調接口的實現
            System.out.println("🔍 創建回調接口...");
            Class<?> callbackInterface = Class.forName("bigproject.ai.AIProgressDialog$ProgressCallback");
            Object callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackInterface.getClassLoader(),
                new Class[]{callbackInterface},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "onProgress":
                            double progress = (Double) args[0];
                            String status = (String) args[1];
                            System.out.println("AI 初始化進度: " + (int)(progress * 100) + "% - " + status);
                            return null;
                        case "onComplete":
                            boolean success = (Boolean) args[0];
                            Platform.runLater(() -> {
                                // 關閉對話框
                                try {
                                    java.lang.reflect.Method closeMethod = dialog.getClass().getMethod("close");
                                    closeMethod.invoke(dialog);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                
                                if (success) {
                                    System.out.println("✅ AI 功能初始化完成，啟動主程式...");
                                } else {
                                    System.out.println("⚠️ AI 功能初始化失敗，但仍繼續啟動主程式...");
                                }
                                // 無論成功失敗，都啟動主應用程式
                                startMainApplication(primaryStage);
                            });
                            return null;
                        case "onError":
                            String error = (String) args[0];
                            Platform.runLater(() -> {
                                // 關閉對話框
                                try {
                                    java.lang.reflect.Method closeMethod = dialog.getClass().getMethod("close");
                                    closeMethod.invoke(dialog);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                
                                System.err.println("❌ AI 初始化錯誤: " + error);
                                // 即使錯誤，也要啟動主程式
                                startMainApplication(primaryStage);
                            });
                            return null;
                        default:
                            return null;
                    }
                }
            );
            System.out.println("✅ 回調接口創建成功");
            
            // 開始 AI 檢查/初始化流程
            if (needsInitialization) {
                System.out.println("🔧 開始 AI 初始化流程...");
            } else {
                System.out.println("🔧 開始 AI 狀態檢查流程...");
            }
            java.lang.reflect.Method startInitMethod = dialog.getClass().getMethod("startAIInitialization", callbackInterface);
            startInitMethod.invoke(dialog, callback);
            System.out.println("✅ AI 檢查流程已啟動");
            
        } catch (Exception e) {
            System.err.println("❌ 無法顯示 AI 初始化對話框: " + e.getMessage());
            e.printStackTrace();
            // 如果對話框無法顯示，直接啟動主程式
            System.out.println("🚀 回退到直接啟動主程式...");
            startMainApplication(primaryStage);
        }
    }
    
    /**
     * 啟動主應用程式
     */
    private void startMainApplication(Stage primaryStage) {
        try {
            System.out.println("🚀 啟動主應用程式...");
            
            // 恢復正常的退出行為
            Platform.setImplicitExit(true);
            
            compare compareApp = new compare();
            compareApp.start(primaryStage);
        } catch (Exception ex) {
            System.err.println("無法啟動主應用程式: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        // 設置應用程式在系統中顯示的名稱
        System.setProperty("apple.awt.application.name", "Restaurant Analyzer");
        System.setProperty("javafx.preloader", "bigproject.CustomSplashScreen");
        
        launch(args);
    }
} 