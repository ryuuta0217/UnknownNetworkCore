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

package net.unknown.core.dependency;

import com.ryuuta0217.file.ArchiveUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.world.regen.AutomatedRegenWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CloneWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.core.world.options.RegenWorldOptions;
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions;
import org.mvplugins.multiverse.core.world.reasons.*;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MultiverseCore {
    public static boolean isMultiverseCoreEnabled() {
        return Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null && Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core");
    }

    public static Location getSpawnLocation(World world) {
        if (!isMultiverseCoreEnabled()) return world.getSpawnLocation();
        MultiverseWorld mvWorld = getInstance().getWorldManager().getLoadedWorld(world.getName()).getOrNull();
        if (mvWorld != null) {
            return world.getSpawnLocation();
        }
        return null;
    }

    public static Location getSpawnLocation(Level level) {
        Vec3 positionVector3 = level.getSharedSpawnPos().getCenter();
        Vec2 rotationVector2 = new Vec2(level.getSharedSpawnAngle(), 0);
        if (isMultiverseCoreEnabled()) {
            Location multiverseSpawnLocation = getSpawnLocation(MinecraftAdapter.world(level));
            positionVector3 = MinecraftAdapter.vec3(multiverseSpawnLocation);
            rotationVector2 = MinecraftAdapter.vec2(multiverseSpawnLocation);
        }
        return MinecraftAdapter.location(level, positionVector3, rotationVector2);
    }

    public static MultiverseCoreApi getInstance() {
        return MultiverseCoreApi.get();
    }

    public static WorldManager getWorldManager() {
        return getInstance().getWorldManager();
    }

    public static boolean isWorldLoaded(String worldName) {
        return getWorldManager().getLoadedWorld(worldName).getOrNull() != null;
    }

    public static boolean isWorldLoaded(MultiverseWorld world) {
        return getWorldManager().getLoadedWorld(world).getOrNull() != null;
    }

    public static void loadWorld(String worldName) throws IllegalStateException, IllegalArgumentException {
        MultiverseWorld world = getWorldManager().getWorld(worldName).getOrElseThrow(() -> new IllegalArgumentException("World " + worldName + " does not exist."));
        loadWorld(world);
    }

    public static void loadWorld(MultiverseWorld world) throws IllegalStateException, IllegalArgumentException {
        if (!Bukkit.isPrimaryThread()) {
            Future<Throwable> result = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> {
                try {
                    loadWorld0(world);
                } catch (Throwable t) {
                    return t;
                }
                return null;
            });

            try {
                if (result.get() != null) throw result.get(); // Redirect the exception to the current thread
            } catch(Throwable t) {
                throw new IllegalStateException("Failed to load world " + world.getName(), t);
            }
        } else {
            loadWorld0(world);
        }
    }

    private static void loadWorld0(MultiverseWorld world) throws IllegalStateException, IllegalArgumentException {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("This method must be called on the main thread.");
        if (world.isLoaded()) throw new IllegalArgumentException("World " + world.getName() + " is already loaded.");

        Attempt<LoadedMultiverseWorld, LoadFailureReason> loadResult = getWorldManager().loadWorld(world);
        if (loadResult.isFailure()) {
            throw new IllegalStateException(loadResult.getFailureMessage().formatted() + ": " + loadResult.getFailureReason());
        }
    }

    public static void unloadWorld(String worldName, boolean save, boolean removePlayers) throws IllegalStateException, IllegalArgumentException {
        MultiverseWorld world = getWorldManager().getWorld(worldName).getOrElseThrow(() -> new IllegalArgumentException("World " + worldName + " does not exist."));
        if (world.isLoaded()) {
            LoadedMultiverseWorld loadedWorld = getWorldManager().getLoadedWorld(world).getOrNull();
            if (loadedWorld != null) {
                unloadWorld(loadedWorld, save, removePlayers);
            }
        }
        throw new IllegalStateException("World " + worldName + " is not loaded.");
    }

    public static void unloadWorld(LoadedMultiverseWorld loadedWorld, boolean save, boolean removePlayers) throws IllegalStateException, IllegalArgumentException {
        if (!loadedWorld.isLoaded()) throw new IllegalArgumentException("World " + loadedWorld.getName() + " is not loaded.");

        if (!Bukkit.isPrimaryThread()) {
            Future<Throwable> result = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> {
                try {
                    unloadWorld0(loadedWorld, save, removePlayers);
                } catch (Throwable t) {
                    return t;
                }
                return null;
            });

            try {
                if (result.get() != null) throw result.get(); // Redirect the exception to the current thread
            } catch(Throwable t) {
                throw new IllegalStateException("Failed to unload world " + loadedWorld.getName(), t);
            }
        } else {
            unloadWorld0(loadedWorld, save, removePlayers);
        }
    }

    private static void unloadWorld0(LoadedMultiverseWorld loadedWorld, boolean save, boolean removePlayers) throws IllegalStateException, IllegalArgumentException {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("This method must be called on the main thread.");
        if (!loadedWorld.isLoaded()) throw new IllegalArgumentException("World " + loadedWorld.getName() + " is not loaded.");

        UnloadWorldOptions options = UnloadWorldOptions.world(loadedWorld).saveBukkitWorld(save);
        if (removePlayers) removePlayersFromWorld(loadedWorld, false);

        Attempt<MultiverseWorld, UnloadFailureReason> unloadResult = getWorldManager().unloadWorld(options);
        if (unloadResult.isFailure()) {
            throw new IllegalStateException(unloadResult.getFailureMessage().formatted() + ": " + unloadResult.getFailureReason());
        }
    }

    public static boolean deleteWorld(String worldName, List<String> keepFiles, boolean removePlayers) throws IllegalArgumentException {
        MultiverseWorld world = getWorldManager().getWorld(worldName).getOrElseThrow(() -> new IllegalArgumentException("World " + worldName + " does not exist."));
        return deleteWorld(world, keepFiles, removePlayers);
    }

    public static boolean deleteWorld(MultiverseWorld world, List<String> keepFiles, boolean removePlayers) {
        if (!Bukkit.isPrimaryThread()) {
            try {
                return Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> deleteWorld(world, keepFiles, removePlayers)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException("Failed to delete world " + world.getName(), e);
            }
        } else {
            DeleteWorldOptions options = DeleteWorldOptions.world(world)
                    .keepFiles(keepFiles);

            if (removePlayers && world.isLoaded()) removePlayersFromWorld(getWorldManager().getLoadedWorld(world).getOrNull(), false);
            Attempt<String, DeleteFailureReason> deleteResult = getWorldManager().deleteWorld(options);
            return deleteResult.isSuccess();
        }
    }

    public static boolean cloneWorld(String from, String to, boolean keepGameRule, boolean keepWorldBorder, boolean keepWorldConfig, boolean saveBeforeCloning, boolean movePlayers, boolean overwrite) throws IllegalArgumentException {
        MultiverseWorld fromWorld = getWorldManager().getWorld(from).getOrElseThrow(() -> new IllegalArgumentException("World " + from + " does not exist."));
        if (!fromWorld.isLoaded()) throw new IllegalArgumentException("World " + from + " is not loaded.");
        return cloneWorld(getWorldManager().getLoadedWorld(fromWorld).getOrNull(), to, keepGameRule, keepWorldBorder, keepWorldConfig, saveBeforeCloning, movePlayers, overwrite);
    }

    public static boolean cloneWorld(LoadedMultiverseWorld from, String to, boolean keepGameRule, boolean keepWorldBorder, boolean keepWorldConfig, boolean saveBeforeCloning, boolean movePlayers, boolean overwrite) throws IllegalArgumentException {
        if (!Bukkit.isPrimaryThread()) {
            try {
                return Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> cloneWorld(from, to, keepGameRule, keepWorldBorder, keepWorldConfig, saveBeforeCloning, movePlayers, overwrite)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException("Failed to clone world " + from.getName() + " to " + to, e);
            }
        }

        if (getWorldManager().getWorld(to).isDefined()) {
            if (overwrite) {
                if (!MultiverseCore.deleteWorld(to, Collections.emptyList(), true)) {
                    throw new IllegalStateException("Failed to delete existing world \"" + to + "\".");
                }
            } else {
                throw new IllegalArgumentException("World \"" + to + "\" already exists.");
            }
        }

        CloneWorldOptions options = CloneWorldOptions.fromTo(from, to)
                .keepGameRule(keepGameRule)
                .keepWorldBorder(keepWorldBorder)
                .keepWorldConfig(keepWorldConfig)
                .saveBukkitWorld(saveBeforeCloning);
        Attempt<LoadedMultiverseWorld, CloneFailureReason> cloneResult = getWorldManager().cloneWorld(options);
        if (cloneResult.isSuccess()) {
            LoadedMultiverseWorld toWorld = cloneResult.get();
            if (movePlayers) {
                from.getPlayers()
                        .getOrElse(Collections.emptyList())
                        .forEach(player -> {
                            Location currentLocation = player.getLocation();
                            if (currentLocation.getWorld().getUID().equals(from.getBukkitWorld().get().getUID())) {
                                currentLocation.setWorld(toWorld.getBukkitWorld().get());
                                player.teleport(currentLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                            }
                        });
            }
            return true;
        }
        return false;
    }

    public static boolean renameWorld(String from, String to, boolean movePlayers, boolean overwrite) throws IllegalArgumentException {
        MultiverseWorld fromWorld = getWorldManager().getWorld(from).getOrElseThrow(() -> new IllegalArgumentException("World " + from + " does not exist."));
        return renameWorld(fromWorld, to, movePlayers, overwrite);
    }

    public static boolean renameWorld(MultiverseWorld fromWorld, String to, boolean movePlayers, boolean overwrite) throws IllegalArgumentException {
        boolean loadAfter = MultiverseCore.isWorldLoaded(fromWorld);
        if (!loadAfter) MultiverseCore.loadWorld(fromWorld);
        fromWorld = MultiverseCore.getWorldManager().getLoadedWorld(fromWorld).getOrNull();

        if (MultiverseCore.cloneWorld((LoadedMultiverseWorld) fromWorld, to, true, true, true, true, movePlayers, overwrite)) {
            LoadedMultiverseWorld toWorld = MultiverseCore.getWorldManager().getLoadedWorld(to).getOrNull();

            if (!MultiverseCore.deleteWorld(fromWorld, Collections.emptyList(), true)) return false;
            if (loadAfter && !toWorld.isLoaded()) {
                MultiverseCore.loadWorld(toWorld);
            } else if (!loadAfter && toWorld.isLoaded()) {
                MultiverseCore.unloadWorld(toWorld, true, true);
            }
            return true;
        }
        return false;
    }

    public static void removePlayersFromWorld(String worldName, boolean async) throws IllegalArgumentException {
        MultiverseWorld world = getWorldManager().getWorld(worldName).getOrElseThrow(() -> new IllegalArgumentException("World " + worldName + " does not exist."));
        if (world.isLoaded()) {
            LoadedMultiverseWorld loadedWorld = getWorldManager().getLoadedWorld(world).getOrNull();
            if (loadedWorld != null) {
                removePlayersFromWorld(loadedWorld, async);
            }
        } else {
            throw new IllegalArgumentException("World " + worldName + " is not loaded.");
        }
    }

    public static void removePlayersFromWorld(LoadedMultiverseWorld loadedWorld, boolean async) {
        if (async) {
            removePlayersFromWorldAsync(loadedWorld);
        } else {
            removePlayersFromWorldSync(loadedWorld);
        }
    }

    private static void removePlayersFromWorldSync(LoadedMultiverseWorld loadedWorld) {
        loadedWorld.getPlayers().getOrElse(Collections.emptyList())
                .forEach(player -> {
                    Location spawnLocation = getSpawnLocation(Bukkit.getWorld("world"));
                    if (spawnLocation != null) {
                        player.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    }
                });
    }

    private static void removePlayersFromWorldAsync(LoadedMultiverseWorld loadedWorld) {
        loadedWorld.getPlayers().getOrElse(Collections.emptyList())
                .parallelStream()
                .forEach(player -> {
                    Location spawnLocation = getSpawnLocation(Bukkit.getWorld("world"));
                    if (spawnLocation != null) {
                        player.teleportAsync(spawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    }
                });
    }

    public static boolean regenerateWorld(String worldName, @Nullable String seed, boolean keepGameRule) throws IllegalArgumentException, IllegalStateException {
        MultiverseWorld world = getWorldManager().getWorld(worldName).getOrElseThrow(() -> new IllegalArgumentException("World " + worldName + " does not exist."));
        if (!world.isLoaded()) throw new IllegalArgumentException("World " + worldName + " is not loaded.");
        LoadedMultiverseWorld loadedWorld = getWorldManager().getLoadedWorld(world).getOrNull();
        return regenerateWorld(loadedWorld, seed, keepGameRule);
    }

    public static synchronized boolean regenerateWorld(LoadedMultiverseWorld world, @Nullable String seed, boolean keepGameRule) throws IllegalArgumentException, IllegalStateException {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("This method must be called on the main thread.");
        if (!world.isLoaded()) throw new IllegalArgumentException("World " + world.getName() + " is not loaded.");

        RegenWorldOptions options = RegenWorldOptions.world(world);
        if (seed != null && !seed.isBlank()) {
            options.seed(seed);
            options.randomSeed(false);
        } else {
            options.randomSeed(true);
        }
        options.keepGameRule(keepGameRule);

        removePlayersFromWorld(world, false);
        Attempt<LoadedMultiverseWorld, RegenFailureReason> regenResult = getWorldManager().regenWorld(options);
        return regenResult.isSuccess();
    }

    public static boolean compressWorld(String worldName, String backupFolderStr, String backupFileStr, boolean overwrite) throws IllegalArgumentException, IllegalStateException {
        MultiverseWorld world = getWorldManager().getWorld(worldName).getOrElseThrow(() -> new IllegalArgumentException("World " + worldName + " does not exist."));
        File backupFolder = new File(backupFolderStr);
        File backupFile = new File(backupFolder, backupFileStr);
        return compressWorld(world, backupFile, overwrite);
    }

    public static boolean compressWorld(MultiverseWorld world, File backupFile, boolean overwrite) throws IllegalArgumentException, IllegalStateException {
        boolean loadAfter = MultiverseCore.isWorldLoaded(world);

        if (!loadAfter) MultiverseCore.loadWorld(world);
        LoadedMultiverseWorld loadedWorld = getWorldManager().getLoadedWorld(world).get();
        File worldFolder = loadedWorld.getBukkitWorld().get().getWorldFolder();
        MultiverseCore.unloadWorld(loadedWorld, true, true);

        try {
            ArchiveUtil.createArchiveWithZstd(Files.walk(worldFolder.toPath()).map(Path::toAbsolutePath).map(Path::toFile).toList(), backupFile, worldFolder, overwrite);
        } catch (IOException e) {
            throw new IllegalStateException("An exception occurred while opening the backup file.", e);
        }

        if (loadAfter) {
            MultiverseCore.loadWorld(world);
        }

        return true;
    }
}
