package dev.ell.mrgreen.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface SlashCommand {
    String getName();

    SlashCommandData build();

    void execute(SlashCommandInteractionEvent event);
}
