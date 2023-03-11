package net.unknown.survival.enchants;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.events.ModifiableBlockBreakEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

public class AutoReplant implements Listener {
    private static final Set<Material> SUPPORTED_CROPS = new HashSet<>() {{
        add(Material.WHEAT);
        add(Material.BEETROOTS);
        add(Material.CARROTS);
        add(Material.POTATOES);
    }};

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(ModifiableBlockBreakEvent event) {
        if (!SUPPORTED_CROPS.contains(event.getOriginal().getBlock().getType())) return;
        ServerPlayer player = MinecraftAdapter.player(event.getOriginal().getPlayer());
        if (player == null) return;

        org.bukkit.inventory.ItemStack selectedBukkitStack = event.getOriginal().getPlayer().getInventory().getItemInMainHand();
        ItemStack selectedStack = MinecraftAdapter.ItemStack.itemStack(selectedBukkitStack);
        if (!(selectedStack.getItem() instanceof HoeItem hoe)) return;
        if (!CustomEnchantUtil.hasEnchantment("自動再植", selectedBukkitStack)) return;

        event.setCustomDrops(true);
        event.getDrops().forEach(bukkitStack -> {
            if (event.getOriginal().getBlock().getType() == Material.WHEAT) {
                if (bukkitStack.getType() == Material.WHEAT_SEEDS) {
                    bukkitStack.setAmount(bukkitStack.getAmount() - 1);
                }
            } else if (event.getOriginal().getBlock().getType() == Material.BEETROOTS) {
                if (bukkitStack.getType() == Material.BEETROOT_SEEDS) {
                    bukkitStack.setAmount(bukkitStack.getAmount() - 1);
                }
            } else {
                bukkitStack.setAmount(bukkitStack.getAmount() - 1);
            }
        });
        event.getOriginal().getBlock().setType(event.getOriginal().getBlock().getType(), true);
    }
}
