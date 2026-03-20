package dev.ell.mrgreen.service;

import dev.ell.mrgreen.util.SexprParser;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Slf4j
@Service
public class ClonkProfileService {
    private static final String API_URL = "https://api.colonq.computer/api/user/%s";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObservationRegistry observationRegistry;

    public ClonkProfileService(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public record ClonkProfile(
            String name,
            String color,
            String identity,
            String element,
            String faction,
            long boost
    ) {
    }

    public Optional<ClonkProfile> getProfile(String username) {
        return Observation
                .createNotStarted("clonk.profile.lookup", observationRegistry)
                .observe(() -> doFetch(username));
    }

    private Optional<ClonkProfile> doFetch(String username) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL.formatted(username)))
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var message = SexprParser.parse(response.body());

            var name = message.get(":name").orElseThrow().asStr();
            var color = message.get(":color").orElseThrow().asStr();
            var identity = message.get(":identity").orElseThrow().asStr();
            var element = message.get(":element").orElseThrow().asStr();
            var faction = ((SexprParser.SExpr.Sym) message.get(":faction").orElseThrow()).name();
            var boost = message.get(":boost").orElseThrow().asNum();

            return Optional.of(new ClonkProfile(name, color, identity, element, faction, boost));
        } catch (Exception e) {
            log.error("Failed to fetch clonk profile for: {}", username, e);
            throw new RuntimeException(e);
        }
    }
}
