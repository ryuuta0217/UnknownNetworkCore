package net.unknown.survival.pass;

import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.unknown.core.advancements.AdvancementManager;
import net.unknown.core.builder.advancement.DisplayInfoBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UnknownNetworkPass {
    public static void send(Player player) {
        Advancement.Builder tabBuilder = Advancement.Builder.advancement();
        tabBuilder.display(new DisplayInfoBuilder()
                .icon(MinecraftAdapter.ItemStack.itemStack(new ItemStack(Material.CREEPER_BANNER_PATTERN)))
                .title(NewMessageUtil.convertAdventure2Minecraft(Component.text("Unknown Network Pass", DefinedTextColor.GOLD)))
                .description(NewMessageUtil.convertAdventure2Minecraft(Component.text("Unknown Network Pass とは？").appendNewline().append(Component.text("様々なミッションをクリアすることで、ミッションポイントがたまり、報酬を受け取ることができます！")).appendNewline().append(Component.text("毎月更新"))))
                .announceChat(false)
                .showToast(true)
                .background(ResourceLocation.tryBySeparator("minecraft:textures/block/dirt.png", ':'))
                .build());
        AdvancementManager.register(tabBuilder.build(ResourceLocation.tryBySeparator("unknown-network:pass/root", ':')));

        AdvancementManager.send(MinecraftAdapter.player(player));
    }
}
