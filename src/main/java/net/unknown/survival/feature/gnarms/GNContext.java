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

package net.unknown.survival.feature.gnarms;

import net.minecraft.nbt.CompoundTag;
import org.bukkit.entity.Player;

public class GNContext {
    private final Player player;
    private final CompoundTag data;
    private final boolean immutable;
    private long particlesToUse;
    private long generatorParticlesOutput;
    private final long currentParticles;
    private final long maximumParticles;

    public GNContext(Player player, CompoundTag data, long particlesToUse, long generatorParticlesOutput, long currentParticles, long maximumParticles) {
        this(player, data, particlesToUse, generatorParticlesOutput, currentParticles, maximumParticles, false);
    }

    public GNContext(Player player, CompoundTag data, long particlesToUse, long generatorParticlesOutput, long currentParticles, long maximumParticles, boolean immutable) {
        this.player = player;
        this.data = data;
        this.particlesToUse = particlesToUse;
        this.generatorParticlesOutput = generatorParticlesOutput;
        this.currentParticles = currentParticles;
        this.maximumParticles = maximumParticles;
        this.immutable = immutable;
    }

    public Player getPlayer() {
        return this.player;
    }

    public long getParticlesToUse() {
        return this.particlesToUse;
    }

    public void setParticlesToUse(long particlesToUse) {
        if (this.immutable) throw new IllegalStateException("this GNContext is Immutable!");
        this.particlesToUse = particlesToUse;
    }

    public long getGeneratorParticlesOutput() {
        return this.generatorParticlesOutput;
    }

    public void setGeneratorParticlesOutput(long generatorParticlesOutput) {
        if (this.immutable) throw new IllegalStateException("this GNContext is Immutable!");
        this.generatorParticlesOutput = generatorParticlesOutput;
    }

    public long getCurrentParticles() {
        return this.currentParticles;
    }

    public long getMaximumParticles() {
        return this.maximumParticles;
    }

    public boolean isImmutable() {
        return this.immutable;
    }

    public void addParticlesToUse(long toAddParticlesToUse) {
        if (this.immutable) throw new IllegalStateException("this GNContext is Immutable!");
        this.particlesToUse += toAddParticlesToUse;
    }
}
