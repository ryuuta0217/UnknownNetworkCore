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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jetbrains.annotations.Nullable;

public class CustomZombieEntity extends Zombie implements RangedAttackMob {
    private final RangedBowAttackGoal<CustomZombieEntity> bowGoal = new RangedBowAttackGoal<>(this, 1.0D, 20, 15.0F);
    private final ZombieAttackGoal meleeGoal = new ZombieAttackGoal(this, 1.0, false);

    public CustomZombieEntity(Level world) {
        super(EntityType.ZOMBIE, world);
        this.reassessWeaponGoal();
    }

    public static void spawn(Location loc) {
        spawn(((CraftWorld) loc.getWorld()).getHandle(), new Vec3(loc.getX(), loc.getY(), loc.getZ()), new Vec2(loc.getPitch(), loc.getYaw()));
    }

    public static void spawn(ServerLevel level, Vec3 position, Vec2 rotation) {
        CustomZombieEntity entity = new CustomZombieEntity(level);
        entity.setPos(position);
        entity.setRot(rotation.y, rotation.x);
        level.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    @Override
    protected void registerGoals() {
        // 攻撃されたらそいつを優先的に攻撃するようになる奴(ターゲット変更)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(ZombifiedPiglin.class));
        // 近くの攻撃できそうなプレイヤー探す奴(ターゲット変更)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // 近くの攻撃できそうな村人探す奴
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        // 近くの攻撃できそうなアイアンゴーレム探す奴
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));

        // あるくやつ(水避け)
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1));
        // 半径8ブロック以内のプレイヤーをランダムに見るやつ
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8));
        // ランダムにその辺のエンティティを見たりするやつ
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public void reassessWeaponGoal() {
        if (!this.level.isClientSide) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);

            ItemStack weapon = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));

            if (weapon.is(Items.BOW)) {
                this.goalSelector.addGoal(1, this.bowGoal);
            } else {
                this.goalSelector.addGoal(1, this.meleeGoal);
            }
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        if (!this.level.isClientSide) this.reassessWeaponGoal();
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (!this.level.isClientSide) {
            this.reassessWeaponGoal();
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!this.level.isClientSide) {
            this.reassessWeaponGoal();
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        ItemStack bow = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW)));
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, bow, pullProgress);
        double x = target.getX() - this.getX();
        double y = target.getY(0.3333333333333333D) - arrow.getY();
        double z = target.getZ() - this.getZ();
        double f = Math.sqrt(x * x + z * z);

        arrow.shoot(x, y + f * 0.20000000298023224D, z, 1.6F, (float) (14 - this.level.getDifficulty().getId() * 4));
        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent(this, this.getMainHandItem(), arrow.getPickupItem(), arrow, net.minecraft.world.InteractionHand.MAIN_HAND, 0.8F, true); // Paper
        if (event.isCancelled()) {
            event.getProjectile().remove();
            return;
        }

        if (event.getProjectile() == arrow.getBukkitEntity()) {
            level.addFreshEntity(arrow);
        }
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public boolean isSunSensitive() {
        return false;
    }
}
