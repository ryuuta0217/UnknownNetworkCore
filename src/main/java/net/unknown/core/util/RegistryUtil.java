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

package net.unknown.core.util;

import com.google.common.collect.BiMap;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Fluid;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R1.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftNamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked", "deprecated"})
public class RegistryUtil {
    private static final Logger LOGGER = Logger.getLogger("UNC/RegistryUtil");

    public static <T> boolean unfreeze(Registry<T> registry) {
        if (registry instanceof MappedRegistry<T>) {
            try {
                ObfuscationUtil.Class mappedRegistry = ObfuscationUtil.getClassByMojangName("net.minecraft.core.MappedRegistry");
                Field frozenField = mappedRegistry.getFieldByMojangName("frozen").getField();
                if (frozenField == null || !frozenField.trySetAccessible()) return false;
                frozenField.set(registry, false);
                return true;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public static <T> Registry<T> freeze(Registry<T> registry) {
        if (registry instanceof MappedRegistry<T>) {
            return registry.freeze();
        }
        return null;
    }

    public static <T> T forceRegister(Registry<T> registry, ResourceLocation id, T value) {
        if (!unfreeze(registry)) LOGGER.warning("Failed to unfreeze Registry! But continue force registering.");
        Registry.register(registry, id, value);
        if(registry == BuiltInRegistries.ENCHANTMENT && value instanceof Enchantment enchant) {
            try {
                Field f = org.bukkit.enchantments.Enchantment.class.getDeclaredField("acceptingNew");
                if (f.trySetAccessible()) {
                    f.set(null, true);
                    org.bukkit.enchantments.Enchantment.registerEnchantment(new org.bukkit.craftbukkit.v1_20_R1.enchantments.CraftEnchantment(enchant));
                    f.set(null, false);
                }
            } catch(IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        freeze(registry);
        return registry.get(id) != null ? value : null;
    }

    public static <T> boolean forceReplace(Registry<T> registry, ResourceLocation id, T objectTo) {
        String logPrefix = "[forceReplace] [" + id.toString() + "] ";
        if (registry instanceof MappedRegistry<T> mappedRegistry) {
            T objectFrom = mappedRegistry.get(id);
            if (objectFrom != null) {
                ResourceKey<T> resourceKey = mappedRegistry.getResourceKey(objectFrom).orElse(null);
                Lifecycle lifecycle = mappedRegistry.lifecycle(objectFrom);
                if (resourceKey != null) {
                    int registryId = mappedRegistry.getId(objectFrom);
                    logPrefix += "[ID=" + registryId + "] ";

                    CraftEnchantment bukkitOldEnchant = null;
                    String bukkitOldEnchantName = null;
                    if (registry == BuiltInRegistries.ENCHANTMENT && objectFrom instanceof Enchantment && objectTo instanceof Enchantment) {
                        bukkitOldEnchant = (CraftEnchantment) org.bukkit.enchantments.Enchantment.getByKey(CraftNamespacedKey.fromMinecraft(registry.getKey(objectFrom)));
                        bukkitOldEnchantName = bukkitOldEnchant.getName();
                    }

                    if (!unfreeze(registry)) LOGGER.warning("Failed to unfreeze Registry! But continue force replace object.");

                    try {
                        ObfuscationUtil.Class obfUtilClass = ObfuscationUtil.getClassByMojangName("net.minecraft.core.MappedRegistry");
                        if (obfUtilClass != null) {
                            LOGGER.info(logPrefix + "MappedRegistry class found, getting required fields...");
                            Field byKeyField = obfUtilClass.getFieldByMojangName("byKey").getField();
                            Map<ResourceKey<T>, Holder.Reference<T>> byKey = getObject(byKeyField, mappedRegistry, Map.class);

                            Field byLocationField = obfUtilClass.getFieldByMojangName("byLocation").getField();
                            Map<ResourceLocation, Holder.Reference<T>> byLocation = getObject(byLocationField, mappedRegistry, Map.class);

                            Field byValueField = obfUtilClass.getFieldByMojangName("byValue").getField();
                            Map<T, Holder.Reference<T>> byValue = getObject(byValueField, mappedRegistry, Map.class);

                            Field byIdField = obfUtilClass.getFieldByMojangName("byId").getField();
                            ObjectList<Holder.Reference<T>> byId = getObject(byIdField, mappedRegistry, ObjectList.class);

                            Field toIdField = obfUtilClass.getFieldByMojangName("toId").getField();
                            Reference2IntOpenHashMap<T> toId = getObject(toIdField, mappedRegistry, Reference2IntOpenHashMap.class);

                            Field lifecyclesField = obfUtilClass.getFieldByMojangName("lifecycles").getField();
                            Map<T, Lifecycle> lifecycles = getObject(lifecyclesField, mappedRegistry, Map.class);

                            if (byKey != null && byLocation != null && byValue != null && byId != null && toId != null && lifecycles != null) {
                                int registryCount = registry.size();
                                LOGGER.info(logPrefix + "Got fields successfully, proceed as field-modify mode.");
                                Holder.Reference<T> oldReference = byKey.get(resourceKey);
                                Holder.Reference<T> reference = Holder.Reference.createStandAlone(mappedRegistry.holderOwner(), resourceKey);

                                /* ResourceKey => Reference Map */
                                int originalByKeySize = byKey.size();
                                int byKeySize = originalByKeySize;

                                byKey.remove(resourceKey);
                                if (byKeySize - 1 == byKey.size() && byKey.getOrDefault(resourceKey, null) == null) {
                                    LOGGER.info(logPrefix + "[byKey] Successfully removed " + resourceKey + " => " + oldReference + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[byKey] Failed to remove " + resourceKey + " => " + oldReference + " Map");
                                }

                                byKeySize = byKey.size();

                                byKey.put(resourceKey, reference);
                                if (byKeySize + 1 == byKey.size() && byKey.size() == originalByKeySize && byKey.getOrDefault(resourceKey, null) == reference) {
                                    LOGGER.info(logPrefix + "[byKey] Successfully added " + resourceKey + " => " + reference + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[byKey] Failed to add " + resourceKey + " => " + reference + " Map");
                                }
                                /* END OF ResourceKey => Reference Map */

                                /* ResourceLocation => Reference Map */
                                int originalByLocationSize = byLocation.size();
                                int byLocationSize = originalByLocationSize;

                                byLocation.remove(resourceKey.location());
                                if ((byLocationSize - 1) == byLocation.size() && byLocation.getOrDefault(resourceKey.location(), null) == null) {
                                    LOGGER.info(logPrefix + "[byLocation] Successfully removed " + resourceKey.location() + " => " + oldReference + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[byLocation] Failed to remove " + resourceKey.location() + " => " + oldReference + " Map");
                                }

                                byLocationSize = byLocation.size();

                                byLocation.put(resourceKey.location(), reference);
                                if ((byLocationSize + 1) == byLocation.size() && byLocation.size() == originalByLocationSize && byLocation.getOrDefault(resourceKey.location(), null) == reference) {
                                    LOGGER.info(logPrefix + "[byLocation] Successfully added " + resourceKey.location() + " => " + reference + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[byLocation] Failed to add " + resourceKey.location() + " => " + reference + " Map");
                                }
                                /* END OF ResourceLocation => Reference Map */

                                /* Object => Reference Map */
                                int originalByValueSize = byValue.size();
                                int byValueSize = originalByValueSize;

                                byValue.remove(objectFrom);
                                if ((byValueSize - 1) == byValue.size() && byValue.getOrDefault(objectFrom, null) == null) {
                                    LOGGER.info(logPrefix + "[byValue] Successfully removed " + objectFrom + " => " + oldReference + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[byValue] Failed to remove " + objectFrom + " => " + oldReference + " Map");
                                }

                                byValueSize = byValue.size();

                                byValue.put(objectTo, reference);
                                if ((byValueSize + 1) == byValue.size() && byValue.size() == originalByValueSize && byValue.getOrDefault(objectTo, null) == reference) {
                                    LOGGER.info(logPrefix + "[byValue] Successfully added " + objectTo + " => " + reference + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[byValue] Failed to add " + objectTo + " => " + reference + " Map");
                                }
                                /* END OF Object => Reference Map */

                                /* ID => Object Map */
                                byId.set(registryId, reference);
                                if (byId.get(registryId) == reference) {
                                    LOGGER.info(logPrefix + "[byId] Successfully overwritten as " + registryId + " => " + reference);
                                } else {
                                    LOGGER.warning(logPrefix + "[byId] Failed to overwrite as " + registryId + " => " + reference);
                                }
                                /* END OF ID => Object Map */

                                /* Object => ID Map*/
                                int originalToIdSize = toId.size();
                                int toIdSize = originalToIdSize;

                                toId.remove(objectFrom);
                                if ((toIdSize - 1) == toId.size() && toId.getOrDefault(objectFrom, -1) == -1) {
                                    LOGGER.info(logPrefix + "[toId] Successfully removed " + objectFrom + " => " + registryId + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[toId] Failed to remove " + objectFrom + " => " + registryId + " Map");
                                }

                                toIdSize = toId.size();

                                toId.put(objectTo, registryId);
                                if ((toIdSize + 1) == toId.size() && toId.size() == originalToIdSize && toId.getOrDefault(objectTo, -1) == registryId) {
                                    LOGGER.info(logPrefix + "[toId] Successfully added " + objectTo + " => " + registryId + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[toId] Failed to add " + objectTo + " => " + registryId + " Map");
                                }
                                /* END OF Object => ID Map */

                                /* LIFECYCLES */
                                int originalLifecyclesSize = lifecycles.size();
                                int lifecyclesSize = originalLifecyclesSize;

                                lifecycles.remove(objectFrom);
                                if (lifecyclesSize - 1 == lifecycles.size() && lifecycles.getOrDefault(objectFrom, null) == null) {
                                    LOGGER.info(logPrefix + "[lifecycles] Successfully removed " + objectFrom + " => " + lifecycle + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[lifecycles] Failed to remove " + objectFrom + " => " + lifecycle + " Map");
                                }

                                lifecyclesSize = lifecycles.size();

                                lifecycles.put(objectTo, lifecycle);
                                if ((lifecyclesSize + 1) == lifecycles.size() && lifecycles.size() == originalLifecyclesSize && lifecycles.get(objectTo) == lifecycle) {
                                    LOGGER.info(logPrefix + "[lifecycles] Successfully added " + objectTo + " => " + lifecycle + " Map");
                                } else {
                                    LOGGER.warning(logPrefix + "[lifecycles] Failed to add " + objectTo + " => " + lifecycle + " Map");
                                }
                                /* END OF LIFECYCLES */

                                if (bukkitOldEnchant != null) {
                                    LOGGER.info(logPrefix + "[Bukkit/Enchantment] Enchantment registry replacing detected, applying to Bukkit's enchantment registry as new object.");

                                    Enchantment oldEnchant = (Enchantment) objectFrom;
                                    Enchantment newEnchant = (Enchantment) objectTo;

                                    CraftEnchantment bukkitNewEnchant = new CraftEnchantment(newEnchant);

                                    if (bukkitOldEnchant.getHandle().equals(oldEnchant) && bukkitNewEnchant.getHandle().equals(newEnchant)) {
                                        Class<org.bukkit.enchantments.Enchantment> bukkitEnchantmentClass = org.bukkit.enchantments.Enchantment.class;

                                        Field bukkitByKeyField = bukkitEnchantmentClass.getDeclaredField("byKey");
                                        Map<NamespacedKey, org.bukkit.enchantments.Enchantment> bukkitByKey = getObject(bukkitByKeyField, null, Map.class);

                                        Field bukkitByNameField = bukkitEnchantmentClass.getDeclaredField("byName");
                                        Map<String, org.bukkit.enchantments.Enchantment> bukkitByName = getObject(bukkitByNameField, null, Map.class);

                                        if (bukkitByKey != null && bukkitByName != null) {
                                            LOGGER.info(logPrefix + "[Bukkit/Enchantment] Got bukkit fields successfully, proceed as field-modify mode.");

                                            /* [Bukkit] Key => Enchantment Map */
                                            int originalBukkitByKeySize = bukkitByKey.size();
                                            int bukkitByKeySize = originalBukkitByKeySize;

                                            bukkitByKey.remove(bukkitOldEnchant.getKey());
                                            if ((bukkitByKeySize - 1) == bukkitByKey.size() && bukkitByKey.getOrDefault(bukkitOldEnchant.getKey(), null) == null) {
                                                LOGGER.info(logPrefix + "[Bukkit/Enchantment] [byKey] Successfully removed " + bukkitOldEnchant.getKey() + " => " + bukkitOldEnchant + " Map");
                                            } else {
                                                LOGGER.warning(logPrefix + "[Bukkit/Enchantment] [byKey] Failed to remove " + bukkitOldEnchant.getKey() + " => " + bukkitOldEnchant + " Map");
                                            }

                                            bukkitByKeySize = bukkitByKey.size();

                                            bukkitByKey.put(bukkitNewEnchant.getKey(), bukkitNewEnchant);
                                            if ((bukkitByKeySize + 1) == bukkitByKey.size() && bukkitByKey.size() == originalBukkitByKeySize && bukkitByKey.get(bukkitNewEnchant.getKey()) == bukkitNewEnchant) {
                                                LOGGER.info(logPrefix + "[Bukkit/Enchantment] [byKey] Successfully added " + bukkitNewEnchant.getKey() + " => " + bukkitNewEnchant + " Map");
                                            } else {
                                                LOGGER.warning(logPrefix + "[Bukkit/Enchantment] [byKey] Failed to add " + bukkitNewEnchant.getKey() + " => " + bukkitNewEnchant + " Map");
                                            }
                                            /* [Bukkit] END OF Key => Enchantment Map */

                                            /* [Bukkit] EnchantName => Enchantment Map */
                                            int originalBukkitByNameSize = bukkitByName.size();
                                            int bukkitByNameSize = originalBukkitByNameSize;

                                            bukkitByName.remove(bukkitOldEnchantName);
                                            if ((bukkitByNameSize - 1) == bukkitByName.size() && bukkitByName.getOrDefault(bukkitOldEnchantName, null) == null) {
                                                LOGGER.info(logPrefix + "[Bukkit/Enchantment] [byName] Successfully removed " + bukkitOldEnchantName + " => " + bukkitOldEnchant + " Map");
                                            } else {
                                                LOGGER.warning(logPrefix + "[Bukkit/Enchantment] [byName] Failed to remove " + bukkitOldEnchantName + " => " + bukkitOldEnchant + " Map");
                                            }

                                            bukkitByNameSize = bukkitByName.size();

                                            bukkitByName.put(bukkitNewEnchant.getName(), bukkitNewEnchant);
                                            if ((bukkitByNameSize + 1) == bukkitByName.size() && bukkitByName.size() == originalBukkitByNameSize && bukkitByName.get(bukkitNewEnchant.getName()) == bukkitNewEnchant) {
                                                LOGGER.info(logPrefix + "[Bukkit/Enchantment] [byName] Successfully added " + bukkitNewEnchant.getName() + " => " + bukkitNewEnchant + " Map");
                                            } else {
                                                LOGGER.warning(logPrefix + "[Bukkit/Enchantment] [byName] Failed to add " + bukkitNewEnchant.getName() + " => " + bukkitNewEnchant + " Map");
                                            }
                                            /* [Bukkit] END OF EnchantName => Enchantment Map */
                                        }
                                    }
                                }

                                if (mappedRegistry.getId(objectTo) == registryId && registry.size() == registryCount) {
                                    if (!oldReference.equals(mappedRegistry.getHolder(resourceKey).orElse(null))) {
                                        if (byKey.size() == originalByKeySize && byLocation.size() == originalByLocationSize && byValue.size() == originalByValueSize && toId.size() == originalToIdSize && lifecycles.size() == originalLifecyclesSize) {
                                            LOGGER.info(logPrefix + "Successfully replaced object.");
                                            freeze(mappedRegistry);
                                            return true;
                                        }
                                    }
                                }
                            } else {
                                LOGGER.warning("Failed to get some field(s). Proceed to duplicate key registration.");
                                mappedRegistry.register(resourceKey, objectTo, lifecycle);
                                if (mappedRegistry.getKey(objectTo) != null) {
                                    LOGGER.info(logPrefix + "Successfully registered object.");
                                    freeze(mappedRegistry);
                                    return true;
                                }
                            }
                            freeze(mappedRegistry);
                        }
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    private static Object getObject(Field field, Object instance) {
        if (field.trySetAccessible()) {
            try {
                return field.get(instance);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    private static <T> T getObject(Field field, Object instance, Class<T> type) {
        if (field.trySetAccessible()) {
            try {
                return type.cast(field.get(instance));
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static class Bukkit {
        public static boolean unregisterBlockDataMap(Class<? extends Block> nms) {
            try {
                Map<Class<? extends Block>, Function<BlockState, CraftBlockData>> MAP = (Map<Class<? extends Block>, Function<BlockState, CraftBlockData>>) getObject(CraftBlockData.class.getDeclaredField("MAP"), null);
                if (MAP == null) return false;
                MAP.remove(nms);
                return true;
            } catch (NoSuchFieldException ignored) {
            }
            return false;
        }

        public static boolean registerBlockDataMap(Class<? extends Block> nms, Function<BlockState, CraftBlockData> bukkit) {
            try {
                Map<Class<? extends Block>, Function<BlockState, CraftBlockData>> MAP = (Map<Class<? extends Block>, Function<BlockState, CraftBlockData>>) getObject(CraftBlockData.class.getDeclaredField("MAP"), null);
                if (MAP == null) return false;
                MAP.put(nms, bukkit);
                return true;
            } catch (NoSuchFieldException ignored) {
            }
            return false;
        }

        public static boolean reloadCraftMagicNumbers() {
            try {
                Map<Block, Material> BLOCK_MATERIAL = (Map<Block, Material>) getObject(CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL"), null);
                Map<Item, Material> ITEM_MATERIAL = (Map<Item, Material>) getObject(CraftMagicNumbers.class.getDeclaredField("ITEM_MATERIAL"), null);
                Map<net.minecraft.world.level.material.Fluid, Fluid> FLUID_MATERIAL = (Map<net.minecraft.world.level.material.Fluid, Fluid>) getObject(CraftMagicNumbers.class.getDeclaredField("FLUID_MATERIAL"), null);
                if (BLOCK_MATERIAL == null || ITEM_MATERIAL == null || FLUID_MATERIAL == null) return false;

                BLOCK_MATERIAL.clear();
                for (Block block : BuiltInRegistries.BLOCK) {
                    BLOCK_MATERIAL.put(block, Material.getMaterial(BuiltInRegistries.BLOCK.getKey(block).getPath().toUpperCase(Locale.ROOT)));
                }

                ITEM_MATERIAL.clear();
                for (Item item : BuiltInRegistries.ITEM) {
                    ITEM_MATERIAL.put(item, Material.getMaterial(BuiltInRegistries.ITEM.getKey(item).getPath().toUpperCase(Locale.ROOT)));
                }

                FLUID_MATERIAL.clear();
                for (net.minecraft.world.level.material.Fluid fluid : BuiltInRegistries.FLUID) {
                    FLUID_MATERIAL.put(fluid, org.bukkit.Registry.FLUID.get(CraftNamespacedKey.fromMinecraft(BuiltInRegistries.FLUID.getKey(fluid))));
                }

                Map<Material, Item> MATERIAL_ITEM = (Map<Material, Item>) getObject(CraftMagicNumbers.class.getDeclaredField("MATERIAL_ITEM"), null);
                Map<Material, Block> MATERIAL_BLOCK = (Map<Material, Block>) getObject(CraftMagicNumbers.class.getDeclaredField("MATERIAL_BLOCK"), null);
                Map<Material, net.minecraft.world.level.material.Fluid> MATERIAL_FLUID = (Map<Material, net.minecraft.world.level.material.Fluid>) getObject(CraftMagicNumbers.class.getDeclaredField("MATERIAL_FLUID"), null);
                if (MATERIAL_ITEM == null || MATERIAL_BLOCK == null || MATERIAL_FLUID == null) return false;

                MATERIAL_ITEM.clear();
                MATERIAL_BLOCK.clear();
                MATERIAL_FLUID.clear();
                for (Material material : Material.values()) {
                    if (material.isLegacy()) {
                        continue;
                    }

                    ResourceLocation key = CraftMagicNumbers.key(material);
                    BuiltInRegistries.ITEM.getOptional(key).ifPresent((item) -> {
                        MATERIAL_ITEM.put(material, item);
                    });
                    BuiltInRegistries.BLOCK.getOptional(key).ifPresent((block) -> {
                        MATERIAL_BLOCK.put(material, block);
                    });
                    BuiltInRegistries.FLUID.getOptional(key).ifPresent((fluid) -> {
                        MATERIAL_FLUID.put(material, fluid);
                    });
                }
                return true;
            } catch (NoSuchFieldException ignored) {
            }
            return false;
        }
    }
}
