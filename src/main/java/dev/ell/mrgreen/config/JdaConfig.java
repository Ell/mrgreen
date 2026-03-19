package dev.ell.mrgreen.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class JdaConfig {

    private final DiscordProperties properties;
    private final List<ListenerAdapter> listeners;

    public JdaConfig(DiscordProperties properties, List<ListenerAdapter> listeners) {
        this.properties = properties;
        this.listeners = listeners;
    }

    @Bean
    public JDA jda() throws InterruptedException {
        return JDABuilder.createDefault(properties.token())
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .addEventListeners(listeners.toArray())
                .build()
                .awaitReady();
    }
}
