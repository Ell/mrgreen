package dev.ell.mrgreen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ell.mrgreen.config.Judge0Properties;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(name = "judge0.api-key")
public class Judge0Service {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("py", "python"),
            Map.entry("js", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("rb", "ruby"),
            Map.entry("rs", "rust"),
            Map.entry("cpp", "c++"),
            Map.entry("cs", "c#"),
            Map.entry("csharp", "c#"),
            Map.entry("golang", "go"),
            Map.entry("sh", "bash"),
            Map.entry("shell", "bash"),
            Map.entry("pl", "perl"),
            Map.entry("hs", "haskell"),
            Map.entry("kt", "kotlin"),
            Map.entry("swift", "swift"),
            Map.entry("scala", "scala"),
            Map.entry("lua", "lua"),
            Map.entry("r", "r")
    );

    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
            Map.entry("py", "python"),
            Map.entry("js", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("rb", "ruby"),
            Map.entry("rs", "rust"),
            Map.entry("go", "go"),
            Map.entry("java", "java"),
            Map.entry("c", "c"),
            Map.entry("cpp", "c++"),
            Map.entry("cc", "c++"),
            Map.entry("cxx", "c++"),
            Map.entry("h", "c"),
            Map.entry("cs", "c#"),
            Map.entry("php", "php"),
            Map.entry("pl", "perl"),
            Map.entry("sh", "bash"),
            Map.entry("bash", "bash"),
            Map.entry("hs", "haskell"),
            Map.entry("kt", "kotlin"),
            Map.entry("scala", "scala"),
            Map.entry("swift", "swift"),
            Map.entry("lua", "lua"),
            Map.entry("r", "r"),
            Map.entry("sql", "sql"),
            Map.entry("dart", "dart")
    );

    public record Language(int id, String name) {}

    public record SubmissionResult(String stdout, String stderr, String compileOutput,
                                   String time, int memory, int statusId, String statusDescription) {}

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ObservationRegistry observationRegistry;

    private volatile List<Language> cachedLanguages;

    public Judge0Service(Judge0Properties properties, ObservationRegistry observationRegistry,
                         RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.observationRegistry = observationRegistry;
        this.objectMapper = objectMapper;

        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .defaultHeader("X-RapidAPI-Key", properties.apiKey())
                .defaultHeader("X-RapidAPI-Host", properties.apiHost())
                .baseUrl("https://" + properties.apiHost())
                .build();
    }

    public List<Language> getLanguages() {
        if (cachedLanguages != null) {
            return cachedLanguages;
        }
        synchronized (this) {
            if (cachedLanguages != null) {
                return cachedLanguages;
            }
            try {
                var response = restClient.get()
                        .uri("/languages")
                        .retrieve()
                        .body(String.class);
                var root = objectMapper.readTree(response);
                cachedLanguages = new java.util.ArrayList<>();
                for (var node : root) {
                    cachedLanguages.add(new Language(node.get("id").asInt(), node.get("name").asText()));
                }
                cachedLanguages = List.copyOf(cachedLanguages);
                log.info("Loaded {} languages from Judge0", cachedLanguages.size());
                return cachedLanguages;
            } catch (Exception e) {
                log.error("Failed to fetch languages from Judge0", e);
                return List.of();
            }
        }
    }

    public Map<String, String> getAliases() {
        return ALIASES;
    }

    public Optional<Language> findLanguage(String input) {
        var resolved = ALIASES.getOrDefault(input.toLowerCase(), input);
        var languages = getLanguages();

        // Exact match on name
        for (var lang : languages) {
            if (lang.name().equalsIgnoreCase(resolved)) {
                return Optional.of(lang);
            }
        }

        // Starts with match
        var lower = resolved.toLowerCase();
        for (var lang : languages) {
            if (lang.name().toLowerCase().startsWith(lower)) {
                return Optional.of(lang);
            }
        }

        // Contains match
        for (var lang : languages) {
            if (lang.name().toLowerCase().contains(lower)) {
                return Optional.of(lang);
            }
        }

        return Optional.empty();
    }

    public Optional<Language> findLanguageByExtension(String extension) {
        var langName = EXTENSION_TO_LANGUAGE.get(extension.toLowerCase());
        if (langName == null) {
            return Optional.empty();
        }
        return findLanguage(langName);
    }

    public SubmissionResult submit(int languageId, String sourceCode) {
        return Observation.createNotStarted("judge0.submission", observationRegistry)
                .observe(() -> doSubmit(languageId, sourceCode));
    }

    private SubmissionResult doSubmit(int languageId, String sourceCode) {
        try {
            var body = objectMapper.writeValueAsString(Map.of(
                    "source_code", sourceCode,
                    "language_id", languageId
            ));

            var response = restClient.post()
                    .uri("/submissions?base64_encoded=false&wait=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            var root = objectMapper.readTree(response);
            var status = root.get("status");

            return new SubmissionResult(
                    textOrNull(root, "stdout"),
                    textOrNull(root, "stderr"),
                    textOrNull(root, "compile_output"),
                    textOrNull(root, "time"),
                    root.has("memory") && !root.get("memory").isNull() ? root.get("memory").asInt() : 0,
                    status.get("id").asInt(),
                    status.get("description").asText()
            );
        } catch (Exception e) {
            log.error("Failed to submit code to Judge0", e);
            throw new RuntimeException("Failed to execute code: " + e.getMessage(), e);
        }
    }

    private String textOrNull(com.fasterxml.jackson.databind.JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }
}
