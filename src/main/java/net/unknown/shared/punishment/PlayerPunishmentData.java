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

package net.unknown.shared.punishment;

import net.unknown.shared.SharedConstants;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class PlayerPunishmentData {
    private static final File DATA_FOLDER = new File(SharedConstants.DATA_FOLDER, "punishments");
    protected final Map<Long, PunishmentState> punishmentHistories = new HashMap<>();
    protected final UUID uniqueId;
    protected final File source;
    protected final Logger logger;
    protected PunishmentState currentState = PunishmentState.Default.getInstance();

    public PlayerPunishmentData(UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.source = new File(DATA_FOLDER, this.uniqueId.toString() + ".yml");
        this.logger = Logger.getLogger("UNC/PunishmentData/" + this.uniqueId);
        this.load();
    }

    public abstract void load();

    public void mute(UUID executor, String reason) {
        this.currentState = new PunishmentState.Mute(this.uniqueId, executor, reason);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
    }

    public void muteTemp(UUID executor, String reason, long expiresIn) {
        this.currentState = new PunishmentState.Mute.Temp(this.uniqueId, executor, reason, expiresIn);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
    }

    public void unMute(UUID executor, String reason) {
        this.currentState = PunishmentState.Default.getInstance();
        this.punishmentHistories.put(System.currentTimeMillis(), new PunishmentState.Unmute(this.uniqueId, executor, reason));
    }

    public void kick(UUID executor, String reason) {
        this.currentState = new PunishmentState.Kick(this.uniqueId, executor, reason);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
    }

    public void ban(UUID executor, String reason) {
        this.currentState = new PunishmentState.Ban(this.uniqueId, executor, reason);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
    }

    public void banTemp(UUID executor, String reason, long expiresIn) {
        this.currentState = new PunishmentState.Ban.Temp(this.uniqueId, executor, reason, expiresIn);
        this.punishmentHistories.put(System.currentTimeMillis(), this.currentState);
    }

    public void unBan(UUID executor, String reason) {
        this.currentState = PunishmentState.Default.getInstance();
        this.punishmentHistories.put(System.currentTimeMillis(), new PunishmentState.Unban(this.uniqueId, executor, reason));
    }

    public boolean isMuted() {
        return this.currentState instanceof PunishmentState.Mute;
    }

    public boolean isBanned() {
        return this.currentState instanceof PunishmentState.Ban;
    }

    public abstract void write();
}
