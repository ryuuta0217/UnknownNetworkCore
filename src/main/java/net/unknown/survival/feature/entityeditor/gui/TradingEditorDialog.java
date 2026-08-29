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

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.unknown.core.define.DefinedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class TradingEditorDialog {

    public static void showTradeListDialog(Player player, Merchant merchant, @Nullable Runnable afterAction) {
        List<MerchantRecipe> recipes = merchant.getRecipes();

        List<ActionButton> tradeButtons = new ArrayList<>();
        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe r = recipes.get(i);
            int index = i;
            tradeButtons.add(ActionButton.create(
                    Component.text("#" + (i + 1) + " ")
                            .append(Component.translatable(r.getResult().getType().translationKey()))
                            .append(Component.text(" x" + r.getResult().getAmount())),
                    Component.text("使用: " + r.getUses() + "/" + r.getMaxUses()),
                    200,
                    DialogAction.customClick((response, audience) -> {
                        showTradeEditDialog(player, merchant, index,
                                () -> showTradeListDialog(player, merchant, afterAction));
                    }, ClickCallback.Options.builder().uses(1).build())
            ));
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("取引エディタ"))
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .body(List.of(DialogBody.plainMessage(
                                Component.text("取引数: " + recipes.size()), 200)))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(
                        tradeButtons,
                        ActionButton.create(
                                Component.text("閉じる", DefinedTextColor.YELLOW),
                                Component.empty(), 128,
                                DialogAction.customClick((response, audience) -> {
                                    if (afterAction != null) afterAction.run();
                                }, ClickCallback.Options.builder().uses(1).build())),
                        1
                ))
        );

        player.showDialog(dialog);
    }

    public static void showTradeEditDialog(Player player, Merchant merchant, int recipeIndex, @Nullable Runnable afterAction) {
        MerchantRecipe recipe = merchant.getRecipe(recipeIndex);

        List<DialogInput> inputs = List.of(
                DialogInput.text("uses", 200, Component.text("取引回数"), true,
                        String.valueOf(recipe.getUses()), Integer.MAX_VALUE, null),
                DialogInput.text("max_uses", 200, Component.text("最大取引回数"), true,
                        String.valueOf(recipe.getMaxUses()), Integer.MAX_VALUE, null),
                DialogInput.text("villager_exp", 200, Component.text("村人XP"), true,
                        String.valueOf(recipe.getVillagerExperience()), Integer.MAX_VALUE, null),
                DialogInput.text("price_multiplier", 200, Component.text("価格変動倍率"), true,
                        String.valueOf(recipe.getPriceMultiplier()), Integer.MAX_VALUE, null),
                DialogInput.text("demand", 200, Component.text("需要"), true,
                        String.valueOf(recipe.getDemand()), Integer.MAX_VALUE, null),
                DialogInput.text("special_price", 200, Component.text("特別価格"), true,
                        String.valueOf(recipe.getSpecialPrice()), Integer.MAX_VALUE, null),

                DialogInput.singleOption("exp_reward", 200, List.of(
                        SingleOptionDialogInput.OptionEntry.create("true",
                                Component.text("有効", DefinedTextColor.GREEN), recipe.hasExperienceReward()),
                        SingleOptionDialogInput.OptionEntry.create("false",
                                Component.text("無効", DefinedTextColor.RED), !recipe.hasExperienceReward())
                ), Component.text("経験値報酬"), true),

                DialogInput.singleOption("ignore_discounts", 200, List.of(
                        SingleOptionDialogInput.OptionEntry.create("false",
                                Component.text("適用", DefinedTextColor.GREEN), !recipe.shouldIgnoreDiscounts()),
                        SingleOptionDialogInput.OptionEntry.create("true",
                                Component.text("無視", DefinedTextColor.RED), recipe.shouldIgnoreDiscounts())
                ), Component.text("ディスカウント"), true)
        );

        List<ItemStack> ings = recipe.getIngredients();
        Component bodyText = Component.text("取引 #" + (recipeIndex + 1) + " の編集\n\n")
                .append(Component.text("素材1: ")).append(Component.translatable(ings.get(0).getType().translationKey()))
                .append(Component.text(" x" + ings.get(0).getAmount() + "\n"))
                .append(ings.size() > 1 && !ings.get(1).getType().isAir()
                        ? Component.text("素材2: ").append(Component.translatable(ings.get(1).getType().translationKey()))
                        .append(Component.text(" x" + ings.get(1).getAmount() + "\n"))
                        : Component.empty())
                .append(Component.text("結果: ")).append(Component.translatable(recipe.getResult().getType().translationKey()))
                .append(Component.text(" x" + recipe.getResult().getAmount()))
                .append(Component.text("\n\n※ アイテム自体の変更はGUIエディタを使用してください", DefinedTextColor.GRAY));

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("取引 #" + (recipeIndex + 1) + " 編集"))
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .body(List.of(DialogBody.plainMessage(bodyText, 300)))
                        .inputs(inputs)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("保存", DefinedTextColor.GREEN),
                                Component.text("変更を保存します"), 128,
                                DialogAction.customClick((response, audience) -> {
                                    applyDialogResponse(merchant, recipeIndex, response);
                                    if (afterAction != null) afterAction.run();
                                }, ClickCallback.Options.builder().uses(1).build())),
                        ActionButton.create(
                                Component.text("キャンセル", DefinedTextColor.YELLOW),
                                Component.text("変更を破棄します"), 128,
                                DialogAction.customClick((response, audience) -> {
                                    if (afterAction != null) afterAction.run();
                                }, ClickCallback.Options.builder().uses(1).build()))
                ))
        );

        player.showDialog(dialog);
    }

    private static void applyDialogResponse(Merchant merchant, int recipeIndex, DialogResponseView response) {
        MerchantRecipe old = merchant.getRecipe(recipeIndex);

        String usesStr = response.getText("uses");
        String maxUsesStr = response.getText("max_uses");
        String villagerExpStr = response.getText("villager_exp");
        String priceMultiplierStr = response.getText("price_multiplier");
        String demandStr = response.getText("demand");
        String specialPriceStr = response.getText("special_price");

        String expReward = response.getText("exp_reward");
        String ignoreDiscounts = response.getText("ignore_discounts");

        int uses = parseIntOr(usesStr, old.getUses());
        int maxUses = Math.max(1, parseIntOr(maxUsesStr, old.getMaxUses()));
        int villagerExp = parseIntOr(villagerExpStr, old.getVillagerExperience());
        float priceMultiplier = parseFloatOr(priceMultiplierStr, old.getPriceMultiplier());
        int demand = parseIntOr(demandStr, old.getDemand());
        int specialPrice = parseIntOr(specialPriceStr, old.getSpecialPrice());

        MerchantRecipe newRecipe = new MerchantRecipe(
                old.getResult(),
                uses,
                maxUses,
                expReward != null ? "true".equals(expReward) : old.hasExperienceReward(),
                villagerExp,
                priceMultiplier,
                demand,
                specialPrice,
                ignoreDiscounts != null ? "true".equals(ignoreDiscounts) : old.shouldIgnoreDiscounts()
        );
        newRecipe.setIngredients(old.getIngredients());

        merchant.setRecipe(recipeIndex, newRecipe);
    }

    private static int parseIntOr(@Nullable String str, int fallback) {
        if (str == null || str.isEmpty()) return fallback;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloatOr(@Nullable String str, float fallback) {
        if (str == null || str.isEmpty()) return fallback;
        try {
            return Float.parseFloat(str.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
