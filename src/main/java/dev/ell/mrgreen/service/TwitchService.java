package dev.ell.mrgreen.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ell.mrgreen.config.TwitchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "twitch.client-id")
public class TwitchService {

    private static final String TOKEN_URL = "https://id.twitch.tv/oauth2/token";
    private static final String HELIX_BASE_URL = "https://api.twitch.tv/helix";

    private final RestClient helixClient;
    private final RestClient tokenClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public record TwitchUser(
            String id,
            String loginName,
            String displayName,
            String profileImageUrl,
            Instant createdAt
    ) {
    }

    public TwitchService(TwitchProperties twitchProperties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.clientId = twitchProperties.clientId();
        this.clientSecret = twitchProperties.clientSecret();
        this.objectMapper = objectMapper;
        this.helixClient = restClientBuilder.clone().baseUrl(HELIX_BASE_URL).build();
        this.tokenClient = restClientBuilder.clone().build();
    }

    public Optional<TwitchUser> getUserByUsername(String username) {
        var userResponse = fetchUsers(List.of(username));

        if (userResponse.data() == null || userResponse.data().isEmpty()) {
            return Optional.empty();
        }

        var helixUser = userResponse.data().getFirst();
        return Optional.of(new TwitchUser(
                helixUser.id(),
                helixUser.login(),
                helixUser.displayName(),
                helixUser.profileImageUrl(),
                Instant.parse(helixUser.createdAt())
        ));
    }

    private UsersResponse fetchUsers(List<String> usernames) {
        var query = usernames.stream()
                .map(u -> "login=" + URLEncoder.encode(u, StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        try {
            var body = sendHelixRequest("/users?" + query);
            return objectMapper.readValue(body, UsersResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users", e);
        }
    }

    private String sendHelixRequest(String path) {
        try {
            return executeHelixRequest(path);
        } catch (HttpClientErrorException.Unauthorized e) {
            // Token expired — invalidate and retry once
            invalidateToken();
            return executeHelixRequest(path);
        }
    }

    private String executeHelixRequest(String path) {
        return helixClient.get()
                .uri(path)
                .header("Authorization", "Bearer " + getToken())
                .header("Client-Id", clientId)
                .retrieve()
                .body(String.class);
    }

    private synchronized void invalidateToken() {
        cachedToken = null;
        tokenExpiry = Instant.EPOCH;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UsersResponse(List<User> data) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record User(
                String id,
                String login,
                @JsonProperty("display_name") String displayName,
                String type,
                @JsonProperty("broadcaster_type") String broadcasterType,
                String description,
                @JsonProperty("profile_image_url") String profileImageUrl,
                @JsonProperty("offline_image_url") String offlineImageUrl,
                @JsonProperty("created_at") String createdAt
        ) {}
    }

    private synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        try {
            var body = "client_id=%s&client_secret=%s&grant_type=client_credentials"
                    .formatted(clientId, clientSecret);
            var response = tokenClient.post()
                    .uri(TOKEN_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            var node = objectMapper.readTree(response);
            cachedToken = node.get("access_token").asText();
            tokenExpiry = Instant.now().plusSeconds(node.get("expires_in").asLong() - 60);
            return cachedToken;
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain Twitch OAuth token", e);
        }
    }
}
