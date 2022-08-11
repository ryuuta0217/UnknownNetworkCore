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

package net.unknown.shared.punishment;

import net.unknown.shared.punishment.interfaces.PermanentPunishment;
import net.unknown.shared.punishment.interfaces.Punishment;
import net.unknown.shared.punishment.interfaces.TemporaryPunishment;

import java.util.UUID;

public class PunishmentState {
    private final PunishmentType type;

    protected PunishmentState(PunishmentType type) {
        this.type = type;
    }

    public PunishmentType getType() {
        return type;
    }

    public static class Default extends PunishmentState {
        private static final Default INSTANCE = new Default();

        protected Default() {
            super(PunishmentType.NONE);
        }

        public static Default getInstance() {
            return INSTANCE;
        }
    }

    public static class Mute extends PunishmentState implements PermanentPunishment {
        private final UUID target;
        private final UUID executor;
        private final String reason;

        public Mute(UUID target, UUID executor, String reason) {
            super(PunishmentType.MUTE);
            this.target = target;
            this.executor = executor;
            this.reason = reason;
        }

        private Mute(PunishmentType type, UUID target, UUID executor, String reason) {
            super(type);
            this.target = target;
            this.executor = executor;
            this.reason = reason;
        }

        @Override
        public UUID getTarget() {
            return this.target;
        }

        @Override
        public UUID getExecutor() {
            return this.executor;
        }

        @Override
        public String getReason() {
            return this.reason;
        }

        public static class Temp extends Mute implements TemporaryPunishment {
            private final long expiresIn;

            public Temp(UUID target, UUID executor, String reason, long expiresIn) {
                super(PunishmentType.TEMP_MUTE, target, executor, reason);
                this.expiresIn = expiresIn;
            }

            @Override
            public long getExpiresIn() {
                return this.expiresIn;
            }
        }
    }

    public static class Unmute extends PunishmentState implements Punishment {
        private final UUID target;
        private final UUID executor;
        private final String reason;

        public Unmute(UUID target, UUID executor, String reason) {
            super(PunishmentType.UNMUTE);
            this.target = target;
            this.executor = executor;
            this.reason = reason;
        }

        @Override
        public UUID getTarget() {
            return this.target;
        }

        @Override
        public UUID getExecutor() {
            return this.executor;
        }

        @Override
        public String getReason() {
            return this.reason;
        }
    }

    public static class Kick extends PunishmentState implements Punishment {
        private final UUID target;
        private final UUID executor;
        private final String reason;

        public Kick(UUID target, UUID executor, String reason) {
            super(PunishmentType.KICK);
            this.target = target;
            this.executor = executor;
            this.reason = reason;
        }

        @Override
        public UUID getTarget() {
            return this.target;
        }

        @Override
        public UUID getExecutor() {
            return this.executor;
        }

        @Override
        public String getReason() {
            return this.reason;
        }
    }

    public static class Ban extends PunishmentState implements PermanentPunishment {
        private final UUID target;
        private final UUID executor;
        private final String reason;

        public Ban(UUID target, UUID executor, String reason) {
            super(PunishmentType.BAN);
            this.target = target;
            this.executor = executor;
            this.reason = reason;
        }

        private Ban(PunishmentType type, UUID target, UUID executor, String reason) {
            super(type);
            this.target = target;
            this.executor = executor;
            this.reason = reason;
        }

        @Override
        public UUID getTarget() {
            return this.target;
        }

        @Override
        public UUID getExecutor() {
            return this.executor;
        }

        @Override
        public String getReason() {
            return this.reason;
        }

        public static class Temp extends Ban implements TemporaryPunishment {
            private final long expiresIn;

            public Temp(UUID target, UUID executor, String reason, long expiresIn) {
                super(PunishmentType.TEMP_BAN, target, executor, reason);
                this.expiresIn = expiresIn;
            }

            @Override
            public long getExpiresIn() {
                return this.expiresIn;
            }
        }
    }

    public static class Unban extends PunishmentState implements Punishment {
        private final UUID target;
        private final UUID executor;
        private final String reason;

        public Unban(UUID target, UUID executor, String reason) {
            super(PunishmentType.UNBAN);
            this.target = target;
            this.executor = executor;
            this.reason = reason;
        }

        @Override
        public UUID getTarget() {
            return this.target;
        }

        @Override
        public UUID getExecutor() {
            return this.executor;
        }

        @Override
        public String getReason() {
            return this.reason;
        }
    }
}
