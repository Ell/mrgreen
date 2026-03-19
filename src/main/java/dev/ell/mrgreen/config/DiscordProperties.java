package dev.ell.mrgreen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties("discord")
public record DiscordProperties(
        String token,
        String clientId,
        String prefix,
        @DefaultValue List<String> bridgeBotIds
) {
}