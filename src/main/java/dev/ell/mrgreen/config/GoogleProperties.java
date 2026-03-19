package dev.ell.mrgreen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("google")
public record GoogleProperties(String apiKey) {
}
