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

import net.unknown.proxy.UnknownNetworkProxyCore;
import net.unknown.shared.punishment.PlayerPunishmentData;
import net.unknown.shared.punishment.interfaces.Punishment;
import net.unknown.shared.punishment.interfaces.TemporaryPunishment;
import net.unknown.shared.punishment.PunishmentState;
import net.unknown.shared.punishment.PunishmentType;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.util.UUID;

public class ProxyPlayerPunishmentData extends PlayerPunishmentData {
    private YamlConfigurationLoader loader;
    private CommentedConfigurationNode data;
    public ProxyPlayerPunishmentData(UUID uniqueId) {
        super(uniqueId);
    }

    private static PunishmentState getStateFromConfigSection(CommentedConfigurationNode section) {
        if (section != null && section.hasChild("type")) {
            PunishmentType type = PunishmentType.valueOf(section.node("type").getString().toUpperCase());
            if (type != PunishmentType.NONE) {
                UUID target = UUID.fromString(section.node("target").getString());
                UUID executor = UUID.fromString(section.node("executor").getString());
                String reason = section.node("reason").getString();
                long expiresIn = section.hasChild("expires_in") ? section.node("expires_in").getLong() : -1;

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

    private static void writeStateToConfiguration(PunishmentState state, String[] paths, CommentedConfigurationNode config) {
        try {
            CommentedConfigurationNode section = CommentedConfigurationNode.root();
            section.node("type").set(state.getType().name().toLowerCase());
            if (state instanceof Punishment data) {
                section.node("target").set(data.getTarget().toString());
                section.node("executor").set(data.getExecutor().toString());
                section.node("reason").set(data.getReason());

                if (data instanceof TemporaryPunishment temp) {
                    section.node("expires_in").set(temp.getExpiresIn());
                }
            }
            config.node(paths).set(section);
        } catch (SerializationException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            if (!this.source.exists() && !this.source.createNewFile())
                throw new IOException("ファイルの作成に失敗しました: " + this.source);
            this.loader = UnknownNetworkProxyCore.createConfigLoader(this.source);
            this.data = this.loader.load();
            if (this.data.hasChild("current_state"))
                this.currentState = getStateFromConfigSection(this.data.node("current_state"));
            if (this.data.hasChild("histories")) {
                CommentedConfigurationNode histories = this.data.node("histories");
                if (histories != null) {
                    histories.childrenMap().forEach((timeStampObj, section) -> {
                        long executedTimeStamp = Long.parseLong(timeStampObj.toString());
                        this.punishmentHistories.put(executedTimeStamp, getStateFromConfigSection(section));
                    });
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("ファイルの読み込みに失敗しました", e);
        }
    }

    @Override
    public void write() {
        this.data.removeChild("current_state");
        writeStateToConfiguration(this.currentState, new String[] {"current_state"}, this.data);

        this.data.removeChild("histories");
        this.punishmentHistories.forEach((executedTimeStamp, state) -> {
            writeStateToConfiguration(state, new String[] {"histories", String.valueOf(executedTimeStamp)}, this.data);
        });

        try {
            this.loader.save(this.data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
