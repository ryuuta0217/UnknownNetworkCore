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

package net.unknown.survival.feature.gnarms;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.launchwrapper.util.ComponentUtil;
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

// TODO 装備を外した時にItemStackがEMPTYになるのでModulesなどの内容をtick()で読み出して保持しておく
public class GNDrive implements GN, Listener {
    public static final Map<UUID, GNDrive> INSTANCES = new HashMap<>();
    private static final Set<GNModule> AVAILABLE_MODULES = new HashSet<>() {{
        add(GNModules.FLY);
    }};
    private static final String LORE_SEPARATOR = "{\"text\":\"==========\", \"color\":\"gray\", \"italic\":\"false\"}";
    private static final String LORE_EMPTY = "{\"text\":\"\"}";
    private static final Map<UUID, UUID> PLAYER_OWNED_DRIVES = new HashMap<>();
    private static final Map<UUID, UUID> PLAYER_CURRENT_DRIVE = new HashMap<>();

    private final UUID owner;
    private final UUID id;
    private final ItemStack drive;
    private BukkitTask task;

    public GNDrive(org.bukkit.inventory.ItemStack drive) {
        this.drive = MinecraftAdapter.ItemStack.itemStack(drive);
        this.owner = getOwner(this.drive);
        this.id = getId(this.drive);
        if (!isTagValid(this.drive.has(DataComponents.CUSTOM_DATA) ? this.drive.get(DataComponents.CUSTOM_DATA).getUnsafe() : null)) throw new IllegalArgumentException("Invalid GNDrive!");
        INSTANCES.put(this.id, this);
    }

    public GNDrive(ItemStack drive) {
        this.drive = drive;
        this.owner = getOwner(this.drive);
        this.id = getId(this.drive);
        if (!isTagValid(this.drive.has(DataComponents.CUSTOM_DATA) ? this.drive.get(DataComponents.CUSTOM_DATA).getUnsafe() : null)) throw new IllegalArgumentException("Invalid GNDrive!");
        INSTANCES.put(this.id, this);
    }

    public static void check() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerInventory inv = player.getInventory();
            if (inv.getChestplate() != null) { // チェストプレートを装備している
                ItemStack chestPlate = MinecraftAdapter.ItemStack.itemStack(inv.getChestplate());
                if (isTagValid(chestPlate.has(DataComponents.CUSTOM_DATA) ? chestPlate.get(DataComponents.CUSTOM_DATA).getUnsafe() : null)) { // NBTタグが一致
                    UUID owner = getOwner(chestPlate); // OwnerのUUIDがUtil.NIL_UUIDの場合はIndividual Information Attestation Systemで所有権の初期化を行う
                    if (owner != null && player.getUniqueId().equals(owner)) { // 所有者が適切
                        UUID currentDriveId = getId(chestPlate);

                        if (PLAYER_CURRENT_DRIVE.containsKey(owner)) {
                            UUID oldDriveId = PLAYER_CURRENT_DRIVE.get(owner);

                            if (INSTANCES.containsKey(oldDriveId) && oldDriveId.equals(currentDriveId)) {
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
                    INSTANCES.remove(driveId).stopTick();
                }
            }
            PLAYER_CURRENT_DRIVE.remove(player.getUniqueId()); // 何も装備してないならカレントから外す
        });
    }

    public static UUID getId(ItemStack drive) {
        if (isTagValid(drive.has(DataComponents.CUSTOM_DATA) ? drive.get(DataComponents.CUSTOM_DATA).getUnsafe() : null)) {
            return TagValueInput.createGlobal(ProblemReporter.DISCARDING, drive.get(DataComponents.CUSTOM_DATA).getUnsafe().getCompoundOrEmpty("GNDrive")).read("UUID", UUIDUtil.CODEC).orElse(null);
        }
        return null;
    }

    public static UUID getOwner(ItemStack drive) {
        if (isTagValid(drive.has(DataComponents.CUSTOM_DATA) ? drive.get(DataComponents.CUSTOM_DATA).getUnsafe() : null)) {
            return TagValueInput.createGlobal(ProblemReporter.DISCARDING, drive.get(DataComponents.CUSTOM_DATA).getUnsafe().getCompoundOrEmpty("GNDrive")).read("Owner", UUIDUtil.CODEC).orElse(null);
        }
        return null;
    }

    public static boolean isTagValid(CompoundTag tag) {
        if (tag == null) return false;
        if (tag.contains("GNDrive")) {
            CompoundTag GNDrive = tag.getCompoundOrEmpty("GNDrive");
            if (!GNDrive.contains("ID")) return false;
            if (!GNDrive.contains("Owner")) return false;

            if (GNDrive.contains("Generator")) {
                CompoundTag Generator = GNDrive.getCompoundOrEmpty("Generator");
                if (!Generator.contains("Current") || !Generator.contains("Minimum") || !Generator.contains("Maximum"))
                    return false;
            }

            if (GNDrive.contains("Capacity")) {
                CompoundTag Capacity = GNDrive.getCompoundOrEmpty("Capacity");
                if (!Capacity.contains("Current") || !Capacity.contains("Maximum")) return false;
            }

            if (GNDrive.contains("Modules")) {
                CompoundTag Modules = GNDrive.getCompoundOrEmpty("Modules");
                return Modules.contains("Enabled") && Modules.contains("Disabled");
            }
            return true;
        }
        return false;
    }

    @Override
    public void startTick() {
        if (!Bukkit.getOfflinePlayer(this.getOwner()).isOnline())
            throw new IllegalStateException("Player is now offline!");

        if (this.task == null || this.task.isCancelled()) {
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
        if (Bukkit.getOfflinePlayer(this.getOwner()).isOnline()) {
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
            if (tooMuchParticles < 0) tooMuchParticles = 0;
            long toStoreParticles = remainParticles;
            if (toStoreParticles > this.getMaximumStorableParticles())
                toStoreParticles = this.getMaximumStorableParticles();
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
        if (event.getPlayer().getUniqueId().equals(this.getOwner()) && !this.task.isCancelled()) {
            this.stopTick();
            PLAYER_CURRENT_DRIVE.remove(this.getOwner());
            INSTANCES.remove(this.getId());
        }
    }

    @Override
    public void stopTick() {
        if (this.task != null && !this.task.isCancelled()) {
            ListenerManager.unregisterListener(this);
            this.task.cancel();

            if (Bukkit.getOfflinePlayer(this.getOwner()).isOnline()) {
                this.getEnabledModules().forEach(module -> {
                    GNContext ctx = new GNContext(Bukkit.getPlayer(this.getOwner()), this.getTag(), 0, -1,
                            -1, -1, true);
                    module.onDisable(ctx);
                });
            }
        }
    }

    @Override
    public void installModule(GNModule module) {
        if (AVAILABLE_MODULES.contains(module.getClass())) {

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
        return this.getModulesTag().getListOrEmpty("Enabled").stream()
                .filter(tag -> tag instanceof StringTag)
                .map(Tag::asString)
                .map(Optional::get)
                .filter(id -> id.chars().allMatch(c -> ResourceLocation.isAllowedInResourceLocation((char) c)))
                .map(ResourceLocation::tryParse)
                .filter(GNModules::isModule)
                .map(GNModules::getModule)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<GNModule> getDisabledModules() {
        return this.getModulesTag().getListOrEmpty("Disabled").stream()
                .filter(tag -> tag instanceof StringTag)
                .map(Tag::asString)
                .map(Optional::get)
                .filter(id -> id.chars().allMatch(c -> ResourceLocation.isAllowedInResourceLocation((char) c)))
                .map(ResourceLocation::tryParse)
                .filter(GNModules::isModule)
                .map(GNModules::getModule)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<GNModule> getInstalledModules() {
        ListTag Enabled = this.getModulesTag().getListOrEmpty("Enabled");
        ListTag Disabled = this.getModulesTag().getListOrEmpty("Disabled");
        List<String> allModulesIds = new ArrayList<>();
        Enabled.stream()
                .filter(tag -> tag instanceof StringTag)
                .map(tag -> (StringTag) tag)
                .forEach(tag -> allModulesIds.add(tag.asString().get()));
        Disabled.stream()
                .filter(tag -> tag instanceof StringTag)
                .map(tag -> (StringTag) tag)
                .forEach(tag -> allModulesIds.add(tag.asString().get()));
        return allModulesIds.stream()
                .filter(id -> id.chars().allMatch(c -> ResourceLocation.isAllowedInResourceLocation((char) c)))
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
        ListTag Lore = new ListTag();

        Set<GNModule> enabled = this.getEnabledModules();
        Lore.add(StringTag.valueOf("{\"text\":\"==== 有効なモジュール ===\", \"color\":\"green\", \"italic\":false}"));
        if (enabled.size() > 0) {
            enabled.forEach(module -> {
                Lore.add(StringTag.valueOf("[{\"text\":\"[*]\", \"color\":\"green\", \"bold\":true, \"italic\":false}, {\"text\":\" \", \"color\":\"white\"}, {\"text\":\"" + module.getName() + "\", \"color\":\"green\", \"italic\":false}]"));
            });
        } else {
            Lore.add(StringTag.valueOf("{\"text\":\"     なし\", \"color\":\"gray\", \"italic\":false}"));
        }
        Lore.add(StringTag.valueOf(LORE_EMPTY));
        Set<GNModule> disabled = this.getDisabledModules();
        Lore.add(StringTag.valueOf("{\"text\":\"==== 無効なモジュール ===\", \"color\":\"red\", \"italic\":false}"));
        if (disabled.size() > 0) {
            disabled.forEach(module -> {
                Lore.add(StringTag.valueOf("[{\"text\":\"[-]\", \"color\":\"red\", \"bold\":true, \"italic\":false}, {\"text\":\" \", \"color\":\"white\"}, {\"text\":\"" + module.getName() + "\", \"color\":\"red\", \"italic\":false}]"));
            });
        } else {
            Lore.add(StringTag.valueOf("{\"text\":\"     なし\", \"color\":\"gray\", \"italic\":false}"));
        }
        Lore.add(StringTag.valueOf(LORE_EMPTY));
        Lore.add(StringTag.valueOf("{\"text\":\"現在の粒子生産量: " + getGeneratorParticlesOutput() + "\", \"color\":\"aqua\", \"italic\":false}"));
        Lore.add(StringTag.valueOf("{\"text\":\"現在の粒子貯蔵量: " + getStoredParticles() + "/" + getMaximumStorableParticles() + "\", \"color\":\"aqua\", \"italic\":false}"));

        List<Component> styledLoreLines = Lore.stream()
                .filter(tag -> tag instanceof StringTag)
                .map(tag -> (StringTag) tag)
                .map(StringTag::asString)
                .map(Optional::get)
                .map(json -> {
                    try {
                        return TagParser.parseCompoundFully(json);
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(tag -> ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, tag))
                .map(result -> result.getOrThrow())
                .map(pair -> pair.getFirst())
                .toList();

        ItemLore lore = new ItemLore(styledLoreLines, styledLoreLines);
        this.drive.set(DataComponents.LORE, lore);
    }

    @Override
    public UUID getOwner() {
        return this.owner;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    public long getGeneratorParticlesOutput() {
        return this.getGeneratorTag().getLongOr("Current", -1L);
        //return RANDOM.nextLong(this.getGeneratorParticlesMaximumOutput() - this.getGeneratorParticlesMinimumOutput()) + this.getGeneratorParticlesMinimumOutput();
    }

    public void setGeneratorParticlesOutput(long particlesGeneratedInNextTick) {
        this.getGeneratorTag().putLong("Current", particlesGeneratedInNextTick);
    }

    public long getGeneratorParticlesMinimumOutput() {
        return this.getGeneratorTag().getLongOr("Minimum", -1L);
    }

    public long getGeneratorParticlesMaximumOutput() {
        return this.getGeneratorTag().getLongOr("Maximum", -1L);
    }

    public long getStoredParticles() {
        return this.getCapacityTag().getLongOr("Current", -1L);
    }

    public void setStoredParticles(long particles) {
        this.getCapacityTag().putLong("Current", particles);
    }

    public long getMaximumStorableParticles() {
        return this.getCapacityTag().getLongOr("Maximum", -1L);
    }

    public void setMaximumStorableParticles(long maximumParticles) {
        this.getCapacityTag().putLong("Maximum", maximumParticles);
    }

    @Nonnull
    private CompoundTag getTag() {
        return Objects.requireNonNull(this.drive.has(DataComponents.CUSTOM_DATA) ? this.drive.get(DataComponents.CUSTOM_DATA).getUnsafe() : null).getCompoundOrEmpty("GNDrive");
    }

    private CompoundTag getGeneratorTag() {
        return this.getTag().getCompoundOrEmpty("Generator");
    }

    private CompoundTag getCapacityTag() {
        return this.getTag().getCompoundOrEmpty("Capacity");
    }

    private CompoundTag getModulesTag() {
        return this.getTag().getCompoundOrEmpty("Modules");
    }

    public static class Builder {
        private final ItemStack drive;
        private final CompoundTag GNDrive = new CompoundTag();

        public Builder(Item driveItemType) {
            this.drive = new ItemStack(driveItemType);

            TagValueOutput output = TagValueOutput.createWrappingGlobal(ProblemReporter.DISCARDING, this.GNDrive);
            output.store("ID", UUIDUtil.CODEC, UUID.randomUUID());
            output.store("Owner", UUIDUtil.CODEC, Util.NIL_UUID);
            this.GNDrive.put("Capacity", new CompoundTag());
            this.GNDrive.put("Generator", new CompoundTag());
            this.GNDrive.put("Modules", new CompoundTag());
            this.GNDrive.getCompoundOrEmpty("Modules").put("Enabled", new ListTag());
            this.GNDrive.getCompoundOrEmpty("Modules").put("Disabled", new ListTag());
        }

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

        public Builder setID(UUID id) {
            TagValueOutput output = TagValueOutput.createWrappingGlobal(ProblemReporter.DISCARDING, this.GNDrive);
            output.store("ID", UUIDUtil.CODEC, id);
            return this;
        }

        public Builder setOwner(UUID uuid) {
            TagValueOutput output = TagValueOutput.createWrappingGlobal(ProblemReporter.DISCARDING, this.GNDrive);
            output.store("Owner", UUIDUtil.CODEC, uuid);
            return this;
        }

        public Builder setCurrentCapacity(long currentCapacity) {
            this.GNDrive.getCompoundOrEmpty("Capacity").putLong("Current", currentCapacity);
            return this;
        }

        public Builder setMaximumCapacity(long maximumCapacity) {
            this.GNDrive.getCompoundOrEmpty("Capacity").putLong("Maximum", maximumCapacity);
            return this;
        }

        public Builder setGeneratorParticlesOutput(long particlesOutput) {
            this.GNDrive.getCompoundOrEmpty("Generator").putLong("Current", particlesOutput);
            return this;
        }

        public Builder setGeneratorParticlesMinimumOutput(long particlesMinimumOutput) {
            this.GNDrive.getCompoundOrEmpty("Generator").putLong("Minimum", particlesMinimumOutput);
            return this;
        }

        public Builder setGeneratorParticlesMaximumOutput(long particlesMaximumOutput) {
            this.GNDrive.getCompoundOrEmpty("Generator").putLong("Maximum", particlesMaximumOutput);
            return this;
        }

        public Builder addModule(GNModule module, boolean enabled) {
            if (enabled) {
                this.GNDrive.getCompoundOrEmpty("Modules").getListOrEmpty("Enabled").add(StringTag.valueOf(module.getId().toString()));
            } else {
                this.GNDrive.getCompoundOrEmpty("Modules").getListOrEmpty("Disabled").add(StringTag.valueOf(module.getId().toString()));
            }
            return this;
        }

        public Builder custom(Consumer<ItemStack> custom) {
            custom.accept(this.drive);
            return this;
        }

        public ItemStack build() {
            this.drive.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag())).getUnsafe().put("GNDrive", this.GNDrive);
            this.drive.set(DataComponents.CUSTOM_NAME, Component.literal("GNドライヴ").setStyle(Style.EMPTY.withItalic(false).withBold(true).withColor(ChatFormatting.GREEN)));
            if (!net.unknown.survival.feature.gnarms.GNDrive.isTagValid(this.drive.get(DataComponents.CUSTOM_DATA).getUnsafe())) {
                throw new IllegalStateException("Cannot build a drive with invalid tags!");
            }
            return this.drive;
        }
    }
}
