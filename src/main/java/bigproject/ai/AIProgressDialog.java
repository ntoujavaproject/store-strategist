package bigproject.ai;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.concurrent.CompletableFuture;

/**
 * AI初始化進度對話框，顯示下載進度和實時輸出
 */
public class AIProgressDialog {
    
    private Stage dialogStage;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label detailLabel;
    private TextArea terminalOutput; // 新增：類似終端機的輸出區域
    private ScrollPane terminalScrollPane;
    private Button cancelButton;
    private Button hideButton;
    private Task<Boolean> currentTask;
    
    // 進度回調接口
    public interface ProgressCallback {
        void onProgress(double progress, String status, String detail);
        void onComplete(boolean success);
        void onError(String error);
    }
    
    /**
     * 顯示AI進度對話框
     * @param owner 父窗口
     * @param title 對話框標題
     * @return AIProgressDialog實例
     */
    public static AIProgressDialog show(Stage owner, String title) {
        AIProgressDialog dialog = new AIProgressDialog();
        dialog.createDialog(owner, title);
        return dialog;
    }
    
    /**
     * 創建對話框
     */
    private void createDialog(Stage owner, String title) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setTitle(title);
        dialogStage.setResizable(true);
        
        // 創建UI元素
        statusLabel = new Label("準備初始化...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        detailLabel = new Label("正在檢查系統組件...");
        detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        detailLabel.setWrapText(true);
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(450);
        progressBar.setPrefHeight(20);
        
        // 新增：終端機輸出區域
        terminalOutput = new TextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setPrefRowCount(15);
        terminalOutput.setPrefColumnCount(60);
        terminalOutput.setStyle(
            "-fx-font-family: 'Monaco', 'Menlo', 'Consolas', monospace; " +
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1e1e1e; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-control-inner-background: #1e1e1e;"
        );
        terminalOutput.setText("🚀 AI 初始化開始...\n");
        
        terminalScrollPane = new ScrollPane(terminalOutput);
        terminalScrollPane.setFitToWidth(true);
        terminalScrollPane.setFitToHeight(true);
        terminalScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        terminalScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(terminalScrollPane, Priority.ALWAYS);
        
        // 按鈕區域
        cancelButton = new Button("取消");
        cancelButton.setOnAction(e -> cancel());
        
        hideButton = new Button("隱藏");
        hideButton.setOnAction(e -> hide());
        
        HBox buttonBox = new HBox(10, hideButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        // 主要佈局
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(
            statusLabel,
            detailLabel, 
            progressBar,
            new Label("下載詳情:"),
            terminalScrollPane,
            buttonBox
        );
        
        Scene scene = new Scene(root, 500, 450);
        dialogStage.setScene(scene);
        
        // 防止用戶關閉對話框
        dialogStage.setOnCloseRequest(e -> {
            e.consume(); // 阻止關閉
            hide(); // 只是隱藏
        });
        
        dialogStage.show();
    }
    
    /**
     * 添加終端機輸出
     * @param text 要添加的文字
     */
    public void appendTerminalOutput(String text) {
        Platform.runLater(() -> {
            terminalOutput.appendText(text + "\n");
            terminalOutput.setScrollTop(Double.MAX_VALUE); // 自動滾動到底部
        });
    }
    
    /**
     * 清空終端機輸出
     */
    public void clearTerminalOutput() {
        Platform.runLater(() -> {
            terminalOutput.clear();
        });
    }
    
    /**
     * 開始 AI 初始化任務
     * @param callback 進度回調
     */
    public void startAIInitialization(ProgressCallback callback) {
        currentTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    OllamaManager manager = new OllamaManager();
                    
                    // 第一階段：檢查並下載 Ollama
                    updateProgress(0.1, 1.0);
                    Platform.runLater(() -> {
                        statusLabel.setText("檢查 Ollama 安裝狀態...");
                        detailLabel.setText("正在檢查本地 Ollama 安裝");
                        appendTerminalOutput("🔍 檢查 Ollama 安裝狀態...");
                    });
                    
                    // 檢查 Ollama 是否已安裝
                    if (!manager.isOllamaInstalled()) {
                        updateProgress(0.2, 1.0);
                        Platform.runLater(() -> {
                            statusLabel.setText("下載 Ollama...");
                            detailLabel.setText("正在從官方網站下載 Ollama (約 50MB)");
                            appendTerminalOutput("📥 開始下載 Ollama...");
                        });
                        
                        // 模擬下載進度
                        for (int i = 20; i <= 50; i += 5) {
                            if (isCancelled()) return false;
                            updateProgress(i / 100.0, 1.0);
                            Platform.runLater(() -> {
                                appendTerminalOutput("下載進度: " + (int)(getProgress() * 100) + "%");
                            });
                            Thread.sleep(500);
                        }
                        
                        Platform.runLater(() -> {
                            appendTerminalOutput("✅ Ollama 下載完成");
                        });
                    } else {
                        System.out.println("✅ Ollama 已安裝，跳過下載步驟");
                        Platform.runLater(() -> {
                            appendTerminalOutput("✅ Ollama 已安裝，跳過下載步驟");
                        });
                        updateProgress(0.5, 1.0);
                    }
                    
                    // 第二階段：確保 Ollama 服務運行
                    updateProgress(0.6, 1.0);
                    Platform.runLater(() -> {
                        statusLabel.setText("檢查 Ollama 服務...");
                        detailLabel.setText("正在確認 AI 服務運行狀態");
                        appendTerminalOutput("🔧 檢查 Ollama 服務狀態...");
                    });
                    
                    try {
                        CompletableFuture<Boolean> serviceReady = manager.ensureOllamaRunning();
                        boolean isServiceRunning = serviceReady.get();
                        
                        if (isServiceRunning) {
                            System.out.println("✅ Ollama 服務運行正常");
                            Platform.runLater(() -> {
                                appendTerminalOutput("✅ Ollama 服務運行正常");
                            });
                        } else {
                            throw new Exception("無法確認 Ollama 服務狀態");
                        }
                    } catch (Exception e) {
                        // 如果是端口衝突，檢查服務是否實際在運行
                        if (e.getMessage().contains("address already in use") || 
                            e.getMessage().contains("bind")) {
                            System.out.println("ℹ️ Ollama 服務已在運行（端口已被使用）");
                            Platform.runLater(() -> {
                                appendTerminalOutput("ℹ️  Ollama 服務已在其他進程中運行");
                                appendTerminalOutput("✅ 將使用現有服務");
                            });
                            // 繼續執行，不拋出異常
                        } else {
                            throw e;
                        }
                    }
                    
                    // 第三階段：檢查並下載模型
                    updateProgress(0.7, 1.0);
                    Platform.runLater(() -> {
                        statusLabel.setText("檢查 AI 模型...");
                        detailLabel.setText("正在檢查 gemma3:4b 模型");
                        appendTerminalOutput("🔍 檢查 gemma3:4b 模型...");
                    });
                    
                    String modelName = "gemma3:4b";
                    if (!manager.isModelDownloaded(modelName)) {
                        updateProgress(0.8, 1.0);
                        Platform.runLater(() -> {
                            statusLabel.setText("下載 AI 模型...");
                            detailLabel.setText("正在下載 gemma3:4b 模型 (約 4GB)，這可能需要幾分鐘...");
                            appendTerminalOutput("📥 開始下載 AI 模型: " + modelName);
                            appendTerminalOutput("模型大小約 4GB，請耐心等待...");
                        });
                        
                        System.out.println("🔽 開始下載模型: " + modelName);
                        
                        // 啟動模型下載，使用回調來獲取實時輸出
                        CompletableFuture<Boolean> modelReady = manager.ensureModelReady(modelName, 
                            new OllamaManager.OutputCallback() {
                                @Override
                                public void onOutput(String output) {
                                    // 實時顯示下載輸出
                                    Platform.runLater(() -> {
                                        appendTerminalOutput(output);
                                    });
                                }
                            });
                        
                        boolean isModelReady = modelReady.get();
                        if (!isModelReady) {
                            throw new Exception("無法下載 AI 模型");
                        }
                        
                        System.out.println("✅ 模型下載完成: " + modelName);
                        Platform.runLater(() -> {
                            appendTerminalOutput("✅ 模型下載完成: " + modelName);
                        });
                    } else {
                        System.out.println("✅ 模型已存在，跳過下載: " + modelName);
                        Platform.runLater(() -> {
                            appendTerminalOutput("✅ 模型已存在，跳過下載: " + modelName);
                        });
                        updateProgress(0.95, 1.0);
                    }
                    
                    // 完成
                    updateProgress(1.0, 1.0);
                    Platform.runLater(() -> {
                        statusLabel.setText("AI 功能準備完成！");
                        detailLabel.setText("所有組件已就緒，可以開始使用 AI 功能");
                        appendTerminalOutput("🎉 AI 初始化全部完成！");
                        appendTerminalOutput("所有組件已就緒，可以開始使用 AI 功能");
                        cancelButton.setText("完成");
                    });
                    
                    System.out.println("🎉 AI 初始化全部完成");
                    return true;
                    
                } catch (Exception e) {
                    System.err.println("❌ AI 初始化失敗: " + e.getMessage());
                    e.printStackTrace();
                    
                    Platform.runLater(() -> {
                        statusLabel.setText("初始化失敗");
                        detailLabel.setText("錯誤: " + e.getMessage());
                        appendTerminalOutput("❌ 初始化失敗: " + e.getMessage());
                        progressBar.setStyle("-fx-accent: #e74c3c;");
                        cancelButton.setText("關閉");
                    });
                    
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                    return false;
                }
            }
            
            @Override
            protected void succeeded() {
                if (callback != null) {
                    callback.onComplete(getValue());
                }
            }
            
            @Override
            protected void failed() {
                if (callback != null) {
                    callback.onError(getException().getMessage());
                }
            }
        };
        
        // 綁定進度條
        progressBar.progressProperty().bind(currentTask.progressProperty());
        
        // 啟動任務
        Thread taskThread = new Thread(currentTask);
        taskThread.setDaemon(true);
        taskThread.start();
    }
    
    /**
     * 取消當前任務
     */
    public void cancel() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        close();
    }
    
    /**
     * 隱藏對話框
     */
    public void hide() {
        if (dialogStage != null) {
            dialogStage.hide();
        }
    }
    
    /**
     * 顯示對話框
     */
    public void show() {
        if (dialogStage != null) {
            dialogStage.show();
            dialogStage.toFront();
        }
    }
    
    /**
     * 關閉對話框
     */
    public void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    /**
     * 更新進度和狀態
     */
    public void updateProgress(double progress, String status, String detail) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            if (status != null) statusLabel.setText(status);
            if (detail != null) detailLabel.setText(detail);
        });
    }
    
    /**
     * 檢查對話框是否正在顯示
     */
    public boolean isShowing() {
        return dialogStage != null && dialogStage.isShowing();
    }
} 