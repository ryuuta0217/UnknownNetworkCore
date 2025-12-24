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

package net.unknown.core.entity;

import com.mojang.authlib.GameProfile;
import com.ryuuta0217.util.MojangApi;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import io.papermc.paper.util.KeepAlive;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.ObfuscationUtil;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

public class FakePlayer extends ServerPlayer {
    /**
     * Create a Fake Player (not real player), ClientOptions will be default.
     *
     * @param level Player's level (world)
     * @param name Player's name
     * @param uniqueId Player's UUID (if null, random UUID will be generated)
     */
    public FakePlayer(ServerLevel level, String name, @Nullable UUID uniqueId) {
        this(level, name, uniqueId, ClientInformation.createDefault());
    }

    /**
     * Create a Fake Player (not real player)
     *
     * @param level Player's level (world)
     * @param name Player's name
     * @param uniqueId Player's UUID (if null, random UUID will be generated)
     * @param clientOptions Client options
     */
    public FakePlayer(ServerLevel level, String name, @Nullable UUID uniqueId, ClientInformation clientOptions) {
        super(level.getServer(), level, createFakeProfile(name, uniqueId == null ? UUID.randomUUID() : uniqueId, false), clientOptions);
        this.isRealPlayer = false;
        new ServerGamePacketListenerImpl(this);
        this.getAdvancements().stopListening();
    }

    @Override
    public float getAttackStrengthScale(float baseTime) {
        // 通常、strengthScaleは以下の計算式で計算される:
        // Mth.clamp((this.attackStrengthTicker + baseTime) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
        // attackStrengthTicker は、tick() で常に加算され続け、以下の場合に0にリセットされる:
        // ・プレイヤーがアイテムを持ち替えた場合。
        // ・プレイヤーが右クリックを行った場合。(swingHand)
        // baseTime は通常、0.5F が渡される。
        // getCurrentItemAttackStrengthDelay() は以下の計算式で計算される:
        // (float) (1.0D / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0D);
        // つまり、FakePlayerにおいては、attackStrengthTickerが（ほとんどの場合）常に0であるため、strengthScaleは0.2程度に収まってしまう。
        // そのため、FakePlayerにおいては、strengthScaleを1.0に固定する。
        return 1.0F;
    }

    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, boolean silent) {
        super.onEquipItem(slot, oldItem, newItem, silent);
        this.detectEquipmentUpdates();
    }

    public static GameProfile createFakeProfile(String name, UUID uniqueId, boolean validate) {
        GameProfile profile = new GameProfile(uniqueId, name);
        if (validate) {
            boolean isValid = true;

            try {
                MojangApi.getUUID(name);
            } catch(MojangApi.MojangException e) {
                isValid = false;
            }

            try {
                MojangApi.getName(uniqueId);
            } catch(MojangApi.MojangException e) {
                isValid = false;
            }

            if (!isValid) throw new IllegalArgumentException("The provided name or uuid is already used by official Minecraft player! use other or set validate to false to proceed.");
        }
        return profile;
    }

    public static class ServerGamePacketListenerImpl extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        public ServerGamePacketListenerImpl(FakePlayer player) {
            super(MinecraftServer.getServer(), new net.minecraft.network.Connection(PacketFlow.SERVERBOUND) {
                @Override
                public void setListenerForServerboundHandshake(PacketListener packetListener) {
                    // Ignored all
                }
            }, player, new CommonListenerCookie(player.getGameProfile(), 0, ClientInformation.createDefault(), false, null, Collections.emptySet(), new KeepAlive()));
        }

        @Override
        public void send(Packet<?> packet) {
            // Ignored
        }
    }
}
