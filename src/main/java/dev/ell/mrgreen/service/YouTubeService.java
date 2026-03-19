package dev.ell.mrgreen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ell.mrgreen.config.GoogleProperties;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class YouTubeService {

    private static final Pattern VIDEO_ID_PATTERN =
            Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})");

    private static final String API_URL =
            "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails,statistics&id=%s&key=%s";

    private final String apiKey;
    private final ObservationRegistry observationRegistry;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YouTubeService(GoogleProperties properties, ObservationRegistry observationRegistry) {
        this.apiKey = properties.apiKey();
        this.observationRegistry = observationRegistry;
    }

    public record VideoInfo(String title, String channel, String duration, String views, String uploadDate) {}

    public Optional<VideoInfo> fetchVideoInfo(String url) {
        var videoId = extractVideoId(url);
        if (videoId.isEmpty()) return Optional.empty();

        return Observation.createNotStarted("discord.youtube.lookup", observationRegistry)
                .observe(() -> doFetch(videoId.get(), url));
    }

    private Optional<VideoInfo> doFetch(String videoId, String url) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL.formatted(videoId, apiKey)))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var root = objectMapper.readTree(response.body());
            var items = root.get("items");

            if (items == null || items.isEmpty()) return Optional.empty();

            var item = items.get(0);
            var snippet = item.get("snippet");
            var contentDetails = item.get("contentDetails");
            var statistics = item.get("statistics");

            return Optional.of(new VideoInfo(
                    snippet.get("title").asText(),
                    snippet.get("channelTitle").asText(),
                    formatDuration(contentDetails.get("duration").asText()),
                    formatViews(statistics.get("viewCount").asText()),
                    formatDate(snippet.get("publishedAt").asText())
            ));
        } catch (Exception e) {
            log.error("Failed to fetch YouTube video info for: {}", url, e);
            throw new RuntimeException(e);
        }
    }

    private Optional<String> extractVideoId(String url) {
        var matcher = VIDEO_ID_PATTERN.matcher(url);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private String formatDuration(String isoDuration) {
        var duration = Duration.parse(isoDuration);
        var hours = duration.toHours();
        var minutes = duration.toMinutesPart();
        var seconds = duration.toSecondsPart();

        if (hours > 0) {
            return "%d:%02d:%02d".formatted(hours, minutes, seconds);
        }
        return "%d:%02d".formatted(minutes, seconds);
    }

    private String formatViews(String viewCount) {
        return NumberFormat.getNumberInstance(Locale.US).format(Long.parseLong(viewCount));
    }

    private String formatDate(String isoDate) {
        var date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME);
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }
}
