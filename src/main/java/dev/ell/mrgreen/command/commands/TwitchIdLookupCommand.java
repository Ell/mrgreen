package dev.ell.mrgreen.command.commands;

import dev.ell.mrgreen.command.CommandContext;
import dev.ell.mrgreen.command.PrefixCommand;
import dev.ell.mrgreen.command.SlashCommand;
import dev.ell.mrgreen.service.TwitchService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnBean(TwitchService.class)
public class TwitchIdLookupCommand implements SlashCommand, PrefixCommand {
    private final TwitchService twitchService;

    public TwitchIdLookupCommand(TwitchService twitchService) {
        this.twitchService = twitchService;
    }

    @Override
    public String getName() {
        return "twitchid";
    }

    @Override
    public SlashCommandData build() {
        return Commands
                .slash("twitchid", "Lookup a Twitch user's ID")
                .addOption(OptionType.STRING, "username", "The Twitch username to lookup", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var fetchedUser = twitchService
                .getUserByUsername(Objects.requireNonNull(event.getOption("username")).getAsString());

        if (fetchedUser.isEmpty()) {
            event.reply("User not found").setEphemeral(true).queue();
        } else {
            var twitchUser = fetchedUser.get();
            event.reply("Twitch ID for user %s: %s".formatted(twitchUser.displayName(), twitchUser.id())).setEphemeral(true).queue();
        }
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx) {
        if (args.isEmpty()) {
            return;
        }

        var username = args.getFirst();
        var fetchedUser = twitchService.getUserByUsername(username);

        if (fetchedUser.isEmpty()) {
            event.getChannel().sendMessage("User %s not found".formatted(username)).queue();
        } else {
            var twitchUser = fetchedUser.get();
            event.getChannel()
                    .sendMessage("Twitch ID for user %s: %s".formatted(twitchUser.displayName(), twitchUser.id()))
                    .queue();
        }

    }
}
