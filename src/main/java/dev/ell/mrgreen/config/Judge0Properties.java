package dev.ell.mrgreen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("judge0")
public record Judge0Properties(String apiKey, @DefaultValue("judge0-ce.p.rapidapi.com") String apiHost) {
}
