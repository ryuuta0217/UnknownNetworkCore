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

package net.unknown.survival.dependency;

import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.wepif.PermissionsProvider;
import com.sk89q.wepif.PermissionsResolver;
import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.wepif.VaultResolver;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.permission.Permission;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class WorldGuard {
    public static final String SPLITTER = "_";
    public static final Pattern ID_PATTERN = Pattern.compile("(?i)^[\\dA-F]{8}-[\\dA-F]{4}-4[\\dA-F]{3}-[89AB][\\dA-F]{3}-[\\dA-F]{12}_.*");

    public static WorldGuardPlatform getPlatform() {
        return com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
    }

    public static RegionManager getRegionManager(World world) {
        return getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }

    public static List<WrappedProtectedRegion> getProtectedRegions(Player owner) {
        List<WrappedProtectedRegion> regions = new ArrayList<>();

        Bukkit.getWorlds().forEach(world -> {
            RegionManager regionManager = getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                regionManager.getRegions().forEach((name, region) -> {
                    if (region.getOwners().contains(owner.getUniqueId()) || region.getId().startsWith(owner.getUniqueId().toString()))
                        regions.add(new WrappedProtectedRegion(world, region));
                });
            }
        });

        return regions.stream()
                .sorted(Comparator.comparing(WrappedProtectedRegion::getId))
                .toList();
    }

    public record WrappedProtectedRegion(World world, ProtectedRegion region) {
        public UUID getCreatorUniqueId() {
            if(ID_PATTERN.matcher(this.getFullId()).matches()) {
                return UUID.fromString(this.getFullId().split(SPLITTER, 2)[0]);
            }
            return null;
        }

        public String getFullId() {
            return this.region.getId();
        }

        public String getId() {
            if(ID_PATTERN.matcher(this.getFullId()).matches()) {
                return this.getFullId().split(SPLITTER, 2)[1];
            }
            return this.getFullId();
        }

        public RegionManager regionManager() {
            return WorldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(this.world));
        }
    }

    public static class PermissionsResolver implements com.sk89q.wepif.PermissionsResolver {
        private static Permission perms = null;

        public static void inject() {
            try {
                Field f = PermissionsResolverManager.class.getDeclaredField("enabledResolvers");
                if(f.trySetAccessible()) {
                    List<Class<? extends com.sk89q.wepif.PermissionsResolver>> enabledResolvers = (List<Class<? extends com.sk89q.wepif.PermissionsResolver>>) f.get(PermissionsResolverManager.getInstance());
                    enabledResolvers.clear();
                    enabledResolvers.add(PermissionsResolver.class);
                    PermissionsResolverManager.getInstance().findResolver();
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }

        public static PermissionsResolver factory(Server server, YAMLProcessor config) {
            if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
                return null;
            }
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp == null) {
                return null;
            }
            perms = rsp.getProvider();
            return new PermissionsResolver(Bukkit.getServer());
        }

        private final Server server;

        public PermissionsResolver(Server server) {
            this.server = server;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean hasPermission(String name, String permission) {
            return hasPermission(server.getOfflinePlayer(name), permission);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean hasPermission(String worldName, String name, String permission) {
            return hasPermission(worldName, server.getOfflinePlayer(name), permission);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean inGroup(String player, String group) {
            return inGroup(server.getOfflinePlayer(player), group);
        }

        @Override
        @SuppressWarnings("deprecation")
        public String[] getGroups(String player) {
            return getGroups(server.getOfflinePlayer(player));
        }

        @Override
        public boolean hasPermission(OfflinePlayer player, String permission) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null) {
                return perms.playerHas(null, player, permission);
            } else {
                return perms.playerHas(onlinePlayer.getWorld().getName(), player, permission);
            }
        }

        @Override
        public boolean hasPermission(String worldName, OfflinePlayer player, String permission) {
            return perms.playerHas(worldName, player, permission);
        }

        @Override
        public boolean inGroup(OfflinePlayer player, String group) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null) {
                return perms.playerInGroup(null, player, group);
            } else {
                return perms.playerInGroup(onlinePlayer, group);
            }
        }

        @Override
        public String[] getGroups(OfflinePlayer player) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null) {
                return perms.getPlayerGroups(null, player);
            } else {
                return perms.getPlayerGroups(onlinePlayer);
            }
        }

        @Override
        public void load() {

        }

        @Override
        public String getDetectionMessage() {
            return "Using Unknown Network Resolver for Permissions (wrapped Vault)";
        }
    }
}
