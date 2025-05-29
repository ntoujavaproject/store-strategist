package bigproject;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GooglePlacesService {

    private final String apiKey;
    private final HttpClient httpClient;

    public GooglePlacesService(String apiKey) {
        this.apiKey = apiKey;
        // Configure HttpClient to follow redirects
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // --- Method to Fetch Place Photo using Places API --- 
    public void fetchAndSetPlacePhotoBackground(String query, ImageView targetImageView) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            // System.err.println("API Key is not set correctly.");
            Platform.runLater(() -> targetImageView.setImage(null));
            return;
        }

        findPlaceId(query)
            .thenCompose(placeIdOpt -> placeIdOpt.map(this::getPhotoReference)
                    .orElse(CompletableFuture.completedFuture(Optional.empty())))
            .thenCompose(photoRefOpt -> photoRefOpt.map(this::fetchPlacePhoto)
                    .orElse(CompletableFuture.completedFuture(Optional.empty())))
            .thenAccept(imageOpt -> {
                Platform.runLater(() -> {
                    targetImageView.setImage(imageOpt.orElse(null));
                    if (imageOpt.isPresent()) {
                        System.out.println("Place photo background updated for: " + query);
                    } else {
                        System.out.println("Could not find suitable photo for: " + query + ". Background cleared.");
                    }
                });
            })
            .exceptionally(e -> {
                // System.err.println("Error during place photo fetch process: " + e.getMessage());
                Throwable cause = e instanceof java.util.concurrent.CompletionException ? e.getCause() : e;
                // if (cause != null) { cause.printStackTrace(); } else { e.printStackTrace(); }
                Platform.runLater(() -> targetImageView.setImage(null));
                return null;
            });
    }

    // Step 1: Find Place ID
    private CompletableFuture<Optional<String>> findPlaceId(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String findPlaceUrl = String.format(
                "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?input=%s&inputtype=textquery&fields=place_id&key=%s",
                encodedQuery, apiKey
            );
            System.out.println("Finding Place ID: " + findPlaceUrl);
            HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(findPlaceUrl)).build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response.body());
                            if ("OK".equals(jsonResponse.optString("status")) && jsonResponse.has("candidates")) {
                                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                                if (candidates.length() > 0) {
                                    return Optional.ofNullable(candidates.getJSONObject(0).optString("place_id"));
                                }
                            }
                            // System.err.println("Find Place API did not return OK or candidates for query: " + query + " Status: " + jsonResponse.optString("status"));
                        } catch (JSONException jsonEx) {
                            // System.err.println("Error parsing Find Place JSON response: " + jsonEx.getMessage());
                        }
                    } else {
                        // System.err.printf("Find Place API request failed: Status %d, Body: %s\n", response.statusCode(), response.body());
                    }
                    return Optional.<String>empty();
                });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new RuntimeException("Error creating Find Place request: " + e.getMessage(), e));
        }
    }

    // Step 2: Get Photo Reference from Place Details
    private CompletableFuture<Optional<String>> getPhotoReference(String placeId) {
        if (placeId == null || placeId.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        try {
            String detailsUrl = String.format(
                "https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&fields=photo&key=%s",
                placeId, apiKey
            );
             System.out.println("Getting Place Details: " + detailsUrl);
             HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(detailsUrl)).build();

             return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                             JSONObject jsonResponse = new JSONObject(response.body());
                            if ("OK".equals(jsonResponse.optString("status")) && jsonResponse.has("result")) {
                                JSONObject result = jsonResponse.getJSONObject("result");
                                if (result.has("photos")) {
                                    JSONArray photos = result.getJSONArray("photos");
                                    if (photos.length() > 0) {
                                        return Optional.ofNullable(photos.getJSONObject(0).optString("photo_reference"));
                                    }
                                }
                             }
                            // System.err.println("Place Details API did not return OK or photos for placeId: " + placeId + " Status: " + jsonResponse.optString("status"));
                        } catch (JSONException jsonEx) {
                             // System.err.println("Error parsing Place Details JSON response: " + jsonEx.getMessage());
                        }
                    } else {
                         // System.err.printf("Place Details API request failed: Status %d, Body: %s\n", response.statusCode(), response.body());
                    }
                    return Optional.<String>empty();
                });
        } catch (Exception e) {
             return CompletableFuture.failedFuture(new RuntimeException("Error creating Place Details request: " + e.getMessage(), e));
        }
    }

    // Step 3: Fetch Place Photo Image
    private CompletableFuture<Optional<Image>> fetchPlacePhoto(String photoReference) {
        if (photoReference == null || photoReference.isEmpty()) {
             return CompletableFuture.completedFuture(Optional.empty());
        }
         try {
             int maxWidth = 400;
             String photoUrl = String.format(
                 "https://maps.googleapis.com/maps/api/place/photo?photoreference=%s&maxwidth=%d&key=%s",
                 photoReference, maxWidth, apiKey
             );
             System.out.println("Fetching Place Photo: " + photoUrl);
             HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(photoUrl)).build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try (InputStream imageStream = response.body()) {
                             Image placeImage = new Image(imageStream); 
                            if (!placeImage.isError()) {
                                return Optional.of(placeImage);
                            } else {
                                // System.err.println("Failed to decode place photo from stream.");
                            }
                        } catch (Exception streamEx) {
                             // System.err.println("Error processing place photo InputStream: " + streamEx.getMessage());
                        }
                    } else {
                        // System.err.printf("Place Photo API request failed: Status %d\n", response.statusCode());
                    }
                    return Optional.<Image>empty();
                });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new RuntimeException("Error creating Place Photo request: " + e.getMessage(), e));
        }
    }
} 