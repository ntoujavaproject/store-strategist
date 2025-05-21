package bigproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
    
    // 用於存放 callback 方法的接口
    public interface ChatStateChangeListener {
        void onChatStateChanged(boolean isShowing);
    }
    
    private ChatStateChangeListener stateChangeListener;

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
     * 隱藏 AI 聊天視圖
     */
    public void hideChatView() {
        System.out.println("執行 hideChatView 方法");
        
        // 確保在 JavaFX 應用程式執行緒上執行 UI 操作
        Platform.runLater(() -> {
            if (chatContainer != null && mainLayout.getCenter() == chatContainer) {
                System.out.println("恢復原始內容並隱藏聊天視圖");
                // 恢復原來的主內容
                mainLayout.setCenter(originalCenterContent);
                
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
        
        topBar.getChildren().addAll(backButton, titleLabel);
        
        // 創建聊天記錄區域
        chatHistoryArea = new TextArea();
        chatHistoryArea.setEditable(false);
        chatHistoryArea.setWrapText(true);
        chatHistoryArea.setPrefHeight(400);
        chatHistoryArea.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 14px;");
        VBox.setVgrow(chatHistoryArea, Priority.ALWAYS);
        
        // 初始化聊天記錄，添加 AI 歡迎消息
        StringBuilder chatHistory = new StringBuilder();
        chatHistory.append("AI助手: 您好！我們來討論這家餐廳的").append(contentType).append("。\n\n");
        chatHistory.append("以下是分析結果：\n").append(initialContent).append("\n\n");
        chatHistory.append("AI助手: 您對這些").append(contentType).append("有什麼想法或問題嗎？\n");
        chatHistoryArea.setText(chatHistory.toString());
        
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
                chatHistory.append("\n您: ").append(userMessage).append("\n");
                chatHistoryArea.setText(chatHistory.toString());
                
                // 清空輸入框
                userInputField.clear();
                
                // 添加"AI思考中"的提示
                chatHistory.append("\nAI助手: 思考中...");
                chatHistoryArea.setText(chatHistory.toString());
                
                // 滾動到底部
                chatHistoryArea.positionCaret(chatHistoryArea.getText().length());
                
                // 使用CompletableFuture在後台處理AI響應
                CompletableFuture.runAsync(() -> {
                    try {
                        // 獲取AI響應 (實際調用Ollama API)
                        String aiResponse = callOllamaAPI(userMessage, contentType, initialContent);
                        
                        // 更新UI (必須在JavaFX線程中進行)
                        Platform.runLater(() -> {
                            // 移除"思考中"的提示
                            chatHistory.delete(chatHistory.length() - 13, chatHistory.length());
                            // 添加AI的響應
                            chatHistory.append("\nAI助手: ").append(aiResponse).append("\n");
                            chatHistoryArea.setText(chatHistory.toString());
                            
                            // 滾動到底部
                            chatHistoryArea.positionCaret(chatHistoryArea.getText().length());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            // 處理錯誤情況
                            chatHistory.delete(chatHistory.length() - 13, chatHistory.length());
                            chatHistory.append("\nAI助手: 抱歉，我遇到了一些問題，無法回應您的問題。\n");
                            chatHistoryArea.setText(chatHistory.toString());
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
        chatContainer.getChildren().addAll(topBar, chatHistoryArea, inputBox);
        
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
            // 構建提示詞
            String prompt = "你是一個餐廳專業顧問。以下是關於一家餐廳的" + contentType + "的資訊：\n\n" 
                + initialContent + "\n\n用戶的問題或評論是：" + userMessage 
                + "\n\n請以專業、友好的方式回應，提供有價值的見解或建議。";
            
            // 檢查Ollama服務是否可用
            if (bigproject.ai.OllamaAPI.isServiceAvailable()) {
                // 嘗試使用自動下載和安裝的Ollama
                System.out.println("使用Ollama生成回應");
                String response = bigproject.ai.OllamaAPI.generateCompletion(prompt);
                
                // 檢查回應是否有效
                if (response != null && !response.isEmpty() && !response.startsWith("無法啟動Ollama")) {
                    return response;
                } else {
                    System.out.println("Ollama回應無效，使用內置回應：" + response);
                }
            }
            
            // 如果Ollama不可用或回應無效，使用內置回應
            System.out.println("使用內置回應生成");
            
            // 分析用戶查詢類型
            if (userMessage.contains("推薦") || userMessage.contains("建議")) {
                return "基於這家餐廳的" + contentType + "，我建議您可以關注他們的特色菜品，並提前了解價格區間。特別是招牌菜品獲得了多數顧客的好評。";
            } else if (userMessage.contains("價格") || userMessage.contains("多少錢")) {
                return "根據分析，這家餐廳的價格屬於中等水平，適合一般消費者。平均每人消費約在200-300元之間，商業午餐價格較為優惠。";
            } else if (userMessage.contains("人氣") || userMessage.contains("熱門")) {
                return "這家餐廳在當地確實頗受歡迎，尤其是週末晚餐時段可能需要排隊。建議提前1-2天預約以確保座位。";
            } else if (userMessage.contains("改進") || userMessage.contains("提升")) {
                return "如果這家餐廳想要提升體驗，可以考慮改善服務速度，特別是在尖峰時段增加人手，並可以增加一些創新菜品來吸引更多年輕客群。";
            } else {
                // 根據內容類型給出相應回覆
                switch (contentType) {
                    case "餐廳特色":
                        return "這家餐廳的特色主要體現在獨特的菜品風格和氛圍營造上。他們使用當地新鮮食材，菜品呈現精緻，特別是招牌海鮮料理廣受好評。餐廳裝潢融合現代與傳統元素，營造出舒適且有特色的用餐環境。";
                    case "餐廳優點":
                        return "從顧客評價來看，這家餐廳的優勢包括：1) 食材新鮮度高；2) 服務人員態度親切專業；3) 環境舒適且乾淨；4) 菜品口味獨特且穩定；5) 提供靈活的訂位服務。這些因素共同創造了良好的用餐體驗。";
                    case "餐廳缺點":
                        return "根據收集的評論，這家餐廳的改進空間主要在於：1) 高峰期的等待時間較長；2) 部分菜品價格略高；3) 菜單選項可以更多樣化；4) 停車位不足；5) 部分位置靠近廚房，可能較為吵雜。";
                    default:
                        return "這是一家評價不錯的餐廳，擁有特色菜品和舒適的用餐環境。根據多數顧客反饋，服務品質和食物口味都維持在較高水準。如果您有更具體的問題，例如關於特定菜品或最佳用餐時段，請隨時詢問。";
                }
            }
        } catch (Exception e) {
            // 出現異常時，記錄錯誤並返回一個通用回應
            System.err.println("AI回應生成異常: " + e.getMessage());
            e.printStackTrace();
            return "很抱歉，我無法處理您的請求。請嘗試使用更明確的問題，或查看餐廳的具體評論以獲取更多資訊。";
        }
    }

    /**
     * 判斷聊天視圖是否處於活躍狀態
     */
    public boolean isActive() {
        return isActive;
    }
} 