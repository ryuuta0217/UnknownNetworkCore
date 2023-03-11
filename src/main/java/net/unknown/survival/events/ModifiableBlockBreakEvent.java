package net.unknown.survival.events;

import net.minecraft.world.level.block.Block;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModifiableBlockBreakEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean customDrops = false;
    private BlockBreakEvent original;
    private List<ItemStack> toDropItems;

    public ModifiableBlockBreakEvent(BlockBreakEvent original) {
        this.original = original;
    }

    public void setCustomDrops(boolean customDrops) {
        this.customDrops = customDrops;
    }

    public boolean isCustomDrops() {
        return this.customDrops;
    }

    public BlockBreakEvent getOriginal() {
        return this.original;
    }

    public List<ItemStack> getDrops() {
        if (this.toDropItems == null) this.toDropItems = new ArrayList<>(this.getOriginalDrops());
        return this.toDropItems;
    }

    private List<ItemStack> getDropsNullable() {
        return this.toDropItems;
    }

    public List<ItemStack> getOriginalDrops() {
        return Block.getDrops(MinecraftAdapter.blockState(this.getOriginal().getBlock()),
                        MinecraftAdapter.level(this.getOriginal().getBlock().getWorld()),
                        MinecraftAdapter.blockPos(this.getOriginal().getBlock().getLocation()),
                        null,
                        MinecraftAdapter.player(this.getOriginal().getPlayer()),
                        MinecraftAdapter.ItemStack.itemStack(this.getOriginal().getPlayer().getInventory().getItemInMainHand()))
                .stream()
                .map(MinecraftAdapter.ItemStack::itemStack)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public static class Listener implements org.bukkit.event.Listener {
        private static final Listener INSTANCE = new Listener();

        public static Listener getInstance() {
            return INSTANCE;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onBlockBreak(BlockBreakEvent event) {
            ModifiableBlockBreakEvent modifiable = new ModifiableBlockBreakEvent(event);
            Bukkit.getPluginManager().callEvent(modifiable);
            if (modifiable.isCustomDrops()) {
                event.setDropItems(false);
                List<ItemStack> drops = modifiable.getDropsNullable();
                if (drops != null && !event.isCancelled()) {
                    drops.forEach(bukkitStack -> {
                        net.minecraft.world.item.ItemStack stack = MinecraftAdapter.ItemStack.itemStack(bukkitStack);
                        Block.popResource(MinecraftAdapter.level(event.getBlock().getWorld()), MinecraftAdapter.blockPos(event.getBlock().getLocation()), stack);
                    });
                }
            }
        }
    }
}
