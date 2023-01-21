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

package net.unknown.proxy.punishment;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.unknown.shared.punishment.PlayerPunishmentData;
import net.unknown.shared.punishment.interfaces.Punishment;
import net.unknown.shared.punishment.interfaces.TemporaryPunishment;
import net.unknown.shared.punishment.PunishmentState;
import net.unknown.shared.punishment.PunishmentType;

import java.io.IOException;
import java.util.UUID;

public class ProxyPlayerPunishmentData extends PlayerPunishmentData {
    private Configuration data;
    public ProxyPlayerPunishmentData(UUID uniqueId) {
        super(uniqueId);
    }

    private static PunishmentState getStateFromConfigSection(Configuration section) {
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

    private static void writeStateToConfiguration(PunishmentState state, String key, Configuration config) {
        Configuration section = new Configuration();
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
            this.data = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.source);
            if (this.data.contains("current_state"))
                this.currentState = getStateFromConfigSection(this.data.getSection("current_state"));
            if (this.data.contains("histories")) {
                Configuration histories = this.data.getSection("histories");
                if (histories != null) {
                    histories.getKeys().forEach(timeStampStr -> {
                        long executedTimeStamp = Long.parseLong(timeStampStr);
                        this.punishmentHistories.put(executedTimeStamp, getStateFromConfigSection(histories.getSection(timeStampStr)));
                    });
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("ファイルの読み込みに失敗しました", e);
        }
    }

    @Override
    public void write() {
        this.data.set("current_state", null);
        writeStateToConfiguration(this.currentState, "current_state", this.data);

        this.data.set("histories", null);
        this.punishmentHistories.forEach((executedTimeStamp, state) -> {
            writeStateToConfiguration(state, "histories." + executedTimeStamp, this.data);
        });

        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(this.data, this.source);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
