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

package net.unknown.core.commands.vanilla;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.unknown.core.util.BrigadierUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class KillCommand {
    private static final Map<String, DamageSource> NAME_TO_SOURCE = new HashMap<>() {{
        put("inFire", DamageSource.IN_FIRE);
        put("lightningBolt", DamageSource.LIGHTNING_BOLT);
        put("onFire", DamageSource.ON_FIRE);
        put("lava", DamageSource.LAVA);
        put("hotFloor", DamageSource.HOT_FLOOR);
        put("inWall", DamageSource.IN_WALL);
        put("cramming", DamageSource.CRAMMING);
        put("drown", DamageSource.DROWN);
        put("starve", DamageSource.STARVE);
        put("cactus", DamageSource.CACTUS);
        put("fall", DamageSource.FALL);
        put("flyIntoWall", DamageSource.FLY_INTO_WALL);
        put("outOfWorld", DamageSource.OUT_OF_WORLD);
        put("generic", DamageSource.GENERIC);
        put("magic", DamageSource.MAGIC);
        put("wither", DamageSource.WITHER);
        put("anvil", DamageSource.ANVIL);
        put("fallingBlock", DamageSource.FALLING_BLOCK);
        put("dragonBreath", DamageSource.DRAGON_BREATH);
        put("dryout", DamageSource.DRY_OUT);
        put("sweetBerryBush", DamageSource.SWEET_BERRY_BUSH);
        put("freeze", DamageSource.FREEZE);
        put("fallingStalactite", DamageSource.FALLING_STALACTITE);
        put("stalagmite", DamageSource.STALAGMITE);
    }};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        BrigadierUtil.forceUnregisterCommand("kill");
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("kill");
        builder.requires(src -> src.hasPermission(2));

        NAME_TO_SOURCE.keySet().forEach(name -> {
            builder.then(Commands.argument("targets", EntityArgument.entities())
                    .executes(ctx -> execute(ctx, DamageSource.OUT_OF_WORLD, Float.MAX_VALUE))
                    .then(Commands.literal(name)
                            .executes(ctx -> execute(ctx, NAME_TO_SOURCE.get(name), Float.MAX_VALUE))
                            .then(Commands.argument("by", EntityArgument.player())
                                    .executes(ctx -> execute(ctx, NAME_TO_SOURCE.get(name), Float.MAX_VALUE))
                                    .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                            .executes(ctx -> execute(ctx, NAME_TO_SOURCE.get(name), FloatArgumentType.getFloat(ctx, "amount")))))));
        });

        dispatcher.register(builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, DamageSource source, float amount) throws CommandSyntaxException {
        ServerPlayer by = BrigadierUtil.isArgumentKeyExists(ctx, "by") ? EntityArgument.getPlayer(ctx, "by") : null;
        Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "targets");
        entities.forEach(target -> {
            if (target instanceof LivingEntity entity) {
                if (by != null) entity.hurt(DamageSource.playerAttack(by), 0.01F);
                entity.hurt(source, amount);
            } else target.kill();
        });

        if (entities.size() == 1) {
            ctx.getSource().sendSuccess(MutableComponent.create(new TranslatableContents("commands.kill.success.single", entities.iterator().next().getDisplayName())), true);
        } else {
            ctx.getSource().sendSuccess(MutableComponent.create(new TranslatableContents("commands.kill.success.multiple", entities.size())), true);
        }

        return entities.size();
    }
}
