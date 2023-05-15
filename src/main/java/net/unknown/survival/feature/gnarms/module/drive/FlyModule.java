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

package net.unknown.survival.feature.gnarms.module.drive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.unknown.survival.feature.gnarms.GNArms;
import net.unknown.survival.feature.gnarms.GNContext;
import net.unknown.survival.feature.gnarms.module.GNModule;
import net.unknown.survival.feature.gnarms.module.GNModules;
import org.bukkit.GameMode;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FlyModule implements GNModule {
    public static final FlyModule INSTANCE = new FlyModule();
    private static final long USE_PARTICLE_IN_IDLE = 20;
    private static final long USE_PARTICLE_IN_USE = 105;

    private long overheatTicks = 0;

    FlyModule() {
        GNModules.registerMapping(this.getId(), this);
    }

    @Override
    public void onEnable(GNContext ctx) {
        if (ctx.getPlayer().getGameMode() != GameMode.CREATIVE && ctx.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            ctx.getPlayer().setAllowFlight(true);
        }
    }

    @Override
    public void onDisable(GNContext ctx) {
        if (ctx.getPlayer().getGameMode() != GameMode.CREATIVE && ctx.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            ctx.getPlayer().setAllowFlight(false);
            ctx.getPlayer().setFlying(false);
            ctx.getPlayer().setFlySpeed(0.1f);
        }
    }

    @Override
    public void tick(GNContext ctx) {
        if (overheatTicks > 0) {
            overheatTicks--;
            return;
        }

        if (ctx.getPlayer().getGameMode() != GameMode.CREATIVE && ctx.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            if (ctx.getPlayer().isFlying()) {
                if (isParticlesAvailable(ctx, USE_PARTICLE_IN_USE)) {
                    this.onParticlesAvailable(ctx);
                    ctx.addParticlesToUse(USE_PARTICLE_IN_USE);
                    if (ctx.getPlayer().isSprinting()) {
                        ctx.getPlayer().setFlySpeed(Mth.clamp(ctx.getPlayer().getFlySpeed() + 0.005f, -1.0f, 1.0f));
                    } // TODO 静止時、徐々に減少
                } else {
                    this.onParticlesEmpty(ctx);
                    this.overheatTicks = 20 * 5; // 5 seconds
                }
            } else {
                if (isParticlesAvailable(ctx, USE_PARTICLE_IN_IDLE)) {
                    this.onParticlesAvailable(ctx);
                    ctx.addParticlesToUse(USE_PARTICLE_IN_IDLE);
                    ctx.getPlayer().setFlySpeed(0.1f);
                } else {
                    this.onParticlesEmpty(ctx);
                }
            }
        }
    }

    private static boolean isParticlesAvailable(GNContext ctx, long useParticle) {
        return (((ctx.getCurrentParticles() + ctx.getGeneratorParticlesOutput()) - ctx.getParticlesToUse()) - useParticle) > 0;
    }

    @Override
    public void onParticlesEmpty(GNContext ctx) {
        this.onDisable(ctx);
    }

    @Override
    public void onParticlesAvailable(GNContext ctx) {
        this.onEnable(ctx);
    }

    @Override
    public boolean onInstall(GNContext ctx) {
        return true;
    }

    @Override
    public boolean onUninstall(GNContext ctx) {
        return true;
    }

    @Nullable
    @Override
    public List<Item> getInstallRequiredItem() {
        return null;
    }

    @Override
    public GNArms.Type[] getAcceptableGNArmTypes() {
        return new GNArms.Type[] {GNArms.Type.DRIVE};
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.of("gnarms:fly", ':');
    }

    @Override
    public String getName() {
        return "飛行";
    }
}
