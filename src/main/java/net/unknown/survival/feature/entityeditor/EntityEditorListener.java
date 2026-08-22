/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

package net.unknown.survival.feature.entityeditor;

import net.kyori.adventure.text.Component;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.enums.Permissions;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.concurrent.ExecutionException;

public class EntityEditorListener implements Listener {
    public static final EntityEditorListener INSTANCE = new EntityEditorListener();
    private static BukkitTask HINT_TASK;

    public static void register() {
        ListenerManager.registerListener(INSTANCE);
        HINT_TASK = RunnableManager.runAsyncRepeating(() -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                PlayerInventory inventory = player.getInventory();
                if (inventory.getItemInMainHand().getType() == Material.DEBUG_STICK) {
                    AttributeInstance rangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
                    double range = rangeAttr != null ? rangeAttr.getValue() : 3.0;
                    try {
                        Entity target = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> rayTraceTarget(player, range)).get();
                        if (target != null) {
                            player.spawnParticle(Particle.END_ROD, target.getLocation(), 1, 0, 0, 0, 0, null, true);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }, 1L, 1L);
    }

    public static void unregister() {
        ListenerManager.unregisterListener(INSTANCE);
        if (HINT_TASK != null) {
            HINT_TASK.cancel();
            HINT_TASK = null;
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.DEBUG_STICK) return;
        if (!event.getPlayer().hasPermission(Permissions.ENTITY_EDITOR.getPermissionNode())) return;

        // ツールアイテムの場合はGUIを開かない (onInteractで処理する)
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(EntityEditor.TOOL_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        new EntityEditor(player, event.getRightClicked()).open(player);
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.DEBUG_STICK) return;
        if (!player.hasPermission(Permissions.ENTITY_EDITOR.getPermissionNode())) return;

        // ツールアイテムかどうか判定
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        String toolActionKey = null;
        if (heldItem.hasItemMeta()) {
            ItemMeta meta = heldItem.getItemMeta();
            toolActionKey = meta.getPersistentDataContainer().get(EntityEditor.TOOL_KEY, PersistentDataType.STRING);
        }

        if (toolActionKey != null) {
            // --- ツールアイテムモード ---
            handleToolAction(player, event, toolActionKey);
            return;
        }

        // --- 通常モード (既存のGUI起動) ---
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // BoundingBoxを持たないエンティティなどのためのフォールバックRayTrace判定
        AttributeInstance rangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        double range = rangeAttr != null ? rangeAttr.getValue() : 3.0;
        Entity target = rayTraceTarget(player, range);

        if (target != null) {
            new EntityEditor(player, target).open(player);
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleToolAction(Player player, PlayerInteractEvent event, String fullActionKey) {
        // アクションキーから精度サフィックスを分離: "display_scale_x_coarse" -> "display_scale_x" + "_coarse"
        float step;
        String baseKey;
        if (fullActionKey.endsWith("_fine")) {
            baseKey = fullActionKey.substring(0, fullActionKey.length() - "_fine".length());
            step = 0.01f;
        } else if (fullActionKey.endsWith("_coarse")) {
            baseKey = fullActionKey.substring(0, fullActionKey.length() - "_coarse".length());
            step = 0.1f;
        } else {
            return; // 不明なサフィックス
        }

        EntityEditorRegistry.ToolAction<Entity> toolAction = EntityEditorRegistry.getToolAction(baseKey);
        if (toolAction == null) return;

        // RayTrace でエンティティを特定
        AttributeInstance rangeAttr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        double range = rangeAttr != null ? rangeAttr.getValue() : 3.0;
        Entity target = rayTraceTarget(player, range);
        if (target == null) return;

        // 型チェック: 対象エンティティがアクションの登録型を継承しているか
        if (!toolAction.entityType().isAssignableFrom(target.getClass())) return;

        // 左クリック: 減少, 右クリック: 増加
        Action action = event.getAction();
        boolean isIncrease = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);
        boolean isDecrease = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        if (!isIncrease && !isDecrease) return;

        float oldValue = toolAction.currentValueGetter().apply(target);
        float delta = isIncrease ? step : -step;
        toolAction.applyAction().accept(target, oldValue + delta);
        float newValue = toolAction.currentValueGetter().apply(target);

        // ActionBar フィードバック
        player.sendActionBar(Component.text(toolAction.displayName() + ": " + String.format("%.2f", oldValue) + " → " + String.format("%.2f", newValue), DefinedTextColor.GREEN));
        event.setCancelled(true);
    }

    /**
     * RayTrace + フォールバックでプレイヤーが見ているエンティティを取得する。
     * 既存の onInteract 内のロジックを抽出したもの。
     */
    private static Entity rayTraceTarget(Player player, double range) {
        Entity target = null;
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range, 0.0, e -> e != player);
        if (result != null && result.getHitEntity() != null) {
            target = result.getHitEntity();
        } else {
            double closestDot = -1.0;
            Location eyeLoc = player.getEyeLocation();
            Vector lookDir = eyeLoc.getDirection().normalize();
            for (Entity e : player.getNearbyEntities(range, range, range)) {
                if (e == player) continue;
                Vector toEntity = e.getLocation().clone().add(0, e.getHeight() / 2.0, 0).subtract(eyeLoc).toVector();
                if (toEntity.lengthSquared() > range * range) continue;
                
                // 距離が近すぎる場合の0除算やNaNを避けるため
                if (toEntity.lengthSquared() < 0.01) {
                    target = e;
                    break;
                }
                
                Vector toEntityNorm = toEntity.clone().normalize();
                double dot = lookDir.dot(toEntityNorm);
                if (dot > 0.95 && dot > closestDot) {
                    closestDot = dot;
                    target = e;
                }
            }
        }
        return target;
    }
}
