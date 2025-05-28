package bigproject.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AI對話記錄管理器
 * 負責存儲和管理AI對話歷史、餐廳特色資料、經營建議等資訊
 */
public class ChatHistoryManager {
    private static final String CHAT_HISTORY_DIR = "chat_history";
    private static final String CHAT_HISTORY_FILE = "ai_conversation_records.json";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private ObjectMapper mapper;
    private File historyFile;
    
    /**
     * 建構函數
     */
    public ChatHistoryManager() {
        this.mapper = new ObjectMapper();
        
        // 創建對話記錄目錄
        File historyDir = new File(CHAT_HISTORY_DIR);
        if (!historyDir.exists()) {
            historyDir.mkdirs();
        }
        
        this.historyFile = new File(historyDir, CHAT_HISTORY_FILE);
        
        // 如果檔案不存在，創建初始結構
        if (!historyFile.exists()) {
            initializeHistoryFile();
        }
    }
    
    /**
     * 初始化對話記錄檔案
     */
    private void initializeHistoryFile() {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("version", "1.0");
            root.put("created_at", LocalDateTime.now().format(DATE_FORMAT));
            root.put("last_updated", LocalDateTime.now().format(DATE_FORMAT));
            root.set("conversations", mapper.createArrayNode());
            
            mapper.writerWithDefaultPrettyPrinter().writeValue(historyFile, root);
            System.out.println("✅ 初始化AI對話記錄檔案: " + historyFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("❌ 初始化對話記錄檔案失敗: " + e.getMessage());
        }
    }
    
    /**
     * 開始新的對話會話
     * @param restaurantName 餐廳名稱
     * @param restaurantId 餐廳ID
     * @param initialFeatures 初始特色資料
     * @return 會話ID
     */
    public String startNewConversation(String restaurantName, String restaurantId, String initialFeatures) {
        try {
            JsonNode root = mapper.readTree(historyFile);
            ObjectNode rootNode = (ObjectNode) root;
            ArrayNode conversations = (ArrayNode) root.get("conversations");
            
            // 生成會話ID（基於時間戳）
            String sessionId = "session_" + System.currentTimeMillis();
            
            // 創建新會話記錄
            ObjectNode conversation = mapper.createObjectNode();
            conversation.put("session_id", sessionId);
            conversation.put("restaurant_name", restaurantName);
            conversation.put("restaurant_id", restaurantId);
            conversation.put("start_time", LocalDateTime.now().format(DATE_FORMAT));
            conversation.put("initial_features", initialFeatures);
            conversation.set("messages", mapper.createArrayNode());
            conversation.set("business_suggestions", mapper.createArrayNode());
            conversation.put("is_active", true);
            
            conversations.add(conversation);
            
            // 更新根節點的最後更新時間
            rootNode.put("last_updated", LocalDateTime.now().format(DATE_FORMAT));
            
            // 保存到檔案
            mapper.writerWithDefaultPrettyPrinter().writeValue(historyFile, rootNode);
            
            System.out.println("✅ 開始新對話會話: " + sessionId + " (餐廳: " + restaurantName + ")");
            return sessionId;
            
        } catch (IOException e) {
            System.err.println("❌ 開始新對話會話失敗: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 添加對話訊息
     * @param sessionId 會話ID
     * @param userMessage 用戶訊息
     * @param aiResponse AI回應
     */
    public void addChatMessage(String sessionId, String userMessage, String aiResponse) {
        try {
            JsonNode root = mapper.readTree(historyFile);
            ObjectNode rootNode = (ObjectNode) root;
            ArrayNode conversations = (ArrayNode) root.get("conversations");
            
            // 找到對應的會話
            for (JsonNode conversation : conversations) {
                if (sessionId.equals(conversation.get("session_id").asText())) {
                    ArrayNode messages = (ArrayNode) conversation.get("messages");
                    
                    // 創建訊息記錄
                    ObjectNode messageRecord = mapper.createObjectNode();
                    messageRecord.put("timestamp", LocalDateTime.now().format(DATE_FORMAT));
                    messageRecord.put("user_message", userMessage);
                    messageRecord.put("ai_response", aiResponse);
                    
                    messages.add(messageRecord);
                    
                    // 更新最後活動時間
                    ((ObjectNode) conversation).put("last_activity", LocalDateTime.now().format(DATE_FORMAT));
                    break;
                }
            }
            
            // 更新根節點的最後更新時間
            rootNode.put("last_updated", LocalDateTime.now().format(DATE_FORMAT));
            
            // 保存到檔案
            mapper.writerWithDefaultPrettyPrinter().writeValue(historyFile, rootNode);
            
            System.out.println("✅ 添加對話訊息到會話: " + sessionId);
            
        } catch (IOException e) {
            System.err.println("❌ 添加對話訊息失敗: " + e.getMessage());
        }
    }
    
    /**
     * 添加經營建議
     * @param sessionId 會話ID
     * @param suggestions 經營建議列表
     */
    public void addBusinessSuggestions(String sessionId, List<BusinessSuggestion> suggestions) {
        try {
            JsonNode root = mapper.readTree(historyFile);
            ObjectNode rootNode = (ObjectNode) root;
            ArrayNode conversations = (ArrayNode) root.get("conversations");
            
            // 找到對應的會話
            for (JsonNode conversation : conversations) {
                if (sessionId.equals(conversation.get("session_id").asText())) {
                    ArrayNode suggestionsArray = (ArrayNode) conversation.get("business_suggestions");
                    
                    // 添加建議記錄
                    ObjectNode suggestionRecord = mapper.createObjectNode();
                    suggestionRecord.put("generated_at", LocalDateTime.now().format(DATE_FORMAT));
                    
                    ArrayNode suggestionsList = mapper.createArrayNode();
                    for (BusinessSuggestion suggestion : suggestions) {
                        ObjectNode suggestionNode = mapper.createObjectNode();
                        suggestionNode.put("aspect", suggestion.getAspect());
                        suggestionNode.put("suggestion", suggestion.getSuggestion());
                        suggestionNode.put("timeline", suggestion.getTimeline());
                        suggestionNode.put("priority", suggestion.getPriority());
                        suggestionsList.add(suggestionNode);
                    }
                    
                    suggestionRecord.set("suggestions", suggestionsList);
                    suggestionsArray.add(suggestionRecord);
                    
                    break;
                }
            }
            
            // 更新根節點的最後更新時間
            rootNode.put("last_updated", LocalDateTime.now().format(DATE_FORMAT));
            
            // 保存到檔案
            mapper.writerWithDefaultPrettyPrinter().writeValue(historyFile, rootNode);
            
            System.out.println("✅ 添加經營建議到會話: " + sessionId);
            
        } catch (IOException e) {
            System.err.println("❌ 添加經營建議失敗: " + e.getMessage());
        }
    }
    
    /**
     * 結束對話會話
     * @param sessionId 會話ID
     */
    public void endConversation(String sessionId) {
        try {
            JsonNode root = mapper.readTree(historyFile);
            ObjectNode rootNode = (ObjectNode) root;
            ArrayNode conversations = (ArrayNode) root.get("conversations");
            
            // 找到對應的會話並標記為結束
            for (JsonNode conversation : conversations) {
                if (sessionId.equals(conversation.get("session_id").asText())) {
                    ((ObjectNode) conversation).put("is_active", false);
                    ((ObjectNode) conversation).put("end_time", LocalDateTime.now().format(DATE_FORMAT));
                    break;
                }
            }
            
            // 更新根節點的最後更新時間
            rootNode.put("last_updated", LocalDateTime.now().format(DATE_FORMAT));
            
            // 保存到檔案
            mapper.writerWithDefaultPrettyPrinter().writeValue(historyFile, rootNode);
            
            System.out.println("✅ 結束對話會話: " + sessionId);
            
        } catch (IOException e) {
            System.err.println("❌ 結束對話會話失敗: " + e.getMessage());
        }
    }
    
    /**
     * 獲取餐廳的所有對話記錄
     * @param restaurantName 餐廳名稱
     * @return 對話記錄列表
     */
    public List<ConversationRecord> getConversationsByRestaurant(String restaurantName) {
        List<ConversationRecord> records = new ArrayList<>();
        
        try {
            JsonNode root = mapper.readTree(historyFile);
            ArrayNode conversations = (ArrayNode) root.get("conversations");
            
            for (JsonNode conversation : conversations) {
                if (restaurantName.equals(conversation.get("restaurant_name").asText())) {
                    ConversationRecord record = parseConversationRecord(conversation);
                    records.add(record);
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ 獲取對話記錄失敗: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * 獲取最近的對話記錄
     * @param limit 限制數量
     * @return 對話記錄列表
     */
    public List<ConversationRecord> getRecentConversations(int limit) {
        List<ConversationRecord> records = new ArrayList<>();
        
        try {
            JsonNode root = mapper.readTree(historyFile);
            ArrayNode conversations = (ArrayNode) root.get("conversations");
            
            // 遍歷會話記錄（假設已按時間排序）
            int count = 0;
            for (int i = conversations.size() - 1; i >= 0 && count < limit; i--) {
                JsonNode conversation = conversations.get(i);
                ConversationRecord record = parseConversationRecord(conversation);
                records.add(record);
                count++;
            }
            
        } catch (IOException e) {
            System.err.println("❌ 獲取最近對話記錄失敗: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * 解析對話記錄節點
     */
    private ConversationRecord parseConversationRecord(JsonNode conversation) {
        ConversationRecord record = new ConversationRecord();
        record.setSessionId(conversation.get("session_id").asText());
        record.setRestaurantName(conversation.get("restaurant_name").asText());
        record.setRestaurantId(conversation.get("restaurant_id").asText());
        record.setStartTime(conversation.get("start_time").asText());
        record.setInitialFeatures(conversation.get("initial_features").asText());
        record.setActive(conversation.get("is_active").asBoolean());
        
        if (conversation.has("end_time")) {
            record.setEndTime(conversation.get("end_time").asText());
        }
        
        if (conversation.has("last_activity")) {
            record.setLastActivity(conversation.get("last_activity").asText());
        }
        
        // 解析訊息
        ArrayNode messages = (ArrayNode) conversation.get("messages");
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (JsonNode message : messages) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setTimestamp(message.get("timestamp").asText());
            chatMessage.setUserMessage(message.get("user_message").asText());
            chatMessage.setAiResponse(message.get("ai_response").asText());
            chatMessages.add(chatMessage);
        }
        record.setMessages(chatMessages);
        
        return record;
    }
    
    /**
     * 清理舊的對話記錄（保留最近30天的記錄）
     */
    public void cleanupOldRecords() {
        try {
            JsonNode root = mapper.readTree(historyFile);
            ObjectNode rootNode = (ObjectNode) root;
            ArrayNode conversations = (ArrayNode) root.get("conversations");
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            ArrayNode filteredConversations = mapper.createArrayNode();
            
            for (JsonNode conversation : conversations) {
                String startTimeStr = conversation.get("start_time").asText();
                LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DATE_FORMAT);
                
                if (startTime.isAfter(cutoffDate)) {
                    filteredConversations.add(conversation);
                }
            }
            
            rootNode.set("conversations", filteredConversations);
            rootNode.put("last_updated", LocalDateTime.now().format(DATE_FORMAT));
            
            mapper.writerWithDefaultPrettyPrinter().writeValue(historyFile, rootNode);
            
            System.out.println("✅ 清理舊對話記錄完成，保留 " + filteredConversations.size() + " 筆記錄");
            
        } catch (IOException e) {
            System.err.println("❌ 清理舊對話記錄失敗: " + e.getMessage());
        }
    }
    
    /**
     * 匯出對話記錄到指定檔案
     * @param outputFile 輸出檔案
     * @param restaurantName 餐廳名稱（可選，為null時匯出所有記錄）
     */
    public void exportConversations(File outputFile, String restaurantName) {
        try {
            JsonNode root = mapper.readTree(historyFile);
            
            if (restaurantName != null) {
                // 只匯出特定餐廳的記錄
                ObjectNode filteredRoot = mapper.createObjectNode();
                filteredRoot.put("version", root.get("version").asText());
                filteredRoot.put("exported_at", LocalDateTime.now().format(DATE_FORMAT));
                filteredRoot.put("restaurant_filter", restaurantName);
                
                ArrayNode filteredConversations = mapper.createArrayNode();
                ArrayNode conversations = (ArrayNode) root.get("conversations");
                
                for (JsonNode conversation : conversations) {
                    if (restaurantName.equals(conversation.get("restaurant_name").asText())) {
                        filteredConversations.add(conversation);
                    }
                }
                
                filteredRoot.set("conversations", filteredConversations);
                mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, filteredRoot);
            } else {
                // 匯出所有記錄
                mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, root);
            }
            
            System.out.println("✅ 成功匯出對話記錄到: " + outputFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("❌ 匯出對話記錄失敗: " + e.getMessage());
        }
    }
    
    /**
     * 經營建議資料類
     */
    public static class BusinessSuggestion {
        private String aspect;
        private String suggestion;
        private String timeline;
        private String priority;
        
        public BusinessSuggestion(String aspect, String suggestion, String timeline, String priority) {
            this.aspect = aspect;
            this.suggestion = suggestion;
            this.timeline = timeline;
            this.priority = priority;
        }
        
        // Getters
        public String getAspect() { return aspect; }
        public String getSuggestion() { return suggestion; }
        public String getTimeline() { return timeline; }
        public String getPriority() { return priority; }
    }
    
    /**
     * 對話記錄資料類
     */
    public static class ConversationRecord {
        private String sessionId;
        private String restaurantName;
        private String restaurantId;
        private String startTime;
        private String endTime;
        private String lastActivity;
        private String initialFeatures;
        private boolean isActive;
        private List<ChatMessage> messages;
        
        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getRestaurantName() { return restaurantName; }
        public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
        
        public String getRestaurantId() { return restaurantId; }
        public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
        
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        
        public String getLastActivity() { return lastActivity; }
        public void setLastActivity(String lastActivity) { this.lastActivity = lastActivity; }
        
        public String getInitialFeatures() { return initialFeatures; }
        public void setInitialFeatures(String initialFeatures) { this.initialFeatures = initialFeatures; }
        
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        
        public List<ChatMessage> getMessages() { return messages; }
        public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    }
    
    /**
     * 聊天訊息資料類
     */
    public static class ChatMessage {
        private String timestamp;
        private String userMessage;
        private String aiResponse;
        
        // Getters and Setters
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
        
        public String getAiResponse() { return aiResponse; }
        public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
    }
} 