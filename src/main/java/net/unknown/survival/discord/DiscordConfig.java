package net.unknown.survival.discord;

import net.unknown.core.discord.UnknownNetworkDiscordBot;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class DiscordConfig {
    private static String GLOBAL_CHAT_SYNC_CHANNEL_ID = null;

    public static void load() {
        try {
            List<String> configLines = Files.readAllLines(UnknownNetworkDiscordBot.getConfigFile().toPath());
            GLOBAL_CHAT_SYNC_CHANNEL_ID = configLines.stream().filter(s -> s.startsWith("global-chat-channel-id:")).map(s -> s.split(": ?", 2)[1]).findAny().orElse(null);
        } catch (IOException ignored) {}
    }
}
