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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class DebugStickEntityEditor extends GuiBase {
    private final Entity entity;

    public DebugStickEntityEditor(Player opener, Entity target) {
        super(opener,
                54,
                Component.empty()
                        .append(Component.text("[ｴﾝﾃｨﾃｨｴﾃﾞｨﾀ] ", DefinedTextColor.DARK_PURPLE))
                        .append(target.customName() != null
                                ? Component.empty()
                                .append(target.customName())
                                .append(Component.text(" (", DefinedTextColor.GRAY).append(Component.translatable(target.getType().translationKey())).append(Component.text(")")))
                                : Component.translatable(target.getType().translationKey())),
                true);
        this.entity = target;
        this.update();
    }

    private void update() {
        this.getInventory().clear();
        net.minecraft.world.entity.Entity nms = MinecraftAdapter.entity(this.entity);
        this.getInventory().setItem(0, new ItemStackBuilder(Material.PLAYER_HEAD)
                .displayName(((Mob) this.entity).hasAI() ? Component.text("AI: 有効", DefinedTextColor.GREEN) : Component.text("AI: 無効", DefinedTextColor.RED))
                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                .build());
        this.getInventory().setItem(1, new ItemStackBuilder(Material.SHIELD)
                .displayName(this.entity.isInvulnerable() ? Component.text("無敵: 有効", DefinedTextColor.GREEN) : Component.text("無敵: 無効", DefinedTextColor.RED))
                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                .build());
        this.getInventory().setItem(2, new ItemStackBuilder(Material.NOTE_BLOCK)
                .displayName(this.entity.isSilent() ? Component.text("消音: 有効", DefinedTextColor.GREEN) : Component.text("消音: 無効", DefinedTextColor.RED))
                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                .build());
        this.getInventory().setItem(3, new ItemStackBuilder(Material.END_PORTAL_FRAME)
                .displayName(!this.entity.hasGravity() ? Component.text("無重力: 有効", DefinedTextColor.GREEN) : Component.text("無重力: 無効", DefinedTextColor.RED))
                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                .build());
        this.getInventory().setItem(4, new ItemStackBuilder(Material.LIGHT)
                .displayName(this.entity.isGlowing() ? Component.text("発光: 有効", DefinedTextColor.GREEN) : Component.text("発光: 無効", DefinedTextColor.RED))
                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                .build());
        this.getInventory().setItem(5, new ItemStackBuilder(Material.GLASS)
                .displayName(((Mob) this.entity).isInvisible() ? Component.text("透明: 有効", DefinedTextColor.GREEN) : Component.text("透明: 無効", DefinedTextColor.RED))
                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                .build());
        this.getInventory().setItem(27, new ItemStackBuilder(Material.COMPASS)
                .displayName(Component.text("向きを変更", DefinedTextColor.YELLOW))
                .lore(Component.text("X: " + nms.getXRot() + " | Y: " + nms.getYRot(), DefinedTextColor.GREEN),
                        Component.empty(),
                        Component.text("左クリックで向き(X)をインクリメント", DefinedTextColor.YELLOW),
                        Component.text("右クリックで向き(Y)をインクリメント", DefinedTextColor.YELLOW),
                        Component.text("Shiftを押しながらでデクリメント", DefinedTextColor.YELLOW),
                        Component.text("中クリックでリセット", DefinedTextColor.YELLOW))
                .build());

        if(this.entity instanceof Pig pig) {
            this.getInventory().setItem(45, new ItemStackBuilder(Material.SADDLE)
                    .displayName(pig.hasSaddle() ? Component.text("サドル: 装備中", DefinedTextColor.GREEN) : Component.text("サドル: 未装備", DefinedTextColor.RED))
                    .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                    .build());
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if(event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        switch (event.getSlot()) {
            case 0 -> {
                if(this.entity instanceof Mob mob) {
                    mob.setAI(!mob.hasAI());
                    this.update();
                }
            }
            case 1 -> {
                this.entity.setInvulnerable(!this.entity.isInvulnerable());
                this.update();
            }
            case 2 -> {
                this.entity.setSilent(!this.entity.isSilent());
                this.update();
            }
            case 3 -> {
                this.entity.setGravity(!this.entity.hasGravity());
                this.update();
            }
            case 4 -> {
                this.entity.setGlowing(!this.entity.isGlowing());
                this.update();
            }
            case 5 -> {
                if(this.entity instanceof Mob mob) {
                    mob.setInvisible(!mob.isInvisible());
                    boolean oldGlowing = mob.isGlowing(); // 透明化させたときに既に発光しているかどうか
                    mob.setGlowing(mob.isInvisible()); // 透明化の際に発光するようにする、実体化に戻したら発光を取り消す
                    if (!oldGlowing && mob.isGlowing()) {
                        event.getWhoClicked().sendMessage(Component.text("自動的に [発光] が有効になりました。\n" +
                                "[発光] はオフにすることもできます。", DefinedTextColor.YELLOW));
                    }
                    this.update();
                }
            }
            case 27 -> {
                net.minecraft.world.entity.Entity entity = ((CraftEntity) this.entity).getHandle();
                if(event.isLeftClick()) {
                    // XRot (上下)
                    if(!event.isShiftClick()) entity.setXRot(loop(entity.getXRot() + 10f, -180f, 180f));
                    else entity.setXRot(loop(entity.getXRot() - 10f, -180f, 180f));
                } else if (event.isRightClick()) {
                    // YRot (左右)
                    if(!event.isShiftClick()) entity.setYRot(loop(entity.getYRot() + 10f, -180f, 180f));
                    else entity.setYRot(loop(entity.getYRot() - 10f, -180f, 180f));

                    entity.setYHeadRot(entity.getYRot()); // NoAI時の頭と体の追従
                    entity.setYBodyRot(entity.getYRot()); // YBodyRotはYHeadRotに追従するが念のため。
                } else if (event.getClick() == ClickType.MIDDLE) {
                    entity.setXRot(0f);
                    entity.setYRot(0f);
                    entity.setYHeadRot(0f);
                    entity.setYBodyRot(0f);
                }
                this.update();
            }
            case 45 -> {
                if(this.entity instanceof Pig pig) {
                    pig.setSaddle(!pig.hasSaddle());
                    this.update();
                }
            }
        }
    }

    public static float loop(float exp, float min, float max) {
        float result = exp;
        if(exp >= max) {
            float rem = exp - max;
            result = min + rem;
        } else if (exp <= min) {
            float rem = exp - min;
            result = max + rem;
        }
        return result;
    }

    public static class Listener implements org.bukkit.event.Listener {
        public static final Listener INSTANCE = new Listener();

        public static void register() {
            ListenerManager.registerListener(INSTANCE);
        }

        public static void unregister() {
            ListenerManager.unregisterListener(INSTANCE);
        }

        @EventHandler
        public void onInteractEntity(PlayerInteractEntityEvent event) {
            if(event.getHand() == EquipmentSlot.HAND) {
                if(event.getPlayer().getInventory().getItemInMainHand().getType() == Material.DEBUG_STICK) {
                    if(event.getPlayer().hasPermission(Permissions.ENTITY_EDITOR.getPermissionNode())) {
                        if(event.getRightClicked() instanceof Mob mob) {
                            event.getPlayer().openInventory(new DebugStickEntityEditor(event.getPlayer(), mob).getInventory());
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
