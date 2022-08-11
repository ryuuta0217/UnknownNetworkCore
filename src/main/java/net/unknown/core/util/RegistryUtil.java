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

package net.unknown.core.util;

import com.google.common.collect.BiMap;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Fluid;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftNamespacedKey;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class RegistryUtil {
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
        unfreeze(registry);
        Registry.register(registry, id, value);
        freeze(registry);
        return registry.get(id) != null ? value : null;
    }

    public static <T> boolean forceReplace(Registry<T> registry, ResourceLocation id, T objectTo) {
        if (registry instanceof MappedRegistry<T> mappedRegistry) {
            T objectFrom = mappedRegistry.get(id);
            if (objectFrom != null) {
                ResourceKey<T> key = mappedRegistry.getResourceKey(objectFrom).orElse(null);
                Lifecycle lifecycle = mappedRegistry.lifecycle(objectFrom);
                if (key != null) {
                    int rawId = mappedRegistry.getId(objectFrom);
                    unfreeze(registry);
                    mappedRegistry.registerOrOverride(OptionalInt.of(rawId), key, objectTo, lifecycle);
                    freeze(registry);

                    T result = registry.get(id);
                    return result != null && result.equals(objectTo);
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
                for (Block block : net.minecraft.core.Registry.BLOCK) {
                    BLOCK_MATERIAL.put(block, Material.getMaterial(net.minecraft.core.Registry.BLOCK.getKey(block).getPath().toUpperCase(Locale.ROOT)));
                }

                ITEM_MATERIAL.clear();
                for (Item item : net.minecraft.core.Registry.ITEM) {
                    ITEM_MATERIAL.put(item, Material.getMaterial(net.minecraft.core.Registry.ITEM.getKey(item).getPath().toUpperCase(Locale.ROOT)));
                }

                FLUID_MATERIAL.clear();
                for (net.minecraft.world.level.material.Fluid fluid : net.minecraft.core.Registry.FLUID) {
                    FLUID_MATERIAL.put(fluid, org.bukkit.Registry.FLUID.get(CraftNamespacedKey.fromMinecraft(net.minecraft.core.Registry.FLUID.getKey(fluid))));
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
                    net.minecraft.core.Registry.ITEM.getOptional(key).ifPresent((item) -> {
                        MATERIAL_ITEM.put(material, item);
                    });
                    net.minecraft.core.Registry.BLOCK.getOptional(key).ifPresent((block) -> {
                        MATERIAL_BLOCK.put(material, block);
                    });
                    net.minecraft.core.Registry.FLUID.getOptional(key).ifPresent((fluid) -> {
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
