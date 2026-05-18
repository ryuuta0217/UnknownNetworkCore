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

package net.unknown.survival.gui.protection.dialog;

import com.google.common.collect.Maps;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.registry.Keyed;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.*;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.dependency.WorldGuard;
import net.unknown.survival.enums.Permissions;
import net.unknown.survival.gui.protection.ProtectionGuiState;
import net.unknown.survival.gui.protection.ProtectionGuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class FlagSettingsDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/ProtectionGui/FlagSettingsDialog");
    private static final Map<String, FlagHandler<?, ?>> FLAG_HANDLERS = new HashMap<>();
    private static final Set<String> GLOBALLY_EDITING_ALLOWED_FLAGS = new HashSet<>() {{
        add("pvp");
        add("use");
        add("pistons");
        add("tnt");
    }};

    static {
        init();
    }

    public static void init() {
        registerFlagHandler(StateFlag.class,
                (flag, region, player, width) -> {
                    StateFlag.State currentValue = region.region().getFlag(flag);

                    int choice = 0;
                    if (currentValue != null) {
                        choice = currentValue == StateFlag.State.ALLOW ? 1 : 2;
                    }

                    return DialogInput.singleOption(
                            getFlagNameForDialogInputKey(flag),
                            width,
                            Arrays.asList(
                                    SingleOptionDialogInput.OptionEntry.create("unset", ProtectionGuiUtil.getStateFlagValueDisplayName(flag, null), choice == 0),
                                    SingleOptionDialogInput.OptionEntry.create("allow", ProtectionGuiUtil.getStateFlagValueDisplayName(flag, StateFlag.State.ALLOW), choice == 1),
                                    SingleOptionDialogInput.OptionEntry.create("deny", ProtectionGuiUtil.getStateFlagValueDisplayName(flag, StateFlag.State.DENY), choice == 2)
                            ),
                            Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)),
                            true
                    );
                },
                (flag, region, player, response) -> {
                    String rawValue = response.getText(getFlagNameForDialogInputKey(flag));
                    if (rawValue != null) {
                        StateFlag.State oldState = region.region().getFlag(flag);
                        StateFlag.State state = rawValue.equals("unset") ? null : (rawValue.equals("allow") ? StateFlag.State.ALLOW : StateFlag.State.DENY);
                        region.region().setFlag(flag, state);
                        if (!Objects.equals(oldState, state)) {
                            NewMessageUtil.sendMessage(player, getSuccessfullyModifiedMessage(flag, region, ProtectionGuiUtil.getStateFlagValueDisplayName(flag, oldState), ProtectionGuiUtil.getStateFlagValueDisplayName(flag, state)), true);
                        }
                    }
                }
        );

        registerFlagHandler(BooleanFlag.class,
                (flag, region, player, width) -> {
                    Boolean currentValue = region.region().getFlag(flag);

                    return DialogInput.singleOption(
                            getFlagNameForDialogInputKey(flag),
                            width,
                            Arrays.asList(
                                    SingleOptionDialogInput.OptionEntry.create("unset", ProtectionGuiUtil.getBooleanFlagValueDisplayName(flag, null), currentValue == null),
                                    SingleOptionDialogInput.OptionEntry.create("true", ProtectionGuiUtil.getBooleanFlagValueDisplayName(flag, true), Boolean.TRUE.equals(currentValue)),
                                    SingleOptionDialogInput.OptionEntry.create("false", ProtectionGuiUtil.getBooleanFlagValueDisplayName(flag, false), Boolean.FALSE.equals(currentValue))
                            ),
                            Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)),
                            true
                    );
                },
                (flag, region, player, response) -> {
                    String rawValue = response.getText(getFlagNameForDialogInputKey(flag));
                    if (rawValue != null) {
                        Boolean oldValue = region.region().getFlag(flag);
                        Boolean value = rawValue.equals("unset") ? null : (rawValue.equals("true") ? true : false);
                        region.region().setFlag(flag, value);
                        if (!Objects.equals(oldValue, value)) {
                            NewMessageUtil.sendMessage(player, getSuccessfullyModifiedMessage(flag, region, ProtectionGuiUtil.getBooleanFlagValueDisplayName(flag, oldValue), ProtectionGuiUtil.getBooleanFlagValueDisplayName(flag, value)), true);
                        }
                    }
                }
        );

        registerFlagHandler(StringFlag.class,
                (flag, region, player, width) -> {
                    String defaultValue = flag.getDefault();
                    String currentValue = region.region().getFlag(flag);

                    return DialogInput.text(
                            getFlagNameForDialogInputKey(flag),
                            width,
                            Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)),
                            true,
                            LegacyComponentSerializer.legacyAmpersand().serialize(LegacyComponentSerializer.legacySection().deserialize((currentValue != null ? currentValue : (defaultValue != null ? defaultValue : "")))),
                            Integer.MAX_VALUE,
                            TextDialogInput.MultilineOptions.create(null, null)
                    );
                },
                (flag, region, player, response) -> {
                    String rawValue = response.getText(getFlagNameForDialogInputKey(flag));
                    if (rawValue != null) {
                        String defaultValue = flag.getDefault(); // メッセージが設定されてたりnullだったり セクションで装飾済みの値

                        String oldValue = region.region().getFlag(flag); // セクションで装飾済みの値
                        if (oldValue != null && oldValue.isEmpty()) oldValue = null;

                        String value = rawValue.isEmpty() ? null : LegacyComponentSerializer.legacySection().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(rawValue));
                        if (value != null && value.equals(defaultValue)) value = null;

                        region.region().setFlag(flag, value);
                        if (!Objects.equals(oldValue, value)) {
                            NewMessageUtil.sendMessage(player, getSuccessfullyModifiedMessage(flag, region, Component.text(oldValue == null ? "null" : oldValue), Component.text(value == null ? "null" : value)), true);
                        }
                    }
                }
        );

        registerFlagHandler(IntegerFlag.class,
                (flag, region, player, width) -> {
                    Integer defaultValue = flag.getDefault();
                    Integer currentValue = region.region().getFlag(flag);

                    return DialogInput.numberRange(
                            getFlagNameForDialogInputKey(flag),
                            width,
                            Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)),
                            "%s: %s",
                            -1f,
                            Math.max(100f, (currentValue != null ? currentValue : -1f)),
                            (currentValue != null ? currentValue : (defaultValue != null ? defaultValue : -1f)),
                            1f
                    );
                },
                (flag, region, player, response) -> {
                    Float rawValue = response.getFloat(getFlagNameForDialogInputKey(flag));
                    if (rawValue != null) {
                        Integer defaultValue = flag.getDefault();
                        Integer oldValue = region.region().getFlag(flag);
                        Integer value = rawValue == -1 || (defaultValue != null && rawValue.intValue() == defaultValue) ? null : rawValue.intValue();
                        region.region().setFlag(flag, value);
                        if (!Objects.equals(oldValue, value)) {
                            NewMessageUtil.sendMessage(player, getSuccessfullyModifiedMessage(flag, region, Component.text(oldValue == null ? "null" : String.valueOf(oldValue)), Component.text(value == null ? "null" : String.valueOf(value))), true);
                        }
                    }
                }
        );

        registerFlagHandler(DoubleFlag.class,
                (flag, region, player, width) -> {
                    Double defaultValue = flag.getDefault();
                    Double currentValue = region.region().getFlag(flag);

                    return DialogInput.numberRange(
                            getFlagNameForDialogInputKey(flag),
                            width,
                            Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)),
                            "%s: %s",
                            -1f,
                            Math.max(100f, (currentValue != null ? currentValue.floatValue() : -1f)),
                            (currentValue != null ? currentValue.floatValue() : (defaultValue != null ? defaultValue.floatValue() : -1f)),
                            0.1f
                    );
                },
                (flag, region, player, response) -> {
                    Float rawValue = response.getFloat(getFlagNameForDialogInputKey(flag));
                    if (rawValue != null) {
                        Double defaultValue = flag.getDefault();
                        Double oldValue = region.region().getFlag(flag);
                        Double value = rawValue == -1 || (defaultValue != null && rawValue.doubleValue() == defaultValue) ? null : rawValue.doubleValue();
                        region.region().setFlag(flag, value);
                        if (!Objects.equals(oldValue, value)) {
                            NewMessageUtil.sendMessage(player, getSuccessfullyModifiedMessage(flag, region, Component.text(oldValue == null ? "null" : String.valueOf(oldValue)), Component.text(value == null ? "null" : String.valueOf(value))), true);
                        }
                    }
                }
        );

        registerFlagHandler(LocationFlag.class,
                (flag, region, player, width) -> {
                    Location defaultValue = flag.getDefault();
                    Location currentValue = region.region().getFlag(flag);

                    return DialogInput.text(
                            getFlagNameForDialogInputKey(flag),
                            width,
                            Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)).append(Component.text(" (world, x, y, z, yaw, pitch), (here)")),
                            true,
                            (currentValue != null ? String.format("%s, %.2f, %.2f, %.2f, %.2f, %.2f", ((com.sk89q.worldedit.world.World) currentValue.getExtent()).getName(), currentValue.getX(), currentValue.getY(), currentValue.getZ(), currentValue.getYaw(), currentValue.getPitch()) : (defaultValue != null ? String.format("%s, %.2f,%.2f,%.2f,%.2f,%.2f", defaultValue.getExtent(), defaultValue.getX(), defaultValue.getY(), defaultValue.getZ(), defaultValue.getYaw(), defaultValue.getPitch()) : "")),
                            Integer.MAX_VALUE,
                            TextDialogInput.MultilineOptions.create(null, null)
                    );
                },
                (flag, region, player, response) -> {
                    String rawValue = response.getText(getFlagNameForDialogInputKey(flag));
                    if (rawValue != null) {
                        Location defaultValue = flag.getDefault();
                        Location oldValue = region.region().getFlag(flag);
                        Location value;

                        rawValue = rawValue.replace(", ", ",");

                        if (!rawValue.isEmpty()) {
                            try {
                                String world = null;
                                if (rawValue.contains(",")) {
                                    world = rawValue.substring(0, rawValue.indexOf(','));
                                    rawValue = rawValue.substring(rawValue.indexOf(',') + 1);
                                }
                                // Its hacky but FlagContext.create().build() is calling Event internally, and that not allowed asynchronously call.
                                // So we use reflection to create FlagContext instance directly.
                                Constructor<FlagContext> ctxConstructor = (Constructor<FlagContext>) FlagContext.class.getDeclaredConstructors()[0];
                                ctxConstructor.trySetAccessible();
                                FlagContext ctx = ctxConstructor.newInstance(WorldGuardPlugin.inst().wrapPlayer(player), rawValue, Map.of("region", region.region()));
                                // Tried this but thrown TimeoutException always.
                                // Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> FlagContext.create().setInput(rawValue).build()).get();
                                value = flag.parseInput(ctx);
                                if (world != null) { // Parse world manually
                                    World bukkitWorld = Bukkit.getWorld(world);
                                    if (bukkitWorld == null) throw new IllegalArgumentException("World not found: " + world);
                                    value.setExtent(BukkitAdapter.adapt(bukkitWorld));
                                }
                            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InvalidFlagFormat e) {
                                NewMessageUtil.sendErrorMessage(player, Component.text("フラグ " + ProtectionGuiUtil.getFlagDisplayName(flag) + " の入力値「" + rawValue + "」を解釈できませんでした").hoverEvent(HoverEvent.showText(Component.text(e.getLocalizedMessage(), DefinedTextColor.RED))));
                                LOGGER.warn("Failed to parse LocationFlag value \"{}\" while parsing player {}'s input for flag {} in region {}", rawValue, player.getName(), flag.getName(), region.region().getId(), e);
                                return;
                            }
                        } else {
                            value = null;
                        }

                        if (Objects.equals(oldValue, value)) {
                            return; // Nothing changed
                        }

                        if (defaultValue != null && value != null && value.equals(defaultValue)) {
                            value = null;
                        }

                        region.region().setFlag(flag, value);

                        if (!Objects.equals(oldValue, value)) {
                            NewMessageUtil.sendMessage(player, getSuccessfullyModifiedMessage(flag, region, Component.text(oldValue == null ? "null" : oldValue.toString()), Component.text(value == null ? "null" : value.toString())), true);
                        }
                    }
                }
        );

        registerUnsafeFlagHandler(RegistryFlag.class,
                new DialogInputBuilder() {
                    @Override
                    public DialogInput buildDialogInput(Flag rawFlag, WorldGuard.WrappedProtectedRegion region, Player player, int width) {
                        if (rawFlag instanceof RegistryFlag<?> flag) {
                            Keyed defaultValue = flag.getDefault();
                            Keyed currentValue = region.region().getFlag(flag);

                            return DialogInput.text(
                                    getFlagNameForDialogInputKey(flag),
                                    width,
                                    Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)).appendSpace().append(Component.text(flag.getRegistry().values().toString())),
                                    true,
                                    (currentValue != null ? currentValue.id() : (defaultValue != null ? defaultValue.id() : "")),
                                    Integer.MAX_VALUE,
                                    TextDialogInput.MultilineOptions.create(null, null)
                            );
                        }
                        return null;
                    }
                },
                new SaveProcessor() {
                    @Override
                    public void processSave(Flag rawFlag, WorldGuard.WrappedProtectedRegion region, Player player, DialogResponseView response) {
                        if (rawFlag instanceof RegistryFlag<?> flag) {
                            String rawValue = response.getText(getFlagNameForDialogInputKey(flag));
                            if (rawValue != null) {
                                Keyed defaultValue = flag.getDefault();

                                Keyed oldValue = region.region().getFlag(flag);

                                Keyed value = null;
                                if (!rawValue.isEmpty()) {
                                    try {
                                        // Its hacky but FlagContext.create().build() is calling Event internally, and that not allowed asynchronously call.
                                        // So we use reflection to create FlagContext instance directly.
                                        Constructor<FlagContext> ctxConstructor = (Constructor<FlagContext>) FlagContext.class.getDeclaredConstructors()[0];
                                        ctxConstructor.trySetAccessible();
                                        FlagContext ctx = ctxConstructor.newInstance(null, rawValue, Maps.newHashMap());
                                        // Tried this but thrown TimeoutException always.
                                        // Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> FlagContext.create().setInput(rawValue).build()).get();
                                        value = flag.parseInput(ctx);
                                    } catch (InvalidFlagFormat | InstantiationException | IllegalAccessException |
                                             InvocationTargetException e) {
                                        NewMessageUtil.sendErrorMessage(player, Component.text("フラグ " + ProtectionGuiUtil.getFlagDisplayName(flag) + " の入力値「" + rawValue + "」を解釈できませんでした").hoverEvent(HoverEvent.showText(Component.text(e.getLocalizedMessage(), DefinedTextColor.RED))));
                                        LOGGER.warn("Failed to parse RegistryFlag value \"{}\" while parsing player {}'s input for flag {} in region {}", rawValue, player.getName(), flag.getName(), region.region().getId(), e);
                                        return;
                                    }
                                }

                                if (Objects.equals(oldValue, value)) {
                                    return; // Nothing changed
                                }

                                if (defaultValue != null && value != null && value.id().equals(defaultValue.id())) {
                                    value = null;
                                }

                                region.region().setFlag((RegistryFlag) flag, value);

                                if (!Objects.equals(oldValue, value)) {
                                    NewMessageUtil.sendMessage(player, getSuccessfullyModifiedMessage(flag, region, Component.text(oldValue == null ? "null" : oldValue.id()), Component.text(value == null ? "null" : value.id())), true);
                                }
                            }
                        }
                    }
                });

        registerUnsafeFlagHandler(SetFlag.class,
                new DialogInputBuilder() {
                    @Override
                    public DialogInput buildDialogInput(Flag rawFlag, WorldGuard.WrappedProtectedRegion region, Player player, int width) {
                        if (rawFlag instanceof SetFlag<?> flag) {
                            if (flag.getType() instanceof CommandStringFlag || flag.getType() instanceof StringFlag) {
                                Set<String> defaultValues = (Set<String>) flag.getDefault();
                                Set<String> currentValues = (Set<String>) region.region().getFlag(flag);

                                return DialogInput.text(
                                        getFlagNameForDialogInputKey(flag),
                                        width,
                                        Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)).append(Component.text(" [改行ごとに1個]")),
                                        true,
                                        String.join("\n", (currentValues != null ? currentValues : (defaultValues != null ? defaultValues : Collections.emptyList()))),
                                        Integer.MAX_VALUE,
                                        TextDialogInput.MultilineOptions.create(null, Math.min(512, currentValues != null ? 64 + (8*currentValues.size()) : (defaultValues != null ? 64 + (8*defaultValues.size()) : 64)))
                                );
                            }

                            if (flag.getType() instanceof RegistryFlag<?>) { // RegistryFlag<? extends Keyed>
                                Set<Keyed> defaultValues = (Set<Keyed>) flag.getDefault();
                                Set<Keyed> currentValues = (Set<Keyed>) region.region().getFlag(flag);

                                return DialogInput.text(
                                        getFlagNameForDialogInputKey(flag),
                                        width,
                                        Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)).append(Component.text(" [改行ごとに1個]")),
                                        true,
                                        String.join("\n", (currentValues != null ? currentValues.stream().map(Keyed::id).toList() : (defaultValues != null ? defaultValues.stream().map(Keyed::id).toList() : Collections.emptyList()))),
                                        Integer.MAX_VALUE,
                                        TextDialogInput.MultilineOptions.create(null, Math.min(512, currentValues != null ? 64 + (8*currentValues.size()) : (defaultValues != null ? 64 + (8*defaultValues.size()) : 64)))
                                );
                            }
                            LOGGER.warn("Unsupported SetFlag flag type: " + flag.getType() + "(" + flag.getType().getClass() + "): " + flag.getName());
                        }
                        return null;
                    }
                },
                new SaveProcessor() {
                    @Override
                    public void processSave(Flag rawFlag, WorldGuard.WrappedProtectedRegion region, Player player, DialogResponseView response) {
                        if (rawFlag instanceof SetFlag<?> flag) {
                            String rawValue = response.getText(getFlagNameForDialogInputKey(flag));
                            if (rawValue != null) {
                                Set<?> defaultValue = flag.getDefault();
                                Set<?> oldValue = region.region().getFlag(flag);
                                Set<?> value = Arrays.stream(rawValue.split("\n"))
                                        .filter(entry -> !entry.isEmpty())
                                        .map(entry -> {
                                            try {
                                                // Its hacky but FlagContext.create().build() is calling Event internally, and that not allowed asynchronously call.
                                                // So we use reflection to create FlagContext instance directly.
                                                Constructor<FlagContext> ctxConstructor = (Constructor<FlagContext>) FlagContext.class.getDeclaredConstructors()[0];
                                                ctxConstructor.trySetAccessible();
                                                FlagContext ctx = ctxConstructor.newInstance(null, entry, Maps.newHashMap());
                                                // Tried this but thrown TimeoutException always.
                                                // Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> FlagContext.create().setInput(entry).build()).get();
                                                return flag.getType().parseInput(ctx);
                                            } catch (InvalidFlagFormat | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                                NewMessageUtil.sendErrorMessage(player, Component.text("フラグ " + ProtectionGuiUtil.getFlagDisplayName(flag) + " の行「" + entry + "」を解釈できませんでした").hoverEvent(HoverEvent.showText(Component.text(e.getLocalizedMessage(), DefinedTextColor.RED))));
                                                LOGGER.warn("Failed to parse SetFlag entry \"{}\" while parsing player {}'s input for flag {} in region {}", entry, player.getName(), flag.getName(), region.region().getId(), e);
                                                return null;
                                            }
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet());

                                if (oldValue != null && oldValue.containsAll(value) && value.size() == oldValue.size()) {
                                    return; // Nothing changed
                                }

                                if (defaultValue == null && value.isEmpty()) {
                                    value = null; // デフォルトで何も設定されておらず、ユーザーの入力も空欄だった場合は、「未設定」にするため、nullとする
                                }

                                if (defaultValue != null && !defaultValue.isEmpty() && value.parallelStream().allMatch(defaultValue::contains)) {
                                    value = null; // デフォルトで設定されているが、ユーザーの入力と完全に一致する場合は、「未設定」と解釈するため、nullとする
                                }

                                region.region().setFlag((SetFlag) flag, value);

                                if (!Objects.equals(oldValue, value) || (oldValue != null && value != null && !value.parallelStream().allMatch(oldValue::contains) && value.size() != oldValue.size())) {
                                    NewMessageUtil.sendMessage(player, getSuccessfullyModifiedMessage(flag, region, Component.text(oldValue == null ? "null" : oldValue.toString()), Component.text(value == null ? "null" : value.toString())), true);
                                }
                            }
                        }
                    }
                });
    }

    public static Dialog createFlagEditorDialog(Player player, WorldGuard.WrappedProtectedRegion region, Runnable afterAction) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("フラグの設定"))
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .externalTitle(Component.text("フラグの設定(Ex)"))
                        .pause(false)
                        .body(Collections.singletonList(DialogBody.plainMessage(Component.text("保護領域「" + region.getId() + "」のフラグを設定してください"), 200)))
                        .inputs(com.sk89q.worldguard.WorldGuard.getInstance()
                                .getFlagRegistry()
                                .getAll()
                                .parallelStream()
                                .filter(flag -> {
                                    if (player.hasPermission(Permissions.FEATURE_EDIT_ANY_FLAGS.getPermissionNode())) {
                                        return true;
                                    }
                                    return player.hasPermission(Permissions.FEATURE_EDIT_FLAGS.getPermissionNode()) && GLOBALLY_EDITING_ALLOWED_FLAGS.contains(flag.getName());
                                })
                                .map(flag -> tryBuildDialogInput(flag, region, player, 300))
                                .filter(Objects::nonNull)
                                .sorted(Comparator.comparing(DialogInput::key))
                                .toList())
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(Component.text("変更を保存", DefinedTextColor.GREEN), Component.text("この画面で変更したフラグの設定を適用して、保存します"), 128, DialogAction.customClick((response, audience) -> {
                            com.sk89q.worldguard.WorldGuard.getInstance()
                                    .getFlagRegistry()
                                    .getAll()
                                    .parallelStream()
                                    .forEach(flag -> tryProcessSave(flag, region, player, response));
                            afterAction.run();
                        }, ClickCallback.Options.builder().uses(1).build())),
                        ActionButton.create(Component.text("キャンセル", DefinedTextColor.YELLOW), Component.text("この画面で変更したフラグの設定は破棄して、前の画面に戻ります"), 128, DialogAction.customClick((response, audience) -> {
                            afterAction.run();
                        }, ClickCallback.Options.builder().uses(1).build())))
                ));
    }

    public static <T, F extends Flag<T>> void registerFlagHandler(Class<F> target, DialogInputBuilder<T, F> inputBuilder, SaveProcessor<T, F> saveProcessor) {
        if (FLAG_HANDLERS.containsKey(target.getSimpleName())) {
            LOGGER.warn("Overwriting FlagHandler for {}", target.getSimpleName());
        }
        FLAG_HANDLERS.put(target.getSimpleName(), new FlagHandler<>(inputBuilder, saveProcessor));
    }

    private static <F extends Flag<?>> void registerUnsafeFlagHandler(Class<F> target, DialogInputBuilder<?, F> inputBuilder, SaveProcessor<?, F> saveProcessor) {
        if (FLAG_HANDLERS.containsKey(target.getSimpleName())) {
            LOGGER.warn("Overwriting FlagHandler for {}", target.getSimpleName());
        }
        FLAG_HANDLERS.put(target.getSimpleName(), new FlagHandler(inputBuilder, saveProcessor));
    }

    @Nullable
    public static <T, F extends Flag<T>> FlagHandler<T, F> getHandler(F flag) {
        Class<?> targetClass = flag.getClass();
        if (!FLAG_HANDLERS.containsKey(targetClass.getSimpleName())) {
            if (FLAG_HANDLERS.containsKey(targetClass.getSuperclass().getSimpleName())) {
                LOGGER.info("Use the handler for {} to interpret {}.", targetClass.getSuperclass().getSimpleName(), targetClass.getSimpleName());
                targetClass = targetClass.getSuperclass();
            } else {
                LOGGER.warn("Unknown flag: {}({}): {}", flag, flag.getClass(), flag.getName());
                return null;
            }
        }
        return (FlagHandler<T, F>) FLAG_HANDLERS.get(targetClass.getSimpleName());
    }

    @Nullable
    public static <T, F extends Flag<T>> DialogInput tryBuildDialogInput(F flag, WorldGuard.WrappedProtectedRegion region, Player player, int width) {
        FlagHandler<T, F> handler = getHandler(flag);
        return handler != null ? handler.buildDialogInput(flag, region, player, width) : null;
    }

    public static <T, F extends Flag<T>> void tryProcessSave(F flag, WorldGuard.WrappedProtectedRegion region, Player player, DialogResponseView response) {
        FlagHandler<T, F> handler = getHandler(flag);
        if (handler != null) handler.processSave(flag, region, player, response);
    }

    private static String getFlagNameForDialogInputKey(Flag<?> flag) {
        return flag.getName().replace('-', '_');
    }

    private static Component getSuccessfullyModifiedMessage(Flag<?> flag, WorldGuard.WrappedProtectedRegion region, Component from, Component to) {
        return Component.empty()
                .append(Component.text("保護領域 " + region.getId() + " のフラグ " + ProtectionGuiUtil.getFlagDisplayName(flag) + " を "))
                .append(from)
                .append(Component.text(" から "))
                .append(to)
                .append(Component.text(" に変更しました"));
    }

    public static class FlagHandler<T, F extends Flag<T>> {
        private final DialogInputBuilder<T, F> inputBuilder;
        private final SaveProcessor<T, F> saveProcessor;

        public FlagHandler(DialogInputBuilder<T, F> inputBuilder, SaveProcessor<T, F> saveProcessor) {
            this.inputBuilder = inputBuilder;
            this.saveProcessor = saveProcessor;
        }

        public DialogInput buildDialogInput(F flag, WorldGuard.WrappedProtectedRegion region, Player player, int width) {
            return this.inputBuilder.buildDialogInput(flag, region, player, width);
        }

        public void processSave(F flag, WorldGuard.WrappedProtectedRegion region, Player player, DialogResponseView response) {
            this.saveProcessor.processSave(flag, region, player, response);
        }
    }

    @FunctionalInterface
    public interface DialogInputBuilder<T, F extends Flag<T>> {
        DialogInput buildDialogInput(F flag, WorldGuard.WrappedProtectedRegion region, Player player, int width);
    }

    @FunctionalInterface
    public interface SaveProcessor<T, F extends Flag<T>> {
        void processSave(F flag, WorldGuard.WrappedProtectedRegion region, Player player, DialogResponseView response);
    }
}
