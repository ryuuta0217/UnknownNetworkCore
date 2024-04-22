package net.unknown.proxy;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ChatLogging implements Listener {
    @EventHandler
    public void onChat(ChatEvent event) {
        boolean isCommand = event.getMessage().startsWith("/");
        boolean isProxyCommand = isCommand && UnknownNetworkProxyCore.getInstance().getProxy().getPluginManager().getCommands().stream().anyMatch(entry -> {
            String[] commandLine = event.getMessage().replaceFirst("^/", "").split(" ");
            return entry.getKey().equalsIgnoreCase(commandLine[0]);
        });

        String logMessage = "[CHAT] " + ((ProxiedPlayer) event.getSender()).getName() + " (" + ((ProxiedPlayer) event.getSender()).getServer().getInfo().getName() + "): " + event.getMessage();
        if (isCommand && !isProxyCommand) {
            logMessage = ((ProxiedPlayer) event.getSender()).getName() + " executed target server (" + ((ProxiedPlayer) event.getSender()).getServer().getInfo().getName() + ") command: " + event.getMessage();
        }
        UnknownNetworkProxyCore.getInstance().getSLF4JLogger().info(logMessage);
    }
}
