package dev.ell.mrgreen.command.commands;

import dev.ell.mrgreen.command.CommandContext;
import dev.ell.mrgreen.command.PrefixCommand;
import dev.ell.mrgreen.command.SlashCommand;
import dev.ell.mrgreen.service.ClonkProfileService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class BoostsCommand implements SlashCommand, PrefixCommand {
    private final ClonkProfileService clonkProfileService;

    public BoostsCommand(ClonkProfileService clonkProfileService) {
        this.clonkProfileService = clonkProfileService;
    }

    @Override
    public String getName() {
        return "boosts";
    }

    @Override
    public SlashCommandData build() {
        return Commands
                .slash("boosts", "Show current boost amount for user")
                .addOption(OptionType.STRING, "username", "Twitch username", true);
    }

    private String getResponse(String username) {
        var profile = clonkProfileService.getProfile(username);

        return profile
                .map(clonkProfile -> "%s has %s boosts".formatted(username, clonkProfile.boost()))
                .orElseGet(() -> "No boosts found for username: " + username);

    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var username = Objects.requireNonNull(event.getOption("username")).getAsString();
        var response = getResponse(username);

        event.reply(response).setEphemeral(true).queue();
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx) {
        if (args.isEmpty()) {
            return;
        }

        var username = args.getFirst();
        var response = getResponse(username);

        event.getChannel().sendMessage(response).queue();
    }
}
