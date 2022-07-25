/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.survival.feature.gnarms;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.feature.gnarms.module.GNModule;
import net.unknown.survival.feature.gnarms.module.GNModules;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GNDrive implements GN, Listener {
    private static final Set<GNModule> AVAILABLE_MODULES = new HashSet<>() {{
        add(GNModules.FLY);
    }};
    private static final String LORE_SEPARATOR = "{\"text\":\"==========\", \"color\":\"gray\", \"italic\":\"false\"}";
    private static final String LORE_EMPTY = "{\"text\":\"\"}";

    private static final Map<UUID, UUID> PLAYER_OWNED_DRIVES = new HashMap<>();
    public static final Map<UUID, GNDrive> INSTANCES = new HashMap<>();
    private static final Map<UUID, UUID> PLAYER_CURRENT_DRIVE = new HashMap<>();

    private final UUID owner;
    private final UUID id;
    private final ItemStack drive;
    private BukkitTask task;

    public GNDrive(org.bukkit.inventory.ItemStack drive) {
        this.drive = MinecraftAdapter.ItemStack.itemStack(drive);
        this.owner = getOwner(this.drive);
        this.id = getId(this.drive);
        if(!isTagValid(this.drive.getOrCreateTag())) throw new IllegalArgumentException("Invalid GNDrive!");
        INSTANCES.put(this.id, this);
    }

    public GNDrive(ItemStack drive) {
        this.drive = drive;
        this.owner = getOwner(this.drive);
        this.id = getId(this.drive);
        if(!isTagValid(this.drive.getOrCreateTag())) throw new IllegalArgumentException("Invalid GNDrive!");
        INSTANCES.put(this.id, this);
    }

    public static void check() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerInventory inv = player.getInventory();
            if(inv.getChestplate() != null) { // チェストプレートを装備している
                ItemStack chestPlate = MinecraftAdapter.ItemStack.itemStack(inv.getChestplate());
                if(isTagValid(chestPlate.getTag())) { // NBTタグが一致
                    UUID owner = getOwner(chestPlate); // OwnerのUUIDがUtil.NIL_UUIDの場合はIndividual Information Attestation Systemで所有権の初期化を行う
                    if(owner != null && player.getUniqueId().equals(owner)) { // 所有者が適切
                        UUID currentDriveId = getId(chestPlate);

                        if(PLAYER_CURRENT_DRIVE.containsKey(owner)) {
                            UUID oldDriveId = PLAYER_CURRENT_DRIVE.get(owner);

                            if(INSTANCES.containsKey(oldDriveId) && oldDriveId.equals(currentDriveId)) {
                                return; // 以前のチェック時と同じGNドライヴを装備している場合
                            }

                            /* 違うGNドライヴを装備している場合 */
                            GNDrive oldDrive = INSTANCES.get(oldDriveId);
                            oldDrive.stopTick();
                            INSTANCES.remove(oldDriveId);
                        }

                        // 何も装備してない or 普通の装備の状態からGNドライヴを装備 or 以前は違うGNドライヴを装備していたとき
                        GNDrive newDrive = INSTANCES.getOrDefault(currentDriveId, new GNDrive(chestPlate));
                        newDrive.startTick();
                        PLAYER_CURRENT_DRIVE.put(owner, currentDriveId);
                        return;
                    }
                }
            }

            if (PLAYER_CURRENT_DRIVE.containsKey(player.getUniqueId())) {
                UUID driveId = PLAYER_CURRENT_DRIVE.get(player.getUniqueId());
                if (INSTANCES.containsKey(driveId)) {
                    INSTANCES.get(driveId).stopTick();
                    INSTANCES.remove(driveId);
                }
            }
            PLAYER_CURRENT_DRIVE.remove(player.getUniqueId()); // 何も装備してないならカレントから外す
        });
    }

    @Override
    public void startTick() {
        if(!Bukkit.getOfflinePlayer(this.getOwner()).isOnline()) throw new IllegalStateException("Player is now offline!");

        if(this.task == null || this.task.isCancelled()) {
            this.getEnabledModules().forEach(module -> {
                GNContext ctx = new GNContext(Bukkit.getPlayer(this.getOwner()), this.getTag(), 0, -1,
                        -1, -1, true);
                module.onEnable(ctx);
            });
            this.task = RunnableManager.runAsyncRepeating(this::tick, 0, 1);
            ListenerManager.registerListener(this);
        } else {
            throw new IllegalStateException("Task is already running");
        }
    }

    @Override
    public void tick() {
        if(Bukkit.getOfflinePlayer(this.getOwner()).isOnline()) {
            Player player = Bukkit.getPlayer(this.getOwner());
            // TODO マイナスになるのを修正
            GNContext ctx = new GNContext(player, this.getTag(), 0, this.getGeneratorParticlesOutput(),
                    this.getStoredParticles(), this.getMaximumStorableParticles());
            this.getEnabledModules().forEach(module -> module.tick(ctx));

            long useParticles = ctx.getParticlesToUse();
            long generatedParticles = ctx.getGeneratorParticlesOutput();
            long particles = this.getStoredParticles() + generatedParticles;
            long remainParticles = particles - useParticles;
            long tooMuchParticles = remainParticles - this.getMaximumStorableParticles();
            if(tooMuchParticles < 0) tooMuchParticles = 0;
            long toStoreParticles = remainParticles;
            if(toStoreParticles > this.getMaximumStorableParticles()) toStoreParticles = this.getMaximumStorableParticles();
            // TODO this.setGeneratorParticlesOutput();
            this.setStoredParticles(toStoreParticles);
            this.updateLore();
        } else {
            this.stopTick();
            PLAYER_CURRENT_DRIVE.remove(this.getOwner());
            INSTANCES.remove(this.getId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if(event.getPlayer().getUniqueId().equals(this.getOwner()) && !this.task.isCancelled()) {
            this.stopTick();
            PLAYER_CURRENT_DRIVE.remove(this.getOwner());
            INSTANCES.remove(this.getId());
        }
    }

    @Override
    public void stopTick() {
        if(this.task != null && !this.task.isCancelled()) {
            if(Bukkit.getOfflinePlayer(this.getOwner()).isOnline()) {
                this.getEnabledModules().forEach(module -> {
                    GNContext ctx = new GNContext(Bukkit.getPlayer(this.getOwner()), this.getTag(), 0, -1,
                            -1, -1, true);
                    module.onDisable(ctx);
                });
            }
            ListenerManager.unregisterListener(this);
            this.task.cancel();
        }
    }

    @Override
    public void installModule(GNModule module) {
        if(AVAILABLE_MODULES.contains(module.getClass())) {

        }
    }

    @Override
    public void enableModule(GNModule module) {

    }

    @Override
    public void uninstallModule(GNModule module) {

    }

    @Override
    public void disableModule(GNModule module) {
    }

    @Override
    public Set<GNModule> getEnabledModules() {
        return this.getModulesTag().getList("Enabled", Tag.TAG_STRING).stream()
                .filter(tag -> tag instanceof StringTag)
                .map(Tag::getAsString)
                .filter(ResourceLocation::isValidResourceLocation)
                .map(ResourceLocation::tryParse)
                .filter(GNModules::isModule)
                .map(GNModules::getModule)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<GNModule> getDisabledModules() {
        return this.getModulesTag().getList("Disabled", Tag.TAG_STRING).stream()
                .filter(tag -> tag instanceof StringTag)
                .map(Tag::getAsString)
                .filter(ResourceLocation::isValidResourceLocation)
                .map(ResourceLocation::tryParse)
                .filter(GNModules::isModule)
                .map(GNModules::getModule)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<GNModule> getInstalledModules() {
        ListTag Enabled = this.getModulesTag().getList("Enabled", Tag.TAG_STRING);
        ListTag Disabled = this.getModulesTag().getList("Disabled", Tag.TAG_STRING);
        List<String> allModulesIds = new ArrayList<>();
        Enabled.stream()
                .filter(tag -> tag instanceof StringTag)
                .map(tag -> (StringTag) tag)
                .forEach(tag -> allModulesIds.add(tag.getAsString()));
        Disabled.stream()
                .filter(tag -> tag instanceof StringTag)
                .map(tag -> (StringTag) tag)
                .forEach(tag -> allModulesIds.add(tag.getAsString()));
        return allModulesIds.stream()
                .filter(ResourceLocation::isValidResourceLocation)
                .map(ResourceLocation::tryParse)
                .filter(GNModules::isModule)
                .map(GNModules::getModule)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<GNModule> getAvailableModules() {
        return AVAILABLE_MODULES;
    }

    @Override
    public void updateLore() {
        if(!this.drive.getTag().contains("display", Tag.TAG_COMPOUND)) {
            this.drive.getTag().put("display", new CompoundTag());
        }

        ListTag Lore = new ListTag();

        Set<GNModule> enabled = this.getEnabledModules();
        Lore.add(StringTag.valueOf("{\"text\":\"==== 有効なモジュール ===\", \"color\":\"green\", \"italic\":\"false\"}"));
        if(enabled.size() > 0) {
            enabled.forEach(module -> {
                Lore.add(StringTag.valueOf("[{\"text\":\"[*]\", \"color\":\"green\", \"bold\":\"true\", \"italic\":\"false\"}, {\"text\":\" \", \"color\":\"white\"}, {\"text\":\"" + module.getName() + "\", \"color\":\"green\", \"italic\":\"false\"}]"));
            });
        } else {
            Lore.add(StringTag.valueOf("{\"text\":\"     なし\", \"color\":\"gray\", \"italic\":\"false\"}"));
        }
        Lore.add(StringTag.valueOf(LORE_EMPTY));
        Set<GNModule> disabled = this.getDisabledModules();
        Lore.add(StringTag.valueOf("{\"text\":\"==== 無効なモジュール ===\", \"color\":\"red\", \"italic\":\"false\"}"));
        if(disabled.size() > 0) {
            disabled.forEach(module -> {
                Lore.add(StringTag.valueOf("[{\"text\":\"[-]\", \"color\":\"red\", \"bold\":\"true\", \"italic\":\"false\"}, {\"text\":\" \", \"color\":\"white\"}, {\"text\":\"" + module.getName() + "\", \"color\":\"red\", \"italic\":\"false\"}]"));
            });
        } else {
            Lore.add(StringTag.valueOf("{\"text\":\"     なし\", \"color\":\"gray\", \"italic\":\"false\"}"));
        }
        Lore.add(StringTag.valueOf(LORE_EMPTY));
        Lore.add(StringTag.valueOf("{\"text\":\"現在の粒子生産量: " + getGeneratorParticlesOutput() + "\", \"color\":\"aqua\", \"italic\":\"false\"}"));
        Lore.add(StringTag.valueOf("{\"text\":\"現在の粒子貯蔵量: " + getStoredParticles() + "/" + getMaximumStorableParticles() + "\", \"color\":\"aqua\", \"italic\":\"false\"}"));

        this.drive.getTag().getCompound("display").put("Lore", Lore);
    }

    @Override
    public UUID getOwner() {
        return this.owner;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    public void setGeneratorParticlesOutput(long particlesGeneratedInNextTick) {
        this.getGeneratorTag().putLong("Current", particlesGeneratedInNextTick);
    }

    public long getGeneratorParticlesOutput() {
        return this.getGeneratorTag().getLong("Current");
        //return RANDOM.nextLong(this.getGeneratorParticlesMaximumOutput() - this.getGeneratorParticlesMinimumOutput()) + this.getGeneratorParticlesMinimumOutput();
    }

    public long getGeneratorParticlesMinimumOutput() {
        return this.getGeneratorTag().getLong("Minimum");
    }

    public long getGeneratorParticlesMaximumOutput() {
        return this.getGeneratorTag().getLong("Maximum");
    }

    public void setStoredParticles(long particles) {
        this.getCapacityTag().putLong("Current", particles);
    }

    public long getStoredParticles() {
        return this.getCapacityTag().getLong("Current");
    }

    public void setMaximumStorableParticles(long maximumParticles) {
        this.getCapacityTag().putLong("Maximum", maximumParticles);
    }

    public long getMaximumStorableParticles() {
        return this.getCapacityTag().getLong("Maximum");
    }

    @Nonnull
    private CompoundTag getTag() {
        return Objects.requireNonNull(this.drive.getTag()).getCompound("GNDrive");
    }

    private CompoundTag getGeneratorTag() {
        return this.getTag().getCompound("Generator");
    }

    private CompoundTag getCapacityTag() {
        return this.getTag().getCompound("Capacity");
    }

    private CompoundTag getModulesTag() {
        return this.getTag().getCompound("Modules");
    }

    public static UUID getId(ItemStack drive) {
        if(isTagValid(drive.getTag())) {
            return drive.getTag().getCompound("GNDrive").getUUID("ID");
        }
        return null;
    }

    public static UUID getOwner(ItemStack drive) {
        if(isTagValid(drive.getTag())) {
            return drive.getTag().getCompound("GNDrive").getUUID("Owner");
        }
        return null;
    }

    public static boolean isTagValid(CompoundTag tag) {
        if(tag == null) return false;
        if(tag.contains("GNDrive")) {
            CompoundTag GNDrive = tag.getCompound("GNDrive");
            if(!GNDrive.contains("ID", Tag.TAG_INT_ARRAY)) return false;
            if(!GNDrive.contains("Owner", Tag.TAG_INT_ARRAY)) return false;

            if(GNDrive.contains("Generator")) {
                CompoundTag Generator = GNDrive.getCompound("Generator");
                if(!Generator.contains("Current") || !Generator.contains("Minimum") || !Generator.contains("Maximum")) return false;
            }

            if (GNDrive.contains("Capacity")) {
                CompoundTag Capacity = GNDrive.getCompound("Capacity");
                if (!Capacity.contains("Current") || !Capacity.contains("Maximum")) return false;
            }

            if (GNDrive.contains("Modules")) {
                CompoundTag Modules = GNDrive.getCompound("Modules");
                if (!Modules.contains("Enabled") || !Modules.contains("Disabled")) return false;
            }
            return true;
        }
        return false;
    }

    public static class Builder {
        public static Builder simple() {
            return new Builder(Items.IRON_CHESTPLATE)
                    .setCurrentCapacity(5000)
                    .setMaximumCapacity(5000)
                    .setGeneratorParticlesOutput(100)
                    .setGeneratorParticlesMinimumOutput(20)
                    .setGeneratorParticlesMaximumOutput(100)
                    .addModule(GNModules.FLY, true);
        }
        public static ItemStack createSimple() {
            return Builder.simple().build();
        }

        public static net.unknown.survival.feature.gnarms.GNDrive createSimpleAsInstance() {
            return new GNDrive(createSimple());
        }

        private final ItemStack drive;
        private final CompoundTag GNDrive = new CompoundTag();

        public Builder(Item driveItemType) {
            this.drive = new ItemStack(driveItemType);

            this.GNDrive.putUUID("ID", UUID.randomUUID());
            this.GNDrive.putUUID("Owner", Util.NIL_UUID);
            this.GNDrive.put("Capacity", new CompoundTag());
            this.GNDrive.put("Generator", new CompoundTag());
            this.GNDrive.put("Modules", new CompoundTag());
            this.GNDrive.getCompound("Modules").put("Enabled", new ListTag());
            this.GNDrive.getCompound("Modules").put("Disabled", new ListTag());
        }

        public Builder setID(UUID id) {
            this.GNDrive.putUUID("ID", id);
            return this;
        }

        public Builder setOwner(UUID uuid) {
            this.GNDrive.putUUID("Owner", uuid);
            return this;
        }

        public Builder setCurrentCapacity(long currentCapacity) {
            this.GNDrive.getCompound("Capacity").putLong("Current", currentCapacity);
            return this;
        }

        public Builder setMaximumCapacity(long maximumCapacity) {
            this.GNDrive.getCompound("Capacity").putLong("Maximum", maximumCapacity);
            return this;
        }

        public Builder setGeneratorParticlesOutput(long particlesOutput) {
            this.GNDrive.getCompound("Generator").putLong("Current", particlesOutput);
            return this;
        }

        public Builder setGeneratorParticlesMinimumOutput(long particlesMinimumOutput) {
            this.GNDrive.getCompound("Generator").putLong("Minimum", particlesMinimumOutput);
            return this;
        }

        public Builder setGeneratorParticlesMaximumOutput(long particlesMaximumOutput) {
            this.GNDrive.getCompound("Generator").putLong("Maximum", particlesMaximumOutput);
            return this;
        }

        public Builder addModule(GNModule module, boolean enabled) {
            if(enabled) {
                this.GNDrive.getCompound("Modules").getList("Enabled", Tag.TAG_STRING).add(StringTag.valueOf(module.getId().toString()));
            } else {
                this.GNDrive.getCompound("Modules").getList("Disabled", Tag.TAG_STRING).add(StringTag.valueOf(module.getId().toString()));
            }
            return this;
        }

        public Builder custom(Consumer<ItemStack> custom) {
            custom.accept(this.drive);
            return this;
        }

        public ItemStack build() {
            this.drive.getOrCreateTag().put("GNDrive", this.GNDrive);
            this.drive.setHoverName(Component.literal("GNドライヴ").setStyle(Style.EMPTY.withItalic(false).withBold(true).withColor(ChatFormatting.GREEN)));
            if(!net.unknown.survival.feature.gnarms.GNDrive.isTagValid(this.drive.getTag())) {
                throw new IllegalStateException("Cannot build a drive with invalid tags!");
            }
            return this.drive;
        }
    }
}
