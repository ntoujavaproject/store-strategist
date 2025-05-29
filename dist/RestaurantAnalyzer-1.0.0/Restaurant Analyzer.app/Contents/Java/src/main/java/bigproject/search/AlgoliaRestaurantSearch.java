package bigproject.search;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 餐廳模糊搜索系統
 * 
 * 功能：使用 Algolia REST API 進行餐廳資料的模糊搜索
 * 作者：James Su
 * 版本：1.0.0
 * 
 * 此程式使用 Algolia REST API 實現餐廳資料的搜索功能，
 * 支援模糊搜索、多字符容錯和自動降級搜索策略。
 * 
 * 使用方法：
 * 1. 作為庫使用：建立 AlgoliaRestaurantSearch 實例並呼叫 performSearch 方法
 * 2. 作為獨立程式執行：java -cp ".:../../../libs/json-20230227.jar" AlgoliaRestaurantSearch [搜索關鍵字]
 * 
 * 注意：需要 org.json 庫支援，請確保 json-20230227.jar 在 classpath 中
 */
public class AlgoliaRestaurantSearch {
    // Algolia 設定常量
    private static final String APPLICATION_ID = "V269PWJYC3";
    private static final String API_KEY = "956223ed5b8db8cabdf440b82a2e03f9";
    private static final String INDEX_NAME = "restaurants";
    
    public static void main(String[] args) {
        try {
            // 取得用戶搜尋詞
            String queryText = "酒";
            if (args.length > 0) {
                queryText = args[0];
            } else {
                // 如果沒有提供參數，請用戶輸入搜索詞
                Scanner scanner = new Scanner(System.in);
                System.out.print("請輸入您想搜尋的餐廳關鍵字: ");
                queryText = scanner.nextLine().trim();
                scanner.close();
            }
            
            // 執行精確搜索
            AlgoliaRestaurantSearch searcher = new AlgoliaRestaurantSearch();
            JSONObject searchResult = searcher.performSearch(queryText, true);
            int hitsCount = searchResult.getInt("nbHits");
            
            System.out.println("找到 " + hitsCount + " 家與「" + queryText + "」相關的餐廳：\n");
            
            if (hitsCount > 0) {
                // 顯示搜尋結果
                displaySearchResults(searchResult.getJSONArray("hits"), false);
            } else {
                // 如果沒有找到結果，嘗試使用第一個字符搜尋
                if (queryText.length() > 0) {
                    System.out.println("正在嘗試使用第一個字符進行搜索...\n");
                    
                    String firstChar = queryText.substring(0, 1);
                    JSONObject relaxedResult = searcher.performSearch(firstChar, true);
                    int relaxedHitsCount = relaxedResult.getInt("nbHits");
                    
                    if (relaxedHitsCount > 0) {
                        System.out.println("使用第一個字「" + firstChar + "」找到 " + relaxedHitsCount + " 家餐廳：\n");
                        displaySearchResults(relaxedResult.getJSONArray("hits"), true);
                    } else {
                        // 提供搜尋建議
                        showSearchSuggestions(queryText);
                    }
                } else {
                    showSearchSuggestions(queryText);
                }
            }
            
        } catch (Exception e) {
            System.out.println("搜索時發生錯誤：");
            e.printStackTrace();
            System.out.println("\n請確保 org.json 的 JAR 檔案在 classpath 中：");
            System.out.println("編譯指令: javac -cp \".:../../../libs/json-20230227.jar\" AlgoliaRestaurantSearch.java");
            System.out.println("執行指令: java -cp \".:../../../libs/json-20230227.jar\" AlgoliaRestaurantSearch");
        }
    }
    
    /**
     * 執行 Algolia 搜索
     * 
     * @param query 搜索關鍵字
     * @param enableTypoTolerance 是否啟用模糊搜索
     * @return JSONObject 搜索結果
     * @throws Exception 如有連接或解析錯誤
     */
    public JSONObject performSearch(String query, boolean enableTypoTolerance) throws Exception {
        // 建立 HTTP 連接
        URL url = new URL("https://" + APPLICATION_ID + "-dsn.algolia.net/1/indexes/" + INDEX_NAME + "/query");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-Algolia-API-Key", API_KEY);
        connection.setRequestProperty("X-Algolia-Application-Id", APPLICATION_ID);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        // 使用 URL 編碼的搜索參數
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String searchParams = "query=" + encodedQuery + 
                             "&typoTolerance=" + (enableTypoTolerance ? "true" : "false") + 
                             "&hitsPerPage=20" +
                             "&attributesToRetrieve=name,address" +
                             "&minWordSizefor1Typo=2" + // 2個字符以上允許1個錯誤
                             "&minWordSizefor2Typos=5"; // 5個字符以上允許2個錯誤
        
        // 構建請求 JSON
        String requestJson = "{\"params\":\"" + searchParams + "\"}";
        
        // 發送請求
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestJson.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // 讀取響應
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // 解析 JSON 響應
        return new JSONObject(response.toString());
    }
    
    /**
     * 顯示搜索結果
     * 
     * @param hits 搜索結果數組
     * @param isRelaxedSearch 是否為寬鬆搜索結果
     */
    private static void displaySearchResults(JSONArray hits, boolean isRelaxedSearch) {
        int limit = Math.min(hits.length(), isRelaxedSearch ? 5 : hits.length());
        
        for (int i = 0; i < limit; i++) {
            JSONObject hit = hits.getJSONObject(i);
            String name = hit.getString("name");
            String address = hit.getString("address");
            
            // 檢查是否有高亮結果（表示模糊匹配）
            String matchType = "";
            if (isRelaxedSearch) {
                matchType = " (寬鬆匹配)";
            } else if (hit.has("_highlightResult")) {
                matchType = " (模糊匹配)";
            }
            
            System.out.println((i + 1) + ". 餐廳：" + name + matchType);
            System.out.println("   地址：" + address);
            System.out.println();
        }
    }
    
    /**
     * 顯示搜索建議
     * 
     * @param queryText 用戶輸入的搜索詞
     */
    private static void showSearchSuggestions(String queryText) {
        System.out.println("沒有找到符合「" + queryText + "」的餐廳。");
        System.out.println("建議您:");
        System.out.println("1. 嘗試使用不同的關鍵詞");
        System.out.println("2. 使用更短、更一般的詞彙");
        System.out.println("3. 檢查拼寫是否正確");
        System.out.println("4. 嘗試搜尋餐廳常見類型，如: 火鍋、咖啡、牛肉麵等");
    }
} 