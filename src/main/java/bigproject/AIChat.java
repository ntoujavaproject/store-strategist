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
    
    // 新增：專門的ChatRestaurantAdvisor實例，持續管理餐廳數據
    private bigproject.ai.ChatRestaurantAdvisor advisor;
    
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
        
        // 將聊天容器放在 ScrollPane 中 - 改善滾動條顯示
        ScrollPane chatScrollPane = new ScrollPane(chatMessagesContainer);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setFitToHeight(false); // 確保高度能自動調整
        chatScrollPane.setPrefHeight(400);
        chatScrollPane.setMaxHeight(Double.MAX_VALUE); // 允許無限制高度
        
        // 改善滾動條樣式 - 讓滾動條更明顯可見
        chatScrollPane.setStyle(
            "-fx-background-color: #2C2C2C; " +
            "-fx-border-color: transparent; " +
            "-fx-background: #2C2C2C; " +
            "-fx-control-inner-background: #2C2C2C;"
        );
        
        // 強制顯示滾動條，讓用戶能清楚看到並使用
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);  // 垂直滾動條始終顯示
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);    // 水平滾動條隱藏
        
        // 設置滾動條樣式
        chatScrollPane.getStylesheets().add("data:text/css," +
            ".scroll-pane .scroll-bar:vertical { " +
            "    -fx-background-color: #4A4A4A; " +
            "    -fx-opacity: 1.0; " +
            "    -fx-pref-width: 15px; " +
            "} " +
            ".scroll-pane .scroll-bar:vertical .track { " +
            "    -fx-background-color: #3A3A3A; " +
            "    -fx-opacity: 1.0; " +
            "} " +
            ".scroll-pane .scroll-bar:vertical .thumb { " +
            "    -fx-background-color: #6A6A6A; " +
            "    -fx-opacity: 1.0; " +
            "} " +
            ".scroll-pane .scroll-bar:vertical .thumb:hover { " +
            "    -fx-background-color: #8A8A8A; " +
            "}"
        );
        
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        
        // 改善滾動行為 - 確保內容完整顯示
        chatScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            chatMessagesContainer.setPrefWidth(newBounds.getWidth() - 20); // 留出滾動條空間
            chatMessagesContainer.setMaxWidth(newBounds.getWidth() - 20);
        });
        
        // 初始化歡迎訊息
        addWelcomeMessage(chatMessagesContainer, initialContent);
        
        // 創建用戶輸入區域
        userInputField = new TextField();
        userInputField.setPromptText("輸入您的問題或想法...");
        userInputField.setPrefHeight(40);
        userInputField.setStyle("-fx-background-color: #444444; -fx-text-fill: white; -fx-font-size: 14px; -fx-prompt-text-fill: #AAAAAA;");
        
        // 設置較高的字體大小以改善顯示
        userInputField.setFont(Font.font("System", 16));
        
        // 添加中文輸入法支援和字符過濾
        userInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // 過濾掉注音符號，只保留正常的中文字符、英文字符和標點符號
                String filteredText = newValue.replaceAll("[ㄅ-ㄩˊˇˋ˙]", "");
                
                // 如果過濾後的文字與原文字不同，更新文字框內容
                if (!filteredText.equals(newValue)) {
                    Platform.runLater(() -> {
                        int caretPosition = userInputField.getCaretPosition();
                        userInputField.setText(filteredText);
                        // 保持游標位置
                        userInputField.positionCaret(Math.min(caretPosition, filteredText.length()));
                    });
                }
            }
        });
        
        // 當輸入框獲得焦點時，確保正確的輸入法狀態
        userInputField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                Platform.runLater(() -> {
                    userInputField.positionCaret(userInputField.getText().length());
                });
            }
        });
        
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
                
                // 滾動到底部 - 使用更好的滾動方法
                scrollToBottom(chatScrollPane);
                
                // 使用CompletableFuture在後台處理AI響應
                CompletableFuture.runAsync(() -> {
                    try {
                        System.out.println("🤖 開始調用AI生成回應...");
                        
                        // 獲取AI響應 (實際調用Ollama API)
                        String aiResponse = callOllamaAPI(userMessage, contentType, initialContent);
                        
                        System.out.println("✅ AI回應生成完成");
                        System.out.println("📏 AI回應總長度: " + aiResponse.length() + " 字元");
                        System.out.println("📝 AI回應完整內容: " + aiResponse);
                        
                        // 記錄對話到JSON檔案
                        if (currentSessionId != null) {
                            chatHistoryManager.addChatMessage(currentSessionId, userMessage, aiResponse);
                        }
                        
                        // 更新UI (必須在JavaFX線程中進行)
                        Platform.runLater(() -> {
                            System.out.println("🖥️ 開始更新UI顯示AI回應");
                            
                            // 移除"思考中"的提示
                            chatMessagesContainer.getChildren().remove(chatMessagesContainer.getChildren().size() - 1);
                            
                            System.out.println("📦 創建AI訊息框，內容長度: " + aiResponse.length());
                            
                            // 添加AI的響應 - 確保完整內容都顯示
                            HBox aiMessageBox = createAIMessageBox(aiResponse);
                            chatMessagesContainer.getChildren().add(aiMessageBox);
                            
                            System.out.println("✅ AI訊息框已添加到容器");
                            
                            // 確保容器佈局更新
                            chatMessagesContainer.applyCss();
                            chatMessagesContainer.layout();
                            
                            // 滾動到底部以顯示新回應
                            scrollToBottom(chatScrollPane);
                            
                            System.out.println("🎉 AI回應UI更新完成");
                        });
                    } catch (Exception e) {
                        System.err.println("❌ AI回應處理異常: " + e.getMessage());
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            // 處理錯誤情況
                            chatMessagesContainer.getChildren().remove(chatMessagesContainer.getChildren().size() - 1);
                            String errorMessage = "AI助手: 抱歉，我遇到了一些問題，無法回應您的問題。\n錯誤詳情：" + e.getMessage();
                            chatMessagesContainer.getChildren().add(createSystemMessageBox(errorMessage));
                            scrollToBottom(chatScrollPane);
                        });
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
            advisor = new bigproject.ai.ChatRestaurantAdvisor();
            
            // 使用最新的初始內容（包含分析完成後的詳細數據）
            String contentToUse = currentInitialContent != null ? currentInitialContent : initialContent;
            System.out.println("📊 傳遞給AI的內容長度: " + contentToUse.length() + " 字元");
            System.out.println("📝 內容預覽: " + contentToUse.substring(0, Math.min(100, contentToUse.length())) + "...");
            
            // 設置餐廳特色資訊
            advisor.setRestaurantFeatures(contentToUse);
            
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
     * 創建用戶訊息框（顯示在右側）- 改善長文字寬度控制
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
        
        // 改善：動態調整最大寬度 - 支援更長的用戶訊息
        int contentLength = message.length();
        System.out.println("🔍 用戶訊息內容長度: " + contentLength + " 字元");
        
        // 為長用戶訊息提供足夠的顯示空間（與AI回覆類似的分級）
        if (contentLength > 1500) {
            messageLabel.setMaxWidth(850);  // 超長用戶訊息
            messageLabel.setPrefWidth(850);
            System.out.println("✅ 設置超長用戶訊息寬度: 850px");
        } else if (contentLength > 800) {
            messageLabel.setMaxWidth(750);  // 長用戶訊息
            messageLabel.setPrefWidth(750);
            System.out.println("✅ 設置長用戶訊息寬度: 750px");
        } else if (contentLength > 300) {
            messageLabel.setMaxWidth(650);  // 中等用戶訊息
            messageLabel.setPrefWidth(650);
            System.out.println("✅ 設置中等用戶訊息寬度: 650px");
        } else if (contentLength > 100) {
            messageLabel.setMaxWidth(500);  // 一般用戶訊息
            messageLabel.setPrefWidth(500);
            System.out.println("✅ 設置一般用戶訊息寬度: 500px");
        } else {
            messageLabel.setMaxWidth(400);  // 短用戶訊息
            messageLabel.setPrefWidth(400);
            System.out.println("✅ 設置短用戶訊息寬度: 400px");
        }
        
        // 確保標籤能自動調整高度以容納所有文字
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);
        messageLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        messageLabel.setMaxHeight(Region.USE_COMPUTED_SIZE);
        
        messageBox.getChildren().add(messageLabel);
        
        System.out.println("✅ 用戶訊息框創建完成，內容長度: " + contentLength);
        return messageBox;
    }
    
    /**
     * 創建AI訊息框（顯示在左側）- 改善完整內容顯示
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
        
        // 訊息氣泡 - 改善長文字顯示
        Label messageLabel = new Label(message);
        messageLabel.setPadding(new Insets(20, 25, 20, 25)); // 增加內邊距
        messageLabel.setStyle("-fx-background-color: #444444; " +
                             "-fx-text-fill: white; " +
                             "-fx-background-radius: 18; " +
                             "-fx-font-size: 16px; " +
                             "-fx-wrap-text: true; " +
                             "-fx-text-alignment: left; " +        // 文字左對齊
                             "-fx-line-spacing: 3px;");            // 增加行間距
        
        messageLabel.setWrapText(true);
        
        // 確保能顯示完整內容 - 根據內容長度設置合適的寬度
        int contentLength = message.length();
        System.out.println("🔍 AI回應內容長度: " + contentLength + " 字元");
        System.out.println("📝 AI回應前100字元: " + message.substring(0, Math.min(100, contentLength)));
        
        // 為長回應提供足夠的顯示空間
        if (contentLength > 2000) {
            messageLabel.setMaxWidth(900);  // 超長回應用最大寬度
            messageLabel.setPrefWidth(900);
            System.out.println("✅ 設置超長回應寬度: 900px");
        } else if (contentLength > 1000) {
            messageLabel.setMaxWidth(850);  // 長回應
            messageLabel.setPrefWidth(850);
            System.out.println("✅ 設置長回應寬度: 850px");
        } else if (contentLength > 500) {
            messageLabel.setMaxWidth(750);  // 中等回應
            messageLabel.setPrefWidth(750);
            System.out.println("✅ 設置中等回應寬度: 750px");
        } else {
            messageLabel.setMaxWidth(650);  // 短回應
            messageLabel.setPrefWidth(650);
            System.out.println("✅ 設置短回應寬度: 650px");
        }
        
        // 確保標籤能自動調整高度以容納所有文字
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);
        messageLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        messageLabel.setMaxHeight(Region.USE_COMPUTED_SIZE);
        
        // 確保文字不被截斷
        messageLabel.setMaxWidth(Double.MAX_VALUE);  // 允許無限寬度
        messageLabel.setWrapText(true);               // 強制換行
        
        messageBox.getChildren().addAll(avatarLabel, messageLabel);
        messageBox.setSpacing(10);
        
        // 設置 messageBox 也能自動調整
        messageBox.setMinHeight(Region.USE_PREF_SIZE);
        messageBox.setPrefHeight(Region.USE_COMPUTED_SIZE);
        messageBox.setMaxHeight(Region.USE_COMPUTED_SIZE);
        
        System.out.println("✅ AI訊息框創建完成，內容長度: " + contentLength);
        return messageBox;
    }
    
    /**
     * 創建系統訊息框（置中顯示）- 改善長文字顯示
     */
    private HBox createSystemMessageBox(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(10, 20, 10, 20));
        
        Label messageLabel = new Label(message);
        messageLabel.setPadding(new Insets(20, 25, 20, 25)); // 增加內邊距
        messageLabel.setStyle("-fx-background-color: #333333; " +
                             "-fx-text-fill: #CCCCCC; " +
                             "-fx-background-radius: 15; " +
                             "-fx-font-size: 15px; " +
                             "-fx-font-style: italic; " +
                             "-fx-wrap-text: true; " +
                             "-fx-text-alignment: left; " +  // 文字左對齊，更易閱讀
                             "-fx-line-spacing: 2px;");      // 增加行間距
        
        messageLabel.setWrapText(true);
        
        // 改善文字顯示：為長內容提供更大的顯示空間
        int contentLength = message.length();
        if (contentLength > 2000) {
            messageLabel.setMaxWidth(850);  // 超長內容用最大寬度
            messageLabel.setPrefWidth(850);
        } else if (contentLength > 1000) {
            messageLabel.setMaxWidth(800);  // 長內容
            messageLabel.setPrefWidth(800);
        } else if (contentLength > 500) {
            messageLabel.setMaxWidth(700);  // 中等內容
            messageLabel.setPrefWidth(700);
        } else {
            messageLabel.setMaxWidth(600);  // 短內容
            messageLabel.setPrefWidth(600);
        }
        
        // 確保標籤能自動調整高度以容納所有文字
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);
        messageLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        messageLabel.setMaxHeight(Region.USE_COMPUTED_SIZE);
        
        messageBox.getChildren().add(messageLabel);
        return messageBox;
    }
    
    /**
     * 添加歡迎訊息
     */
    private void addWelcomeMessage(VBox container, String initialContent) {
        StringBuilder welcomeText = new StringBuilder();
        welcomeText.append("💬 歡迎使用 AI 餐廳經營顧問助手！\n\n");
        welcomeText.append("📋 我已經載入了以下餐廳分析資料：\n\n");
        
        // 改善：顯示完整的分析內容，不再截斷
        if (initialContent != null && !initialContent.trim().isEmpty()) {
            // 如果內容很長，分段顯示以提高可讀性
            if (initialContent.length() > 500) {
                welcomeText.append("📊 詳細分析結果：\n");
                welcomeText.append(initialContent);
            } else {
                welcomeText.append(initialContent);
            }
        } else {
            welcomeText.append("⏳ 正在載入分析資料，請稍候...");
        }
        
        welcomeText.append("\n\n💡 您可以詢問以下類型的問題：");
        welcomeText.append("\n• 如何提升客戶滿意度？");
        welcomeText.append("\n• 建議增加什麼新的服務項目？");
        welcomeText.append("\n• 如何改善營運效率？");
        welcomeText.append("\n• 適合什麼樣的促銷活動？");
        welcomeText.append("\n• 分析我的優缺點");
        welcomeText.append("\n• 給我經營建議");
        welcomeText.append("\n\n🚀 現在就開始提問吧！");
        
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
            
            // 同時更新ChatRestaurantAdvisor的餐廳特色資訊
            if (advisor != null) {
                advisor.setRestaurantFeatures(newContent);
                System.out.println("✅ 已同步更新ChatRestaurantAdvisor的餐廳特色資訊");
            }
            
            Platform.runLater(() -> {
                // 移除舊的歡迎訊息（第一個子元素）
                if (!currentChatMessagesContainer.getChildren().isEmpty()) {
                    currentChatMessagesContainer.getChildren().remove(0);
                }
                
                // 創建新的完整歡迎訊息
                StringBuilder updatedWelcomeText = new StringBuilder();
                updatedWelcomeText.append("🎉 分析完成！\n\n");
                updatedWelcomeText.append("📊 最新完整分析結果：\n\n");
                
                // 顯示完整的分析結果，不再截斷
                if (newContent != null && !newContent.trim().isEmpty()) {
                    updatedWelcomeText.append(newContent);
                } else {
                    updatedWelcomeText.append("⚠️ 分析結果為空，請重新分析");
                }
                
                updatedWelcomeText.append("\n\n💬 您可以基於這些詳細分析結果提問！");
                updatedWelcomeText.append("\n💡 例如：");
                updatedWelcomeText.append("\n• 分析我的主要優缺點");
                updatedWelcomeText.append("\n• 如何改善客戶體驗？");
                updatedWelcomeText.append("\n• 建議具體的經營策略");
                
                // 添加新的歡迎訊息到頂部
                currentChatMessagesContainer.getChildren().add(0, createSystemMessageBox(updatedWelcomeText.toString()));
                
                System.out.println("✅ AI 聊天初始內容已更新，內容長度: " + newContent.length() + " 字元");
            });
        }
    }

    /**
     * 滾動到底部 - 改善滾動體驗
     */
    private void scrollToBottom(ScrollPane scrollPane) {
        Platform.runLater(() -> {
            // 確保佈局更新
            scrollPane.layout();
            scrollPane.applyCss();
            
            // 滾動到底部
            scrollPane.setVvalue(1.0);
            
            // 添加滾動提示（如果內容很長）
            if (scrollPane.getContent() instanceof VBox) {
                VBox content = (VBox) scrollPane.getContent();
                if (content.getChildren().size() > 3) {
                    // 顯示滾動提示
                    showScrollHint();
                }
            }
        });
    }
    
    /**
     * 顯示滾動提示
     */
    private void showScrollHint() {
        Platform.runLater(() -> {
            if (currentChatMessagesContainer != null && currentChatMessagesContainer.getChildren().size() == 2) {
                // 只在開始聊天時顯示一次提示
                HBox hintBox = new HBox();
                hintBox.setAlignment(Pos.CENTER);
                hintBox.setPadding(new Insets(5, 20, 5, 20));
                
                Label hintLabel = new Label("💡 提示：您可以使用滾動條查看上面的完整內容");
                hintLabel.setStyle("-fx-background-color: #444444; " +
                                 "-fx-text-fill: #AAAAAA; " +
                                 "-fx-background-radius: 10; " +
                                 "-fx-font-size: 12px; " +
                                 "-fx-padding: 8 15 8 15;");
                
                hintBox.getChildren().add(hintLabel);
                currentChatMessagesContainer.getChildren().add(1, hintBox); // 在歡迎訊息後插入
                
                // 5秒後自動移除提示
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        Platform.runLater(() -> {
                            if (currentChatMessagesContainer != null && 
                                currentChatMessagesContainer.getChildren().contains(hintBox)) {
                                currentChatMessagesContainer.getChildren().remove(hintBox);
                            }
                        });
                    } catch (InterruptedException e) {
                        // 忽略中斷
                    }
                }).start();
            }
        });
    }
} 