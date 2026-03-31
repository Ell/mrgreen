package dev.ell.mrgreen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GistService {

    private static final Pattern GIST_URL_PATTERN =
            Pattern.compile("https://gist\\.github\\.com/(?:[^/]+/)?([a-f0-9]+)");

    private static final Pattern RAW_GIST_URL_PATTERN =
            Pattern.compile("https://gist\\.githubusercontent\\.com/.+");

    public record GistContent(String filename, String content, String extension) {}

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GistService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.clone().build();
        this.objectMapper = objectMapper;
    }

    public static boolean isGistUrl(String input) {
        return GIST_URL_PATTERN.matcher(input).matches() || RAW_GIST_URL_PATTERN.matcher(input).matches();
    }

    public Optional<GistContent> fetchGist(String url) {
        try {
            var rawMatcher = RAW_GIST_URL_PATTERN.matcher(url);
            if (rawMatcher.matches()) {
                return fetchRawGist(url);
            }

            var matcher = GIST_URL_PATTERN.matcher(url);
            if (!matcher.matches()) {
                return Optional.empty();
            }

            var gistId = matcher.group(1);
            var response = restClient.get()
                    .uri("https://api.github.com/gists/" + gistId)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .body(String.class);

            var root = objectMapper.readTree(response);
            var files = root.get("files");
            if (files == null || files.isEmpty()) {
                return Optional.empty();
            }

            // Take the first file
            var firstFile = files.fields().next();
            var fileNode = firstFile.getValue();
            var filename = fileNode.get("filename").asText();
            var content = fileNode.get("content").asText();
            var extension = extractExtension(filename);

            return Optional.of(new GistContent(filename, content, extension));
        } catch (Exception e) {
            log.error("Failed to fetch gist: {}", url, e);
            return Optional.empty();
        }
    }

    private Optional<GistContent> fetchRawGist(String url) {
        try {
            var content = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            // Extract filename from URL path
            var parts = url.split("/");
            var filename = parts[parts.length - 1];
            var extension = extractExtension(filename);

            return Optional.of(new GistContent(filename, content, extension));
        } catch (Exception e) {
            log.error("Failed to fetch raw gist: {}", url, e);
            return Optional.empty();
        }
    }

    private String extractExtension(String filename) {
        var dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}
