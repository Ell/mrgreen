package dev.ell.mrgreen.listener;

import dev.ell.mrgreen.config.DiscordProperties;
import dev.ell.mrgreen.handler.MessageHandler;
import dev.ell.mrgreen.util.MessageParser;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class MessageHandlerDispatcher extends ListenerAdapter {

    private final List<MessageHandler> handlers;
    private final Set<String> bridgeBotIds;
    private final MeterRegistry registry;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MessageHandlerDispatcher(List<MessageHandler> handlers,
                                    DiscordProperties properties,
                                    MeterRegistry registry) {
        this.handlers = handlers;
        this.bridgeBotIds = new HashSet<>(properties.bridgeBotIds());
        this.registry = registry;
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        MessageParser.parse(event, bridgeBotIds, "MessageHandlerDispatcher").ifPresent(parsed -> {
            for (var handler : handlers) {
                var matcher = handler.getPattern().matcher(parsed.content());
                if (matcher.find()) {
                    executor.submit(() -> {
                        try {
                            registry.counter("discord.handlers", "name", handler.getClass().getSimpleName()).increment();
                            handler.handle(event, matcher, parsed.context());
                        } catch (Exception e) {
                            log.error("Error in message handler: {}", handler.getClass().getSimpleName(), e);
                        }
                    });
                }
            }
        });
    }
}
