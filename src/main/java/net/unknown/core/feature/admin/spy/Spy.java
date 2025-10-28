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

package net.unknown.core.feature.admin.spy;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Spy implements Listener {
    private static final Spy INSTANCE = new Spy();
    private static final Logger LOGGER = Logger.getLogger("UNC/Spy");
    private static final NamespacedKey PLAYER_REGISTRY_KEY = new NamespacedKey("unknown-network", "spy");
    private static final Set<SpyModule> MODULES = new HashSet<>();
    private static boolean FREEZE = false;

    static {
        ListenerManager.registerListener(INSTANCE);
    }

    private Spy() {}

    @EventHandler
    public void onTickStart(ServerTickStartEvent event) {
        MODULES.forEach(module -> module.onTickStart(event));
    }

    @EventHandler
    public void onTickEnd(ServerTickEndEvent event) {
        MODULES.forEach(module -> module.onTickEnd(event));
    }

    public static Set<SpyModule> getRegisteredModules() {
        return Collections.unmodifiableSet(MODULES);
    }

    public static SpyModule getModule(NamespacedKey moduleIdentifier) {
        return MODULES.stream().filter(module -> module.getIdentifier().equals(moduleIdentifier)).findAny().orElse(null);
    }

    public static void registerModule(SpyModule module) {
        if (FREEZE) throw new UnsupportedOperationException("Spy modules registry is already frozen!");
        module.onRegistering();
        MODULES.add(module);
        LOGGER.info("Module " + module.getIdentifier().asString() + " registered!");
    }

    @Deprecated
    public static void unregisterModule(SpyModule module) {
        if (FREEZE) throw new UnsupportedOperationException("Spy modules registry is already frozen!");
        module.onUnRegistering();
        MODULES.remove(module);
    }

    public static void unregisterModule(NamespacedKey moduleIdentifier) {
        if (FREEZE) throw new UnsupportedOperationException("Spy modules registry is already frozen!");
        MODULES.removeIf(module -> {
            if (module.getIdentifier().equals(moduleIdentifier)) {
                module.onUnRegistering();
                return true;
            }
            return false;
        });
    }

    public static boolean isModuleEnabled(Player player, SpyModule module) {
        PlayerData.PlayerRegistry registries = PlayerData.of(player).getRegistries();
        Set<String> disabledModules = registries.containsKey(PLAYER_REGISTRY_KEY, "disabled") ? new HashSet<>(Arrays.asList(registries.getRegistry(PLAYER_REGISTRY_KEY).get("disabled").split(", ?"))) : Collections.emptySet();
        return module.isDefaultEnabled() && !disabledModules.contains(module.getIdentifier().asString());
    }

    public static Set<SpyModule> getEnabledModules(Player player) {
        PlayerData.PlayerRegistry registries = PlayerData.of(player).getRegistries();
        Set<String> disabledModules = registries.containsKey(PLAYER_REGISTRY_KEY, "disabled") ? new HashSet<>(Arrays.asList(registries.getRegistry(PLAYER_REGISTRY_KEY).get("disabled").split(", ?"))) : Collections.emptySet();
        return MODULES.stream().filter(module -> module.isDefaultEnabled() && !disabledModules.contains(module.getIdentifier().toString())).collect(Collectors.toSet());
    }

    public static Set<NamespacedKey> getEnabledModuleIdentifiers(Player player) {
        return getEnabledModules(player).stream().map(SpyModule::getIdentifier).collect(Collectors.toSet());
    }

    public static void enableModule(Player player, SpyModule module) {
        PlayerData.PlayerRegistry registries = PlayerData.of(player).getRegistries();

        Set<String> enabledModules = registries.containsKey(PLAYER_REGISTRY_KEY, "enabled") ? new HashSet<>(Arrays.asList(registries.getRegistry(PLAYER_REGISTRY_KEY).get("enabled").split(", ?"))) : new HashSet<>();
        Set<String> disabledModules = registries.containsKey(PLAYER_REGISTRY_KEY, "disabled") ? new HashSet<>(Arrays.asList(registries.getRegistry(PLAYER_REGISTRY_KEY).get("disabled").split(", ?"))) : new HashSet<>();

        if (!module.isDefaultEnabled()) enabledModules.add(module.getIdentifier().asString());
        disabledModules.remove(module.getIdentifier().asString());
        registries.put(PLAYER_REGISTRY_KEY, "enabled", String.join(",", enabledModules));
        registries.put(PLAYER_REGISTRY_KEY, "disabled", String.join(",", disabledModules));
    }

    public static void enableModule(Player player, NamespacedKey moduleIdentifier) {
        if (MODULES.stream().noneMatch(module -> module.getIdentifier().equals(moduleIdentifier))) throw new IllegalArgumentException("No such spy module registered with identifier: " + moduleIdentifier.asString());
        MODULES.stream().filter(module -> module.getIdentifier().equals(moduleIdentifier)).findAny().ifPresent(module -> enableModule(player, module));
    }

    public static boolean isModuleDisabled(Player player, SpyModule module) {
        PlayerData.PlayerRegistry registries = PlayerData.of(player).getRegistries();
        Set<String> disabledModules = registries.containsKey(PLAYER_REGISTRY_KEY, "disabled") ? new HashSet<>(Arrays.asList(registries.getRegistry(PLAYER_REGISTRY_KEY).get("disabled").split(", ?"))) : Collections.emptySet();
        return !module.isDefaultEnabled() || disabledModules.contains(module.getIdentifier().asString());
    }

    public static Set<SpyModule> getDisabledModules(Player player) {
        PlayerData.PlayerRegistry registries = PlayerData.of(player).getRegistries();
        Set<String> disabledModules = registries.containsKey(PLAYER_REGISTRY_KEY, "disabled") ? new HashSet<>(Arrays.asList(registries.getRegistry(PLAYER_REGISTRY_KEY).get("disabled").split(", ?"))) : Collections.emptySet();
        return MODULES.stream().filter(module -> !module.isDefaultEnabled() || disabledModules.contains(module.getIdentifier().toString())).collect(Collectors.toSet());
    }

    public static Set<NamespacedKey> getDisabledModuleIdentifiers(Player player) {
        return getDisabledModules(player).stream().map(SpyModule::getIdentifier).collect(Collectors.toSet());
    }

    public static void disableModule(Player player, SpyModule module) {
        PlayerData.PlayerRegistry registries = PlayerData.of(player).getRegistries();

        Set<String> enabledModules = registries.containsKey(PLAYER_REGISTRY_KEY, "enabled") ? new HashSet<>(Arrays.asList(registries.getRegistry(PLAYER_REGISTRY_KEY).get("enabled").split(", ?"))) : new HashSet<>();
        Set<String> disabledModules = registries.containsKey(PLAYER_REGISTRY_KEY, "disabled") ? new HashSet<>(Arrays.asList(registries.getRegistry(PLAYER_REGISTRY_KEY).get("disabled").split(", ?"))) : new HashSet<>();

        enabledModules.remove(module.getIdentifier().asString());
        if (module.isDefaultEnabled()) disabledModules.add(module.getIdentifier().asString());
        registries.put(PLAYER_REGISTRY_KEY, "enabled", String.join(",", enabledModules));
        registries.put(PLAYER_REGISTRY_KEY, "disabled", String.join(",", disabledModules));
    }

    public static void disableModule(Player player, NamespacedKey moduleIdentifier) {
        if (MODULES.stream().noneMatch(module -> module.getIdentifier().equals(moduleIdentifier))) throw new IllegalArgumentException("No such spy module registered with identifier: " + moduleIdentifier.asString());
        MODULES.stream().filter(module -> module.getIdentifier().equals(moduleIdentifier)).findAny().ifPresent(module -> disableModule(player, module));
    }

    public static void unregisterAll() {
        if (FREEZE) throw new UnsupportedOperationException("Spy modules registry is already frozen!");
        MODULES.clear();
    }

    public static void freeze() {
        FREEZE = true;
        LOGGER.info("Spy modules registry is frozen!");
    }

    public static void unfreeze() {
        FREEZE = false;
    }

    public static Set<Audience> getSpyMessageReceivers(SpyModule source, Predicate<Player> receiverRemoveIf, boolean logConsole) {
        Set<Audience> receivers = new HashSet<>();
        Bukkit.getOnlinePlayers()
                .stream()
                .filter(player -> player.isOp() || player.hasPermission(Permissions.FEATURE_SPY.getPermissionNode()))
                .filter(player -> !receiverRemoveIf.test(player))
                .filter(player -> isModuleEnabled(player, source))
                .forEach(receivers::add);
        if (logConsole) receivers.add(Bukkit.getConsoleSender());
        return receivers;
    }

    public static void broadcastSpyMessage(SpyModule source, Component message, Predicate<Player> receiverRemoveIf, boolean logConsole, @Nullable UUID sender) {
        Spy.getSpyMessageReceivers(source, receiverRemoveIf, logConsole).forEach(audience -> audience.sendMessage(source.buildSpyMessage(audience, message)));
    }
}
