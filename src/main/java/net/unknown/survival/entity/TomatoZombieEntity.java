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

package net.unknown.survival.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;

public class TomatoZombieEntity extends Zombie {
    private final MeleeAttackGoal attackGoal = new MeleeAttackGoal(this, 1.0, false);
    private final FireBallAttackGoal fireBallGoal = new FireBallAttackGoal(this);
    private final boolean monsters = false;

    public TomatoZombieEntity(Level world, boolean monsters) {
        super(EntityType.ZOMBIE, world);
    }

    public static void spawn(Location loc) {
        spawn(loc, false);
    }

    public static void spawn(Location loc, boolean monsters) {
        spawn(((CraftWorld) loc.getWorld()).getHandle(), new Vec3(loc.getX(), loc.getY(), loc.getZ()), new Vec2(loc.getPitch(), loc.getYaw()), monsters);
    }

    public static void spawn(ServerLevel level, Vec3 position, Vec2 rotation, boolean monsters) {
        TomatoZombieEntity entity = new TomatoZombieEntity(level, monsters);
        entity.setPos(position);
        entity.setRot(rotation.y, rotation.x);
        level.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    @Override
    protected void registerGoals() {
        // 近くの攻撃できそうなプレイヤー探す奴(ターゲット変更)
        if (monsters) this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        else this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // 攻撃されたらそいつを優先的に攻撃するようになる奴(ターゲット変更)
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers(ZombifiedPiglin.class));
        // 近くの攻撃できそうな村人探す奴
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        // 近くの攻撃できそうなアイアンゴーレム探す奴
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));

        // こうげき
        this.goalSelector.addGoal(4, new FireBallAttackGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        // あるくやつ(水避け)
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1));
        // 半径8ブロック以内のプレイヤーをランダムに見るやつ
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8));
        // ランダムにその辺のエンティティを見たりするやつ
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        LivingEntity livingEntity = this.getTarget();
        if (livingEntity != null && this.canAttack(livingEntity)) {
            this.hasImpulse = true;
        }

        super.customServerAiStep();
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return super.createNavigation(world);
    }

    @Override
    public boolean isSunSensitive() {
        return false;
    }

    static class MeleeAttackGoal extends ZombieAttackGoal {
        public MeleeAttackGoal(Zombie zombie, double speed, boolean pauseWhenMobIdle) {
            super(zombie, speed, pauseWhenMobIdle);
        }

        @Override
        public void tick() {
            if (this.mob.getTarget() != null) {
                double distance = this.mob.distanceToSqr(this.mob.getTarget());
                if (distance >= 4) {
                    this.mob.goalSelector.removeGoal(this);
                    this.mob.goalSelector.addGoal(1, ((TomatoZombieEntity) this.mob).fireBallGoal);
                }
            }
            super.tick();
        }
    }

    static class FireBallAttackGoal extends Goal {
        private final TomatoZombieEntity zombie;
        private int attackStep;
        private int attackTime;
        private int lastSeen;

        public FireBallAttackGoal(TomatoZombieEntity zombie) {
            this.zombie = zombie;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingEntity = this.zombie.getTarget();
            return livingEntity != null && livingEntity.isAlive() && this.zombie.canAttack(livingEntity);
        }

        @Override
        public void start() {
            this.attackStep = 0;
        }

        @Override
        public void stop() {
            this.lastSeen = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            --this.attackTime;
            LivingEntity attackTarget = this.zombie.getTarget();
            if (attackTarget != null) {
                boolean lineOfSight = this.zombie.getSensing().hasLineOfSight(attackTarget);
                if (lineOfSight) {
                    this.lastSeen = 0;
                } else {
                    ++this.lastSeen;
                }

                double distance = this.zombie.distanceToSqr(attackTarget);
                RunnableManager.runAsync(() -> {
                    if (this.zombie.getTarget() instanceof ServerPlayer player) {
                        player.getBukkitEntity().sendActionBar("§c§lゾンビに狙われている！ §r§b距離: " + this.zombie.distanceToSqr(player) + "m " + " §r| §d射線: " + (lineOfSight ? "§aクリア" : "§c障害物有"));
                    }
                });

                if (distance < 1.8) {
                    if (!lineOfSight) return;

                    if (this.attackTime <= 0) {
                        this.attackTime = 20;
                        this.zombie.doHurtTarget(attackTarget);
                    }
                } else if (distance < this.getFollowDistance() * this.getFollowDistance() && lineOfSight) {
                    double x = attackTarget.getX() - this.zombie.getX();
                    double y = attackTarget.getY(0.5D) - this.zombie.getY(0.5D);
                    double z = attackTarget.getZ() - this.zombie.getZ();
                    if (this.attackTime <= 0) {
                        ++this.attackStep;
                        if (this.attackStep == 1) {
                            this.attackTime = 20;
                        } else if (this.attackStep <= 4) {
                            this.attackTime = 6;
                        } else {
                            this.attackTime = 20;
                            this.attackStep = 0;
                        }

                        if (this.attackStep > 1) {
                            if (!this.zombie.isSilent()) {
                                this.zombie.level.levelEvent(null, 1018, this.zombie.blockPosition(), 0);
                            }

                            for (int i = 0; i < 1; ++i) {
                                SmallFireball smallFireball = new SmallFireball(this.zombie.level, this.zombie, x + (this.zombie.getRandom().nextGaussian() / 2), y, z + (this.zombie.getRandom().nextGaussian() / 2));
                                smallFireball.setPos(smallFireball.getX(), this.zombie.getY(0.5D) + 0.5D, smallFireball.getZ());
                                this.zombie.level.addFreshEntity(smallFireball);
                            }
                        }
                    }

                    this.zombie.getLookControl().setLookAt(attackTarget, 10.0F, 10.0F);
                }

                if (distance > 1)
                    this.zombie.getMoveControl().setWantedPosition(attackTarget.getX(), attackTarget.getY(), attackTarget.getZ(), 1.0D);
            }
        }

        private double getFollowDistance() {
            return this.zombie.getAttributeValue(Attributes.FOLLOW_RANGE);
        }
    }
}
