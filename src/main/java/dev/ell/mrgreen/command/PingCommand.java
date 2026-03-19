package dev.ell.mrgreen.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PingCommand implements SlashCommand, PrefixCommand {
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public SlashCommandData build() {
        return Commands.slash("ping", "Shows the bot's gateway latency");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var ping = event.getJDA().getGatewayPing();
        event.reply("Pong! **" + ping + "ms**").queue();
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> args, CommandContext ctx) {
        var ping = event.getJDA().getGatewayPing();
        event.getChannel().sendMessage("Pong! **" + ping + "ms**").queue();
    }
}
