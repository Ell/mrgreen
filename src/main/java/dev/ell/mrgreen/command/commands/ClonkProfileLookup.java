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
public class ClonkProfileLookup implements SlashCommand, PrefixCommand {
    private static final String BASE_URL = "https://api.colonq.computer/charsheet#";

    private final TwitchService twitchService;

    public ClonkProfileLookup(TwitchService twitchService) {
        this.twitchService = twitchService;
    }

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public SlashCommandData build() {
        return Commands
                .slash("profile", "Lookup a Clonk profile")
                .addOption(OptionType.STRING, "username", "Twitch username", true);
    }

    private String getProfileUrl(String twitchId) {
        return BASE_URL + twitchId;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var username = Objects.requireNonNull(event.getOption("username")).getAsString();
        var twitchUser = twitchService.getUserByUsername(username);

        if (twitchUser.isEmpty()) {
            event.reply("User not found").setEphemeral(true).queue();

            return;
        }

        event.reply("Clonk profile URL for %s: %s".formatted(username, getProfileUrl(twitchUser.get().id()))).setEphemeral(true).queue();
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx) {
        if (args.isEmpty()) {
            return;
        }

        var username = args.getFirst();
        var twitchUser = twitchService.getUserByUsername(username);

        if (twitchUser.isEmpty()) {
            event.getChannel().sendMessage("User not found").queue();
            return;
        }

        event.getChannel().sendMessage("Clonk profile URL for %s: %s".formatted(username, getProfileUrl(twitchUser.get().id()))).queue();
    }
}
