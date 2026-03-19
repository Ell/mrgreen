package dev.ell.mrgreen.handler;

import dev.ell.mrgreen.command.CommandContext;
import dev.ell.mrgreen.service.YouTubeService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YouTubeEmbedSuppressor implements MessageHandler {

    private static final Pattern YOUTUBE_MENTION = Pattern.compile("youtube\\.com|youtu\\.be");

    private final YouTubeService youTubeService;

    public YouTubeEmbedSuppressor(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    @Override
    public Pattern getPattern() {
        return YOUTUBE_MENTION;
    }

    @Override
    public void handle(MessageReceivedEvent event, Matcher matcher, CommandContext context) {
        for (var embed : event.getMessage().getEmbeds()) {
            var url = embed.getUrl();
            if (url != null && (url.contains("youtube.com") || url.contains("youtu.be"))) {
                event.getMessage().suppressEmbeds(true).queue();
                youTubeService.fetchVideoInfo(url).ifPresent(info -> {
                    var message = "%s — %s [%s | %s views | %s]".formatted(
                            info.title(), info.channel(), info.duration(), info.views(), info.uploadDate());
                    event.getMessage().reply(message).queue();
                });
                return;
            }
        }
    }
}
