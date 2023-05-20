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

package net.unknown.survival.fun;

import net.unknown.UnknownNetworkCore;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PathfinderGrapple implements Listener {
    public static final Map<UUID, Color> CUSTOM_DRAW_LINE_COLOR = new HashMap<>();

    @EventHandler
    public void onEntityBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        ItemStack bow = event.getBow();
        if (bow == null || !bow.getItemMeta().getDisplayName().equals("§6§lパスくんのグラップル")) return;

        if (!player.hasPermission("unknown.survival.feature.pathfinder_grapple")) return;
        arrow.setVelocity(arrow.getVelocity().multiply(2.0D));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || !arrow.getLocation().getWorld().equals(player.getLocation().getWorld())) {
                    arrow.remove();
                    this.cancel();
                }

                if (arrow.getLocation().distance(player.getLocation()) > 50) {
                    arrow.remove();
                    this.cancel();
                }

                if (player.isSneaking()) {
                    player.setVelocity(player.getLocation().getDirection().multiply(arrow.getDamage()));
                    arrow.remove();
                    this.cancel();
                }

                if (arrow.isOnGround() && !arrow.isDead()) {
                    Vector direction = arrow.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                    player.setVelocity(direction.multiply(2));
                    drawLine(player, arrow.getLocation(), 2);
                    if (player.getLocation().distance(arrow.getLocation()) <= 3) {
                        this.cancel();
                        arrow.remove();
                    }
                }
            }
        }.runTaskTimer(UnknownNetworkCore.getInstance(), 0, 0);
    }

    private void drawLine(Player player, Location point2, double space) {
        Location point1 = player.getLocation();
        World world = point1.getWorld();

        double distance = point1.distance(point2);

        Vector p1 = point1.toVector();
        Vector p2 = point2.toVector();

        Vector vector = p2.clone().subtract(p1).normalize().multiply(space);

        double covered = 0;

        Color color = CUSTOM_DRAW_LINE_COLOR.getOrDefault(player.getUniqueId(), Color.ORANGE);

        for (; covered < distance; p1.add(vector)) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 2);
            world.spawnParticle(Particle.REDSTONE, p1.getX(), p1.getY(), p1.getZ(), 2, dustOptions);
            covered += space;
        }
    }
}
