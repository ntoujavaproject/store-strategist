import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FirebaseAddRestaurant {

    public static void addRestaurant(String restaurantId, String name, String address) {
        try {
            String projectId = "java2025-91d74";
            String urlString = "https://firestore.googleapis.com/v1/projects/" + projectId +
                    "/databases/(default)/documents/restaurants?documentId=" + restaurantId;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            String json = String.format(
                    "{\"fields\": {\"name\": {\"stringValue\": \"%s\"},\"id\": {\"stringValue\": \"%s\"},\"address\": {\"stringValue\": \"%s\"}}}",
                    name, restaurantId,
                    address);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            System.out.println("新增餐廳 HTTP 狀態碼：" + conn.getResponseCode());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateRestaurantAttribute(String restaurantId, String attributeName, String attributeValue) {
        try {
            String projectId = "java2025-91d74";
            String urlString = "https://firestore.googleapis.com/v1/projects/" + projectId +
                    "/databases/(default)/documents/restaurants/" + restaurantId + "?updateMask.fieldPaths="
                    + attributeName;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            String json = String.format(
                    "{\"fields\": {\"%s\": {\"stringValue\": \"%s\"}}}", attributeName, attributeValue);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            System.out.println("新增或更新屬性 HTTP 狀態碼：" + conn.getResponseCode());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 新增餐廳
        addRestaurant("restaurantId123", "Restaurant Name", "123 Main St");

        // 更新餐廳屬性
        updateRestaurantAttribute("restaurantId123", "tt", "hi");
    }
}
