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
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.launchwrapper.linkchest.LinkChestMode;
import net.unknown.launchwrapper.mixininterfaces.IMixinChestBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ChestLinkStick implements Listener {
    private static boolean SKIP_NEXT_INTERACT_EVENT = false;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (SKIP_NEXT_INTERACT_EVENT) {
            SKIP_NEXT_INTERACT_EVENT = false;
            return;
        }
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.CHEST) return;
        ItemStack handItem = event.getItem();
        if (handItem == null || !isChestLinkStick(event.getItem())) return;
        event.setCancelled(true);

        StickMode stickMode = getStickMode(handItem);
        if (stickMode == null) return;

        switch (stickMode) {
            case SHOW_INFORMATION -> showInformation(event.getPlayer(), handItem, clickedBlock.getLocation());
            case SET_SOURCE -> setSource(event.getPlayer(), handItem, clickedBlock.getLocation());
            case ADD_CLIENT -> addClient(event.getPlayer(), handItem, clickedBlock.getLocation());
            case REMOVE_CLIENT -> removeClient(event.getPlayer(), handItem, clickedBlock.getLocation());
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getPreviousSlot() == event.getNewSlot()) return;
        ItemStack heldItem = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        if (!isChestLinkStick(heldItem)) return;
        event.setCancelled(true);
        switchStickMode(event.getPlayer(), heldItem);
    }

    private static boolean isChestLinkStick(ItemStack stack) {
        return stack != null && stack.getType() == Material.STICK && stack.hasItemMeta() && Component.text("チェストリンクの杖", Style.style(DefinedTextColor.GREEN, TextDecoration.BOLD)).decoration(TextDecoration.ITALIC, false).equals(stack.getItemMeta().displayName()) && stack.getItemMeta().hasLore();
    }

    private static boolean hasStickMode(ItemStack stack) {
        if (!isChestLinkStick(stack)) return false;
        if (!stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasLore()) return false;
        return meta.lore().stream().anyMatch(lore -> PlainTextComponentSerializer.plainText().serialize(lore).startsWith("リンクモード: "));
    }

    private static StickMode switchStickMode(Player player, ItemStack stack) {
        StickMode current = getStickMode(stack);
        if (current == null) current = StickMode.SET_SOURCE;
        StickMode next = StickMode.values()[(current.ordinal() + 1) % StickMode.values().length];
        updateStickMode(player, stack, next);
        return next;
    }

    private static void updateStickMode(Player player, ItemStack stack, StickMode newMode) {
        if (!stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();

        Component loreLine = Component.text("リンクモード: " + newMode.getModeName(), DefinedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);

        if (hasStickMode(stack)) {
            meta.lore(meta.lore().stream().map(lore -> {
                if (PlainTextComponentSerializer.plainText().serialize(lore).startsWith("リンクモード: ")) return loreLine;
                return lore;
            }).toList());
        } else {
            meta.lore(new ArrayList<>() {{
                if (meta.hasLore()) addAll(meta.lore());
                add(loreLine);
            }});
        }
        stack.setItemMeta(meta);

        player.sendActionBar(Component.empty()
                .append(stack.displayName())
                .appendSpace()
                .append(Component.text("のモードを切り替えました:"))
                .appendSpace()
                .append(newMode.getDisplayName()));
    }

    private static StickMode getStickMode(ItemStack stick) {
        if (!stick.hasItemMeta()) return null;
        if (!stick.getItemMeta().hasLore()) return null;
        return stick.getItemMeta().lore().stream()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .filter(s -> s.startsWith("リンクモード: "))
                .map(s -> s.split(": ?", 2)[1])
                .map(StickMode::fromModeName)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private static void showInformation(Player player, ItemStack stack, Location blockLoc) {
        if (!isChestLinkStick(stack)) return;
        if (!stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasLore()) return;

        IMixinChestBlockEntity chestBlockEntity = getChestBlockEntity(blockLoc);
        if (chestBlockEntity == null) return;

        if (chestBlockEntity.getChestTransportMode() == LinkChestMode.SOURCE) {
            NewMessageUtil.sendMessage(player, Component.text("このチェストは現在、ソースとなるチェストとして設定されています"));
        } else if (chestBlockEntity.getChestTransportMode() == LinkChestMode.CLIENT) {
            BlockEntity sourceBlockEntity = chestBlockEntity.getLinkSource().getBlockEntity(true, 3);
            if (sourceBlockEntity instanceof IMixinChestBlockEntity sourceChestBlockEntity) {
                if (sourceChestBlockEntity.getChestTransportMode() == LinkChestMode.SOURCE) {
                    Location sourcePos = MinecraftAdapter.location(chestBlockEntity.getLinkSource().serverLevel(), chestBlockEntity.getLinkSource().blockPos().getCenter(), Vec2.ZERO);
                    NewMessageUtil.sendMessage(player, Component.text("このチェストは " + getLocationStrForDisplay(sourcePos) + " のクライアントとして設定されています"));
                } else {
                    NewMessageUtil.sendMessage(player, Component.text("このチェストはクライアントとして設定されていますが、宛先のチェストはクライアントでした"));
                }
            } else {
                NewMessageUtil.sendMessage(player, Component.text("このチェストはクライアントとして設定されていますが、ソースとなるチェストが見つかりませんでした"));
            }
        }
    }

    private static boolean hasSource(ItemStack stack) {
        if (!stack.hasItemMeta()) return false;
        if (!stack.getItemMeta().hasLore()) return false;
        return stack.getItemMeta().lore().stream().anyMatch(lore -> PlainTextComponentSerializer.plainText().serialize(lore).startsWith("ソース: "));
    }

    private static void setSource(Player player, ItemStack stack, @Nullable Location sourcePos) {
        if (!stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasLore()) return;

        if (sourcePos != null) {
            IMixinChestBlockEntity chestBlockEntity = getChestBlockEntity(sourcePos);
            if (chestBlockEntity != null) {
                chestBlockEntity.setChestTransportMode(LinkChestMode.SOURCE);

                Component sourceLore = Component.text(String.format("ソース: %s, %s, %s, %s", sourcePos.getWorld().getName(), sourcePos.getBlockX(), sourcePos.getBlockY(), sourcePos.getBlockZ()), DefinedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);

                if (hasSource(stack)) {
                    meta.lore(meta.lore().stream().map(lore -> {
                        if (PlainTextComponentSerializer.plainText().serialize(lore).startsWith("ソース: "))
                            return sourceLore;
                        return lore;
                    }).toList());
                } else {
                    meta.lore(new ArrayList<>() {{
                        addAll(meta.lore());
                        add(sourceLore);
                    }});
                }
                stack.setItemMeta(meta);
                NewMessageUtil.sendMessage(player, Component.text("ソースとなるチェストを " + getLocationStrForDisplay(sourcePos) + " に設定しました"));
                updateStickMode(player, stack, StickMode.ADD_CLIENT);
            } else {
                NewMessageUtil.sendErrorMessage(player, Component.text("クリックされたチェストは IMixinChestBlockEntity ではありませんでした。これはバグの可能性があります。"));
            }
        } else {
            if (meta.hasLore()) {
                List<Component> lore = meta.lore();
                if (lore != null) {
                    lore.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).startsWith("ソース: "));
                    meta.lore(lore);
                    stack.setItemMeta(meta);
                }
            }
        }
    }

    private static void addClient(Player player, ItemStack stack, Location clientPos) {
        if (!isChestLinkStick(stack)) return;

        IMixinChestBlockEntity chestBlockEntity = getChestBlockEntity(clientPos);
        if (chestBlockEntity != null) {
            Location sourcePos = getSource(stack);
            if (sourcePos != null) {
                IMixinChestBlockEntity sourceChestBlockEntity = getChestBlockEntity(sourcePos);
                if (sourceChestBlockEntity != null && sourceChestBlockEntity.getChestTransportMode() == LinkChestMode.SOURCE) {
                    if (!sourcePos.isChunkLoaded()) sourcePos.getWorld().loadChunk(sourcePos.getChunk());
                    PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, stack, sourcePos.getBlock(), BlockFace.SELF);
                    SKIP_NEXT_INTERACT_EVENT = true;
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.useInteractedBlock() != Event.Result.DENY) {
                        chestBlockEntity.setChestTransportMode(LinkChestMode.CLIENT);
                        chestBlockEntity.setLinkSource(MinecraftAdapter.level(sourcePos.getWorld()), MinecraftAdapter.blockPos(sourcePos));

                        NewMessageUtil.sendMessage(player, Component.text(getLocationStrForDisplay(clientPos) + " のチェストをクライアントとして設定しました"));
                    }
                } else {
                    NewMessageUtil.sendErrorMessage(player, Component.text("ソースとして設定されている座標にチェストが見つかりませんでした。先にソースとなるチェストを設定してください。"));
                    setSource(player, stack, null);
                    updateStickMode(player, stack, StickMode.SET_SOURCE);
                }
            } else {
                NewMessageUtil.sendErrorMessage(player, Component.text("ソースとなるチェストが設定されていません。先にソースとなるチェストを設定してください。"));
                updateStickMode(player, stack, StickMode.SET_SOURCE);
            }
        } else {
            NewMessageUtil.sendErrorMessage(player, Component.text("クリックされたチェストは IMixinChestBlockEntity ではありませんでした。これはバグの可能性があります。"));
        }
    }

    private static void removeClient(Player player, ItemStack stack, Location clientPos) {
        if (!isChestLinkStick(stack)) return;

        IMixinChestBlockEntity chestBlockEntity = getChestBlockEntity(clientPos);
        if (chestBlockEntity != null) {
            if (chestBlockEntity.getChestTransportMode() == LinkChestMode.CLIENT && chestBlockEntity.getLinkSource() != null) {
                chestBlockEntity.setChestTransportMode(LinkChestMode.DISABLED);
                NewMessageUtil.sendMessage(player, Component.text(getLocationStrForDisplay(clientPos) + " のクライアントを解除しました"));
            } else {
                NewMessageUtil.sendErrorMessage(player, Component.text("クリックされたチェストはクライアントとして設定されていません。"));
            }
        } else {
            NewMessageUtil.sendErrorMessage(player, Component.text("クリックされたチェストは IMixinChestBlockEntity ではありませんでした。これはバグの可能性があります。"));
        }
    }

    public static IMixinChestBlockEntity getChestBlockEntity(Location pos) {
        BlockEntity blockEntity = MinecraftAdapter.level(pos.getWorld()).getBlockEntity(MinecraftAdapter.blockPos(pos));
        if (!(blockEntity instanceof IMixinChestBlockEntity chestBlockEntity)) return null;
        return chestBlockEntity;
    }

    private static Location getSource(ItemStack stack) {
        if (!hasSource(stack)) return null;
        return stack.getItemMeta().lore().stream()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .filter(s -> s.startsWith("ソース: "))
                .map(s -> s.split(": ?", 2)[1])
                .map(s -> s.split(", ?", 4))
                .filter(s -> s.length == 4)
                .map(s -> new Location(Bukkit.getWorld(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3])))
                .findFirst().orElse(null);
    }

    private static String getLocationStrForDisplay(Location loc) {
        return MessageUtil.getWorldName(loc.getWorld()) + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    private enum StickMode {
        SHOW_INFORMATION("情報表示", Component.text("チェストの情報を表示", DefinedTextColor.AQUA)),
        SET_SOURCE("ソース設定", Component.text("ソースの設定", DefinedTextColor.YELLOW)),
        ADD_CLIENT("クライアント追加", Component.text("クライアントの追加", DefinedTextColor.GREEN)),
        REMOVE_CLIENT("クライアント削除", Component.text("クライアントの削除", DefinedTextColor.RED));

        private final String modeName;
        private final Component displayName;

        StickMode(String modeName, Component displayName) {
            this.modeName = modeName;
            this.displayName = displayName;
        }

        public String getModeName() {
            return this.modeName;
        }

        public Component getDisplayName() {
            return this.displayName;
        }

        public static StickMode fromModeName(String modeName) {
            for (StickMode mode : StickMode.values()) {
                if (mode.getModeName().equals(modeName)) return mode;
            }
            return null;
        }
    }
}
