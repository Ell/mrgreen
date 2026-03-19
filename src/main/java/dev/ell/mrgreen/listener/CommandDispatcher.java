package dev.ell.mrgreen.listener;

import dev.ell.mrgreen.command.PrefixCommand;
import dev.ell.mrgreen.command.SlashCommand;
import dev.ell.mrgreen.config.DiscordProperties;
import dev.ell.mrgreen.util.MessageParser;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class CommandDispatcher extends ListenerAdapter {

    private final Map<String, SlashCommand> slashCommands;
    private final Map<String, PrefixCommand> prefixCommands;
    private final String prefix;
    private final Set<String> bridgeBotIds;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public CommandDispatcher(List<SlashCommand> slashList,
                             List<PrefixCommand> prefixList,
                             DiscordProperties properties) {
        this.prefix = properties.prefix();
        this.bridgeBotIds = new HashSet<>(properties.bridgeBotIds());

        this.slashCommands = new HashMap<>();
        for (var cmd : slashList) {
            slashCommands.put(cmd.getName(), cmd);
        }

        this.prefixCommands = new HashMap<>();
        for (var cmd : prefixList) {
            prefixCommands.put(cmd.getName(), cmd);
            for (var alias : cmd.getAliases()) {
                prefixCommands.put(alias, cmd);
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        var command = slashCommands.get(event.getName());
        if (command == null) {
            event.reply("Unknown command.").setEphemeral(true).queue();
            return;
        }

        executor.submit(() -> {
            try {
                command.execute(event);
            } catch (Exception e) {
                log.error("Error executing slash command: {}", event.getName(), e);
                event.reply("Something went wrong.").setEphemeral(true).queue();
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        MessageParser.parse(event, bridgeBotIds, "CommandDispatcher").ifPresent(parsed -> {
            var content = parsed.content();
            if (!content.startsWith(prefix)) return;

            var parts = content.substring(prefix.length()).trim().split("\\s+");
            var name = parts[0].toLowerCase();
            var args = parts.length > 1 ? Arrays.asList(parts).subList(1, parts.length) : List.<String>of();

            var command = prefixCommands.get(name);
            if (command == null) return;

            executor.submit(() -> {
                try {
                    command.execute(event, args, parsed.context());
                } catch (Exception e) {
                    log.error("Error executing prefix command: {}", name, e);
                    event.getChannel().sendMessage("Something went wrong.").queue();
                }
            });
        });
    }
}
