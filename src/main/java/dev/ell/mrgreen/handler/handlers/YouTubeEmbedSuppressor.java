package dev.ell.mrgreen.handler.handlers;

import dev.ell.mrgreen.config.DiscordProperties;
import dev.ell.mrgreen.service.YouTubeService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@ConditionalOnBean(YouTubeService.class)
public class YouTubeEmbedSuppressor extends ListenerAdapter {

    private final YouTubeService youTubeService;
    private final Set<String> bridgeBotIds;
    private final Set<Long> processedMessages = Collections.newSetFromMap(
            Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return size() > 1000;
                }
            })
    );

    public YouTubeEmbedSuppressor(YouTubeService youTubeService, DiscordProperties properties) {
        this.youTubeService = youTubeService;
        this.bridgeBotIds = new HashSet<>(properties.bridgeBotIds());
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        if (event.isWebhookMessage()) return;
        if (event.getAuthor().isBot() && !bridgeBotIds.contains(event.getAuthor().getId())) return;
        processEmbeds(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NonNull MessageUpdateEvent event) {
        if (event.getMessage().isWebhookMessage()) return;
        if (event.getAuthor().isBot() && !bridgeBotIds.contains(event.getAuthor().getId())) return;
        processEmbeds(event.getMessage());
    }

    private void processEmbeds(Message message) {
        for (var embed : message.getEmbeds()) {
            var url = embed.getUrl();
            if (url != null && isYouTubeUrl(url)) {
                if (!processedMessages.add(message.getIdLong())) return;

                log.info("Processing YouTube embed: {}", url);
                message.suppressEmbeds(true).queue();
                youTubeService.fetchVideoInfo(url).ifPresent(info -> {
                    var reply = "%s — %s [%s | %s views | %s]".formatted(
                            info.title(), info.channel(), info.duration(), info.views(), info.uploadDate());
                    message.reply(reply).queue();
                });
                return;
            }
        }
    }

    private boolean isYouTubeUrl(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }
}
