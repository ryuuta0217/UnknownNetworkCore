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

package net.unknown;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.ApiMirrorRootNode;
import io.papermc.paper.command.brigadier.PaperCommands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.event.RegistryEvents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.unknown.core.commands.Commands;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class UnknownNetworkCorePluginBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext ctx) {
        ctx.getLogger().info(Component.text("Bootstrapping UnknownNetworkCore"));

        ctx.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (e) -> {
            if (e.registrar() instanceof PaperCommands reg) {
                CommandBuildContext buildContext = reg.getBuildContext();
                CommandDispatcher<CommandSourceStack> minecraftDispatcher = null;
                if (reg.getDispatcherInternal().getRoot() instanceof ApiMirrorRootNode mirror) {
                    minecraftDispatcher = mirror.getDispatcher();
                }

                if (buildContext != null && minecraftDispatcher != null) {
                    Commands.init(minecraftDispatcher, buildContext);
                    switch (UnknownNetworkCore.getEnvironment()) {
                        case SURVIVAL -> net.unknown.survival.commands.Commands.init(minecraftDispatcher, buildContext);
                        case ANARCHY_HARDCORE -> net.unknown.anarchyhardcore.commands.Commands.init(minecraftDispatcher, buildContext);
                    }
                }
            }
        });

        ctx.getLogger().info(Component.text("Registering handler for sharpness enchantment max level set to 10"));
        ctx.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.entryAdd(), event -> {
            if (event.key().key().equals(Key.key("minecraft:sharpness"))) {
                event.builder().maxLevel(10);
            }
        });

        ctx.getLogger().info(Component.text("Registering handler for smite enchantment max level set to 10"));
        ctx.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.entryAdd(), event -> {
            if (event.key().key().equals(Key.key("minecraft:smite"))) {
                event.builder().maxLevel(10);
            }
        });

        /*ctx.getLogger().info(Component.text("Registering handler for unknown enchantment"));
        ctx.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.freeze(), e -> {
            Conversions conversions = BuiltInRegistries.BUILT_IN_CONVERSIONS;

            Enchantment.Builder ench = new Enchantment.Builder(Enchantment.definition(
                    PaperRegistrySets.convertToNms(Registries.ITEM, conversions.lookup(), RegistrySet.keySet(RegistryKey.ITEM, ItemTypeKeys.NETHERITE_CHESTPLATE)),
                    PaperRegistrySets.convertToNms(Registries.ITEM, conversions.lookup(), RegistrySet.keySet(RegistryKey.ITEM, ItemTypeKeys.NETHERITE_CHESTPLATE)),
                    1, 10, Enchantment.dynamicCost(10, 20),
                    Enchantment.dynamicCost(60, 20),
                    8,
                    net.minecraft.world.entity.EquipmentSlotGroup.ANY
            )).withEffect(
                    EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.VICTIM,
                    EnchantmentTarget.ATTACKER,
                    AllOf.entityEffects(
                            new DamageEntity(LevelBasedValue.perLevel(1.0f), LevelBasedValue.perLevel(5.0f), Holder.Reference.createStandAlone(conversions.lookup().lookup(Registries.DAMAGE_TYPE).get().owner(), DamageTypes.THORNS)),
                                    new DamageItem(LevelBasedValue.constant(2.0f))
                    ),
                    LootItemRandomChanceCondition.randomChance(EnchantmentLevelProvider.forEnchantmentLevel(LevelBasedValue.perLevel(0.15f)))
            );
            e.registry().register(TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("unknown-network:thorns")), builder -> new Builder(ench.build(ResourceLocation.parse("unknown-network:thorns"))));
        });*/
    }
/*
    public static class Builder implements EnchantmentRegistryEntry.Builder, PaperRegistryBuilder<Enchantment, org.bukkit.enchantments.Enchantment> {
        private final Enchantment internal;

        public Builder(Enchantment internal) {
            this.internal = internal;
        }

        @Override
        public Enchantment build() {
            return this.internal;
        }

        @Override
        public Builder description(@NonNull Component component) {
            return this;
        }

        @Override
        public Builder supportedItems(@NonNull RegistryKeySet<ItemType> registryKeySet) {
            return this;
        }

        @Override
        public Builder primaryItems(@Nullable RegistryKeySet<ItemType> registryKeySet) {
            return this;
        }

        @Override
        public Builder weight(@Range(from = 1L, to = 1024L) int i) {
            return this;
        }

        @Override
        public Builder maxLevel(@Range(from = 1L, to = 255L) int i) {
            return this;
        }

        @Override
        public Builder minimumCost(@NotNull EnchantmentRegistryEntry.EnchantmentCost enchantmentCost) {
            return this;
        }

        @Override
        public Builder maximumCost(@NotNull EnchantmentRegistryEntry.EnchantmentCost enchantmentCost) {
            return this;
        }

        @Override
        public Builder anvilCost(@Range(from = 0L, to = 2147483647L) int i) {
            return this;
        }

        @Override
        public Builder activeSlots(@NonNull Iterable<@NonNull EquipmentSlotGroup> iterable) {
            return this;
        }

        @Override
        public Builder exclusiveWith(@NonNull RegistryKeySet<org.bukkit.enchantments.Enchantment> registryKeySet) {
            return this;
        }

        @Override
        public @NonNull Component description() {
            return null;
        }

        @Override
        public @NonNull RegistryKeySet<ItemType> supportedItems() {
            return null;
        }

        @Override
        public @Nullable RegistryKeySet<ItemType> primaryItems() {
            return null;
        }

        @Override
        public @Range(from = 1L, to = 1024L) int weight() {
            return 0;
        }

        @Override
        public @Range(from = 1L, to = 255L) int maxLevel() {
            return 0;
        }

        @Override
        public EnchantmentCost minimumCost() {
            return null;
        }

        @Override
        public EnchantmentCost maximumCost() {
            return null;
        }

        @Override
        public @Range(from = 0L, to = 2147483647L) int anvilCost() {
            return 0;
        }

        @Override
        public @NonNull @Unmodifiable List<EquipmentSlotGroup> activeSlots() {
            return List.of();
        }

        @Override
        public @NonNull RegistryKeySet<org.bukkit.enchantments.Enchantment> exclusiveWith() {
            return null;
        }
    }
 */
}
