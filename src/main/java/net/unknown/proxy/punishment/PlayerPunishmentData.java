/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.proxy.punishment;

import net.unknown.UnknownNetworkCore;
import net.unknown.core.managers.RunnableManager;
import net.unknown.proxy.punishment.interfaces.Punishment;
import net.unknown.proxy.punishment.interfaces.TemporaryPunishment;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PlayerPunishmentData {
    private static final File DATA_FOLDER = new File(UnknownNetworkCore.getSharedDataFolder(), "punishments");
    private final Map<Long, PunishmentState> punishmentHistories = new HashMap<>();
    private final UUID uniqueId;
    private final File source;
    private final Logger logger;
    private PunishmentState currentState = PunishmentState.Default.getInstance();
    private YamlConfiguration data;

    public PlayerPunishmentData(UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.source = new File(DATA_FOLDER, this.uniqueId.toString() + ".yml");
        this.logger = Logger.getLogger("UNC/PunishmentData/" + (Bukkit.getOfflinePlayer(uniqueId).getName() == null ? uniqueId.toString() : Bukkit.getOfflinePlayer(uniqueId).getName()));
        this.load();
    }

    private static PunishmentState getStateFromConfigSection(ConfigurationSection section) {
        if (section != null && section.contains("type")) {
            PunishmentType type = PunishmentType.valueOf(section.getString("type").toUpperCase());
            if (type != PunishmentType.NONE) {
                UUID target = UUID.fromString(section.getString("target"));
                UUID executor = UUID.fromString(section.getString("executor"));
                String reason = section.getString("reason");
                long expiresIn = section.contains("expires_in") ? section.getLong("expires_in") : -1;

                if (type == PunishmentType.MUTE) return new PunishmentState.Mute(target, executor, reason);
                if (type == PunishmentType.TEMP_MUTE)
                    return new PunishmentState.Mute.Temp(target, executor, reason, expiresIn);
                if (type == PunishmentType.KICK) return new PunishmentState.Kick(target, executor, reason);
                if (type == PunishmentType.BAN) return new PunishmentState.Ban(target, executor, reason);
                if (type == PunishmentType.TEMP_BAN)
                    return new PunishmentState.Ban.Temp(target, executor, reason, expiresIn);
            }
        }
        return PunishmentState.Default.getInstance();
    }

    private static void writeStateToConfiguration(PunishmentState state, String key, YamlConfiguration config) {
        ConfigurationSection section = config.createSection(key);
        section.set("type", state.getType().name().toLowerCase());
        if (state instanceof Punishment data) {
            section.set("target", data.getTarget().toString());
            section.set("executor", data.getExecutor().toString());
            section.set("reason", data.getReason());

            if (data instanceof TemporaryPunishment temp) {
                section.set("expires_in", temp.getExpiresIn());
            }
        }
        config.set(key, section);
    }

    public void load() {
        try {
            if (!this.source.exists() && !this.source.createNewFile())
                throw new IOException("ファイルの作成に失敗しました: " + this.source);
            this.data = YamlConfiguration.loadConfiguration(this.source);
            if (this.data.contains("current_state"))
                this.currentState = getStateFromConfigSection(this.data.getConfigurationSection("current_state"));
            if (this.data.contains("histories")) {
                ConfigurationSection histories = this.data.getConfigurationSection("histories");
                if (histories != null) {
                    histories.getKeys(false).forEach(timeStampStr -> {
                        long executedTimeStamp = Long.parseLong(timeStampStr);
                        this.punishmentHistories.put(executedTimeStamp, getStateFromConfigSection(histories.getConfigurationSection(timeStampStr)));
                    });
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("ファイルの読み込みに失敗しました", e);
        }
    }

    public void mute(UUID executor, String reason) {
        this.currentState = new PunishmentState.Mute(this.uniqueId, executor, reason);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
        RunnableManager.runAsync(this::write);
    }

    public void muteTemp(UUID executor, String reason, long expiresIn) {
        this.currentState = new PunishmentState.Mute.Temp(this.uniqueId, executor, reason, expiresIn);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
        RunnableManager.runAsync(this::write);
    }

    public void unMute(UUID executor, String reason) {
        this.currentState = PunishmentState.Default.getInstance();
        this.punishmentHistories.put(System.currentTimeMillis(), new PunishmentState.Unmute(this.uniqueId, executor, reason));
        RunnableManager.runAsync(this::write);
    }

    public void kick(UUID executor, String reason) {
        this.currentState = new PunishmentState.Kick(this.uniqueId, executor, reason);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
        RunnableManager.runAsync(this::write);
    }

    public void ban(UUID executor, String reason) {
        this.currentState = new PunishmentState.Ban(this.uniqueId, executor, reason);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
        RunnableManager.runAsync(this::write);
    }

    public void banTemp(UUID executor, String reason, long expiresIn) {
        this.currentState = new PunishmentState.Ban.Temp(this.uniqueId, executor, reason, expiresIn);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
        RunnableManager.runAsync(this::write);
    }

    public void unBan(UUID executor, String reason) {
        this.currentState = PunishmentState.Default.getInstance();
        this.punishmentHistories.put(System.currentTimeMillis(), new PunishmentState.Unban(this.uniqueId, executor, reason));
        RunnableManager.runAsync(this::write);
    }

    public void write() {
        this.data.set("current_state", null);
        writeStateToConfiguration(this.currentState, "current_state", this.data);

        this.data.set("histories", null);
        this.punishmentHistories.forEach((executedTimeStamp, state) -> {
            writeStateToConfiguration(state, "histories." + executedTimeStamp, this.data);
        });

        try {
            this.data.save(this.source);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
