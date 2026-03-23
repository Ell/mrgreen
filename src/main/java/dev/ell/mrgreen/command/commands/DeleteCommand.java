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
public class DeleteCommand implements SlashCommand, PrefixCommand {

    private final RememberService rememberService;

    public DeleteCommand(RememberService rememberService) {
        this.rememberService = rememberService;
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public SlashCommandData build() {
        return Commands
                .slash("delete", "Delete remembered entries")
                .addOption(OptionType.STRING, "key", "The key to delete from", true)
                .addOption(OptionType.INTEGER, "index", "Entry index to delete", false)
                .addOption(OptionType.BOOLEAN, "all", "Delete all entries for key", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        if (guildId == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        var key = Objects.requireNonNull(event.getOption("key")).getAsString();

        var allOpt = event.getOption("all");
        if (allOpt != null && allOpt.getAsBoolean()) {
            var count = rememberService.deleteAllEntries(guildId, key);
            if (count == 0) {
                event.reply("No entries found for %s".formatted(key)).setEphemeral(true).queue();
            } else {
                event.reply("Deleted all %d entries for %s".formatted(count, key)).setEphemeral(true).queue();
            }
            return;
        }

        var indexOpt = event.getOption("index");
        if (indexOpt == null) {
            event.reply("Specify an index or use all:true to delete all entries.").setEphemeral(true).queue();
            return;
        }

        var index = (int) indexOpt.getAsLong();
        var deleted = rememberService.deleteEntry(guildId, key, index);
        if (deleted.isEmpty()) {
            event.reply("%s[%d]: index out of range".formatted(key, index)).setEphemeral(true).queue();
        } else {
            event.reply("Deleted %s[%d]: %s".formatted(key, index, deleted.get().getValue())).setEphemeral(true).queue();
        }
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx) {
        if (args.size() < 2) return;

        var guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        if (guildId == null) return;

        var key = args.getFirst();
        var channel = event.getChannel();

        // %delete foo all
        if (args.get(1).equalsIgnoreCase("all")) {
            var count = rememberService.deleteAllEntries(guildId, key);
            if (count == 0) {
                channel.sendMessage("No entries found for %s".formatted(key)).queue();
            } else {
                channel.sendMessage("Deleted all %d entries for %s".formatted(count, key)).queue();
            }
            return;
        }

        // %delete foo 1
        try {
            var index = Integer.parseInt(args.get(1));
            var deleted = rememberService.deleteEntry(guildId, key, index);
            if (deleted.isEmpty()) {
                channel.sendMessage("%s[%d]: index out of range".formatted(key, index)).queue();
            } else {
                channel.sendMessage("Deleted %s[%d]: %s".formatted(key, index, deleted.get().getValue())).queue();
            }
        } catch (NumberFormatException e) {
            channel.sendMessage("Usage: %delete <key> <index> or %delete <key> all").queue();
        }
    }
}
