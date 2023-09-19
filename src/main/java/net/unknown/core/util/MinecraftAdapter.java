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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MinecraftAdapter {
    public static net.minecraft.world.item.Item item(Material bukkit) {
        return CraftMagicNumbers.getItem(bukkit);
    }

    public static net.minecraft.world.level.block.Block block(Material bukkit) {
        return CraftMagicNumbers.getBlock(bukkit);
    }

    public static net.minecraft.core.Direction direction(BlockFace bukkit) {
        return switch (bukkit) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
            default -> null;
        };
    }

    public static Vec3 vec3(Location bukkit) {
        return new Vec3(bukkit.getX(), bukkit.getY(), bukkit.getZ());
    }

    public static Vec2 vec2(Location bukkit) {
        return new Vec2(bukkit.getPitch(), bukkit.getYaw());
    }

    public static Location location(Level level, Vec3 coordinates, Vec2 rotation) {
        return new Location(level.getWorld(), coordinates.x(), coordinates.y(), coordinates.z(), rotation.y, rotation.x);
    }

    @Contract("null -> null")
    public static net.minecraft.server.level.ServerPlayer player(@Nullable Player bukkit) {
        if (bukkit instanceof CraftPlayer craft) {
            if (craft.getHandle() != null) return craft.getHandle();
        }
        return null;
    }

    @Nullable
    public static net.minecraft.world.entity.Entity entity(org.bukkit.entity.Entity bukkit) {
        if (bukkit instanceof CraftEntity craft) {
            if (craft.getHandle() != null) return craft.getHandle();
        }
        return null;
    }

    @Nullable
    public static net.minecraft.world.Container container(Inventory bukkit) {
        // Bukkit#createInventoryで作成されたインベントリ(Container)の実態は CraftInventoryCustom$MinecraftInventory
        // その他の（タイルエンティティを含む）インベントリの変換は CraftInventoryCreator を参照
        if (bukkit instanceof CraftInventory craft) {
            if (craft.getInventory() != null) return craft.getInventory();
        }
        return null;
    }

    @Contract("_ -> new;")
    public static net.minecraft.core.BlockPos blockPos(Location bukkit) {
        return new net.minecraft.core.BlockPos(bukkit.getBlockX(), bukkit.getBlockY(), bukkit.getBlockZ());
    }

    public static net.minecraft.server.level.ServerLevel level(World world) {
        return ((CraftWorld) world).getHandle();
    }

    public static World world(Level level) {
        return level.getWorld();
    }

    public static net.minecraft.world.level.block.state.BlockState blockState(Block block) {
        return level(block.getLocation().getWorld()).getBlockState(blockPos(block.getLocation()));
    }

    public static net.minecraft.network.chat.ChatType chatType(net.kyori.adventure.chat.ChatType adventure) {
        Registry<ChatType> chatTypes = MinecraftServer.getServer().registryAccess().registry(Registries.CHAT_TYPE).orElse(null);
        if (chatTypes != null) {
            return chatTypes.get(ResourceLocation.of(adventure.key().asString(), ':'));
        }
        throw new IllegalStateException("Failed to get Minecraft's ChatType registry, early access?");
    }

    public static net.kyori.adventure.chat.ChatType chatType(@Nonnull net.minecraft.network.chat.ChatType minecraft) {
        Registry<ChatType> chatTypes = MinecraftServer.getServer().registryAccess().registry(Registries.CHAT_TYPE).orElse(null);
        if (chatTypes != null) {
            ResourceLocation minecraftKey = chatTypes.getKey(minecraft);
            if (minecraftKey != null) {
                return net.kyori.adventure.chat.ChatType.chatType(Key.key(minecraftKey.toString()));
            }
            throw new IllegalStateException("Unknown ChatType, not registered to Registry.CHAT_TYPE.");
        }
        throw new IllegalStateException("Failed to get Minecraft's ChatType registry, early access?");
    }

    public static net.kyori.adventure.chat.ChatType chatType(ResourceKey<net.minecraft.network.chat.ChatType> minecraft) {
        Registry<ChatType> chatTypes = MinecraftServer.getServer().registryAccess().registry(Registries.CHAT_TYPE).orElse(null);
        if (chatTypes != null) {
            ChatType chatType = chatTypes.get(minecraft);
            if (chatType != null) {
                return chatType(chatType);
            }
            throw new IllegalArgumentException("Unknown ChatType, not registered to Registry.CHAT_TYPE.");
        }
        throw new IllegalStateException("Failed to get Minecraft's ChatType registry, early access?");
    }

    @ParametersAreNonnullByDefault
    public static class ItemStack {
        @Nullable
        public static net.minecraft.world.item.ItemStack json(String json) {
            try {
                return net.minecraft.world.item.ItemStack.of(TagParser.parseTag(json));
            } catch (CommandSyntaxException e) {
                return null;
            }
        }

        @Nonnull
        public static String json(net.minecraft.world.item.ItemStack itemStack) {
            return itemStack.save(new CompoundTag()).getAsString();
        }

        public static net.minecraft.world.item.ItemStack itemStack(org.bukkit.inventory.ItemStack bukkit) {
            if (bukkit instanceof CraftItemStack craft) {
                if (craft.handle != null) return craft.handle;
            }
            return CraftItemStack.asNMSCopy(bukkit);
        }

        public static org.bukkit.inventory.ItemStack itemStack(net.minecraft.world.item.ItemStack minecraft) {
            return itemStack(minecraft, true);
        }

        public static org.bukkit.inventory.ItemStack itemStack(net.minecraft.world.item.ItemStack minecraft, boolean mirror) {
            if (mirror) return CraftItemStack.asCraftMirror(minecraft);
            else return CraftItemStack.asBukkitCopy(minecraft);
        }
    }

    @ParametersAreNonnullByDefault
    public static class Adventure {
        public static net.minecraft.network.chat.PlayerChatMessage playerChatMessage(SignedMessage adventure) {
            PlayerChatMessage.AdventureView minecraft = (PlayerChatMessage.AdventureView) adventure;
            return minecraft.playerChatMessage();
        }
    }
}
