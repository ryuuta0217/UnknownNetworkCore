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

package net.unknown.survival.gui.hopper.view;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.launchwrapper.hopper.ItemFilter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class CreateItemFilterView extends ConfigureHopperViewBase {
    private ItemStack filterItem = null;
    private boolean useTag = false;
    private CompoundTag tag = null;

    public CreateItemFilterView(ConfigureHopperView parentView) {
        super(parentView);
    }

    @Override
    public void initialize() {
        this.checkInput();

        this.getGui().getInventory().setItem(45, DefinedItemStackBuilders.leftArrow()
                .displayName(Component.text("戻る", DefinedTextColor.YELLOW))
                .build());

        // S L O T S
        // 13: insert the item slot (copy item by clickEvent)
        // 31: Equals Tag (use Material.NAME_TAG) right click to edit tag using BookGui TODO: create BookGui class
        // 40: Confirm (create filter)
        // 45: Back, cancel (to parent view)
    }

    private void checkInput() {
        if (this.filterItem == null) {
            this.getGui().getInventory().setItem(21, new ItemStackBuilder(Material.PAPER)
                    .displayName(Component.text("フィルター対象アイテム", DefinedTextColor.GOLD))
                    .lore(Component.empty(), Component.text("フィルター設定したいアイテムを持ってクリックしてください", DefinedTextColor.AQUA))
                    .build());
        } else {
            this.getGui().getInventory().setItem(21, this.filterItem);
        }

        if (this.useTag) {
            this.getGui().getInventory().setItem(23, new ItemStackBuilder(Material.NAME_TAG)
                    .displayName(Component.text("NBTタグを指定する", DefinedTextColor.GREEN))
                    .lore(Component.empty(), Component.text("クリックで切り替え", DefinedTextColor.AQUA))
                    .build());

            this.getGui().getInventory().setItem(32, new ItemStackBuilder(Material.WRITABLE_BOOK)
                    .displayName(Component.text("NBTタグを指定する", DefinedTextColor.GREEN))
                    .lore(Component.empty(),
                            (this.tag != null ? Component.text("設定済", DefinedTextColor.GREEN) : Component.text("未設定", DefinedTextColor.RED)),
                            (NewMessageUtil.convertMinecraft2Adventure(new TextComponentTagVisitor("", 0).visit(this.tag != null ? this.tag : new CompoundTag()))),
                            Component.text("クリックで編集", DefinedTextColor.AQUA))
                    .build());
        } else {
            this.getGui().getInventory().setItem(23, new ItemStackBuilder(Material.BARRIER)
                    .displayName(Component.text("NBTタグを無視する", DefinedTextColor.YELLOW))
                    .lore(Component.empty(), Component.text("クリックで切り替え", DefinedTextColor.AQUA))
                    .build());

            this.getGui().getInventory().setItem(32, null);
        }

        if (this.filterItem != null) {
            this.getGui().getInventory().setItem(40, new ItemStackBuilder(Material.LIME_WOOL)
                    .displayName(Component.text("フィルターを作成する", DefinedTextColor.GREEN))
                    .lore(Component.empty(), Component.text("クリックすると、フィルターを作成します。", DefinedTextColor.AQUA))
                    .build());
        } else {
            this.getGui().getInventory().setItem(40, null);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch(event.getSlot()) {
            case 21 -> {
                ItemStack carriedItem = event.getCursor();
                if (carriedItem == null || carriedItem.getType() == Material.AIR) return;
                carriedItem = carriedItem.clone(); // Copy of Cursor Item
                this.filterItem = carriedItem.asOne();
            }

            case 23 -> {
                ItemStack item = event.getCurrentItem();
                if (item == null) return; // If clicked item is null, return;
                this.useTag = item.getType() == Material.BARRIER;
            }

            case 32 -> {
                if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
                this.getGui().onceDeferUnregisterOnClose();
                event.getWhoClicked().closeInventory();

                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.text("チャット欄にNBTタグを入力して送信してください", DefinedTextColor.GREEN), false);
                if (this.tag != null) {
                    NewMessageUtil.sendMessage(event.getWhoClicked(), Component.text("[ここをクリックして現在設定されているタグを補完]", DefinedTextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand(this.tag.getAsString())));
                }

                ListenerManager.waitForEvent(AsyncChatEvent.class, false, EventPriority.LOWEST, (e) -> {
                    try {
                        TagParser.parseTag(PlainTextComponentSerializer.plainText().serialize(e.message()));
                        return e.getPlayer().equals(event.getWhoClicked());
                    } catch (CommandSyntaxException ex) {
                        NewMessageUtil.sendErrorMessage(e.getPlayer(), "タグを解析中にエラーが発生しました: " + ex.getLocalizedMessage());
                        return false;
                    }
                }, (e) -> {
                    // Here is only parse input NBT tag to CompoundTag. Tag parse is already checked in before.
                    String tagStr = PlainTextComponentSerializer.plainText().serialize(e.message());
                    try {
                        this.tag = TagParser.parseTag(tagStr);
                    } catch (CommandSyntaxException ignored) {
                        // Unreachable in here, already checked before.
                    }
                    e.setCancelled(true);
                    Bukkit.getScheduler().callSyncMethod(UnknownNetworkCore.getInstance(), () -> { // this event is asynchronous event, use callSyncMethod to open gui.
                        this.checkInput();
                        this.getGui().open(event.getWhoClicked());
                        return null;
                    });
                }, 1, ListenerManager.TimeType.MINUTES, () -> {
                    Bukkit.getScheduler().callSyncMethod(UnknownNetworkCore.getInstance(), () -> { // TODO: check waitForEvent is asynchronous?
                        this.getGui().open(event.getWhoClicked());
                        return null;
                    }); // When timed out, open again this gui.
                });
            }

            case 40 -> {
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.LIME_WOOL) {
                    ItemFilter filter = new ItemFilter(CraftMagicNumbers.getItem(this.filterItem.getType()), this.useTag ? this.tag : null);
                    this.getGui().getMixinHopper().addFilter(filter);
                    NewMessageUtil.sendMessage(event.getWhoClicked(), "アイテムフィルターを作成しました。", false);
                    if (this.getParentView() instanceof FiltersView filters) {
                        filters.setData(this.getGui().getMixinHopper().getFilters(), false); // update data on filters view.
                    }
                    this.getGui().setView(this.getParentView());
                    return; // avoid execute #checkInput method.
                }
            }

            case 45 -> {
                this.getGui().setView(this.getParentView());
                return; // avoid execute #checkInput method.
            }
        }

        this.checkInput();
    }

    @Override
    public void clearInventory() {
        this.getGui().getInventory().clear();
    }
}
