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
import net.minecraft.tags.EntityTypeTags;
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
        // Paper start - PlayerAttackEntityEvent
        boolean willAttack = target.isAttackable() && !target.skipAttackInteraction(this); // Vanilla logic
        io.papermc.paper.event.player.PrePlayerAttackEntityEvent playerAttackEntityEvent = new io.papermc.paper.event.player.PrePlayerAttackEntityEvent(
                this.getBukkitEntity(),
                target.getBukkitEntity(),
                willAttack
        );

        if (playerAttackEntityEvent.callEvent() && willAttack) { // Logic moved to willAttack local variable.
            {
                // Paper end - PlayerAttackEntityEvent
                float damage = this.isAutoSpinAttack() ? this.autoSpinAttackDmg : (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                ItemStack weapon = this.getWeaponItem();
                DamageSource damageSource = this.damageSources().playerAttack(this);
                float enchantedDamage = this.getEnchantedDamage(target, damage, damageSource) - damage;
                float scaledDamage = this.getAttackStrengthScale(0.5F);

                damage *= 0.2F + scaledDamage * scaledDamage * 0.8F;
                enchantedDamage *= scaledDamage;
                // this.resetAttackStrengthTicker(); // CraftBukkit - Moved to EntityLiving to reset the cooldown after the damage is dealt
                if (target.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && target instanceof Projectile projectile) {
                    // CraftBukkit start
                    if (CraftEventFactory.handleNonLivingEntityDamageEvent(target, damageSource, enchantedDamage, false)) {
                        return;
                    }
                    // CraftBukkit end
                    if (projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this, this, true)) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource());
                        return;
                    }
                }

                if (damage > 0.0F || enchantedDamage > 0.0F) {
                    boolean fullyCharged = scaledDamage > 0.9F;
                    boolean fullyKnockback;

                    if (this.isSprinting() && fullyCharged) {
                        // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility // Unknown Network - Its fake player, do not sent sound!
                        fullyKnockback = true;
                    } else {
                        fullyKnockback = false;
                    }

                    damage += weapon.getItem().getAttackDamageBonus(target, damage, damageSource);
                    boolean isCriticalDamage = fullyCharged && this.fallDistance > 0.0F && !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && target instanceof LivingEntity && !this.isSprinting();

                    isCriticalDamage = isCriticalDamage && !this.level().paperConfig().entities.behavior.disablePlayerCrits; // Paper - Toggleable player crits
                    if (isCriticalDamage) {
                        damageSource = damageSource.critical(true); // Paper start - critical damage API
                        damage *= 1.5F;
                    }

                    float finallyDamage = damage + enchantedDamage;
                    boolean isSword = false;
                    double d0 = this.walkDist - this.walkDistO;

                    if (fullyCharged && !isCriticalDamage && !fullyKnockback && this.onGround() && d0 < (double) this.getSpeed()) {
                        ItemStack mainHandItem = this.getItemInHand(InteractionHand.MAIN_HAND);

                        if (mainHandItem.getItem() instanceof SwordItem) {
                            isSword = true;
                        }
                    }

                    float targetHealth = 0.0F;

                    if (target instanceof LivingEntity livingEntity) {
                        targetHealth = livingEntity.getHealth();
                    }

                    Vec3 vec3d = target.getDeltaMovement();
                    boolean damaged = target.hurt(damageSource, finallyDamage);

                    if (damaged) {
                        float knockBackStrength = this.getKnockback(target, damageSource) + (fullyKnockback ? 1.0F : 0.0F);

                        if (knockBackStrength > 0.0F) {
                            if (target instanceof LivingEntity livingEntity) {
                                livingEntity.knockback(knockBackStrength * 0.5F, Mth.sin(this.getYRot() * 0.017453292F), -Mth.cos(this.getYRot() * 0.017453292F), this, io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.ENTITY_ATTACK); // Paper - knockback events
                            } else {
                                target.push(-Mth.sin(this.getYRot() * 0.017453292F) * knockBackStrength * 0.5F, 0.1D, Mth.cos(this.getYRot() * 0.017453292F) * knockBackStrength * 0.5F, this); // Paper - Add EntityKnockbackByEntityEvent and EntityPushedByEntityAttackEvent
                            }

                            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
                            // Paper start - Configurable sprint interruption on attack
                            if (!this.level().paperConfig().misc.disableSprintInterruptionOnAttack) {
                                this.setSprinting(false);
                            }
                            // Paper end - Configurable sprint interruption on attack
                        }

                        LivingEntity livingEntity;

                        if (isSword) {
                            float f6 = 1.0F + (float) this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * damage;
                            List<LivingEntity> sweepDamageHitLivingEntities = this.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D));

                            for (LivingEntity sweepHitLivingEntity : sweepDamageHitLivingEntities) {
                                livingEntity = sweepHitLivingEntity;
                                if (livingEntity != this && livingEntity != target && !this.isAlliedTo(livingEntity) && (!(livingEntity instanceof ArmorStand) || !((ArmorStand) livingEntity).isMarker()) && this.distanceToSqr(livingEntity) < 9.0D) {
                                    float sweepDamage = this.getEnchantedDamage(livingEntity, f6, damageSource) * scaledDamage;

                                    // CraftBukkit start - Only apply knockback if the damage hits
                                    if (livingEntity.hurt(this.damageSources().playerAttack(this).sweep().critical(isCriticalDamage), sweepDamage)) { // Paper - add critical damage API
                                        livingEntity.knockback(0.4000000059604645D, Mth.sin(this.getYRot() * 0.017453292F), -Mth.cos(this.getYRot() * 0.017453292F), this, io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.SWEEP_ATTACK); // CraftBukkit // Paper - knockback events
                                    }
                                    // CraftBukkit end
                                    Level level = this.level();

                                    if (level instanceof ServerLevel serverLevel) {
                                        EnchantmentHelper.doPostAttackEffects(serverLevel, livingEntity, damageSource);
                                    }
                                }
                            }

                            // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility // Unknown Network - Its fake player, do not sent sound!
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

                        if (isCriticalDamage) {
                            // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility // Unknown Network - Its fake player, do not sent sound!
                            this.crit(target);
                        }

                        if (!isCriticalDamage && !isSword) {
                            if (fullyCharged) {
                                // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility // Unknown Network - Its fake player, do not sent sound!
                            } else {
                                // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility // Unknown Network - Its fake player, do not sent sound!
                            }
                        }

                        if (enchantedDamage > 0.0F) {
                            this.magicCrit(target);
                        }

                        this.setLastHurtMob(target);
                        Object object = target;

                        if (target instanceof EnderDragonPart) {
                            object = ((EnderDragonPart) target).parentMob;
                        }

                        boolean specialEntityDamaged = false;
                        Level level = this.level();

                        if (level instanceof ServerLevel serverLevel) {
                            if (object instanceof LivingEntity) {
                                livingEntity = (LivingEntity) object;
                                specialEntityDamaged = weapon.hurtEnemy(livingEntity, this);
                            }

                            EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
                        }

                        if (!this.level().isClientSide && !weapon.isEmpty() && object instanceof LivingEntity) {
                            if (specialEntityDamaged) {
                                weapon.postHurtEnemy((LivingEntity) object, this);
                            }

                            if (weapon.isEmpty()) {
                                if (weapon == this.getMainHandItem()) {
                                    this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                                } else {
                                    this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                                }
                            }
                        }

                        if (target instanceof LivingEntity) {
                            float damageDeal = targetHealth - ((LivingEntity) target).getHealth();

                            this.awardStat(Stats.DAMAGE_DEALT, Math.round(damageDeal * 10.0F));
                            if (this.level() instanceof ServerLevel && damageDeal > 2.0F) {
                                int damageIndicatorParticleCount = (int) ((double) damageDeal * 0.5D);

                                ((ServerLevel) this.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5D), target.getZ(), damageIndicatorParticleCount, 0.1D, 0.0D, 0.1D, 0.2D);
                            }
                        }

                        this.causeFoodExhaustion(this.level().spigotConfig.combatExhaustion, EntityExhaustionEvent.ExhaustionReason.ATTACK); // CraftBukkit - EntityExhaustionEvent // Spigot - Change to use configurable value
                    } else {
                        // sendSoundEffect(this, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F); // Paper - send while respecting visibility // Unknown Network - Its fake player, do not sent sound!
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
