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

package net.unknown.minigame.hideandseek.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import io.netty.buffer.Unpooled;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.unknown.UnknownNetworkCore;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.JavaAdapter;

public class CustomFallingBlockEntity extends FallingBlockEntity {
    private final ServerPlayer srcPlayer;
    private Vec3 lastPos;
    private long stayTick = 0L;
    private boolean isStayed = false;

    public CustomFallingBlockEntity(Level world, ServerPlayer srcPlayer, BlockState block) {
        super(world, srcPlayer.getX(), srcPlayer.getY(), srcPlayer.getZ(), block);
        this.srcPlayer = srcPlayer;
        this.dropItem = false;
        this.setNoGravity(true);
        this.time = 590;
        // TODO world.addFreshEntity(this, CreatureSpawnEvent.SpawnReason.NATURAL);
    }

    public static String spawnTest(Player player) {
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(UnknownNetworkCore.getInstance(), player)); //

        CraftWorld cW = (CraftWorld) player.getWorld();
        Level level = cW.getHandle();

        CraftPlayer cP = (CraftPlayer) player;
        ServerPlayer p = cP.getHandle();

        CustomFallingBlockEntity e = new CustomFallingBlockEntity(level, p, Blocks.DIAMOND_ORE.defaultBlockState());
        level.addFreshEntity(e);

        return e.getStringUUID();
    }

    int ticks = 0;
    long sec = 0;

    @Override
    public void tick() {
        if (this.isRemoved()) return;
        this.time = 101;
        if(!this.srcPlayer.position().equals(this.lastPos)) {
            this.copyPosition(this.srcPlayer);
            this.setDeltaMovement(Vec3.ZERO);
            this.srcPlayer.connection.send(new ClientboundTeleportEntityPacket(this));
            this.lastPos = this.position();
        }
        //this.copyPosition(this.srcPlayer);

        /*if (!this.isStayed) this.setPos(this.srcPlayer.position());
        this.time = 590;
        if (!this.srcPlayer.blockPosition().equals(this.lastPos)) {
            this.isStayed = false;
            this.stayTick = 0L;
            this.level.setBlock(this.lastPos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            if (!this.isStayed) {
                this.isStayed = true;
            }
            if (this.stayTick <= 20L) this.stayTick++;
        }

        if (this.isStayed && stayTick == 20L) {
            BlockPos bp = new BlockPos(this.position());
            if (this.level.getBlockState(bp).getBlock() != Blocks.AIR) {
                this.srcPlayer.sendSystemMessage(MutableComponent.create(new LiteralContents("そこには固定できねえ！")), ChatType.SYSTEM);
            } else {
                //this.setPos(Vec3.atCenterOf(bp));
                this.setInvisible(true);
                this.level.setBlock(bp, this.getBlockState(), 3);
                this.srcPlayer.collides = false;
                this.srcPlayer.sendSystemMessage(MutableComponent.create(new LiteralContents("固定されました")), ChatType.SYSTEM);
            }
            this.stayTick++;
        }
        this.lastPos = this.srcPlayer.blockPosition();*/
        //super.tick();
    }
}
