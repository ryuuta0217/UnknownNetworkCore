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

package net.unknown.survival.gui.protection.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.view.View;
import net.unknown.survival.gui.protection.ProtectionGui;
import net.unknown.survival.gui.protection.ProtectionGuiState;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ProtectionGuiErrorView extends ProtectionGuiViewBase {
    private final Component errorTitle;
    private final List<Component> errorDetail;
    private final View prevView;
    private final ProtectionGuiState prevState;
    private final Consumer<Inventory> initializer;

    public ProtectionGuiErrorView(@Nullable Component errorTitle,
                                  @Nullable List<Component> errorDetail,
                                  @Nonnull ProtectionGui gui,
                                  @Nonnull View prevView,
                                  @Nonnull ProtectionGuiState prevState,
                                  @Nullable Consumer<Inventory> initializer) {
        super(gui);
        this.errorTitle = errorTitle;
        this.errorDetail = errorDetail;
        this.prevView = prevView;
        this.prevState = prevState;
        this.initializer = initializer;
        this.showError();
    }

    private void showError() {
        if (this.initializer != null) this.initializer.accept(this.gui.getInventory());
        IntStream.rangeClosed(0, 44).forEach(i -> {
            this.gui.getInventory().setItem(i, new ItemStackBuilder(Material.BARRIER).displayName(Component.empty()).build());
        });
        if (this.errorTitle == null) return;
        this.gui.getInventory().setItem(13, new ItemStackBuilder(Material.PAPER)
                .displayName(Component.empty()
                        .style(Style.style(DefinedTextColor.WHITE, TextDecoration.ITALIC.withState(false)))
                        .append(this.errorTitle))
                .lore(this.errorDetail.stream()
                        .map(c -> Component.empty()
                                .style(Style.style(DefinedTextColor.WHITE, TextDecoration.ITALIC.withState(false)))
                                .append(c)).toArray(Component[]::new))
                .build());
    }

    @Override
    public void initialize() {
        this.showError();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getSlot() == 45) {
            this.clearInventory();
            this.gui.setGuiState(prevState);
            this.gui.setView(prevView);
            this.prevView.initialize();
        }
    }
}
