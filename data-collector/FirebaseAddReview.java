import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FirebaseAddReview {

    public static void addReview(String restaurantId, String reviewId, Map<String, Object> reviewData) {
        try {
            String projectId = "java2025-91d74";
            String urlString = "https://firestore.googleapis.com/v1/projects/" + projectId +
                    "/databases/(default)/documents/restaurants/" + restaurantId +
                    "/reviews?documentId=" + reviewId;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            String json = String.format(
                    "{\"fields\": {\"reviewer_id\": {\"stringValue\": \"%s\"},\"review_id\": {\"stringValue\": \"%s\"},\"comment\": {\"stringValue\": \"%s\"},\"star_rating\": {\"doubleValue\": %s},\"comment_date\": {\"timestampValue\": \"%s\"}}}",
                    reviewData.get("reviewer_id"),
                    reviewData.get("review_id"),
                    reviewData.get("comment"),
                    reviewData.get("star_rating"),
                    reviewData.get("comment_date"));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            System.out.println("新增評論 HTTP 狀態碼：" + conn.getResponseCode());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
