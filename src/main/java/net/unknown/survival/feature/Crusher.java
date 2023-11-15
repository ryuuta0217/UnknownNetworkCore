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

package net.unknown.survival.feature;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.launchwrapper.event.BlockDispenseBeforeEvent;
import net.unknown.launchwrapper.mixininterfaces.IMixinBlockEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// Code name: Tempest
public class Crusher implements Listener {
    @EventHandler
    public void onDispenserPreShoot(BlockDispenseBeforeEvent event) {
        DispenserBlockEntity dispenser = event.getBlockSource().blockEntity();
        BlockPos pos = event.getBlockSource().pos();
        if (!dispenser.hasCustomName()) return;
        if (!PlainTextComponentSerializer.plainText().serialize(NewMessageUtil.convertMinecraft2Adventure(dispenser.getCustomName())).equals("Crusher")) return;
        event.setCancelled(true);

        UUID placer = ((IMixinBlockEntity) dispenser).getPlacer();

        FakePlayer player = new FakePlayer(dispenser, placer == null ? UUID.randomUUID() : placer);
        player.setItemInHand(InteractionHand.MAIN_HAND, event.getItem());
        if (player.getMainHandItem().equals(event.getItem())) {
            //System.out.println("Validation completed - Dispenser's shoot item is in player's hand.");
        }

        List<LivingEntity> detectedEntities = getEntities(dispenser, 2, 2, 0, 2, 2);
        detectedEntities.forEach(entity -> {
            if (entity instanceof Player) return;
            player.attack(entity);
            //entity.hurt(player.damageSources().playerAttack(player), Integer.MAX_VALUE);
            //int exp = entity.getExperienceReward();
            //ExperienceOrb.award(event.getBlockSource().level(), Vec3.atCenterOf(new Vec3i(pos.getX(), pos.getY(), pos.getZ())), exp, org.bukkit.entity.ExperienceOrb.SpawnReason.ENTITY_DEATH, entity);
        });
    }

    private static List<LivingEntity> getEntities(DispenserBlockEntity dispenser, int front, int up, int down, int left, int right) {
        ServerLevel level = (ServerLevel) dispenser.getLevel(); // DedicatedServerでは必ずServerLevelです
        if (level == null) {
            // ここに到達することはない。getLevel()の結果がnullであった場合、キャストに失敗し、ClassCastExceptionが出るためである。
            throw new IllegalStateException("What? getLevel() returns null?");
        }

        Direction dispenserDirection = dispenser.getBlockState().getValue(DispenserBlock.FACING);
        BlockPos frontPos = dispenser.getBlockPos().relative(dispenserDirection, 1);
        AABB box = getBox(frontPos, front, up, down, left, right, dispenserDirection);
        return box != null ? level.getEntitiesOfClass(LivingEntity.class, box) : Collections.emptyList();
    }

    private static AABB getBox(BlockPos start, int front, int up, int down, int left, int right, Direction direction) {
        List<BlockPos> positions = new ArrayList<>();

        //BlockPos.betweenClosedStream(BoundingBox.orientBox(start.getX(), start.getY(), start.getZ(), -left, -down, -right, front, up, left, direction));

        for (int f = 0; f <= front; f++) {
            for (int u = 0; u <= up; u++) {
                for (int d = 0; d <= down; d++) {
                    BlockPos bp = start.relative(direction, f).above(u).below(d);

                    for (int l = 1; l <= left; l++) {
                        positions.add(bp.relative(getLeft(direction), l));
                    }

                    for (int r = 1; r <= right; r++) {
                        positions.add(bp.relative(getRight(direction), r));
                    }

                    positions.add(bp);
                }
            }
        }

        BlockPos min = null;
        BlockPos max = null;

        for (BlockPos pos : positions) {
            if (min == null || pos.compareTo(min) < 0) {
                min = pos;
            }

            if (max == null || pos.compareTo(max) > 0) {
                max = pos;
            }
        }

        Vec3 min3 = (min != null) ? new Vec3(min.getX(), min.getY(), min.getZ()) : null;
        Vec3 max3 = (max != null) ? new Vec3(max.getX() + 1, max.getY(), max.getZ() + 1) : null;

        return min != null ? new AABB(min3, max3) : null;
    }

    private static Direction getLeft(Direction direction) {
        return direction.getCounterClockWise();
    }

    private static Direction getRight(Direction direction) {
        return direction.getClockWise();
    }

    public static class FakePlayer extends net.unknown.core.entity.FakePlayer {
        private final DispenserBlockEntity dispenser;

        public FakePlayer(DispenserBlockEntity dispenser, @Nullable UUID uniqueId) {
            super((ServerLevel) dispenser.getLevel(), dispenser.getName().getString(), uniqueId);
            this.dispenser = dispenser;
            this.moveTo(dispenser.getBlockPos(), 0.0f, 0.0f);
        }

        @Override
        public void setLevel(Level world) {
            this.dispenser.setLevel(world);
        }
    }
}
