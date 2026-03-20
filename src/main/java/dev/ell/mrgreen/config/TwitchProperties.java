package dev.ell.mrgreen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("twitch")
public record TwitchProperties(String clientId, String clientSecret) {
}
