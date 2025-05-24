import com.algolia.search.DefaultSearchClient;
import com.algolia.search.SearchClient;
import com.algolia.search.SearchIndex;
import com.algolia.search.models.indexing.SearchResult;
import com.algolia.search.models.indexing.Hit;

import java.util.List;

public class AlgoliaSearchExample {
    public static void main(String[] args) {
        String applicationID = "V269PWJYC3";
        String apiKey = "956223ed5b8db8cabdf440b82a2e03f9";
        String indexName = "restaurants";

        SearchClient client = DefaultSearchClient.create(applicationID, apiKey);
        SearchIndex<Hit> index = client.initIndex(indexName, Hit.class);

        try {
            SearchResult<Hit> result = index.search("牛肉麵");
            List<Hit> hits = result.getHits();

            for (Hit hit : hits) {
                System.out.println("找到餐廳：" + hit.get("name") + " / 地址：" + hit.get("address"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
