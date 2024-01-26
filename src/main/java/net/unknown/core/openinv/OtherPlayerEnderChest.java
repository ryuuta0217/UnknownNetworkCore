package net.unknown.core.openinv;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.ObfuscationUtil;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class OtherPlayerEnderChest extends PlayerEnderChestContainer implements Listener {
    public static final Set<OtherPlayerEnderChest> INSTANCES = new HashSet<>();

    static {
        RunnableManager.runRepeating(OtherPlayerEnderChest::gc, 20 * 30, 20 * 30);
    }

    private static void gc() {
        INSTANCES.removeIf(instance -> {
            if (instance.active && !instance.getViewers().isEmpty()) {
                return false;
            } else {
                instance.active = false;
                if (instance.saveTask != null && !instance.saveTask.isCancelled()) instance.saveTask.cancel();
                ListenerManager.unregisterListener(instance);
                //System.out.println(instance.player.getName() + "'s ender chest gc() executed. (" + instance + ")");
                return true;
            }
        });
    }

    private boolean active;
    private final CraftInventory bukkit = new CraftInventory(this);
    private net.minecraft.world.entity.player.Player player;
    private boolean online = false;
    private boolean dirty = false;
    private final BukkitTask saveTask;

    public OtherPlayerEnderChest(ServerPlayer minecraft) {
        super(minecraft);

        ListenerManager.registerListener(this);

        this.active = true;

        INSTANCES.add(this);
        this.online = minecraft.isRealPlayer;
        this.player = minecraft;

        NonNullList<ItemStack> items = this.player.getEnderChestInventory().items;

        try {
            ObfuscationUtil.Class obfuscatedSimpleContainerClass = ObfuscationUtil.getClassByName(SimpleContainer.class.getName());
            if (obfuscatedSimpleContainerClass == null) throw new IllegalStateException("What? SimpleContainer is not obfuscated? Class name is " + SimpleContainer.class.getName());

            ReflectionUtil.setFinalObject(obfuscatedSimpleContainerClass.getFieldByMojangName("items").getField(), this, items);
        } catch(NoSuchFieldException e) {
            e.printStackTrace(); // ここには到達しないでほしい (お祈り)
        }

        this.saveTask = RunnableManager.runRepeating(() -> {
            if (this.active && this.dirty && !this.online) {
                System.out.println("Background saving " + this.player.getName() + "'s ender chest...");
                ((ServerPlayer) this.player).getBukkitEntity().saveData();
                this.dirty = false;
            }
        }, 20, 20);
    }

    public OtherPlayerEnderChest(Player bukkit) {
        this(MinecraftAdapter.player(bukkit));
    }

    public OtherPlayerEnderChest(net.minecraft.world.entity.player.Player minecraft) {
        this((Player) minecraft.getBukkitEntity());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

    }

    public CraftInventory getBukkitInventory() {
        return this.bukkit;
    }

    public InventoryView open(Player bukkitPlayer) {
        // TODO: CraftHumanEntity#openCustomInventory の処理を参照する, またはOpenInv/internal/PlayerDataManager#openInventory
        return bukkitPlayer.openInventory(this.bukkit);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!this.online) this.dirty = true;
    }
}
