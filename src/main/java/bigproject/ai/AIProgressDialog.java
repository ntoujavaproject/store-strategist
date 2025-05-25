package bigproject.ai;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.concurrent.Task;
import java.util.concurrent.CompletableFuture;

/**
 * AI 進度對話框 - 顯示 Ollama 和模型下載進度
 */
public class AIProgressDialog {
    
    private Stage dialogStage;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label detailLabel;
    private Button cancelButton;
    private Task<Boolean> currentTask;
    
    // 進度回調接口
    public interface ProgressCallback {
        void onProgress(double progress, String status, String detail);
        void onComplete(boolean success);
        void onError(String error);
    }
    
    /**
     * 創建並顯示 AI 進度對話框
     * @param owner 父視窗
     * @param title 對話框標題
     * @return AIProgressDialog 實例
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
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setTitle(title);
        dialogStage.setResizable(false);
        
        // 創建UI元素
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f8f9fa;");
        
        // 標題
        Label titleLabel = new Label("正在準備 AI 功能...");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        // 狀態標籤
        statusLabel = new Label("初始化中...");
        statusLabel.setFont(Font.font("System", 14));
        statusLabel.setStyle("-fx-text-fill: #34495e;");
        
        // 進度條
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(20);
        progressBar.setStyle(
            "-fx-accent: #3498db; " +
            "-fx-background-color: #ecf0f1; " +
            "-fx-border-color: #bdc3c7; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10;"
        );
        
        // 詳細信息標籤
        detailLabel = new Label("");
        detailLabel.setFont(Font.font("System", 12));
        detailLabel.setStyle("-fx-text-fill: #7f8c8d;");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(300);
        
        // 按鈕區域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        cancelButton = new Button("取消");
        cancelButton.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 5;"
        );
        cancelButton.setOnAction(e -> cancel());
        
        Button hideButton = new Button("隱藏");
        hideButton.setStyle(
            "-fx-background-color: #95a5a6; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 5;"
        );
        hideButton.setOnAction(e -> hide());
        
        buttonBox.getChildren().addAll(cancelButton, hideButton);
        
        // 組裝UI
        root.getChildren().addAll(
            titleLabel,
            statusLabel,
            progressBar,
            detailLabel,
            buttonBox
        );
        
        Scene scene = new Scene(root, 350, 200);
        dialogStage.setScene(scene);
        
        // 防止用戶關閉對話框
        dialogStage.setOnCloseRequest(e -> {
            e.consume(); // 阻止關閉
            hide(); // 只是隱藏
        });
        
        dialogStage.show();
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
                    // 第一階段：檢查並下載 Ollama
                    updateProgress(0.1, 1.0);
                    Platform.runLater(() -> {
                        statusLabel.setText("檢查 Ollama 安裝狀態...");
                        detailLabel.setText("正在檢查本地 Ollama 安裝");
                    });
                    
                    OllamaManager manager = new OllamaManager();
                    
                    // 檢查 Ollama 是否已安裝
                    if (!manager.isOllamaInstalled()) {
                        updateProgress(0.2, 1.0);
                        Platform.runLater(() -> {
                            statusLabel.setText("下載 Ollama...");
                            detailLabel.setText("正在從官方網站下載 Ollama (約 50MB)");
                        });
                        
                        // 模擬下載進度
                        for (int i = 20; i <= 50; i += 5) {
                            if (isCancelled()) return false;
                            updateProgress(i / 100.0, 1.0);
                            Thread.sleep(500);
                        }
                    }
                    
                    // 第二階段：啟動 Ollama 服務
                    updateProgress(0.6, 1.0);
                    Platform.runLater(() -> {
                        statusLabel.setText("啟動 Ollama 服務...");
                        detailLabel.setText("正在啟動本地 AI 服務");
                    });
                    
                    CompletableFuture<Boolean> serviceReady = manager.ensureOllamaRunning();
                    boolean isServiceRunning = serviceReady.get();
                    
                    if (!isServiceRunning) {
                        throw new Exception("無法啟動 Ollama 服務");
                    }
                    
                    // 第三階段：檢查並下載模型
                    updateProgress(0.7, 1.0);
                    Platform.runLater(() -> {
                        statusLabel.setText("檢查 AI 模型...");
                        detailLabel.setText("正在檢查 gemma3:4b 模型 (4GB)");
                    });
                    
                    String modelName = "gemma3:4b";
                    if (!manager.isModelDownloaded(modelName)) {
                        updateProgress(0.8, 1.0);
                        Platform.runLater(() -> {
                            statusLabel.setText("下載 AI 模型...");
                            detailLabel.setText("正在下載 gemma3:4b 模型，這可能需要幾分鐘...");
                        });
                        
                        // 啟動模型下載
                        CompletableFuture<Boolean> modelReady = manager.ensureModelReady(modelName);
                        
                        // 模擬下載進度（實際進度需要從 Ollama API 獲取）
                        for (int i = 80; i <= 95; i += 2) {
                            if (isCancelled()) return false;
                            updateProgress(i / 100.0, 1.0);
                            Platform.runLater(() -> {
                                detailLabel.setText("下載進度: " + (int)(getProgress() * 100) + "%");
                            });
                            Thread.sleep(1000);
                        }
                        
                        boolean isModelReady = modelReady.get();
                        if (!isModelReady) {
                            throw new Exception("無法下載 AI 模型");
                        }
                    }
                    
                    // 完成
                    updateProgress(1.0, 1.0);
                    Platform.runLater(() -> {
                        statusLabel.setText("AI 功能準備完成！");
                        detailLabel.setText("所有組件已就緒，可以開始使用 AI 功能");
                        cancelButton.setText("完成");
                    });
                    
                    return true;
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("初始化失敗");
                        detailLabel.setText("錯誤: " + e.getMessage());
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