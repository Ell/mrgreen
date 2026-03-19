package dev.ell.mrgreen.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public interface PrefixCommand {
    String getName();

    default List<String> getAliases() {
        return List.of();
    }

    void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx);
}
