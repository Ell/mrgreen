package dev.ell.mrgreen;

import dev.ell.mrgreen.config.DiscordProperties;
import dev.ell.mrgreen.config.GoogleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DiscordProperties.class, GoogleProperties.class})
public class MrgreenApplication {
    public static void main(String[] args) {
        SpringApplication.run(MrgreenApplication.class, args);
    }
}
