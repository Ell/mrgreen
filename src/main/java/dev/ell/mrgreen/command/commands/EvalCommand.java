package dev.ell.mrgreen.command.commands;

import dev.ell.mrgreen.command.CommandContext;
import dev.ell.mrgreen.command.PrefixCommand;
import dev.ell.mrgreen.command.SlashCommand;
import dev.ell.mrgreen.service.GistService;
import dev.ell.mrgreen.service.Judge0Service;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnBean(Judge0Service.class)
public class EvalCommand extends ListenerAdapter implements SlashCommand, PrefixCommand {

    private static final int MAX_OUTPUT_LENGTH = 1900;
    private static final Duration COOLDOWN = Duration.ofSeconds(10);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");
    private static final Pattern IRC_PREFIX_PATTERN = Pattern.compile("^`?<[^>]+>`?\\s*");
    private static final String MODAL_ID = "eval-modal";

    private final Judge0Service judge0Service;
    private final GistService gistService;
    private final ConcurrentHashMap<String, Instant> cooldowns = new ConcurrentHashMap<>();

    public EvalCommand(Judge0Service judge0Service, GistService gistService) {
        this.judge0Service = judge0Service;
        this.gistService = gistService;
    }

    @Override
    public String getName() {
        return "eval";
    }

    @Override
    public List<String> getAliases() {
        return List.of("run", "exec");
    }

    // --- Slash Command ---

    @Override
    public SlashCommandData build() {
        return Commands.slash("eval", "Execute code in 70+ programming languages");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var languageInput = TextInput.create("language", "Language", TextInputStyle.SHORT)
                .setPlaceholder("python, js, c++, etc. (leave blank if providing a gist URL)")
                .setRequired(false)
                .setMaxLength(50)
                .build();

        var codeInput = TextInput.create("code", "Code", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Your code here...")
                .setRequired(false)
                .setMaxLength(4000)
                .build();

        var gistInput = TextInput.create("gist_url", "GitHub Gist URL", TextInputStyle.SHORT)
                .setPlaceholder("https://gist.github.com/...")
                .setRequired(false)
                .setMaxLength(200)
                .build();

        var modal = Modal.create(MODAL_ID, "Eval — Run Code")
                .addComponents(
                        ActionRow.of(languageInput),
                        ActionRow.of(codeInput),
                        ActionRow.of(gistInput)
                )
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NonNull ModalInteractionEvent event) {
        if (!event.getModalId().equals(MODAL_ID)) return;
        if (isOnCooldown(event.getUser().getId())) return;

        var language = getModalValue(event, "language");
        var code = getModalValue(event, "code");
        var gistUrl = getModalValue(event, "gist_url");

        // Handle "languages" keyword
        if ("languages".equalsIgnoreCase(language) && code.isEmpty() && gistUrl.isEmpty()) {
            event.reply(formatLanguageList()).setEphemeral(true).queue();
            return;
        }

        // Resolve code from gist if provided
        if (!gistUrl.isEmpty()) {
            var gist = gistService.fetchGist(gistUrl);
            if (gist.isEmpty()) {
                event.reply("Failed to fetch gist from: " + gistUrl).setEphemeral(true).queue();
                return;
            }
            code = gist.get().content();
            if (language.isEmpty()) {
                var langFromExt = judge0Service.findLanguageByExtension(gist.get().extension());
                if (langFromExt.isEmpty()) {
                    event.reply("Could not detect language from gist file extension: " + gist.get().filename()).setEphemeral(true).queue();
                    return;
                }
                language = langFromExt.get().name();
            }
        }

        if (code.isEmpty()) {
            event.reply("No code provided. Enter code or a gist URL.").setEphemeral(true).queue();
            return;
        }
        if (language.isEmpty()) {
            event.reply("No language specified. Provide a language name or a gist URL with a file extension.").setEphemeral(true).queue();
            return;
        }

        var lang = judge0Service.findLanguage(language);
        if (lang.isEmpty()) {
            event.reply("Unknown language: " + language + ". Use `/eval` with language `languages` to see available options.").setEphemeral(true).queue();
            return;
        }

        // Defer only for the actual submission (long-running)
        event.deferReply().queue();

        try {
            var result = judge0Service.submit(lang.get().id(), code);
            cooldowns.put(event.getUser().getId(), Instant.now());
            event.getHook().sendMessage(formatResult(result)).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("Execution failed: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    // --- Prefix Command ---

    @Override
    public void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx) {
        var userId = ctx.source() == CommandContext.Source.IRC ? ctx.ircUsername() : event.getAuthor().getId();
        if (isOnCooldown(userId)) return;

        var msg = event.getMessage();

        if (args.isEmpty()) {
            msg.reply("Usage: `%eval <language> <code>`, `%eval` with a code block, or `%eval <gist-url>`").queue();
            return;
        }

        // Handle "languages" subcommand
        if (args.size() == 1 && "languages".equalsIgnoreCase(args.getFirst())) {
            msg.reply(formatLanguageList()).queue();
            return;
        }

        // Handle "language <name>" subcommand
        if (args.size() == 2 && "language".equalsIgnoreCase(args.getFirst())) {
            msg.reply(formatLanguageInfo(args.get(1))).queue();
            return;
        }

        // Get full content after the command name for proper whitespace/newline preservation
        var rawContent = getRawContent(event, ctx);
        if (rawContent == null) {
            msg.reply("Failed to parse message content.").queue();
            return;
        }

        String language = null;
        String code = null;

        // Check for code block
        var codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(rawContent);
        if (codeBlockMatcher.find()) {
            var blockLang = codeBlockMatcher.group(1);
            code = codeBlockMatcher.group(2);

            // Check if there's an explicit language before the code block
            var beforeBlock = rawContent.substring(0, codeBlockMatcher.start()).trim();
            if (!beforeBlock.isEmpty()) {
                language = beforeBlock.split("\\s+")[0];
            } else if (blockLang != null && !blockLang.isEmpty()) {
                language = blockLang;
            }
        }

        // Check for gist URL
        if (code == null) {
            for (var arg : args) {
                if (GistService.isGistUrl(arg)) {
                    msg.suppressEmbeds(true).queue();
                    var gist = gistService.fetchGist(arg);
                    if (gist.isEmpty()) {
                        msg.reply("Failed to fetch gist from: " + arg).queue();
                        return;
                    }
                    code = gist.get().content();

                    // Check for explicit language override (arg before the URL)
                    var firstArg = args.getFirst();
                    if (!GistService.isGistUrl(firstArg)) {
                        language = firstArg;
                    } else if (!gist.get().extension().isEmpty()) {
                        var langFromExt = judge0Service.findLanguageByExtension(gist.get().extension());
                        if (langFromExt.isPresent()) {
                            language = langFromExt.get().name();
                        }
                    }
                    break;
                }
            }
        }

        // Fallback: first arg is language, rest is code
        if (code == null) {
            language = args.getFirst();
            // Get everything after the language name from raw content
            var langEnd = rawContent.indexOf(language) + language.length();
            code = rawContent.substring(langEnd).trim();
        }

        if (language == null || language.isEmpty()) {
            msg.reply("Could not determine language. Use `%eval <language> <code>` or a code block with a language tag.").queue();
            return;
        }

        if (code == null || code.isEmpty()) {
            msg.reply("No code provided.").queue();
            return;
        }

        var lang = judge0Service.findLanguage(language);
        if (lang.isEmpty()) {
            msg.reply("Unknown language: " + language + ". Use `%eval languages` to see available options.").queue();
            return;
        }

        try {
            var result = judge0Service.submit(lang.get().id(), code);
            cooldowns.put(userId, Instant.now());
            msg.reply(formatResult(result)).queue();
        } catch (Exception e) {
            msg.reply("Execution failed: " + e.getMessage()).queue();
        }
    }

    // --- Helpers ---

    private boolean isOnCooldown(String userId) {
        var last = cooldowns.get(userId);
        return last != null && Instant.now().isBefore(last.plus(COOLDOWN));
    }

    private String getRawContent(MessageReceivedEvent event, CommandContext ctx) {
        var raw = event.getMessage().getContentRaw();

        if (ctx.source() == CommandContext.Source.IRC) {
            // Strip IRC bridge prefix: `<user>` or <user>
            raw = IRC_PREFIX_PATTERN.matcher(raw).replaceFirst("");
        }

        // Strip the command prefix and name (%eval, %run, %exec)
        var spaceIndex = raw.indexOf(' ');
        if (spaceIndex < 0) return "";
        return raw.substring(spaceIndex + 1);
    }

    private String formatResult(Judge0Service.SubmissionResult result) {
        var sb = new StringBuilder();

        // Status 3 = Accepted (success)
        if (result.statusId() != 3) {
            sb.append("**Status:** ").append(result.statusDescription()).append("\n");
        }

        // Compilation error
        if (result.statusId() == 6 && result.compileOutput() != null) {
            sb.append("```\n").append(truncate(result.compileOutput())).append("\n```");
            return sb.toString();
        }

        // Output
        if (result.stdout() != null && !result.stdout().isEmpty()) {
            sb.append("```\n").append(truncate(result.stdout())).append("\n```");
        } else if (result.stderr() != null && !result.stderr().isEmpty()) {
            sb.append("**stderr:**\n```\n").append(truncate(result.stderr())).append("\n```");
        } else if (result.statusId() == 3) {
            sb.append("Program produced no output.");
        }

        // Show stderr alongside stdout if both present
        if (result.stdout() != null && !result.stdout().isEmpty()
                && result.stderr() != null && !result.stderr().isEmpty()) {
            sb.append("\n**stderr:**\n```\n").append(truncate(result.stderr())).append("\n```");
        }

        // Execution info
        if (result.time() != null) {
            sb.append("\n*").append(result.time()).append("s");
            if (result.memory() > 0) {
                sb.append(" | ").append(result.memory()).append(" KB");
            }
            sb.append("*");
        }

        return sb.toString();
    }

    private String formatLanguageInfo(String input) {
        var lang = judge0Service.findLanguage(input);
        if (lang.isEmpty()) {
            return "Unknown language: " + input + ". Use `%eval languages` to see available options.";
        }

        var l = lang.get();
        var shorthand = l.name().split("\\s+")[0].toLowerCase();

        // Find aliases that resolve to this language
        var aliases = judge0Service.getAliases().entrySet().stream()
                .filter(e -> {
                    var resolved = e.getValue().toLowerCase();
                    return resolved.equals(shorthand) || l.name().toLowerCase().startsWith(resolved);
                })
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.joining(", "));

        var sb = new StringBuilder();
        sb.append("**").append(l.name()).append("**\n");
        sb.append("ID: `").append(l.id()).append("`\n");
        sb.append("Usage: `%eval ").append(shorthand).append(" <code>`");
        if (!aliases.isEmpty()) {
            sb.append("\nAliases: ").append(aliases);
        }
        return sb.toString();
    }

    private String formatLanguageList() {
        var languages = judge0Service.getLanguages();
        if (languages.isEmpty()) {
            return "Failed to fetch language list.";
        }

        var names = languages.stream()
                .map(l -> l.name().split("\\s+")[0].toLowerCase())
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return "**Available languages:** " + names;
    }

    private String truncate(String text) {
        if (text.length() <= MAX_OUTPUT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_OUTPUT_LENGTH) + "\n... (truncated)";
    }

    private String getModalValue(ModalInteractionEvent event, String id) {
        var mapping = event.getValue(id);
        return mapping != null ? mapping.getAsString().trim() : "";
    }
}
