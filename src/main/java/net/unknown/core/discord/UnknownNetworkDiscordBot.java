/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.unknown.shared.SharedConstants;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

public class UnknownNetworkDiscordBot {
    private static final Logger LOGGER = Logger.getLogger("UNC/DiscordBot");
    private static final File CONFIG_FILE = new File(SharedConstants.DATA_FOLDER, "discord-config.txt");
    private static final String BOT_TOKEN;

    private static Thread JDA_THREAD;
    private static JDA JDA;

    static {
        String token = null;
        try {
            token = CONFIG_FILE.exists() ? Files.readAllLines(CONFIG_FILE.toPath()).stream().filter(s -> s.matches("^token: ?.*$")).map(s -> s.split(": ?", 2)[1]).findFirst().orElse(null) : null;
        } catch (IOException ignored) {}
        BOT_TOKEN = token;
    }

    @Nonnull
    public static JDABuilder defaultBuilder() throws IllegalStateException, IllegalThreadStateException {
        if (BOT_TOKEN == null) throw new IllegalStateException("Bot token is not set! please write bot token into config.txt.");
        return JDABuilder.create(List.of(GatewayIntent.values()))
                .setToken(BOT_TOKEN)
                .addEventListeners(new ListenerAdapter() {
                    @Override
                    public void onReady(@NotNull ReadyEvent event) {
                        UnknownNetworkDiscordBot.JDA = event.getJDA();
                        LOGGER.info("Bot is ready.");
                    }

                    @Override
                    public void onShutdown(@NotNull ShutdownEvent event) {
                        if (Thread.currentThread().equals(UnknownNetworkDiscordBot.JDA_THREAD)) {
                            UnknownNetworkDiscordBot.JDA_THREAD = null;
                        }
                    }
                });
    }

    public static void runAnotherThread(JDABuilder builder) {
        if (JDA_THREAD != null && JDA_THREAD.isAlive()) throw new IllegalThreadStateException("Bot is already running?");
        JDA_THREAD = new Thread(builder::build);
        JDA_THREAD.setName("UnknownNetworkDiscordBot - JDA main thread");
        JDA_THREAD.start();
    }

    @Nullable
    public static Thread getThread() {
        return JDA_THREAD;
    }

    @Nullable
    public static JDA getJDA() {
        return JDA;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static File getConfigFile() {
        return CONFIG_FILE;
    }
}
