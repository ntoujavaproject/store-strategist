import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FirebaseGetComments {

    public static void getComments() {
        try {
            String projectId = "java2025-91d74";
            String id = "0x3442ab5850b1fd47:0xc2116080b5a70d16";
            String urlString = "https://firestore.googleapis.com/v1/projects/" + projectId +
                    "/databases/(default)/documents/restaurants/" + id + "/reviews";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));

            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            in.close();

            System.out.println("取得評論資料：");
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        getComments();
    }
}
