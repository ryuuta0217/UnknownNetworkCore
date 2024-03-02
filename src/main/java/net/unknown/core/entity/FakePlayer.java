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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.ObfuscationUtil;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftVector;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
        return super.getAttackStrengthScale(baseTime);
        //return 1.0F; // Always returns 1.0F (charged)
    }

    @Override
    public void attack(Entity target) {
        this.detectEquipmentUpdatesPublic();

        // Paper start - PlayerAttackEntityEvent
        boolean willAttack = target.isAttackable() && !target.skipAttackInteraction(this); // Vanilla logic
        io.papermc.paper.event.player.PrePlayerAttackEntityEvent playerAttackEntityEvent = new io.papermc.paper.event.player.PrePlayerAttackEntityEvent(
                this.getBukkitEntity(),
                target.getBukkitEntity(),
                willAttack
        );

        if (playerAttackEntityEvent.callEvent() && willAttack) { // Logic moved to willAttack local variable.
            {
                // Paper end
                float attackDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float damageBonus;

                if (target instanceof LivingEntity) {
                    damageBonus = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity) target).getMobType());
                } else {
                    damageBonus = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), MobType.UNDEFINED);
                }

                float strengthScale = this.getAttackStrengthScale(0.5F); // TODO: attackStrengthTicker どうするか (FakePlayerは常に0になる, 普通のプレイヤーは2-3になったりする)

                attackDamage *= 0.2F + strengthScale * strengthScale * 0.8F;
                damageBonus *= strengthScale;
                // this.resetAttackCooldown(); // CraftBukkit - Moved to EntityLiving to reset the cooldown after the damage is dealt
                if (attackDamage > 0.0F || damageBonus > 0.0F) {
                    boolean charged = strengthScale > 0.9F;
                    boolean criticalKnockback = false;
                    byte b0 = 0;
                    int knockback = b0 + EnchantmentHelper.getKnockbackBonus(this);

                    if (this.isSprinting() && charged) {
                        //sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility
                        ++knockback;
                        criticalKnockback = true;
                    }

                    boolean critical = true;// charged && this.fallDistance > 0.0F && !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && target instanceof LivingEntity; // Paper - Add critical damage API - conflict on change

                    // critical = critical && !this.level().paperConfig().entities.behavior.disablePlayerCrits; // Paper // Disable - this is not a player
                    // critical = critical && !this.isSprinting(); // Disable - this is not a player, always critical.
                    if (critical) {
                        attackDamage *= 1.5F;
                    }

                    attackDamage += damageBonus;
                    boolean flag3 = false;
                    double d0 = (double) (this.walkDist - this.walkDistO);

                    if (charged && !critical && !criticalKnockback && this.onGround() && d0 < (double) this.getSpeed()) {
                        ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);

                        if (itemstack.getItem() instanceof SwordItem) {
                            flag3 = true;
                        }
                    }

                    float f3 = 0.0F;
                    boolean flag4 = false;
                    int j = EnchantmentHelper.getFireAspect(this);

                    if (target instanceof LivingEntity) {
                        f3 = ((LivingEntity) target).getHealth();
                        if (j > 0 && !target.isOnFire()) {
                            // CraftBukkit start - Call a combust event when somebody hits with a fire enchanted item
                            EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), target.getBukkitEntity(), 1);
                            org.bukkit.Bukkit.getPluginManager().callEvent(combustEvent);

                            if (!combustEvent.isCancelled()) {
                                flag4 = true;
                                target.setSecondsOnFire(combustEvent.getDuration(), false);
                            }
                            // CraftBukkit end
                        }
                    }

                    Vec3 vec3d = target.getDeltaMovement();
                    boolean damaged = target.hurt(this.damageSources().playerAttack(this).critical(critical), attackDamage); // Paper - add critical damage API

                    if (damaged) {
                        if (knockback > 0) {
                            if (target instanceof LivingEntity) {
                                ((LivingEntity) target).knockback((double) ((float) knockback * 0.5F), (double) Mth.sin(this.getYRot() * 0.017453292F), (double) (-Mth.cos(this.getYRot() * 0.017453292F)), this); // Paper
                            } else {
                                target.push((double) (-Mth.sin(this.getYRot() * 0.017453292F) * (float) knockback * 0.5F), 0.1D, (double) (Mth.cos(this.getYRot() * 0.017453292F) * (float) knockback * 0.5F), this); // Paper
                            }

                            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
                            // Paper start - Configuration option to disable automatic sprint interruption
                            if (!this.level().paperConfig().misc.disableSprintInterruptionOnAttack) {
                                this.setSprinting(false);
                            }
                            // Paper end
                        }

                        if (flag3) {
                            float f4 = 1.0F + EnchantmentHelper.getSweepingDamageRatio(this) * attackDamage;
                            List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D));

                            for (LivingEntity entityliving : list) {
                                if (entityliving != this && entityliving != target && !this.isAlliedTo((Entity) entityliving) && (!(entityliving instanceof ArmorStand) || !((ArmorStand) entityliving).isMarker()) && this.distanceToSqr((Entity) entityliving) < 9.0D) {
                                    // CraftBukkit start - Only apply knockback if the damage hits
                                    if (entityliving.hurt(this.damageSources().playerAttack(this).sweep().critical(critical), f4)) { // Paper - add critical damage API
                                        entityliving.knockback(0.4000000059604645D, (double) Mth.sin(this.getYRot() * 0.017453292F), (double) (-Mth.cos(this.getYRot() * 0.017453292F)), this); // Pa
                                    }
                                    // CraftBukkit end
                                }
                            }

                            //sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility
                            this.sweepAttack();
                        }

                        if (target instanceof ServerPlayer && target.hurtMarked) {
                            // CraftBukkit start - Add Velocity Event
                            boolean cancelled = false;
                            org.bukkit.entity.Player player = (org.bukkit.entity.Player) target.getBukkitEntity();
                            org.bukkit.util.Vector velocity = CraftVector.toBukkit(vec3d);

                            PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
                            this.level().getCraftServer().getPluginManager().callEvent(event);

                            if (event.isCancelled()) {
                                cancelled = true;
                            } else if (!velocity.equals(event.getVelocity())) {
                                player.setVelocity(event.getVelocity());
                            }

                            if (!cancelled) {
                                ((ServerPlayer) target).connection.send(new ClientboundSetEntityMotionPacket(target));
                                target.hurtMarked = false;
                                target.setDeltaMovement(vec3d);
                            }
                            // CraftBukkit end
                        }

                        if (critical) {
                            //sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility
                            this.crit(target);
                        }

                        if (!critical && !flag3) {
                            if (charged) {
                                //sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility
                            } else {
                                //sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility
                            }
                        }

                        if (damageBonus > 0.0F) {
                            this.magicCrit(target);
                        }

                        this.setLastHurtMob(target);
                        if (target instanceof LivingEntity) {
                            EnchantmentHelper.doPostHurtEffects((LivingEntity) target, this);
                        }

                        EnchantmentHelper.doPostDamageEffects(this, target);
                        ItemStack itemstack1 = this.getMainHandItem();
                        Object object = target;

                        if (target instanceof EnderDragonPart) {
                            object = ((EnderDragonPart) target).parentMob;
                        }

                        if (!this.level().isClientSide && !itemstack1.isEmpty() && object instanceof LivingEntity) {
                            itemstack1.hurtEnemy((LivingEntity) object, this);
                            if (itemstack1.isEmpty()) {
                                this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            }
                        }

                        if (target instanceof LivingEntity) {
                            float f5 = f3 - ((LivingEntity) target).getHealth();

                            this.awardStat(Stats.DAMAGE_DEALT, Math.round(f5 * 10.0F));
                            if (j > 0) {
                                // CraftBukkit start - Call a combust event when somebody hits with a fire enchanted item
                                EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), target.getBukkitEntity(), j * 4);
                                org.bukkit.Bukkit.getPluginManager().callEvent(combustEvent);

                                if (!combustEvent.isCancelled()) {
                                    target.setSecondsOnFire(combustEvent.getDuration(), false);
                                }
                                // CraftBukkit end
                            }

                            if (this.level() instanceof ServerLevel && f5 > 2.0F) {
                                int k = (int) ((double) f5 * 0.5D);

                                ((ServerLevel) this.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5D), target.getZ(), k, 0.1D, 0.0D, 0.1D, 0.2D);
                            }
                        }

                        this.causeFoodExhaustion(this.level().spigotConfig.combatExhaustion, EntityExhaustionEvent.ExhaustionReason.ATTACK); // CraftBukkit - EntityExhaustionEvent // Spigot - Change to use configurable value
                    } else {
                        //sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility
                        if (flag4) {
                            target.clearFire();
                        }
                        // CraftBukkit start - resync on cancelled event
                        if (this instanceof ServerPlayer) {
                            this.getBukkitEntity().updateInventory();
                        }
                        // CraftBukkit end
                    }
                }

            }
        }
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
            super(player.getServer(), new Connection(PacketFlow.SERVERBOUND) {
                @Override
                public void setListener(PacketListener packetListener) {
                    // Ignored all
                }
            }, player, new CommonListenerCookie(player.gameProfile, 0, player.clientInformation()));
        }

        @Override
        public void send(Packet<?> packet) {
            // Ignored
        }

        @Override
        public void send(Packet<?> packet, @org.jetbrains.annotations.Nullable PacketSendListener callbacks) {
            // Ignored
        }
    }
}
