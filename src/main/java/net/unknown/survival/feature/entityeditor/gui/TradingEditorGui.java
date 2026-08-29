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

package net.unknown.survival.feature.entityeditor.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.SignGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TradingEditorGui extends GuiBase {
    private final Merchant merchant;
    @Nullable
    private final Runnable onReturn;

    private enum Mode {LIST, EDIT}

    private Mode currentMode = Mode.LIST;
    private int currentPage = 0;
    private int editingIndex = -1;

    public TradingEditorGui(Player opener, Merchant merchant, @Nullable Runnable onReturn) {
        super(opener, 54, Component.text("[取引エディタ]", DefinedTextColor.DARK_PURPLE), true);
        this.merchant = merchant;
        this.onReturn = onReturn;
        this.passOnlyThisInventory = false;
        this.update();
    }

    private static Component getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().displayName();
        }
        return Component.translatable(item.getType().translationKey());
    }

    public void update() {
        this.inventory.clear();

        if (this.currentMode == Mode.LIST) {
            List<MerchantRecipe> recipes = this.merchant.getRecipes();
            int totalPages = Math.max(1, (int) Math.ceil(recipes.size() / 45.0));
            if (this.currentPage >= totalPages) this.currentPage = totalPages - 1;

            int start = this.currentPage * 45;
            int end = Math.min(start + 45, recipes.size());

            for (int i = start; i < end; i++) {
                this.inventory.setItem(i - start, createTradeIcon(recipes.get(i), i));
            }

            renderListNavigation(totalPages);
        } else if (this.currentMode == Mode.EDIT) {
            MerchantRecipe recipe = this.merchant.getRecipe(this.editingIndex);
            List<ItemStack> ingredients = recipe.getIngredients();

            ItemStack filler = new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.text(" ")).build();
            for (int i = 0; i < 9; i++) {
                this.inventory.setItem(i, filler);
            }
            for (int i = 18; i < 27; i++) {
                this.inventory.setItem(i, filler);
            }

            this.inventory.setItem(1, new ItemStackBuilder(Material.CHEST).displayName(Component.text("素材1", DefinedTextColor.GOLD)).build());
            this.inventory.setItem(3, new ItemStackBuilder(Material.CHEST).displayName(Component.text("素材2 (任意)", DefinedTextColor.GOLD)).build());
            this.inventory.setItem(7, new ItemStackBuilder(Material.CHEST).displayName(Component.text("結果", DefinedTextColor.GOLD)).build());

            for (int i = 9; i < 18; i++) {
                this.inventory.setItem(i, filler);
            }
            this.inventory.setItem(10, ingredients.size() > 0 ? ingredients.get(0).clone() : null);
            this.inventory.setItem(12, ingredients.size() > 1 ? ingredients.get(1).clone() : null);
            this.inventory.setItem(14, new ItemStackBuilder(Material.ARROW).displayName(Component.text("→", DefinedTextColor.WHITE)).build());
            this.inventory.setItem(16, recipe.getResult().clone());

            this.inventory.setItem(27, new ItemStackBuilder(Material.REPEATER)
                    .displayName(Component.text("取引回数: " + recipe.getUses(), DefinedTextColor.GREEN))
                    .lore(Component.text("左/右クリック: ±1", DefinedTextColor.YELLOW), Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW))
                    .build());

            this.inventory.setItem(28, new ItemStackBuilder(Material.COMPARATOR)
                    .displayName(Component.text("最大取引回数: " + recipe.getMaxUses(), DefinedTextColor.GREEN))
                    .lore(Component.text("左/右クリック: ±1", DefinedTextColor.YELLOW), Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW))
                    .build());

            this.inventory.setItem(29, new ItemStackBuilder(recipe.hasExperienceReward() ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE)
                    .displayName(Component.text("経験値報酬: " + (recipe.hasExperienceReward() ? "有効" : "無効"),
                            recipe.hasExperienceReward() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                    .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                    .build());

            this.inventory.setItem(30, new ItemStackBuilder(Material.EMERALD)
                    .displayName(Component.text("村人XP: " + recipe.getVillagerExperience(), DefinedTextColor.GREEN))
                    .lore(Component.text("左/右クリック: ±1", DefinedTextColor.YELLOW), Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW))
                    .build());

            this.inventory.setItem(31, new ItemStackBuilder(Material.GOLD_INGOT)
                    .displayName(Component.text("価格変動倍率: " + String.format("%.2f", recipe.getPriceMultiplier()), DefinedTextColor.GREEN))
                    .lore(Component.text("左/右クリック: ±0.01", DefinedTextColor.YELLOW), Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW))
                    .build());

            this.inventory.setItem(32, new ItemStackBuilder(Material.HOPPER)
                    .displayName(Component.text("需要: " + recipe.getDemand(), DefinedTextColor.GREEN))
                    .lore(Component.text("左/右クリック: ±1", DefinedTextColor.YELLOW), Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW))
                    .build());

            this.inventory.setItem(33, new ItemStackBuilder(Material.NAME_TAG)
                    .displayName(Component.text("特別価格: " + recipe.getSpecialPrice(), DefinedTextColor.GREEN))
                    .lore(Component.text("左/右クリック: ±1", DefinedTextColor.YELLOW), Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW))
                    .build());

            this.inventory.setItem(34, new ItemStackBuilder(recipe.shouldIgnoreDiscounts() ? Material.BARRIER : Material.LIME_DYE)
                    .displayName(Component.text("ディスカウント無視: " + (recipe.shouldIgnoreDiscounts() ? "有効" : "無効"),
                            recipe.shouldIgnoreDiscounts() ? DefinedTextColor.RED : DefinedTextColor.GREEN))
                    .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                    .build());

            ItemStack navFiller = new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.text(" ")).build();
            for (int i = 45; i < 54; i++) {
                this.inventory.setItem(i, navFiller);
            }

            this.inventory.setItem(45, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("← 一覧に戻る", DefinedTextColor.YELLOW)).build());
            this.inventory.setItem(49, new ItemStackBuilder(Material.LIME_WOOL)
                    .displayName(Component.text("保存して一覧に戻る", DefinedTextColor.GREEN)).build());
        }
    }

    private void renderListNavigation(int totalPages) {
        ItemStack navFiller = new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.text(" ")).build();
        for (int i = 45; i < 54; i++) {
            this.inventory.setItem(i, navFiller);
        }

        if (this.onReturn != null) {
            this.inventory.setItem(45, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("← 戻る", DefinedTextColor.YELLOW)).build());
        }

        this.inventory.setItem(49, DefinedItemStackBuilders.plus()
                .displayName(Component.text("新規取引を追加", DefinedTextColor.GREEN)).build());
        this.inventory.setItem(50, new ItemStackBuilder(Material.CHEST)
                .displayName(Component.text("全補充 (Restock)", DefinedTextColor.GREEN)).build());
        this.inventory.setItem(51, new ItemStackBuilder(Material.TNT)
                .displayName(Component.text("取引全削除", DefinedTextColor.RED)).build());

        if (this.currentPage > 0) {
            this.inventory.setItem(52, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("前のページ", DefinedTextColor.YELLOW)).build());
        }
        if (this.currentPage < totalPages - 1) {
            this.inventory.setItem(53, DefinedItemStackBuilders.rightArrow()
                    .displayName(Component.text("次のページ", DefinedTextColor.YELLOW)).build());
        }
    }

    private ItemStack createTradeIcon(MerchantRecipe recipe, int index) {
        ItemStack icon = recipe.getResult().clone();
        List<ItemStack> ingredients = recipe.getIngredients();

        Component ing1Name = getItemDisplayName(ingredients.get(0));
        Component ing2Name = ingredients.size() > 1 && !ingredients.get(1).getType().isAir()
                ? getItemDisplayName(ingredients.get(1)) : null;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("#" + (index + 1), DefinedTextColor.DARK_GRAY));
        lore.add(Component.empty());

        lore.add(Component.text("素材1: ", DefinedTextColor.GRAY).append(ing1Name).append(
                Component.text(" x" + ingredients.get(0).getAmount(), DefinedTextColor.WHITE)));
        if (ing2Name != null) {
            lore.add(Component.text("素材2: ", DefinedTextColor.GRAY).append(ing2Name).append(
                    Component.text(" x" + ingredients.get(1).getAmount(), DefinedTextColor.WHITE)));
        }
        lore.add(Component.text("結果: ", DefinedTextColor.GRAY).append(getItemDisplayName(icon)).append(
                Component.text(" x" + icon.getAmount(), DefinedTextColor.WHITE)));
        lore.add(Component.empty());

        lore.add(Component.text("取引回数: " + recipe.getUses() + "/" + recipe.getMaxUses(), DefinedTextColor.AQUA));
        lore.add(Component.text("村人XP: " + recipe.getVillagerExperience(), DefinedTextColor.GREEN));
        lore.add(Component.text("プレイヤーXP: " + (recipe.hasExperienceReward() ? "有効" : "無効"), DefinedTextColor.GREEN));
        lore.add(Component.empty());

        lore.add(Component.text("左クリック: 編集", DefinedTextColor.YELLOW));
        lore.add(Component.text("右クリック: 複製", DefinedTextColor.YELLOW));
        lore.add(Component.text("Shift+右クリック: 削除", DefinedTextColor.RED));

        icon.editMeta(meta -> {
            meta.lore(lore);
            Component displayName = meta.hasDisplayName() ? meta.displayName() : Component.translatable(icon.getType().translationKey());
            meta.displayName(Component.text("#" + (index + 1) + " ", DefinedTextColor.GOLD).append(displayName).color(DefinedTextColor.WHITE));
        });

        return icon;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (this.currentMode == Mode.EDIT) {
            int slot = event.getSlot();
            if (this.inventory.equals(event.getClickedInventory())
                    && (slot == 10 || slot == 12 || slot == 16)) {
                event.setCancelled(true);
                ItemStack cursor = event.getCursor();
                this.inventory.setItem(slot, cursor.getType().isAir() ? null : cursor.clone());
                return;
            }
            if (!this.inventory.equals(event.getClickedInventory())) {
                if (!event.getClick().isShiftClick()) {
                    event.setCancelled(false);
                }
                return;
            }
        }

        if (!this.inventory.equals(event.getClickedInventory())) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (this.currentMode == Mode.LIST) {
            handleListClick(event.getSlot(), player, event);
        } else if (this.currentMode == Mode.EDIT) {
            handleEditClick(event.getSlot(), player, event);
        }
    }

    private void handleListClick(int slot, Player player, InventoryClickEvent event) {
        if (slot == 45 && this.onReturn != null) {
            player.closeInventory();
            this.onReturn.run();
            return;
        }
        if (slot == 49) {
            addNewRecipe();
            this.update();
            return;
        }
        if (slot == 50) {
            if (this.merchant instanceof Villager villager) {
                villager.restock();
            }
            this.update();
            return;
        }
        if (slot == 51) {
            this.merchant.setRecipes(new ArrayList<>());
            this.update();
            return;
        }
        if (slot == 52) {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.update();
            }
            return;
        }
        if (slot == 53) {
            int totalPages = Math.max(1, (int) Math.ceil(this.merchant.getRecipes().size() / 45.0));
            if (this.currentPage < totalPages - 1) {
                this.currentPage++;
                this.update();
            }
            return;
        }

        if (slot >= 0 && slot < 45) {
            int recipeIndex = this.currentPage * 45 + slot;
            List<MerchantRecipe> recipes = this.merchant.getRecipes();
            if (recipeIndex >= recipes.size()) return;

            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                List<MerchantRecipe> mutable = new ArrayList<>(recipes);
                mutable.remove(recipeIndex);
                this.merchant.setRecipes(mutable);
                this.update();
            } else if (event.isRightClick()) {
                MerchantRecipe original = recipes.get(recipeIndex);
                MerchantRecipe copy = new MerchantRecipe(original);
                List<MerchantRecipe> mutable = new ArrayList<>(recipes);
                mutable.add(recipeIndex + 1, copy);
                this.merchant.setRecipes(mutable);
                this.update();
            } else if (event.isLeftClick()) {
                this.currentMode = Mode.EDIT;
                this.editingIndex = recipeIndex;
                this.update();
            }
        }
    }

    private void handleEditClick(int slot, Player player, InventoryClickEvent event) {
        if (slot == 45) {
            this.currentMode = Mode.LIST;
            this.editingIndex = -1;
            this.update();
            return;
        }
        if (slot == 49) {
            saveEditingRecipe();
            this.currentMode = Mode.LIST;
            this.editingIndex = -1;
            this.update();
            return;
        }

        MerchantRecipe recipe = this.merchant.getRecipe(this.editingIndex);

        switch (slot) {
            case 27 -> {
                if (event.getClick() == ClickType.MIDDLE) {
                    openSignInput(player, "取引回数", recipe.getUses(), val -> {
                        recipe.setUses(Math.max(0, val));
                        save(recipe);
                    });
                } else if (event.isLeftClick()) {
                    recipe.setUses(Math.max(0, recipe.getUses() - 1));
                    save(recipe);
                } else if (event.isRightClick()) {
                    recipe.setUses(recipe.getUses() + 1);
                    save(recipe);
                }
            }
            case 28 -> {
                if (event.getClick() == ClickType.MIDDLE) {
                    openSignInput(player, "最大取引回数", recipe.getMaxUses(), val -> {
                        recipe.setMaxUses(Math.max(1, val));
                        save(recipe);
                    });
                } else if (event.isLeftClick()) {
                    recipe.setMaxUses(Math.max(1, recipe.getMaxUses() - 1));
                    save(recipe);
                } else if (event.isRightClick()) {
                    recipe.setMaxUses(recipe.getMaxUses() + 1);
                    save(recipe);
                }
            }
            case 29 -> {
                recipe.setExperienceReward(!recipe.hasExperienceReward());
                save(recipe);
            }
            case 30 -> {
                if (event.getClick() == ClickType.MIDDLE) {
                    openSignInput(player, "村人XP", recipe.getVillagerExperience(), val -> {
                        recipe.setVillagerExperience(Math.max(0, val));
                        save(recipe);
                    });
                } else if (event.isLeftClick()) {
                    recipe.setVillagerExperience(Math.max(0, recipe.getVillagerExperience() - 1));
                    save(recipe);
                } else if (event.isRightClick()) {
                    recipe.setVillagerExperience(recipe.getVillagerExperience() + 1);
                    save(recipe);
                }
            }
            case 31 -> {
                if (event.getClick() == ClickType.MIDDLE) {
                    openFloatSignInput(player, "価格変動倍率", recipe.getPriceMultiplier(), val -> {
                        recipe.setPriceMultiplier(Math.max(0f, val));
                        save(recipe);
                    });
                } else if (event.isLeftClick()) {
                    recipe.setPriceMultiplier(Math.max(0f, recipe.getPriceMultiplier() - 0.01f));
                    save(recipe);
                } else if (event.isRightClick()) {
                    recipe.setPriceMultiplier(recipe.getPriceMultiplier() + 0.01f);
                    save(recipe);
                }
            }
            case 32 -> {
                if (event.getClick() == ClickType.MIDDLE) {
                    openSignInput(player, "需要", recipe.getDemand(), val -> {
                        recipe.setDemand(val);
                        save(recipe);
                    });
                } else if (event.isLeftClick()) {
                    recipe.setDemand(recipe.getDemand() - 1);
                    save(recipe);
                } else if (event.isRightClick()) {
                    recipe.setDemand(recipe.getDemand() + 1);
                    save(recipe);
                }
            }
            case 33 -> {
                if (event.getClick() == ClickType.MIDDLE) {
                    openSignInput(player, "特別価格", recipe.getSpecialPrice(), val -> {
                        recipe.setSpecialPrice(val);
                        save(recipe);
                    });
                } else if (event.isLeftClick()) {
                    recipe.setSpecialPrice(recipe.getSpecialPrice() - 1);
                    save(recipe);
                } else if (event.isRightClick()) {
                    recipe.setSpecialPrice(recipe.getSpecialPrice() + 1);
                    save(recipe);
                }
            }
            case 34 -> {
                recipe.setIgnoreDiscounts(!recipe.shouldIgnoreDiscounts());
                save(recipe);
            }
        }

        if (slot != 45 && slot != 49 && event.getClick() != ClickType.MIDDLE) {
            this.update();
        }
    }

    private void addNewRecipe() {
        MerchantRecipe recipe = new MerchantRecipe(
                new ItemStack(Material.STONE),
                0,
                12,
                true,
                1,
                0.0f,
                0,
                0,
                false
        );
        recipe.setIngredients(List.of(new ItemStack(Material.EMERALD)));
        List<MerchantRecipe> recipes = new ArrayList<>(this.merchant.getRecipes());
        recipes.add(recipe);
        this.merchant.setRecipes(recipes);
    }

    private void save(MerchantRecipe recipe) {
        this.merchant.setRecipe(this.editingIndex, recipe);
    }

    private void saveEditingRecipe() {
        MerchantRecipe old = this.merchant.getRecipe(this.editingIndex);
        ItemStack ing1 = this.inventory.getItem(10);
        ItemStack ing2 = this.inventory.getItem(12);
        ItemStack result = this.inventory.getItem(16);
        if (result == null || result.getType().isAir()) result = new ItemStack(Material.STONE);

        MerchantRecipe newRecipe = new MerchantRecipe(
                result, old.getUses(), old.getMaxUses(), old.hasExperienceReward(),
                old.getVillagerExperience(), old.getPriceMultiplier(),
                old.getDemand(), old.getSpecialPrice(), old.shouldIgnoreDiscounts()
        );
        List<ItemStack> ingredients = new ArrayList<>();
        if (ing1 != null && !ing1.getType().isAir()) ingredients.add(ing1);
        else ingredients.add(new ItemStack(Material.EMERALD));
        if (ing2 != null && !ing2.getType().isAir()) ingredients.add(ing2);
        newRecipe.setIngredients(ingredients);
        this.merchant.setRecipe(this.editingIndex, newRecipe);
    }

    private void openSignInput(Player player, String label, int currentValue, java.util.function.IntConsumer onValue) {
        this.onceDeferUnregisterOnClose();
        new SignGui()
                .withTarget(player)
                .withLines(
                        Component.text(String.valueOf(currentValue)),
                        Component.text("^^^"),
                        Component.text(label + "を入力"),
                        Component.empty()
                )
                .onComplete(lines -> {
                    try {
                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                        onValue.accept(val);
                    } catch (NumberFormatException ignored) {
                    }
                    this.update();
                    this.open(player);
                })
                .open();
    }

    private void openFloatSignInput(Player player, String label, float currentValue, java.util.function.Consumer<Float> onValue) {
        this.onceDeferUnregisterOnClose();
        new SignGui()
                .withTarget(player)
                .withLines(
                        Component.text(String.format("%.2f", currentValue)),
                        Component.text("^^^"),
                        Component.text(label + "を入力"),
                        Component.empty()
                )
                .onComplete(lines -> {
                    try {
                        float val = Float.parseFloat(((TextComponent) lines.get(0)).content());
                        onValue.accept(val);
                    } catch (NumberFormatException ignored) {
                    }
                    this.update();
                    this.open(player);
                })
                .open();
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (currentMode == Mode.EDIT) {
            ItemStack cursor = event.getPlayer().getItemOnCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                event.getPlayer().setItemOnCursor(null);
                event.getPlayer().getInventory().addItem(cursor);
            }
        }
    }
}
