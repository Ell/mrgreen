package dev.ell.mrgreen.util;

import dev.ell.mrgreen.command.CommandContext;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class MessageParser {

    private static final Pattern IRC_MESSAGE = Pattern.compile("^`?<([^>]+)>`?\\s*(.+)");

    public record ParsedMessage(String content, CommandContext context) {}

    public static Optional<ParsedMessage> parse(MessageReceivedEvent event, Set<String> bridgeBotIds, String caller) {
        var authorId = event.getAuthor().getId();
        var isBot = event.getAuthor().isBot();
        var isWebhook = event.isWebhookMessage();
        var content = event.getMessage().getContentRaw();

        log.debug("[{}] authorId={}, isBot={}, isWebhook={}, bridgeBotIds={}, content='{}'",
                caller, authorId, isBot, isWebhook, bridgeBotIds, content);

        if (isWebhook) {
            log.debug("[{}] skipped — webhook message", caller);
            return Optional.empty();
        }

        if (bridgeBotIds.contains(authorId)) {
            var matcher = IRC_MESSAGE.matcher(content);
            if (!matcher.matches()) {
                log.debug("[{}] bridge bot message did not match IRC pattern", caller);
                return Optional.empty();
            }
            log.debug("[{}] parsed IRC message — user='{}', content='{}'",
                    caller, matcher.group(1), matcher.group(2));
            return Optional.of(new ParsedMessage(
                    matcher.group(2),
                    CommandContext.irc(matcher.group(1))
            ));
        }

        if (isBot) {
            log.debug("[{}] skipped — bot message (not in bridgeBotIds)", caller);
            return Optional.empty();
        }

        return Optional.of(new ParsedMessage(content, CommandContext.discord()));
    }
}
