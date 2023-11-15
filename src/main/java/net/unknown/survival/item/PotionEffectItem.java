package net.unknown.survival.item;

import net.kyori.adventure.text.Component;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.ViewedGuiBase;
import net.unknown.core.gui.view.View;
import net.unknown.core.item.UnknownNetworkItem;
import net.unknown.core.item.UnknownNetworkItemStack;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PotionEffectItem extends UnknownNetworkItem implements Listener {
    private static BukkitTask TASK;

    public PotionEffectItem() {
        super(new NamespacedKey("survival", "potion_effect"));
    }

    // Open a editor
    /*@EventHandler
    public void onInteractItem(PlayerInteractEvent event) {
        if (!event.getPlayer().isOp()) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.OFF_HAND) return;
        if (event.getItem() == null) return;
        if (!this.equals(event.getItem())) return;
        event.setCancelled(true);
        new Editor(event.getPlayer(), new Stack(event.getItem())).open(event.getPlayer());
    }*/

    public static void startTask() {
        if (TASK != null && !TASK.isCancelled()) {
            TASK.cancel();
            TASK = null;
        }

        TASK = RunnableManager.runRepeating(() -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (Items.POTION_EFFECT_ITEM.equals(item)) {
                        Stack stack = new Stack(item);
                        stack.getPotionEffects().forEach((type, level) -> {
                            int duration = getEffectDuration(type, level);
                            player.addPotionEffect(type.createEffect(duration, level));
                        });
                    }
                }
            });
        }, 0L, 1L);
    }

    public static void stopTask() {
        if (TASK != null && !TASK.isCancelled()) {
            TASK.cancel();
            TASK = null;
        }
    }

    public Stack.Builder createBuilder(Material itemType) {
        return new Stack.Builder(new Stack(this.createItemStackBuilder(itemType).build()));
    }

    @Override
    @Deprecated
    public Stack createItemStack() {
        return new Stack.Builder(new Stack(this.createItemStackBuilder(Material.PLAYER_HEAD)
                .displayName(Component.text("ポーション効果のあるプレイヤーの頭", DefinedTextColor.GOLD))
                .custom(is -> is.editMeta(meta -> ((SkullMeta) meta).setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString("65922e55-04b3-48c2-a4f7-5b393692adf4")))))
                .lore(Component.text("浮遊 V", DefinedTextColor.GRAY))
                .build()))
                .addPotionEffect(PotionEffectType.LEVITATION, 5)
                .buildUNStack();
    }

    public static int getEffectDuration(PotionEffectType type, int amplifier) {
        if (type == PotionEffectType.NIGHT_VISION) {
            return 20 * 15;
        }

        return 20 * 10;
    }

    public static class Stack extends UnknownNetworkItemStack<UnknownNetworkItem> {
        private static final NamespacedKey ACTIVE_SLOTS_KEY = new NamespacedKey("survival", "active_slots");
        private static final NamespacedKey EFFECTS_KEY = new NamespacedKey("survival", "potion_effects");
        private final Set<EquipmentSlot> activeSlots = new HashSet<>(Arrays.asList(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND));
        private final Map<PotionEffectType, Integer> effects = new HashMap<>();

        public Stack(ItemStack handle) {
            super(handle, Items.POTION_EFFECT_ITEM);
            this.load();
        }

        private void load() {
            this.activeSlots.clear();
            this.effects.clear();

            PersistentDataContainer dataContainer = this.getHandle().getItemMeta().getPersistentDataContainer();
            PersistentDataContainer effectsContainer = dataContainer.get(EFFECTS_KEY, PersistentDataType.TAG_CONTAINER);

            if (effectsContainer != null) {
                effectsContainer.getKeys().forEach(potionEffectKey -> {
                    PotionEffectType type = PotionEffectType.getByName(potionEffectKey.getNamespace());
                    Integer level = effectsContainer.get(potionEffectKey, PersistentDataType.INTEGER);

                    if (level == null) {
                        throw new IllegalStateException("What? How did this happen? Potion effect level is null!");
                    }

                    this.effects.put(type, level);
                });
            }

            if (dataContainer.has(ACTIVE_SLOTS_KEY, PersistentDataType.INTEGER_ARRAY)) {
                int[] activeSlots = dataContainer.get(ACTIVE_SLOTS_KEY, PersistentDataType.INTEGER_ARRAY);
                if (activeSlots != null) {
                    Arrays.stream(activeSlots)
                            .mapToObj(ordinal -> EquipmentSlot.values()[ordinal])
                            .forEach(this.activeSlots::add);
                }
            } else {
                this.save();
            }
        }

        private void save() {
            this.getHandle().editMeta(meta -> {
                PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
                PersistentDataContainer effectsContainer = dataContainer.getAdapterContext().newPersistentDataContainer();

                this.effects.forEach((type, level) -> effectsContainer.set(type.getKey(), PersistentDataType.INTEGER, level));

                dataContainer.set(EFFECTS_KEY, PersistentDataType.TAG_CONTAINER, effectsContainer);

                dataContainer.set(ACTIVE_SLOTS_KEY, PersistentDataType.INTEGER_ARRAY, this.activeSlots.stream().mapToInt(EquipmentSlot::ordinal).toArray());
            });
        }

        public boolean hasPotionEffect(PotionEffectType type) {
            return this.effects.containsKey(type);
        }

        public void addPotionEffect(PotionEffectType type, int level) {
            if (this.effects.containsKey(type)) {
                throw new IllegalArgumentException("Potion effect already exists on item!");
            }
            this.effects.put(type, level);
            this.save();
        }

        public void removePotionEffect(PotionEffectType type) {
            if (!this.effects.containsKey(type)) {
                throw new IllegalArgumentException("Potion effect does not exist on item!");
            }
            this.effects.remove(type);
            this.save();
        }

        public Map<PotionEffectType, Integer> getPotionEffects() {
            return Collections.unmodifiableMap(this.effects);
        }

        public Builder asBuilder() {
            return new Builder(this);
        }

        public static class Builder extends ItemStackBuilder {
            private final Stack stack;

            public Builder(Stack stack) {
                super(stack.getHandle());
                this.stack = stack;
            }

            public Builder addPotionEffect(PotionEffectType type, int level) {
                this.stack.addPotionEffect(type, level);
                return this;
            }

            public Builder removePotionEffect(PotionEffectType type) {
                this.stack.removePotionEffect(type);
                return this;
            }

            public Builder clearPotionEffects() {
                this.stack.getPotionEffects().keySet().forEach(this.stack::removePotionEffect);
                return this;
            }

            public Builder setPotionEffects(Map<PotionEffectType, Integer> effects) {
                this.clearPotionEffects();
                effects.forEach(this::addPotionEffect);
                return this;
            }

            public Stack buildUNStack() {
                return this.stack;
            }
        }
    }

    /*public static class Editor extends ViewedGuiBase<View> {
        protected final Stack stack;

        public Editor(InventoryHolder owner, Stack stack) {
            super(owner, 54, Component.text("アイテムエディター", DefinedTextColor.DARK_GREEN), true, null);
            this.setView(new Main(this));
            this.stack = stack;
        }

        public static abstract class ViewBase implements View {
            private final Editor gui;

            public ViewBase(Editor gui) {
                this.gui = gui;
            }

            public Editor getGui() {
                return this.gui;
            }
        }

        public static class Main extends ViewBase {
            public Main(Editor gui) {
                super(gui);
            }

            @Override
            public void initialize() {

            }

            @Override
            public void onClick(InventoryClickEvent event) {

            }

            @Override
            public void clearInventory() {

            }
        }
    }*/
}
