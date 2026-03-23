package dev.ell.mrgreen.command.commands;

import dev.ell.mrgreen.command.CommandContext;
import dev.ell.mrgreen.command.PrefixCommand;
import dev.ell.mrgreen.command.SlashCommand;
import dev.ell.mrgreen.entity.RememberedEntry;
import dev.ell.mrgreen.service.RememberService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class LookupCommand implements SlashCommand, PrefixCommand {

    private static final int PAGE_SIZE = 5;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d yyyy").withZone(ZoneOffset.UTC);

    private final RememberService rememberService;

    public LookupCommand(RememberService rememberService) {
        this.rememberService = rememberService;
    }

    @Override
    public String getName() {
        return "lookup";
    }

    @Override
    public SlashCommandData build() {
        return Commands
                .slash("lookup", "Look up remembered entries for a key")
                .addOption(OptionType.STRING, "key", "The key to look up", true)
                .addOption(OptionType.INTEGER, "page", "Page number", false)
                .addOption(OptionType.INTEGER, "index", "Entry index for detail view", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        if (guildId == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        var key = Objects.requireNonNull(event.getOption("key")).getAsString();
        var entries = rememberService.getEntries(guildId, key);

        if (entries.isEmpty()) {
            event.reply("No entries found for %s".formatted(key)).setEphemeral(true).queue();
            return;
        }

        var indexOpt = event.getOption("index");
        if (indexOpt != null) {
            event.reply(formatDetail(key, entries, (int) indexOpt.getAsLong())).setEphemeral(true).queue();
            return;
        }

        var pageOpt = event.getOption("page");
        var page = pageOpt != null ? (int) pageOpt.getAsLong() : 1;
        event.reply(formatPage(key, entries, page)).setEphemeral(true).queue();
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx) {
        if (args.isEmpty()) return;

        var guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        if (guildId == null) return;

        var key = args.getFirst();
        var entries = rememberService.getEntries(guildId, key);

        if (entries.isEmpty()) {
            event.getChannel().sendMessage("No entries found for %s".formatted(key)).queue();
            return;
        }

        // ?foo 2 — detail view (second arg is a number)
        if (args.size() == 2) {
            try {
                var index = Integer.parseInt(args.get(1));
                event.getChannel().sendMessage(noEmbeds(formatDetail(key, entries, index))).queue();
                return;
            } catch (NumberFormatException ignored) {}
        }

        // ?foo page 2
        var page = 1;
        if (args.size() >= 3 && args.get(1).equalsIgnoreCase("page")) {
            try {
                page = Integer.parseInt(args.get(2));
            } catch (NumberFormatException ignored) {}
        }

        event.getChannel().sendMessage(noEmbeds(formatPage(key, entries, page))).queue();
    }

    private static MessageCreateData noEmbeds(String content) {
        return new MessageCreateBuilder().setContent(content).setSuppressEmbeds(true).build();
    }

    private String formatPage(String key, List<RememberedEntry> entries, int page) {
        var totalPages = (int) Math.ceil((double) entries.size() / PAGE_SIZE);
        page = Math.max(1, Math.min(page, totalPages));

        var start = (page - 1) * PAGE_SIZE;
        var end = Math.min(start + PAGE_SIZE, entries.size());

        var entryCount = entries.size() == 1 ? "1 entry" : entries.size() + " entries";
        var items = IntStream.range(start, end)
                .mapToObj(i -> "%d. %s".formatted(i + 1, entries.get(i).getValue()))
                .collect(Collectors.joining(" | "));

        return "%s (%s, page %d/%d): %s".formatted(key, entryCount, page, totalPages, items);
    }

    private String formatDetail(String key, List<RememberedEntry> entries, int oneBasedIndex) {
        var idx = oneBasedIndex - 1;
        if (idx < 0 || idx >= entries.size()) {
            return "%s[%d]: index out of range (1-%d)".formatted(key, oneBasedIndex, entries.size());
        }
        var entry = entries.get(idx);
        var date = DATE_FMT.format(entry.getCreatedAt());
        return "%s[%d]: %s (added by %s, %s)".formatted(key, oneBasedIndex, entry.getValue(), entry.getCreatedBy(), date);
    }
}
