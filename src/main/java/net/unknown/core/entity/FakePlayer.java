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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
    public void attack(Entity target) {
        boolean willAttack = target.isAttackable() && !target.skipAttackInteraction(this);
        PrePlayerAttackEntityEvent playerAttackEntityEvent = new PrePlayerAttackEntityEvent(this.getBukkitEntity(), target.getBukkitEntity(), willAttack);
        if (playerAttackEntityEvent.callEvent() && willAttack) {
            float f = this.isAutoSpinAttack() ? super.autoSpinAttackDmg : (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            ItemStack weaponItem = this.getWeaponItem();
            DamageSource damageSource = Optional.ofNullable(weaponItem.getItem().getDamageSource(this)).orElse(this.damageSources().playerAttack(this));
            float f1 = this.getEnchantedDamage(target, f, damageSource) - f;
            float attackStrengthScale = this.getAttackStrengthScale(0.5F);
            f *= 0.2F + attackStrengthScale * attackStrengthScale * 0.8F;
            f1 *= attackStrengthScale;
            if (target.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && target instanceof Projectile) {
                Projectile projectile = (Projectile)target;
                if (CraftEventFactory.handleNonLivingEntityDamageEvent(target, damageSource, (double)f1, false)) {
                    return;
                }

                if (projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this, this, true)) {
                    this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource());
                    return;
                }
            }

            if (f > 0.0F || f1 > 0.0F) {
                boolean flag = attackStrengthScale > 0.9F;
                boolean flag1;
                if (this.isSprinting() && flag) {
                    // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                    flag1 = true;
                } else {
                    flag1 = false;
                }

                f += weaponItem.getItem().getAttackDamageBonus(target, f, damageSource);
                boolean flag2 = flag && super.fallDistance > 0.0F && !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && target instanceof LivingEntity && !this.isSprinting();
                flag2 = flag2 && !this.level().paperConfig().entities.behavior.disablePlayerCrits;
                if (flag2) {
                    damageSource = damageSource.critical();
                    f *= 1.5F;
                }

                float f2 = f + f1;
                boolean flag3 = false;
                if (flag && !flag2 && !flag1 && this.onGround()) {
                    double d = this.getKnownMovement().horizontalDistanceSqr();
                    double d1 = (double)this.getSpeed() * (double)2.5F;
                    if (d < Mth.square(d1) && this.getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS)) {
                        flag3 = true;
                    }
                }

                float f3 = 0.0F;
                if (target instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity)target;
                    f3 = livingEntity.getHealth();
                }

                Vec3 deltaMovement = target.getDeltaMovement();
                boolean flag4 = target.hurtOrSimulate(damageSource, f2);
                if (flag4) {
                    float f4 = this.getKnockback(target, damageSource) + (flag1 ? 1.0F : 0.0F);
                    if (f4 > 0.0F) {
                        if (target instanceof LivingEntity) {
                            LivingEntity livingEntity1 = (LivingEntity)target;
                            livingEntity1.knockback((double)(f4 * 0.5F), (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))), this, io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.ENTITY_ATTACK);
                        } else {
                            target.push((double)(-Mth.sin(this.getYRot() * ((float)Math.PI / 180F)) * f4 * 0.5F), 0.1, (double)(Mth.cos(this.getYRot() * ((float)Math.PI / 180F)) * f4 * 0.5F), this);
                        }

                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, (double)1.0F, 0.6));
                        if (!this.level().paperConfig().misc.disableSprintInterruptionOnAttack) {
                            this.setSprinting(false);
                        }
                    }

                    if (flag3) {
                        float f5 = 1.0F + (float)this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * f;

                        for(LivingEntity livingEntity2 : this.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate((double)1.0F, (double)0.25F, (double)1.0F))) {
                            if (livingEntity2 != this && livingEntity2 != target && !this.isAlliedTo(livingEntity2) && (!(livingEntity2 instanceof ArmorStand) || !((ArmorStand)livingEntity2).isMarker()) && this.distanceToSqr(livingEntity2) < (double)9.0F) {
                                float f6 = this.getEnchantedDamage(livingEntity2, f5, damageSource) * attackStrengthScale;
                                if (livingEntity2.hurtServer((ServerLevel)this.level(), this.damageSources().playerAttack(this).critical(), f6)) {
                                    livingEntity2.knockback((double)0.4F, (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))), this, io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.SWEEP_ATTACK);
                                    Level var23 = this.level();
                                    if (var23 instanceof ServerLevel) {
                                        ServerLevel serverLevel = (ServerLevel)var23;
                                        EnchantmentHelper.doPostAttackEffects(serverLevel, livingEntity2, damageSource);
                                    }
                                }
                            }
                        }

                        // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                        this.sweepAttack();
                    }

                    if (target instanceof ServerPlayer && target.hurtMarked) {
                        boolean cancelled = false;
                        org.bukkit.entity.Player player = (org.bukkit.entity.Player)target.getBukkitEntity();
                        Vector velocity = CraftVector.toBukkit(deltaMovement);
                        PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
                        this.level().getCraftServer().getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            cancelled = true;
                        } else if (!velocity.equals(event.getVelocity())) {
                            player.setVelocity(event.getVelocity());
                        }

                        if (!cancelled) {
                            ((ServerPlayer)target).connection.send(new ClientboundSetEntityMotionPacket(target));
                            target.hurtMarked = false;
                            target.setDeltaMovement(deltaMovement);
                        }
                    }

                    if (flag2) {
                        // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                        this.crit(target);
                    }

                    if (!flag2 && !flag3) {
                        if (flag) {
                            // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F);
                        } else {
                            // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F);
                        }
                    }

                    if (f1 > 0.0F) {
                        this.magicCrit(target);
                    }

                    this.setLastHurtMob(target);
                    Entity entity = target;
                    if (target instanceof EnderDragonPart) {
                        entity = ((EnderDragonPart)target).parentMob;
                    }

                    boolean flag5 = false;
                    Level var41 = this.level();
                    if (var41 instanceof ServerLevel) {
                        ServerLevel serverLevel1 = (ServerLevel)var41;
                        if (entity instanceof LivingEntity) {
                            LivingEntity livingEntity2x = (LivingEntity)entity;
                            flag5 = weaponItem.hurtEnemy(livingEntity2x, this);
                        }

                        EnchantmentHelper.doPostAttackEffects(serverLevel1, target, damageSource);
                    }

                    if (!this.level().isClientSide && !weaponItem.isEmpty() && entity instanceof LivingEntity) {
                        if (flag5) {
                            weaponItem.postHurtEnemy((LivingEntity)entity, this);
                        }

                        if (weaponItem.isEmpty()) {
                            if (weaponItem == this.getMainHandItem()) {
                                this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            } else {
                                this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                            }
                        }
                    }

                    if (target instanceof LivingEntity) {
                        float f7 = f3 - ((LivingEntity)target).getHealth();
                        this.awardStat(Stats.DAMAGE_DEALT, Math.round(f7 * 10.0F));
                        if (this.level() instanceof ServerLevel && f7 > 2.0F) {
                            int i = (int)((double)f7 * (double)0.5F);
                            ((ServerLevel)this.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY((double)0.5F), target.getZ(), i, 0.1, (double)0.0F, 0.1, 0.2);
                        }
                    }

                    this.causeFoodExhaustion(this.level().spigotConfig.combatExhaustion, EntityExhaustionEvent.ExhaustionReason.ATTACK);
                } else {
                    // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                    if (this instanceof ServerPlayer) {
                        ((ServerPlayer)this).getBukkitEntity().updateInventory();
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
                public void setListenerForServerboundHandshake(PacketListener packetListener) {
                    // Ignored all
                }
            }, player, new CommonListenerCookie(player.getGameProfile(), 0, ClientInformation.createDefault(), false));
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
