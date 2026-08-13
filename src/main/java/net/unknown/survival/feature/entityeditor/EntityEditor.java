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
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class EntityEditor<T extends Entity> extends GuiBase {
    private static final int CONTENT_ROWS = 5;
    private static final int CONTENT_SLOTS = CONTENT_ROWS * 9;
    private static final int NAV_ROW_START = CONTENT_SLOTS;

    private final T target;
    private final List<EntityEditorHandler<? super T>> handlers;
    private final List<List<EntityEditor.Element<? super T>>> allRows = new ArrayList<>();
    private final Map<Integer, BiConsumer<T, InventoryClickEvent>> slotActions = new HashMap<>();
    private int currentPage = 0;

    public EntityEditor(Player opener, T target) {
        super(opener, 54, Component.empty().append(Component.text("[ｴﾝﾃｨﾃｨｴﾃﾞｨﾀ] ", DefinedTextColor.DARK_PURPLE))
                        .append(target.customName() != null
                                ? Component.empty()
                                .append(target.customName())
                                .append(Component.text(" (", DefinedTextColor.GRAY).append(Component.translatable(target.getType().translationKey())).append(Component.text(")")))
                                : Component.translatable(target.getType().translationKey())),
                true);
        this.target = target;
        this.handlers = EntityEditorRegistry.getHandlers((Class<T>) target.getClass());
        this.update();
    }

    public Entity getTarget() {
        return this.target;
    }

    @SuppressWarnings("unchecked")
    public void update() {
        this.allRows.clear();
        List<EntityEditor.Element<? super T>> currentRow = new ArrayList<>();

        for (EntityEditorHandler<? super T> handler : this.handlers) {
            List<? extends EntityEditor.Element<? super T>> elements = handler.getElements(this.target);
            if (elements == null || elements.isEmpty()) continue;

            for (EntityEditor.Element<? super T> element : elements) {
                if (!element.isEmpty() && !element.isLineBreak()) {
                    element = element.withHandlerName(handler.getClass().getSimpleName());
                }

                if (element.isLineBreak()) {
                    if (!currentRow.isEmpty()) {
                        this.allRows.add(new ArrayList<>(currentRow));
                        currentRow.clear();
                    }
                } else {
                    currentRow.add(element);
                    if (currentRow.size() >= 9) {
                        this.allRows.add(new ArrayList<>(currentRow));
                        currentRow.clear();
                    }
                }
            }
        }
        if (!currentRow.isEmpty()) {
            this.allRows.add(new ArrayList<>(currentRow));
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) this.allRows.size() / CONTENT_ROWS));
        if (this.currentPage >= totalPages) this.currentPage = totalPages - 1;

        this.inventory.clear();
        this.slotActions.clear();

        int startRow = this.currentPage * CONTENT_ROWS;
        int endRow = Math.min(startRow + CONTENT_ROWS, this.allRows.size());

        for (int rowIdx = startRow; rowIdx < endRow; rowIdx++) {
            List<EntityEditor.Element<? super T>> row = this.allRows.get(rowIdx);
            int baseSlot = (rowIdx - startRow) * 9;
            for (int col = 0; col < row.size(); col++) {
                EntityEditor.Element<? super T> element = row.get(col);
                if (element.isEmpty()) continue;

                if (element.iconProvider() != null) {
                    ItemStack icon = element.iconProvider().apply(this.target);
                    if (element.handlerName() != null) {
                        icon.editMeta(meta -> {
                            ArrayList<Component> lore = new ArrayList<>(meta.hasLore() ? meta.lore() : Collections.emptyList());
                            lore.add(Component.empty());
                            lore.add(Component.text(element.handlerName(), DefinedTextColor.DARK_GRAY));
                            meta.lore(lore);
                        });
                    }
                    this.inventory.setItem(baseSlot + col, icon);
                }

                if (element.clickHandler() != null) {
                    this.slotActions.put(baseSlot + col, (BiConsumer<T, InventoryClickEvent>) element.clickHandler());
                }
            }
        }

        renderNavigation(totalPages);
    }

    private void renderNavigation(int totalPages) {
        ItemStack navFiller = new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.text(" ")).build();
        for (int i = 0; i < 9; i++) this.inventory.setItem(NAV_ROW_START + i, navFiller);

        if (this.currentPage > 0) {
            this.inventory.setItem(NAV_ROW_START, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("← 前のページ", DefinedTextColor.YELLOW)).build());
        }
        this.inventory.setItem(NAV_ROW_START + 4, new ItemStackBuilder(Material.PAPER)
                .displayName(Component.text("ページ " + (this.currentPage + 1) + "/" + totalPages, DefinedTextColor.WHITE)).build());
        if (this.currentPage < totalPages - 1) {
            this.inventory.setItem(NAV_ROW_START + 8, DefinedItemStackBuilders.rightArrow()
                    .displayName(Component.text("次のページ →", DefinedTextColor.YELLOW)).build());
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == NAV_ROW_START && this.currentPage > 0) {
            this.currentPage--;
            this.update();
            return;
        }
        if (slot == NAV_ROW_START + 8) {
            int totalPages = Math.max(1, (int) Math.ceil((double) this.allRows.size() / CONTENT_ROWS));
            if (this.currentPage < totalPages - 1) {
                this.currentPage++;
                this.update();
                return;
            }
        }

        BiConsumer<T, InventoryClickEvent> action = this.slotActions.get(slot);
        if (action != null) {
            action.accept(this.target, event);
            this.update();
        }
    }

    public record Element<T>(@Nullable String handlerName,
                                            @Nullable Function<T, ItemStack> iconProvider,
                                            @Nullable BiConsumer<T, InventoryClickEvent> clickHandler) {
        private static final Element<?> EMPTY = new Element<>(null, null, null);
        private static final Element<?> LINE_BREAK = new Element<>(null, null, null);
        private static final Element<?> NO_ACTION_BLACK_GLASS_PANE = new Element<>(null, targetEntity -> new ItemStack(Material.BLACK_STAINED_GLASS_PANE), (targetEntity, event) -> {
        });

        public static <T> Element<T> of(@Nullable Function<T, ItemStack> iconProvider,
                                                       @Nullable BiConsumer<T, InventoryClickEvent> clickHandler) {
            return new Element<>(null, iconProvider, clickHandler);
        }

        @SuppressWarnings("unchecked")
        public static <E> Element<E> empty() {
            return (Element<E>) EMPTY;
        }

        @SuppressWarnings("unchecked")
        public static <E> Element<E> lineBreak() {
            return (Element<E>) LINE_BREAK;
        }

        @SuppressWarnings("unchecked")
        public static <E> Element<E> noActionGlassPane() {
            return (Element<E>) NO_ACTION_BLACK_GLASS_PANE;
        }

        public boolean isEmpty() {
            return this.iconProvider == null && this != LINE_BREAK;
        }

        public boolean isLineBreak() {
            return this == LINE_BREAK;
        }

        public Element<T> withHandlerName(String name) {
            return new Element<>(name, this.iconProvider, this.clickHandler);
        }
    }
}
