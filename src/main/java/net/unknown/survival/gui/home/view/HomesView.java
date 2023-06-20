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

package net.unknown.survival.gui.home.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.gui.view.PaginationView;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.model.Home;
import net.unknown.survival.data.model.HomeGroup;
import net.unknown.survival.gui.home.HomeGui;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HomesView extends PaginationView<Home, HomeGui> {
    private final HomeGroupsView homeGroupsView;
    private final HomeGroup homeGroup;

    public HomesView(HomeGui gui, HomeGroupsView homeGroupsView, HomeGroup homeGroup) {
        super(gui,
                homeGroup.getHomes().values(),
                (home) -> new ItemStackBuilder(dimension2Material(home.location()))
                        .displayName(Component.text(home.name(), Style.style(dimension2TextColor(home.location()))))
                        .lore(Component.text("ワールド: " + MessageUtil.getWorldNameDisplay(home.getWorld()), Style.style(TextColor.color(0, 255, 0))),
                                Component.text("座標: " + getCoordinateAsString(home.location()), Style.style(TextColor.color(0, 255, 0))),
                                Component.text("向き: " + getRotationAsString(home.location()), Style.style(TextColor.color(0, 255, 0))),
                                Component.text(""),
                                Component.text("クリックでテレポート", Style.style(TextColor.color(0, 255, 0))),
                                Component.text("Shiftキーを押しながら右クリックで削除", Style.style(TextColor.color(255, 0, 0))))
                        .build(),
                true,
                true);
        this.homeGroupsView = homeGroupsView;
        this.homeGroup = homeGroup;
    }

    @Override
    public void onElementButtonClicked(InventoryClickEvent event, Home home) {
        if (event.isShiftClick() && event.isRightClick()) {
            this.homeGroup.removeHome(home);
            NewMessageUtil.sendMessage(event.getWhoClicked(), "ホーム " + home.name() + " を削除しました");
        } else {
            home.teleportPlayer((Player) event.getWhoClicked());
            event.getWhoClicked().closeInventory();
            NewMessageUtil.sendMessage((Player) event.getWhoClicked(), "ホーム " + home.name() + " にテレポートしました (GUI)");
        }
    }

    @Override
    public void onPreviousButtonClicked(InventoryClickEvent event) {
        this.getGui().setView(this.homeGroupsView);
    }

    @Override
    public void onCreateNewButtonClicked(InventoryClickEvent event) {
        //this.getGui().onceDeferUnregisterOnClose();
        //event.getWhoClicked().closeInventory();
        // TODO SignGui
        NewMessageUtil.sendErrorMessage(event.getWhoClicked(), "未実装だにょーん");
    }

    private static Material dimension2Material(Location loc) {
        return switch (loc.getWorld().getEnvironment()) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.WHITE_WOOL;
        };
    }

    private static TextColor dimension2TextColor(Location loc) {
        return switch (loc.getWorld().getEnvironment()) {
            case NORMAL -> TextColor.color(5635925);
            case NETHER -> TextColor.color(16733525);
            case THE_END -> TextColor.color(16733695);
            default -> TextColor.color(16777215);
        };
    }

    private static String getCoordinateAsString(Location loc) {
        String x = String.valueOf(loc.getX());
        String y = String.valueOf(loc.getY());
        String z = String.valueOf(loc.getZ());

        return x.substring(0, Math.min(x.indexOf(".") + 2, x.length())) + ", " +
                y.substring(0, Math.min(y.indexOf(".") + 2, y.length())) + ", " +
                z.substring(0, Math.min(z.indexOf(".") + 2, z.length()));
    }

    private static String getRotationAsString(Location loc) {
        String yaw = String.valueOf(loc.getYaw());
        String pitch = String.valueOf(loc.getPitch());

        return yaw.substring(0, Math.min(yaw.indexOf(".") + 2, yaw.length())) + ", " + pitch.substring(0, Math.min(pitch.indexOf(".") + 2, pitch.length()));
    }
}
