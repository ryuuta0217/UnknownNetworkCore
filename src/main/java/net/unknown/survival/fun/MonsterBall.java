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

package net.unknown.survival.fun;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

public class MonsterBall implements Listener {
    @EventHandler
    public void onEggHit(ProjectileHitEvent event) {
        if (((CraftEntity) event.getEntity()).getHandle() instanceof ThrownEgg egg) {
            if (event.getHitEntity() != null) {
                if (((CraftEntity) event.getHitEntity()).getHandle() instanceof Mob mob) {
                    if (SpawnEggItem.BY_ID.containsKey(mob.getType())) {
                        CompoundTag entityTag = new CompoundTag();
                        mob.save(entityTag);
                        mob.remove(Entity.RemovalReason.DISCARDED);
                        entityTag.remove("Pos");
                        entityTag.remove("Motion");
                        entityTag.remove("Rotation");

                        ItemStack spawnEgg = new ItemStack(SpawnEggItem.BY_ID.get(mob.getType()));
                        CompoundTag spawnEggTag = new CompoundTag();
                        spawnEggTag.put("EntityTag", entityTag);
                        spawnEgg.setTag(spawnEggTag);

                        ItemEntity e = new ItemEntity(mob.getLevel(), mob.getX(), mob.getY(), mob.getZ(), spawnEgg);
                        mob.getLevel().addFreshEntity(e, CreatureSpawnEvent.SpawnReason.EGG);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
