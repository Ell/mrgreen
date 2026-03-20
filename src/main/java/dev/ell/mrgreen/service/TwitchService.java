package dev.ell.mrgreen.service;

import dev.ell.mrgreen.clients.helix.HelixClient;
import dev.ell.mrgreen.config.TwitchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(name = "twitch.client-id")
public class TwitchService {
    private final HelixClient helixClient;

    public record TwitchUser(
            String id,
            String loginName,
            String displayName,
            String profileImageUrl,
            Instant createdAt
    ) {
    }

    public TwitchService(TwitchProperties twitchProperties) {
        this.helixClient = new HelixClient(twitchProperties.clientId(), twitchProperties.clientSecret());
    }

    public Optional<TwitchUser> getUserByUsername(String username) {
        var userResponse = helixClient.users(username);

        if (userResponse.data().isEmpty()) {
            return Optional.empty();
        }

        var helixUser = userResponse.data().getFirst();
        var twitchUser = new TwitchUser(
                helixUser.id(),
                helixUser.login(),
                helixUser.displayName(),
                helixUser.profileImageUrl(),
                Instant.parse(helixUser.createdAt())
        );

        return Optional.of(twitchUser);
    }
}
