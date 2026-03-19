package dev.ell.mrgreen.handler;

import dev.ell.mrgreen.command.CommandContext;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MessageHandler {
    Pattern getPattern();

    void handle(MessageReceivedEvent event, Matcher matcher, CommandContext context);
}
