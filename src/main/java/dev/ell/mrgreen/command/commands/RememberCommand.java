package dev.ell.mrgreen.command.commands;

import dev.ell.mrgreen.command.CommandContext;
import dev.ell.mrgreen.command.PrefixCommand;
import dev.ell.mrgreen.command.SlashCommand;
import dev.ell.mrgreen.service.RememberService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class RememberCommand implements SlashCommand, PrefixCommand {

    private final RememberService rememberService;

    public RememberCommand(RememberService rememberService) {
        this.rememberService = rememberService;
    }

    @Override
    public String getName() {
        return "remember";
    }

    @Override
    public SlashCommandData build() {
        return Commands
                .slash("remember", "Remember a value for a key")
                .addOption(OptionType.STRING, "key", "The key to store under", true)
                .addOption(OptionType.STRING, "value", "The value to remember", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        if (guildId == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        var key = Objects.requireNonNull(event.getOption("key")).getAsString();
        var value = Objects.requireNonNull(event.getOption("value")).getAsString();
        var createdBy = event.getUser().getName();

        rememberService.addEntry(guildId, key, value, createdBy);
        event.reply("Remembered %s: %s".formatted(key, value)).setEphemeral(true).queue();
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx) {
        if (args.size() < 2) return;

        var guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        if (guildId == null) return;

        var key = args.getFirst();
        var value = String.join(" ", args.subList(1, args.size()));
        var createdBy = ctx.source() == CommandContext.Source.IRC ? ctx.ircUsername() : event.getAuthor().getName();

        rememberService.addEntry(guildId, key, value, createdBy);
        event.getChannel().sendMessage("Remembered %s: %s".formatted(key, value)).queue();
    }
}
