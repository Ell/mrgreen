package dev.ell.mrgreen.command;

public record CommandContext(Source source, String ircUsername) {
    public enum Source {
        DISCORD,
        IRC
    }

    public static CommandContext discord() {
        return new CommandContext(Source.DISCORD, null);
    }

    public static CommandContext irc(String username) {
        return new CommandContext(Source.IRC, username);
    }
}
