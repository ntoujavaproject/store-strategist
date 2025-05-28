package bigproject;

import bigproject.ai.ChatHistoryManager;
import bigproject.ai.ChatHistoryManager.ConversationRecord;
import bigproject.ai.ChatHistoryManager.ChatMessage;

import java.util.List;
import java.util.prefs.Preferences;

// 添加JavaFX imports
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 增強版UIManager，整合AI對話記錄功能
 * 替換預設經營建議為基於真實AI對話的建議
 */
public class UIManagerEnhanced {
    
    private ChatHistoryManager chatHistoryManager;
    private Preferences prefs;
    
    public UIManagerEnhanced() {
        this.chatHistoryManager = new ChatHistoryManager();
        this.prefs = Preferences.userNodeForPackage(UIManagerEnhanced.class);
    }
    
    /**
     * 顯示基於AI對話記錄的經營建議視圖
     * @param currentRestaurantName 當前餐廳名稱
     * @return 建議內容的HTML字串
     */
    public String showAIBasedSuggestionsView(String currentRestaurantName) {
        StringBuilder content = new StringBuilder();
        
        content.append("<html><head><style>");
        content.append("body { font-family: '微軟正黑體', Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }");
        content.append(".header { color: #E67649; font-size: 24px; font-weight: bold; margin-bottom: 20px; }");
        content.append(".restaurant-name { color: #6F6732; font-size: 18px; margin-bottom: 15px; }");
        content.append(".conversation { background: white; padding: 15px; margin: 10px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        content.append(".session-info { color: #666; font-size: 12px; margin-bottom: 10px; }");
        content.append(".message { margin: 8px 0; padding: 8px; border-radius: 5px; }");
        content.append(".user-message { background: #E67649; color: white; text-align: right; }");
        content.append(".ai-message { background: #DCF2CC; color: #2E7D32; }");
        content.append(".no-data { color: #999; font-style: italic; text-align: center; padding: 40px; }");
        content.append(".summary { background: #fff3e0; padding: 15px; margin: 15px 0; border-left: 4px solid #E67649; }");
        content.append("</style></head><body>");
        
        content.append("<div class='header'>🤖 AI 經營建議記錄</div>");
        
        if (currentRestaurantName != null && !currentRestaurantName.isEmpty()) {
            content.append("<div class='restaurant-name'>📍 餐廳：").append(currentRestaurantName).append("</div>");
            
            // 獲取該餐廳的所有對話記錄
            List<ConversationRecord> records = chatHistoryManager.getConversationsByRestaurant(currentRestaurantName);
            
            if (records.isEmpty()) {
                content.append("<div class='no-data'>");
                content.append("尚無AI對話記錄<br>");
                content.append("當您使用AI聊天功能時，對話記錄將會顯示在這裡<br>");
                content.append("這些記錄包含：<br>");
                content.append("• 餐廳特色分析結果<br>");
                content.append("• 經營建議對話內容<br>");
                content.append("• AI生成的改進方案<br>");
                content.append("</div>");
            } else {
                // 顯示對話統計
                content.append("<div class='summary'>");
                content.append("📊 <strong>對話統計</strong><br>");
                content.append("總會話數：").append(records.size()).append(" 次<br>");
                
                int totalMessages = records.stream().mapToInt(r -> r.getMessages().size()).sum();
                content.append("總對話訊息：").append(totalMessages).append(" 條<br>");
                
                long activeConversations = records.stream().filter(ConversationRecord::isActive).count();
                content.append("進行中對話：").append(activeConversations).append(" 個");
                content.append("</div>");
                
                // 顯示每個對話記錄
                for (ConversationRecord record : records) {
                    content.append("<div class='conversation'>");
                    content.append("<div class='session-info'>");
                    content.append("會話ID：").append(record.getSessionId()).append(" | ");
                    content.append("開始時間：").append(record.getStartTime());
                    if (record.getEndTime() != null) {
                        content.append(" | 結束時間：").append(record.getEndTime());
                    }
                    content.append(" | 狀態：").append(record.isActive() ? "進行中" : "已結束");
                    content.append("</div>");
                    
                    // 顯示初始特色資料
                    if (record.getInitialFeatures() != null && !record.getInitialFeatures().isEmpty()) {
                        content.append("<div class='message ai-message'>");
                        content.append("<strong>📋 初始分析資料：</strong><br>");
                        content.append(record.getInitialFeatures().replace("\n", "<br>"));
                        content.append("</div>");
                    }
                    
                    // 顯示對話訊息
                    for (ChatMessage message : record.getMessages()) {
                        // 用戶訊息
                        content.append("<div class='message user-message'>");
                        content.append("<strong>💬 用戶：</strong> ").append(message.getUserMessage());
                        content.append("<br><small>").append(message.getTimestamp()).append("</small>");
                        content.append("</div>");
                        
                        // AI回應
                        content.append("<div class='message ai-message'>");
                        content.append("<strong>🤖 AI助手：</strong><br>");
                        content.append(message.getAiResponse().replace("\n", "<br>"));
                        content.append("</div>");
                    }
                    
                    content.append("</div>");
                }
            }
        } else {
            // 顯示所有餐廳的最近對話記錄
            content.append("<div class='restaurant-name'>📈 最近的AI對話記錄</div>");
            
            List<ConversationRecord> recentRecords = chatHistoryManager.getRecentConversations(10);
            
            if (recentRecords.isEmpty()) {
                content.append("<div class='no-data'>");
                content.append("尚無任何AI對話記錄<br>");
                content.append("開始使用AI聊天功能來獲得個人化的經營建議！");
                content.append("</div>");
            } else {
                content.append("<div class='summary'>");
                content.append("📊 <strong>整體統計</strong><br>");
                content.append("最近 ").append(recentRecords.size()).append(" 次對話記錄");
                content.append("</div>");
                
                for (ConversationRecord record : recentRecords) {
                    content.append("<div class='conversation'>");
                    content.append("<div class='session-info'>");
                    content.append("餐廳：<strong>").append(record.getRestaurantName()).append("</strong> | ");
                    content.append("時間：").append(record.getStartTime()).append(" | ");
                    content.append("訊息數：").append(record.getMessages().size());
                    content.append("</div>");
                    
                    // 只顯示最後一條對話
                    if (!record.getMessages().isEmpty()) {
                        ChatMessage lastMessage = record.getMessages().get(record.getMessages().size() - 1);
                        content.append("<div class='message user-message'>");
                        content.append("<strong>最後問題：</strong> ").append(lastMessage.getUserMessage());
                        content.append("</div>");
                        content.append("<div class='message ai-message'>");
                        content.append("<strong>AI建議：</strong> ");
                        String response = lastMessage.getAiResponse();
                        if (response.length() > 150) {
                            response = response.substring(0, 150) + "...";
                        }
                        content.append(response.replace("\n", "<br>"));
                        content.append("</div>");
                    }
                    
                    content.append("</div>");
                }
            }
        }
        
        content.append("</body></html>");
        return content.toString();
    }
    
    /**
     * 獲取餐廳的對話統計資訊
     */
    public String getRestaurantChatStatistics(String restaurantName) {
        if (restaurantName == null || restaurantName.isEmpty()) {
            return "未指定餐廳";
        }
        
        List<ConversationRecord> records = chatHistoryManager.getConversationsByRestaurant(restaurantName);
        if (records.isEmpty()) {
            return "無對話記錄";
        }
        
        int totalConversations = records.size();
        int totalMessages = records.stream().mapToInt(r -> r.getMessages().size()).sum();
        long activeConversations = records.stream().filter(ConversationRecord::isActive).count();
        
        return String.format("對話次數: %d | 訊息總數: %d | 進行中: %d", 
                           totalConversations, totalMessages, activeConversations);
    }
    
    /**
     * 清理過期的對話記錄
     */
    public void cleanupOldRecords() {
        chatHistoryManager.cleanupOldRecords();
        System.out.println("✅ 已清理過期的AI對話記錄");
    }
    
    /**
     * 匯出餐廳的對話記錄
     */
    public void exportRestaurantConversations(String restaurantName, String outputPath) {
        try {
            chatHistoryManager.exportConversations(new java.io.File(outputPath), restaurantName);
            System.out.println("✅ 已匯出 " + restaurantName + " 的對話記錄到: " + outputPath);
        } catch (Exception e) {
            System.err.println("❌ 匯出對話記錄失敗: " + e.getMessage());
        }
    }
    
    /**
     * 檢查是否有對話記錄
     */
    public boolean hasConversationHistory(String restaurantName) {
        if (restaurantName == null || restaurantName.isEmpty()) {
            return false;
        }
        return !chatHistoryManager.getConversationsByRestaurant(restaurantName).isEmpty();
    }
} 