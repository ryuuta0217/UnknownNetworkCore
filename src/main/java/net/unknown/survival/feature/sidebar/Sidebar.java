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

package net.unknown.survival.feature.sidebar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

public class Sidebar implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/Sidebar");
    private static final Sidebar INSTANCE = new Sidebar();
    private static SidebarProvider SIDEBAR_PROVIDER = new DefaultProvider();
    private static final Map<NamespacedKey, SidebarModule> REGISTERED_MODULES = new HashMap<>();

    static {
        ListenerManager.registerListener(INSTANCE);
    }

    private Sidebar() {}

    public static Sidebar getInstance() {
        return INSTANCE;
    }

    public static SidebarProvider getProvider() {
        return SIDEBAR_PROVIDER;
    }

    public static void setProvider(SidebarProvider provider) {
        SIDEBAR_PROVIDER = provider;
    }

    public static Set<SidebarModule> getRegisteredModules() {
        return Set.copyOf(REGISTERED_MODULES.values());
    }

    @Nullable
    public static SidebarModule getRegisteredModule(NamespacedKey identifier) {
        return REGISTERED_MODULES.getOrDefault(identifier, null);
    }

    public static void registerModule(SidebarModule module, boolean force) {
        if (REGISTERED_MODULES.containsKey(module.getIdentifier()) && !force) {
            LOGGER.warn("Module with identifier {} is already registered. Set force parameter to true to override.", module.getIdentifier());
            return;
        }

        LOGGER.info("Registering sidebar module with identifier {}", module.getIdentifier());
        REGISTERED_MODULES.put(module.getIdentifier(), module);

        if (module instanceof Listener bukkitEventListener) {
            LOGGER.info("Module with identifier {} is a event listener. Registering to ListenerManager.", module.getIdentifier());
            ListenerManager.registerListener(bukkitEventListener);
        }
    }

    public static void registerModule(SidebarModule module) {
        registerModule(module, false);
    }

    public static void unregisterModule(SidebarModule module) {
        LOGGER.info("Unregistering sidebar module with identifier {}", module.getIdentifier());
        SidebarModule removedModule = REGISTERED_MODULES.remove(module.getIdentifier());

        if (removedModule != null && removedModule instanceof Listener bukkitEventListener) {
            LOGGER.info("Module with identifier {} is a event listener. Unregistering from ListenerManager.", module.getIdentifier());
            ListenerManager.unregisterListener(bukkitEventListener);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SIDEBAR_PROVIDER.onPlayerJoin(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SIDEBAR_PROVIDER.onPlayerQuit(event);
    }

    public static class DefaultProvider implements SidebarProvider {
        private final Map<UUID, BukkitTask> playersTickTasks = new HashMap<>();
        private final Map<UUID, BukkitTask> playersDisplayUpdateTasks = new HashMap<>();
        private final Map<UUID, Objective> playersCurrentlySeeingSidebarObjectives = new HashMap<>();
        private final Map<UUID, List<InternalSidebarLine>> playersCurrentlySeeingSidebarLines = new HashMap<>();

        @Override
        public Component getTitle(Player player) {
            return Component.text("Unknown Network", DefinedTextColor.GOLD, TextDecoration.BOLD);
        }

        @Override
        public Objective createNewSidebarObjective(Player player) {
            return new Objective(
                    null,
                    "un_sidebar_" + UUID.randomUUID().toString().split("-")[0],
                    ObjectiveCriteria.DUMMY,
                    NewMessageUtil.convertAdventure2Minecraft(this.getTitle(player)),
                    ObjectiveCriteria.RenderType.INTEGER,
                    true,
                    BlankFormat.INSTANCE
            );
        }

        @Override
        public synchronized void displayUpdate(Player player) {
            if (!this.isSidebarEnabled(player)) {
                if (this.playersCurrentlySeeingSidebarObjectives.containsKey(player.getUniqueId())) {
                    ClientboundSetObjectivePacket removeObjectivePacket = new ClientboundSetObjectivePacket(this.playersCurrentlySeeingSidebarObjectives.get(player.getUniqueId()), ClientboundSetObjectivePacket.METHOD_REMOVE);
                    MinecraftAdapter.player(player).connection.send(removeObjectivePacket);
                    this.playersCurrentlySeeingSidebarObjectives.remove(player.getUniqueId());

                    ClientboundSetDisplayObjectivePacket removeDisplayPacket = new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null);
                    MinecraftAdapter.player(player).connection.send(removeDisplayPacket);
                }

                this.playersCurrentlySeeingSidebarLines.remove(player.getUniqueId());
                return;
            }

            // 1. Create new sidebar object and send to player
            Objective newSidebarObjective = this.createNewSidebarObjective(player);
            ClientboundSetObjectivePacket createObjectivePacket = new ClientboundSetObjectivePacket(newSidebarObjective, ClientboundSetObjectivePacket.METHOD_ADD);
            MinecraftAdapter.player(player).connection.send(createObjectivePacket);

            // 2. Collect lines and map to score (line).
            List<SidebarModule> sidebarModulesSorted = Sidebar.getRegisteredModules()
                    .stream()
                    .filter(module -> module.isEnabled(player))
                    .sorted(Comparator.comparingInt(module -> module.getPriority(player)))
                    .toList()
                    .reversed();

            List<InternalSidebarLine> canSeeLines = new ArrayList<>();
            int remainingScore = 15;
            for (SidebarModule module : sidebarModulesSorted) {
                List<InternalSidebarLine> lines = module.getLines(player);
                if ((remainingScore - lines.size()) >= 0) {
                    for (InternalSidebarLine line : lines) {
                        canSeeLines.add(InternalSidebarLine.ofScore(line, remainingScore--));
                    }
                } else {
                    break; // Skip cannot see modules to process (because improve performance)
                }
            }

            // 3. Send lines (scores) to player
            canSeeLines.parallelStream()
                    .map(line -> new ClientboundSetScorePacket(
                            line.identifier(), // identifier
                            newSidebarObjective.getName(), // objective name
                            line.score(), // score (line number) (higher score is upper line)
                            Optional.of(line.minecraftLine()), // line content
                            Optional.ofNullable(line.format()) // number format (sticky to right w/line
                    ))
                    .forEach(packet -> MinecraftAdapter.player(player).connection.send(packet));

            // 4. Set new sidebar object to sidebar slot
            ClientboundSetDisplayObjectivePacket setDisplayPacket = new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, newSidebarObjective);
            MinecraftAdapter.player(player).connection.send(setDisplayPacket);

            // 5. Remove old sidebar object
            if (this.playersCurrentlySeeingSidebarObjectives.containsKey(player.getUniqueId())) {
                ClientboundSetObjectivePacket removeObjectivePacket = new ClientboundSetObjectivePacket(this.playersCurrentlySeeingSidebarObjectives.get(player.getUniqueId()), ClientboundSetObjectivePacket.METHOD_REMOVE);
                MinecraftAdapter.player(player).connection.send(removeObjectivePacket);
            }

            // 6. OBJECTIVES.put new sidebar object and remove old object
            this.playersCurrentlySeeingSidebarObjectives.remove(player.getUniqueId());
            this.playersCurrentlySeeingSidebarObjectives.put(player.getUniqueId(), newSidebarObjective);

            this.playersCurrentlySeeingSidebarLines.put(player.getUniqueId(), canSeeLines);
        }

        @Override
        public void deltaUpdate(Player player, SidebarModule module, String identifier, InternalSidebarLine newLine) {
            if (!newLine.identifier().equals(identifier)) return;

            if (this.playersCurrentlySeeingSidebarObjectives.containsKey(player.getUniqueId())) {
                if (this.playersCurrentlySeeingSidebarLines.containsKey(player.getUniqueId())) {
                    List<InternalSidebarLine> seeingSidebarLines = this.playersCurrentlySeeingSidebarLines.get(player.getUniqueId());

                    InternalSidebarLine targetLine = seeingSidebarLines.stream().filter(line -> line.identifier().equals(identifier)).findAny().orElse(null);
                    if (targetLine != null) {
                        int idx = seeingSidebarLines.indexOf(targetLine);
                        newLine = InternalSidebarLine.ofScore(newLine, targetLine.score());
                        seeingSidebarLines.set(idx, newLine);

                        ClientboundSetScorePacket setScorePacket = new ClientboundSetScorePacket(
                                newLine.identifier(),
                                this.playersCurrentlySeeingSidebarObjectives.get(player.getUniqueId()).getName(),
                                newLine.score(),
                                Optional.of(newLine.minecraftLine()),
                                Optional.ofNullable(newLine.format())
                        );
                        MinecraftAdapter.player(player).connection.send(setScorePacket);
                    }
                }
            }
        }

        @Override
        public void tick(Player player) {
            Sidebar.getRegisteredModules().forEach(module -> module.tick(player));
        }

        @Override
        public void onPlayerJoin(PlayerJoinEvent event) {
            BukkitTask tickTask = RunnableManager.runAsyncRepeating(() -> this.tick(event.getPlayer()), 0, 1);
            this.playersTickTasks.put(event.getPlayer().getUniqueId(), tickTask);

            BukkitTask displayUpdateTask = RunnableManager.runAsyncRepeating(() -> this.displayUpdate(event.getPlayer()), 0, 20);
            this.playersDisplayUpdateTasks.put(event.getPlayer().getUniqueId(), displayUpdateTask);
        }

        @Override
        public void onPlayerQuit(PlayerQuitEvent event) {
            this.playersCurrentlySeeingSidebarObjectives.remove(event.getPlayer().getUniqueId());
            this.playersCurrentlySeeingSidebarLines.remove(event.getPlayer().getUniqueId());

            Optional.ofNullable(this.playersTickTasks.remove(event.getPlayer().getUniqueId())).ifPresent(BukkitTask::cancel);
            Optional.ofNullable(this.playersDisplayUpdateTasks.remove(event.getPlayer().getUniqueId())).ifPresent(BukkitTask::cancel);
        }
    }
}
