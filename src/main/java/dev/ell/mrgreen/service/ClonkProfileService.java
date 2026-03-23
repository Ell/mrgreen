package dev.ell.mrgreen.service;

import dev.ell.mrgreen.util.SexprParser;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Service
public class ClonkProfileService {
    private static final String API_URL = "https://api.colonq.computer/api/user/%s";

    private final RestClient restClient;
    private final ObservationRegistry observationRegistry;

    public ClonkProfileService(ObservationRegistry observationRegistry, RestClient.Builder restClientBuilder) {
        this.observationRegistry = observationRegistry;
        this.restClient = restClientBuilder.clone().build();
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
            var response = restClient.get()
                    .uri(API_URL.formatted(username))
                    .retrieve()
                    .body(String.class);
            var message = SexprParser.parse(response);

            var name = message.get(":name").orElseThrow().asStr();
            var color = message.get(":color").orElseThrow().asStr();
            var identity = message.get(":identity").orElseThrow().asStr();
            var element = message.get(":element").orElseThrow().asStr();
            var faction = message.get(":faction").orElseThrow().asSym();
            var boost = message.get(":boost").orElseThrow().asNum();

            return Optional.of(new ClonkProfile(name, color, identity, element, faction, boost));
        } catch (Exception e) {
            log.error("Failed to fetch clonk profile for: {}", username, e);
            return Optional.empty();
        }
    }
}
