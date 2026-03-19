package dev.ell.mrgreen.config;

import dev.ell.mrgreen.command.SlashCommand;
import net.dv8tion.jda.api.JDA;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommandRegistrar {

    private final JDA jda;
    private final List<SlashCommand> commands;

    public CommandRegistrar(JDA jda, List<SlashCommand> commands) {
        this.jda = jda;
        this.commands = commands;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerCommands() {
        var data = commands.stream()
                .map(SlashCommand::build)
                .toList();

        jda.getGuilds().forEach(guild -> {
            guild.updateCommands().addCommands(data).queue();
        });
    }
}
