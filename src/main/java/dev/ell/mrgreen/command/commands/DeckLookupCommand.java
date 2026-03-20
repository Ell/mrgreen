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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class DeckLookupCommand implements SlashCommand, PrefixCommand {
    private static final String BASE_URL = "https://api.colonq.computer/api/tcg/binder/";

    private final TwitchService twitchService;

    public DeckLookupCommand(TwitchService twitchService) {
        this.twitchService = twitchService;
    }

    @Override
    public String getName() {
        return "deck";
    }

    @Override
    public SlashCommandData build() {
        return Commands
                .slash("deck", "Lookup a deck given a twitch username")
                .addOption(OptionType.STRING, "username", "Twitch username to lookup", true);
    }

    private String getDeckUrl(String twitchId) {
        return BASE_URL + twitchId;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var username = Objects.requireNonNull(event.getOption("username")).getAsString();
        var twitchUser = twitchService.getUserByUsername(username);

        if (twitchUser.isEmpty()) {
            event.reply("User not found").queue();
            return;
        }

        event.reply("Deck URL for %s: %s".formatted(username, getDeckUrl(twitchUser.get().id()))).queue();
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

        event.getChannel().sendMessage("Deck URL for %s: %s".formatted(username, getDeckUrl(twitchUser.get().id()))).queue();
    }
}
