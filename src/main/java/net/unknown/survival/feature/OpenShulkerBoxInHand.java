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

package net.unknown.survival.feature;

import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.ListenerManager;
import net.unknown.survival.data.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class OpenShulkerBoxInHand implements Listener {
    private static final NamespacedKey PLAYER_DATA_REGISTRY_KEY = new NamespacedKey("unknown-network", "open_shulker_box_in_hand");
    private static final NamespacedKey PERSISTENT_DATA_CONTAINER_KEY = new NamespacedKey("unknown-network", "opened_in_hand");

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(PERSISTENT_DATA_CONTAINER_KEY, PersistentDataType.BOOLEAN) && event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(PERSISTENT_DATA_CONTAINER_KEY, PersistentDataType.BOOLEAN)) {
            event.setCancelled(true);
            return;
        }
        if (event.getWhoClicked().getInventory().equals(event.getClickedInventory())) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                if (blockStateMeta.getBlockState() instanceof ShulkerBox) {
                    InventoryOpenMode playerOpenMode = getInventoryOpenMode((Player) event.getWhoClicked());

                    boolean canExec = switch (playerOpenMode) {
                        case NONE -> false;
                        case LEFT_CLICK -> event.getClick() == ClickType.LEFT;
                        case SHIFT_LEFT_CLICK -> event.getClick() == ClickType.SHIFT_LEFT;
                        case RIGHT_CLICK -> event.getClick() == ClickType.RIGHT;
                        case SHIFT_RIGHT_CLICK -> event.getClick() == ClickType.SHIFT_RIGHT;
                        case SWAP_OFFHAND -> event.getClick() == ClickType.SWAP_OFFHAND;
                        case DROP -> event.getClick() == ClickType.DROP;
                        case CONTROL_DROP -> event.getClick() == ClickType.CONTROL_DROP;
                    };

                    if (canExec) {
                        boolean success = openShulkerBox((Player) event.getWhoClicked(), event.getCurrentItem());
                        event.setCancelled(success);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getItemMeta() instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof ShulkerBox) {
                OpenMode playerOpenMode = getOpenMode(event.getPlayer());

                boolean canExec = switch (playerOpenMode) {
                    case NONE -> false;
                    case LEFT_CLICK -> event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR;
                    case RIGHT_CLICK -> event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR;
                    case LEFT_CLICK_AIR -> event.getAction() == Action.LEFT_CLICK_AIR;
                    case RIGHT_CLICK_AIR -> event.getAction() == Action.RIGHT_CLICK_AIR;
                    case SHIFT_LEFT_CLICK -> (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) && event.getPlayer().isSneaking();
                    case SHIFT_RIGHT_CLICK -> (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) && event.getPlayer().isSneaking();
                    case SHIFT_LEFT_CLICK_AIR -> event.getAction() == Action.LEFT_CLICK_AIR && event.getPlayer().isSneaking();
                    case SHIFT_RIGHT_CLICK_AIR -> event.getAction() == Action.RIGHT_CLICK_AIR && event.getPlayer().isSneaking();
                };

                if (canExec) {
                    boolean success = openShulkerBox(event.getPlayer(), event.getItem());
                    event.setCancelled(success);
                }
            }
        }
    }

    public static boolean openShulkerBox(Player whoOpen, ItemStack stack) {
        if (stack != null && stack.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof org.bukkit.block.ShulkerBox shulkerBox) {
                if (blockStateMeta.getPersistentDataContainer().has(PERSISTENT_DATA_CONTAINER_KEY, PersistentDataType.BOOLEAN) && blockStateMeta.getPersistentDataContainer().get(PERSISTENT_DATA_CONTAINER_KEY, PersistentDataType.BOOLEAN)) {
                    return false;
                }
                Inventory shulkerBoxInventory = shulkerBox.getInventory();
                Listener closeEventListener = new Listener() {};
                ListenerManager.registerEventListener(InventoryCloseEvent.class, closeEventListener, EventPriority.MONITOR, false, (listener, rawEvent) -> {
                    if (rawEvent instanceof InventoryCloseEvent e) {
                        if (e.getPlayer().getUniqueId().equals(whoOpen.getUniqueId())) {
                            if (e.getInventory().equals(shulkerBoxInventory)) {
                                blockStateMeta.getPersistentDataContainer().remove(PERSISTENT_DATA_CONTAINER_KEY);
                                blockStateMeta.setBlockState(shulkerBox);
                                stack.setItemMeta(blockStateMeta);
                                ListenerManager.unregisterListener(closeEventListener);
                            }
                        }
                    }
                });
                ListenerManager.waitForEvent(InventoryOpenEvent.class, false, EventPriority.MONITOR,
                        (e) -> e.getPlayer().getUniqueId().equals(whoOpen.getUniqueId()) && e.getInventory().equals(shulkerBoxInventory),
                        (e) -> stack.editMeta(meta -> meta.getPersistentDataContainer().set(PERSISTENT_DATA_CONTAINER_KEY, PersistentDataType.BOOLEAN, true)),
                        1, ListenerManager.TimeType.MINUTES, () -> {});
                whoOpen.openInventory(shulkerBoxInventory);
                return true;
            }
        }
        return false;
    }

    public static OpenMode getOpenMode(Player player) {
        return getOpenMode(player.getUniqueId());
    }

    public static OpenMode getOpenMode(UUID uniqueId) {
        return OpenMode.valueOf(PlayerData.of(uniqueId).getRegistries().getOrDefault(PLAYER_DATA_REGISTRY_KEY, "open-mode", OpenMode.SHIFT_RIGHT_CLICK_AIR.name()));
    }

    public static void setOpenMode(Player player, OpenMode mode) {
        setOpenMode(player.getUniqueId(), mode);
    }

    public static void setOpenMode(UUID uniqueId, OpenMode mode) {
        PlayerData.of(uniqueId).getRegistries().put(PLAYER_DATA_REGISTRY_KEY, "open-mode", mode.name());
    }

    public static InventoryOpenMode getInventoryOpenMode(Player player) {
        return getInventoryOpenMode(player.getUniqueId());
    }

    public static InventoryOpenMode getInventoryOpenMode(UUID uniqueId) {
        return InventoryOpenMode.valueOf(PlayerData.of(uniqueId).getRegistries().getOrDefault(PLAYER_DATA_REGISTRY_KEY, "inventory-open-mode", InventoryOpenMode.SHIFT_RIGHT_CLICK.name()));
    }

    public static void setInventoryOpenMode(Player player, InventoryOpenMode mode) {
        setInventoryOpenMode(player.getUniqueId(), mode);
    }

    public static void setInventoryOpenMode(UUID uniqueId, InventoryOpenMode mode) {
        PlayerData.of(uniqueId).getRegistries().put(PLAYER_DATA_REGISTRY_KEY, "inventory-open-mode", mode.name());
    }

    public enum OpenMode {
        NONE(Component.text("シュルカーボックスは手に持って").append(Component.text("何をしても開かない", DefinedTextColor.RED)).append(Component.text("ように設定されています")), Component.text("シュルカーボックスを手に持って").append(Component.text("何をしても開かない", DefinedTextColor.RED)).append(Component.text("ように設定されました"))),
        LEFT_CLICK(Component.text("シュルカーボックスを手に持った状態で").append(Component.text("左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開きます")), Component.text("シュルカーボックスを手に持った状態で").append(Component.text("左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開くように設定されました"))),
        RIGHT_CLICK(Component.text("シュルカーボックスを手に持った状態で").append(Component.text("右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開きます")), Component.text("シュルカーボックスを手に持った状態で").append(Component.text("右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開くように設定されました"))),
        LEFT_CLICK_AIR(Component.text("シュルカーボックスを手に持った状態で").append(Component.text("空中を左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開きます")), Component.text("シュルカーボックスを手に持った状態で空中を").append(Component.text("殴る", DefinedTextColor.YELLOW)).append(Component.text("と開くように設定されました"))),
        RIGHT_CLICK_AIR(Component.text("シュルカーボックスを手に持った状態で").append(Component.text("空中を右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開きます")), Component.text("シュルカーボックスを手に持った状態で空中を").append(Component.text("右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開くように設定されました"))),
        SHIFT_LEFT_CLICK(Component.text("シュルカーボックスを手に持った状態で").append(Component.text("しゃがみながら左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開きます")), Component.text("シュルカーボックスを手に持った状態で").append(Component.text("しゃがみながら左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開くように設定されました"))),
        SHIFT_RIGHT_CLICK(Component.text("シュルカーボックスを手に持った状態で").append(Component.text("しゃがみながら右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開きます")), Component.text("シュルカーボックスを手に持った状態で").append(Component.text("しゃがみながら右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開くように設定されました"))),
        SHIFT_LEFT_CLICK_AIR(Component.text("シュルカーボックスを手に持った状態で").append(Component.text("しゃがみながら空中を左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開きます")), Component.text("シュルカーボックスを手に持った状態で").append(Component.text("しゃがみながら空中を左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開くように設定されました"))),
        SHIFT_RIGHT_CLICK_AIR(Component.text("シュルカーボックスを手に持った状態で").append(Component.text("しゃがみながら空中を右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開きます")), Component.text("シュルカーボックスを手に持った状態で").append(Component.text("しゃがみながら空中を右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開くように設定されました")));

        private final Component description;
        private final Component modeChangedMessage;

        OpenMode(Component description, Component modeChangedMessage) {
            this.description = description;
            this.modeChangedMessage = modeChangedMessage;
        }

        public Component getDescription() {
            return this.description;
        }

        public Component getModeChangedMessage() {
            return this.modeChangedMessage;
        }
    }

    public enum InventoryOpenMode {
        NONE(Component.text("手持ちインベントリでシュルカーボックスに").append(Component.text("何をしても開かない", DefinedTextColor.RED)).append(Component.text("ように設定されています")), Component.text("手持ちインベントリでシュルカーボックスに").append(Component.text("何をしても開かない", DefinedTextColor.RED)).append(Component.text("ように設定されました"))),
        LEFT_CLICK(Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開きます")), Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開くように設定されました"))),
        SHIFT_LEFT_CLICK(Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("Shiftを押しながら左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開きます")), Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("Shiftを押しながら左クリック", DefinedTextColor.YELLOW)).append(Component.text("すると開くように設定されました"))),
        RIGHT_CLICK(Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開きます")), Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開くように設定されました"))),
        SHIFT_RIGHT_CLICK(Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("Shiftを押しながら右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開きます")), Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("Shiftを押しながら右クリック", DefinedTextColor.GREEN)).append(Component.text("すると開くように設定されました"))),
        SWAP_OFFHAND(Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("Fキーを押してオフハンドに入れる", DefinedTextColor.GREEN)).append(Component.text("と開きます")), Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("Fキーを押してオフハンドに入れる", DefinedTextColor.GREEN)).append(Component.text("と開くように設定されました"))),
        DROP(Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("Qを押して捨てる", DefinedTextColor.RED)).append(Component.text("と開きます")), Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("Qを押して捨てる", DefinedTextColor.RED)).append(Component.text("と開くように設定されました"))),
        CONTROL_DROP(Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("CtrlとQを押して捨てる", DefinedTextColor.RED)).append(Component.text("と開きます")), Component.text("手持ちインベントリでシュルカーボックスを").append(Component.text("CtrlとQを押して捨てる", DefinedTextColor.RED)).append(Component.text("と開くように設定されました")));

        private final Component description;
        private final Component modeChangedMessage;

        InventoryOpenMode(Component description, Component modeChangedMessage) {
            this.description = description;
            this.modeChangedMessage = modeChangedMessage;
        }

        public Component getDescription() {
            return this.description;
        }

        public Component getModeChangedMessage() {
            return this.modeChangedMessage;
        }
    }
}
