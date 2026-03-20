package dev.ell.mrgreen.clients.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ell.mrgreen.clients.helix.dtos.UsersResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record HelixClient(String clientId, String clientSecret) {

    private static final String TOKEN_URL = "https://id.twitch.tv/oauth2/token";
    private static final String HELIX_BASE_URL = "https://api.twitch.tv/helix/";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String cachedToken;
    private static Instant tokenExpiry = Instant.EPOCH;

    public UsersResponse users(String username) {
        return users(List.of(username));
    }

    public UsersResponse users(List<String> usernames) {
        var query = usernames.stream()
                .map(u -> "login=" + u)
                .collect(Collectors.joining("&"));
        var request = HttpRequest.newBuilder()
                .uri(URI.create(HELIX_BASE_URL + "/users" + "?" + query))
                .header("Authorization", "Bearer " + getToken())
                .header("Client-Id", clientId)
                .GET()
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), UsersResponse.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch users", e);
        }
    }

    private synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        var body = "client_id=%s&client_secret=%s&grant_type=client_credentials"
                .formatted(clientId, clientSecret);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var node = objectMapper.readTree(response.body());
            cachedToken = node.get("access_token").asText();
            tokenExpiry = Instant.now().plusSeconds(node.get("expires_in").asLong() - 60);
            return cachedToken;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to obtain Twitch OAuth token", e);
        }
    }
}
