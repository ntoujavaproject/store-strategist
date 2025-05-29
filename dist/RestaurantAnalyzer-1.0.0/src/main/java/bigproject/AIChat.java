package bigproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// 新增導入
import bigproject.ai.ChatHistoryManager;

/**
 * AI 聊天功能類
 * 提供與 Ollama API 通訊和 AI 聊天介面
 */
public class AIChat {
    private final BorderPane mainLayout;
    private final Node originalCenterContent;
    private final String RICH_MIDTONE_RED = "#E67649";
    
    // AI 聊天相關控制項
    private boolean isActive = false;
    private Button backButton;
    private VBox chatContainer;
    private TextArea chatHistoryArea;
    private TextField userInputField;
    private String currentContentType = "";
    private String currentInitialContent = "";
    
    // 新增：對話記錄管理器和會話資訊
    private ChatHistoryManager chatHistoryManager;
    private String currentSessionId = null;
    private String currentRestaurantName = "";
    private String currentRestaurantId = "";
    
    // 用於存放 callback 方法的接口
    public interface ChatStateChangeListener {
        void onChatStateChanged(boolean isShowing);
    }
    
    private ChatStateChangeListener stateChangeListener;
    
    // 新增：用於存儲聊天訊息容器的引用
    private VBox currentChatMessagesContainer;

    /**
     * 建構函數
     * @param mainLayout 主佈局，用於顯示聊天介面
     * @param originalContent 原始內容，隱藏聊天視圖時恢復
     * @param listener 聊天狀態變更監聽器
     */
    public AIChat(BorderPane mainLayout, Node originalContent, ChatStateChangeListener listener) {
        this.mainLayout = mainLayout;
        this.originalCenterContent = originalContent;
        this.stateChangeListener = listener;
        
        // 初始化對話記錄管理器
        this.chatHistoryManager = new ChatHistoryManager();
    }
    
    /**
     * 切換顯示 AI 聊天視圖
     * @param title 聊天視圖標題
     * @param initialContent 初始內容
     * @param contentType 內容類型（餐廳特色、優點、缺點等）
     */
    public void toggleChatView(String title, String initialContent, String contentType) {
        // 如果 AI 聊天已經是活躍的，先隱藏它
        if (isActive) {
            hideChatView();
            return;
        }
        
        // 保存聊天內容類型和初始內容
        currentContentType = contentType;
        currentInitialContent = initialContent;
        
        // 創建或顯示 AI 聊天視圖
        showChatView(title, initialContent, contentType);
        
        // 更新狀態標誌
        isActive = true;
        
        // 通知監聽器
        if (stateChangeListener != null) {
            stateChangeListener.onChatStateChanged(true);
        }
    }
    
    /**
     * 設置當前餐廳資訊（用於對話記錄）
     * @param restaurantName 餐廳名稱
     * @param restaurantId 餐廳ID
     */
    public void setCurrentRestaurantInfo(String restaurantName, String restaurantId) {
        this.currentRestaurantName = restaurantName;
        this.currentRestaurantId = restaurantId;
        System.out.println("✅ 設置AI聊天餐廳資訊: " + restaurantName + " (" + restaurantId + ")");
    }
    
    /**
     * 隱藏 AI 聊天視圖
     */
    public void hideChatView() {
        System.out.println("執行 hideChatView 方法");
        
        // 結束當前會話
        if (currentSessionId != null) {
            chatHistoryManager.endConversation(currentSessionId);
            currentSessionId = null;
        }
        
        // 確保在 JavaFX 應用程式執行緒上執行 UI 操作
        Platform.runLater(() -> {
            if (chatContainer != null && mainLayout.getCenter() == chatContainer) {
                System.out.println("恢復原始內容並隱藏聊天視圖");
                // 恢復原來的主內容
                mainLayout.setCenter(originalCenterContent);
                
                // 清理聊天訊息容器引用
                currentChatMessagesContainer = null;
                
                // 更新狀態標誌
                isActive = false;
                
                // 通知監聽器
                if (stateChangeListener != null) {
                    stateChangeListener.onChatStateChanged(false);
                }
            } else {
                System.out.println("無法隱藏聊天視圖：chatContainer=" + (chatContainer != null) + 
                                  ", center是聊天容器=" + (mainLayout.getCenter() == chatContainer));
            }
        });
    }
    
    /**
     * 顯示 AI 聊天視圖
     */
    private void showChatView(String title, String initialContent, String contentType) {
        // 開始新的對話會話
        if (currentRestaurantName != null && !currentRestaurantName.isEmpty()) {
            currentSessionId = chatHistoryManager.startNewConversation(
                currentRestaurantName, 
                currentRestaurantId != null ? currentRestaurantId : "", 
                initialContent
            );
            System.out.println("🤖 開始新AI對話會話: " + currentSessionId);
        }
        
        // 創建 AI 聊天容器
        chatContainer = new VBox(15);
        chatContainer.setPadding(new Insets(20));
        chatContainer.setStyle("-fx-background-color: #2C2C2C;");
        
        // 設置聊天容器的 ID，便於後續識別
        chatContainer.setId("aiChatContainer");
        
        // 創建頂部欄，包含標題和返回按鈕
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 15, 0));
        
        // 返回按鈕 - 使用與 compare.java 相同風格的按鈕
        backButton = new Button("← 返回");
        backButton.setStyle("-fx-background-color: #3A3A3A; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
        backButton.setPrefWidth(100); // 確保按鈕足夠寬可以容易點擊
        
        // 修改：確保返回按鈕點擊事件可以正確關閉聊天視窗
        backButton.setOnAction(event -> {
            System.out.println("返回按鈕被點擊");
            hideChatView();
        });
        
        // 添加懸停效果
        backButton.setOnMouseEntered(e -> {
            backButton.setStyle("-fx-background-color: #505050; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
        });
        
        backButton.setOnMouseExited(e -> {
            backButton.setStyle("-fx-background-color: #3A3A3A; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
        });
        
        // 標題標籤
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setStyle("-fx-text-fill: white;");
        
        // 新增：會話資訊標籤
        Label sessionLabel = new Label("會話ID: " + (currentSessionId != null ? currentSessionId : "未知"));
        sessionLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
        sessionLabel.setStyle("-fx-text-fill: #AAAAAA;");
        
        VBox titleBox = new VBox(2);
        titleBox.getChildren().addAll(titleLabel, sessionLabel);
        
        topBar.getChildren().addAll(backButton, titleBox);
        
        // 創建聊天記錄區域 - 改為 VBox 來容納對話框
        VBox chatMessagesContainer = new VBox(15);
        chatMessagesContainer.setPadding(new Insets(10));
        chatMessagesContainer.setStyle("-fx-background-color: #2C2C2C;");
        
        // 保存聊天訊息容器的引用，以便後續動態更新
        currentChatMessagesContainer = chatMessagesContainer;
        
        // 將聊天容器放在 ScrollPane 中
        ScrollPane chatScrollPane = new ScrollPane(chatMessagesContainer);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(400);
        chatScrollPane.setStyle("-fx-background-color: #2C2C2C; -fx-border-color: transparent;");
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        
        // 初始化歡迎訊息
        addWelcomeMessage(chatMessagesContainer, initialContent);
        
        // 創建用戶輸入區域
        userInputField = new TextField();
        userInputField.setPromptText("輸入您的問題或想法...");
        userInputField.setPrefHeight(40);
        userInputField.setStyle("-fx-background-color: #444444; -fx-text-fill: white; -fx-font-size: 14px;");
        
        // 創建發送按鈕
        Button sendButton = new Button("發送");
        sendButton.setPrefHeight(40);
        sendButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white;");
        
        // 添加懸停效果
        sendButton.setOnMouseEntered(e -> {
            sendButton.setStyle("-fx-background-color: #F08159; -fx-text-fill: white;");
        });
        
        sendButton.setOnMouseExited(e -> {
            sendButton.setStyle("-fx-background-color: " + RICH_MIDTONE_RED + "; -fx-text-fill: white;");
        });
        
        // 創建輸入區域佈局
        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.getChildren().addAll(userInputField, sendButton);
        HBox.setHgrow(userInputField, Priority.ALWAYS);
        
        // 處理發送消息的邏輯
        Runnable sendMessageAction = () -> {
            String userMessage = userInputField.getText().trim();
            if (!userMessage.isEmpty()) {
                // 添加用戶消息到聊天記錄
                chatMessagesContainer.getChildren().add(createUserMessageBox(userMessage));
                
                // 清空輸入框
                userInputField.clear();
                
                // 添加"AI思考中"的提示
                chatMessagesContainer.getChildren().add(createSystemMessageBox("AI助手: 思考中..."));
                
                // 滾動到底部
                chatScrollPane.setVvalue(1.0);
                
                // 使用CompletableFuture在後台處理AI響應
                CompletableFuture.runAsync(() -> {
                    try {
                        // 獲取AI響應 (實際調用Ollama API)
                        String aiResponse = callOllamaAPI(userMessage, contentType, initialContent);
                        
                        // 記錄對話到JSON檔案
                        if (currentSessionId != null) {
                            chatHistoryManager.addChatMessage(currentSessionId, userMessage, aiResponse);
                        }
                        
                        // 更新UI (必須在JavaFX線程中進行)
                        Platform.runLater(() -> {
                            // 移除"思考中"的提示
                            chatMessagesContainer.getChildren().remove(chatMessagesContainer.getChildren().size() - 1);
                            // 添加AI的響應
                            chatMessagesContainer.getChildren().add(createAIMessageBox(aiResponse));
                            
                            // 滾動到底部
                            chatScrollPane.setVvalue(1.0);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            // 處理錯誤情況
                            chatMessagesContainer.getChildren().remove(chatMessagesContainer.getChildren().size() - 1);
                            chatMessagesContainer.getChildren().add(createSystemMessageBox("AI助手: 抱歉，我遇到了一些問題，無法回應您的問題。\n錯誤詳情：" + e.getMessage()));
                        });
                        e.printStackTrace();
                    }
                });
            }
        };
        
        // 綁定發送按鈕和回車鍵
        sendButton.setOnAction(e -> sendMessageAction.run());
        userInputField.setOnAction(e -> sendMessageAction.run());
        
        // 組裝UI
        chatContainer.getChildren().addAll(topBar, chatScrollPane, inputBox);
        
        // 替換主佈局中的內容
        mainLayout.setCenter(chatContainer);
        
        // 添加 ESC 按鍵事件處理，按 ESC 可以關閉聊天視窗
        chatContainer.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                System.out.println("ESC 按鍵被按下 (容器監聽)");
                hideChatView();
            }
        });
        
        // 獲取當前場景並添加全局按鍵監聽器
        javafx.scene.Scene scene = mainLayout.getScene();
        if (scene != null) {
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && isActive) {
                    System.out.println("ESC 按鍵被按下 (全局監聽)");
                    hideChatView();
                    event.consume(); // 防止事件傳播
                }
            });
        }
        
        // 聚焦到輸入框
        Platform.runLater(() -> userInputField.requestFocus());
    }
    
    /**
     * 提供AI回應的方法
     */
    private String callOllamaAPI(String userMessage, String contentType, String initialContent) {
        try {
            System.out.println("🤖 使用 ChatRestaurantAdvisor 生成回應");
            
            // 使用 ChatRestaurantAdvisor 來生成回應
            bigproject.ai.ChatRestaurantAdvisor advisor = new bigproject.ai.ChatRestaurantAdvisor();
            
            // 設置餐廳特色資訊
            advisor.setRestaurantFeatures(initialContent);
            
            // 調用 AI 生成回應
            String response = advisor.chatWithAI(userMessage);
            
            System.out.println("✅ ChatRestaurantAdvisor 回應成功");
            return response;
            
        } catch (Exception e) {
            // 出現異常時，記錄錯誤並返回一個通用回應
            System.err.println("❌ ChatRestaurantAdvisor 回應生成異常: " + e.getMessage());
            e.printStackTrace();
            return "很抱歉，AI 服務暫時無法使用，請稍後再試。\n錯誤詳情：" + e.getMessage();
        }
    }

    /**
     * 判斷聊天視圖是否處於活躍狀態
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * 創建用戶訊息框（顯示在右側）
     */
    private HBox createUserMessageBox(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 0, 5, 50)); // 左邊留空間，右對齊
        
        // 訊息氣泡
        Label messageLabel = new Label(message);
        messageLabel.setPadding(new Insets(15, 20, 15, 20));
        messageLabel.setStyle("-fx-background-color: #E67649; " +
                             "-fx-text-fill: white; " +
                             "-fx-background-radius: 18; " +
                             "-fx-font-size: 16px; " +
                             "-fx-font-weight: bold; " +
                             "-fx-wrap-text: true;");
        messageLabel.setWrapText(true);
        
        messageBox.getChildren().add(messageLabel);
        return messageBox;
    }
    
    /**
     * 創建AI訊息框（顯示在左側）
     */
    private HBox createAIMessageBox(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 0)); // 右邊留空間，左對齊
        
        // AI 頭像
        Label avatarLabel = new Label("🤖");
        avatarLabel.setStyle("-fx-font-size: 24px; " +
                            "-fx-padding: 10; " +
                            "-fx-background-color: #4A4A4A; " +
                            "-fx-background-radius: 25; " +
                            "-fx-text-fill: white;");
        avatarLabel.setPrefSize(50, 50);
        avatarLabel.setMaxSize(50, 50);
        avatarLabel.setAlignment(Pos.CENTER);
        
        // 訊息氣泡
        Label messageLabel = new Label(message);
        messageLabel.setPadding(new Insets(15, 20, 15, 20));
        messageLabel.setStyle("-fx-background-color: #444444; " +
                             "-fx-text-fill: white; " +
                             "-fx-background-radius: 18; " +
                             "-fx-font-size: 16px; " +
                             "-fx-wrap-text: true;");
        messageLabel.setWrapText(true);
        
        messageBox.getChildren().addAll(avatarLabel, messageLabel);
        messageBox.setSpacing(10);
        return messageBox;
    }
    
    /**
     * 創建系統訊息框（置中顯示）
     */
    private HBox createSystemMessageBox(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(10, 20, 10, 20));
        
        Label messageLabel = new Label(message);
        messageLabel.setPadding(new Insets(15, 20, 15, 20));
        messageLabel.setStyle("-fx-background-color: #333333; " +
                             "-fx-text-fill: #CCCCCC; " +
                             "-fx-background-radius: 15; " +
                             "-fx-font-size: 15px; " +
                             "-fx-font-style: italic; " +
                             "-fx-wrap-text: true;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(600);
        
        messageBox.getChildren().add(messageLabel);
        return messageBox;
    }
    
    /**
     * 添加歡迎訊息
     */
    private void addWelcomeMessage(VBox container, String initialContent) {
        StringBuilder welcomeText = new StringBuilder();
        welcomeText.append("🔄 正在載入餐廳特色資料...\n\n");
        welcomeText.append("📊 分析結果：\n").append(initialContent).append("\n\n");
        welcomeText.append("💬 您可以開始提問了！");
        
        container.getChildren().add(createSystemMessageBox(welcomeText.toString()));
    }
    
    /**
     * 動態更新初始內容（當 AI 分析完成時調用）
     * @param newContent 新的分析結果內容
     */
    public void updateInitialContent(String newContent) {
        if (isActive && currentChatMessagesContainer != null) {
            System.out.println("🔄 更新 AI 聊天的初始內容");
            
            // 更新保存的初始內容
            currentInitialContent = newContent;
            
            Platform.runLater(() -> {
                // 移除舊的歡迎訊息（第一個子元素）
                if (!currentChatMessagesContainer.getChildren().isEmpty()) {
                    currentChatMessagesContainer.getChildren().remove(0);
                }
                
                // 添加新的歡迎訊息到頂部
                currentChatMessagesContainer.getChildren().add(0, createSystemMessageBox(
                    "🎉 分析完成！\n\n📊 最新分析結果：\n" + newContent + "\n\n💬 您可以基於這些分析結果提問！"
                ));
                
                System.out.println("✅ AI 聊天初始內容已更新");
            });
        }
    }
} 