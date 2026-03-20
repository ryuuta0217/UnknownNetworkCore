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

package net.unknown.survival.feature;

import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.Component;
import net.minecraft.core.UUIDUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.dependency.CoreProtect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Graveyard implements Listener {
    public static final BlockFace[] CHEST_EXPAND_DIRECTIONS = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final NamespacedKey OWNER_DATA_KEY = new NamespacedKey("unknown-network", "graveyard/owner");
    private static final NamespacedKey EXPIRATION_DATA_KEY = new NamespacedKey("unknown-network", "graveyard/expiration");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (!event.getKeepInventory() && !event.getDrops().isEmpty()) {
            GraveyardPlacement graveyardLocation = searchGraveyardPlacementLocation(event.getEntity().getLocation(), event.getDrops().size() > 27);
            if (graveyardLocation == null) {
                NewMessageUtil.sendErrorMessage(event.getEntity(), Component.text("墓を設置できる場所が見つかりませんでした。アイテムはその場に散らばっており、5分で消滅します。"));
                return;
            }

            List<ItemStack> dropsCopy = new ArrayList<>(event.getDrops());
            boolean graveyardPlacementSuccess = placeGraveyard(event.getPlayer(), TimeUnit.MINUTES.toMillis(30), graveyardLocation.location(), graveyardLocation.chestExpandFacing(), dropsCopy);
            if (graveyardPlacementSuccess) {
                event.getDrops().clear();
                Location graveLocation = graveyardLocation.location();
                NewMessageUtil.sendMessage(event.getEntity(), Component.text("墓を " + graveLocation.getBlockX() + "," + graveLocation.getBlockY() + "," + graveLocation.getBlockZ() + " に設置しました。30分以内であれば、回収できます。"));
            } else {
                if (dropsCopy.size() != event.getDrops().size()) {
                    // It may place graveyard successfully, but items are overflowed. Drop the remaining items.
                    event.getDrops().clear();
                    event.getDrops().addAll(dropsCopy);
                    NewMessageUtil.sendErrorMessage(event.getEntity(), Component.text("墓の設置に部分的に成功しました。一部のアイテムはその場に散らばっており、5分で消滅します。"));
                } else {
                    NewMessageUtil.sendErrorMessage(event.getEntity(), Component.text("墓の設置に失敗しました。アイテムはその場に散らばっており、5分で消滅します。"));
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.PHYSICAL) return;
        if (event.getClickedBlock() == null || !(event.getClickedBlock().getState() instanceof Chest chest)) return;

        // Restrict access to graveyard of others
        UUID ownerUniqueId = getGraveyardOwnerUUID(event.getClickedBlock());
        if (ownerUniqueId == null) return;
        if (!event.getPlayer().getUniqueId().equals(ownerUniqueId)) {
            NewMessageUtil.sendErrorMessage(event.getPlayer(), Component.text("これは誰かのお墓です。あなたが触れることはできません。"));
            event.setCancelled(true);
            return;
        }

        // Restrict access to unknown expiration graveyards
        long expirationTimestamp = getExpirationTimestamp(event.getClickedBlock());
        if (expirationTimestamp == -1) {
            NewMessageUtil.sendErrorMessage(event.getPlayer(), Component.text("墓のデータが破損しているようです。いつ土に還るかを確認できないため、開くことができません。"));
            event.setCancelled(true);
            return;
        }

        // Restrict access to expired graveyards
        if (System.currentTimeMillis() > expirationTimestamp) {
            NewMessageUtil.sendErrorMessage(event.getPlayer(), Component.text("墓は既に土に還ってしまったようです"));
            destroyGraveyard(event.getClickedBlock());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory instanceof DoubleChestInventory doubleChestInventory) {
            inventory = doubleChestInventory.getLeftSide();
        }

        Location blockLocation = inventory.getLocation();
        if (blockLocation == null) return;

        Pair<Chest, Chest> graveyard = getGraveyard(blockLocation);
        if (graveyard == null) return; // Not a graveyard

        UUID ownerUniqueId = getGraveyardOwnerUUID(graveyard.first().getLocation());
        if (ownerUniqueId == null) return; // Not a graveyard - but should not happen

        if (event.getInventory().isEmpty() && graveyard.first().getBlockInventory().isEmpty() && (graveyard.second() == null || graveyard.second().getBlockInventory().isEmpty())) {
            destroyGraveyard(blockLocation); // Destroy graveyard if empty, all items taken
        }
    }

    private static GraveyardPlacement searchGraveyardPlacementLocation(Location deathLocation, boolean isDouble) {
        // 死んだ場所に置ける
        if (isReplaceableByGraveyard(deathLocation.getBlock())) {
            // single
            if (!isDouble) return new GraveyardPlacement(deathLocation, null);

            // large
            for(BlockFace expandDirection : CHEST_EXPAND_DIRECTIONS) {
                if (isReplaceableByGraveyard(deathLocation.getBlock().getRelative(expandDirection))) {
                    return new GraveyardPlacement(deathLocation, expandDirection);
                }
            }
        }

        boolean isNether = deathLocation.getWorld().getEnvironment() == World.Environment.NETHER;
        int maxHeight = isNether ? 127 : deathLocation.getWorld().getMaxHeight();

        for (int y = deathLocation.getBlockY(); y <= maxHeight; y++) {
            Location loc = deathLocation.clone();
            loc.setY(y);

            if (isReplaceableByGraveyard(loc.getBlock())) {
                // single
                if (!isDouble) return new GraveyardPlacement(loc, null);

                // large
                for(BlockFace expandDirection : CHEST_EXPAND_DIRECTIONS) {
                    if (isReplaceableByGraveyard(loc.getBlock().getRelative(expandDirection))) {
                        // 早期return可能なのでとっととreturnする
                        return new GraveyardPlacement(loc, expandDirection);
                    }
                }
            }
        }

        return null;
    }

    private static boolean isReplaceableByGraveyard(Block block) {
        Material type = block.getType();
        return type.isAir() || type == Material.WATER || type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.STONE || type == Material.GRAVEL || type == Material.SAND;
    }

    @Nullable
    private static Pair<Chest, Chest> getGraveyard(Location location) {
        return getGraveyard(location.getBlock());
    }

    @Nullable
    private static Pair<Chest, Chest> getGraveyard(Block blockA) {
        if (getGraveyardOwnerUUID(blockA) == null) return null;

        if (blockA.getBlockData() instanceof org.bukkit.block.data.type.Chest chestBlockA && blockA.getState() instanceof Chest chestA) {
            if (chestBlockA.getType() == org.bukkit.block.data.type.Chest.Type.SINGLE) {
                return Pair.of(chestA, null);
            } else {
                Block blockB = getGraveyardPart(blockA);
                if (blockB != null && getGraveyardOwnerUUID(blockB) != null) {
                    if (blockB.getBlockData() instanceof org.bukkit.block.data.type.Chest chestBlockB && blockB.getState() instanceof Chest chestB) {
                        if (chestBlockA.getType() == org.bukkit.block.data.type.Chest.Type.LEFT && chestBlockB.getType() == org.bukkit.block.data.type.Chest.Type.RIGHT) {
                            return Pair.of(chestA, chestB);
                        } else {
                            return Pair.of(chestB, chestA);
                        }
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static Block getGraveyardPart(Block blockA) {
        if (blockA.getBlockData() instanceof org.bukkit.block.data.type.Chest chestBlockA) {
            if (chestBlockA.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
                BlockFace facing = chestBlockA.getFacing();
                org.bukkit.block.data.type.Chest.Type type = chestBlockA.getType();
                BlockFace partOfLargeChestRelativeFacing = switch (facing) {
                    //        N N
                    //
                    // W      C C      E
                    //
                    //        S S
                    case NORTH -> type == org.bukkit.block.data.type.Chest.Type.LEFT ? BlockFace.EAST : BlockFace.WEST;
                    case SOUTH -> type == org.bukkit.block.data.type.Chest.Type.LEFT ? BlockFace.WEST : BlockFace.EAST;
                    case WEST -> type == org.bukkit.block.data.type.Chest.Type.LEFT ? BlockFace.NORTH : BlockFace.SOUTH;
                    case EAST -> type == org.bukkit.block.data.type.Chest.Type.LEFT ? BlockFace.SOUTH : BlockFace.NORTH;
                    default -> null;
                };

                if (partOfLargeChestRelativeFacing != null) {
                    Block blockB = blockA.getRelative(partOfLargeChestRelativeFacing);
                    if (blockB.getBlockData() instanceof org.bukkit.block.data.type.Chest chestBlockB) {
                        if (chestBlockB.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
                            return blockB;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static boolean placeGraveyard(Player owner, long expiresAfter, Location location, @Nullable BlockFace expandFacing, List<ItemStack> items) {
        try {
            // Place Chest(s)
            Pair<Chest, Chest> chests;
            if (items.size() <= 27) {
                Block block = location.getBlock();
                block.setType(Material.CHEST);
                BlockState state = block.getState();
                if (state instanceof Chest chest) {
                    chests = Pair.of(chest, null);
                } else {
                    throw new IllegalStateException("Failed to place chest while placing graveyard");
                }
            } else if (expandFacing != null) {
                chests = placeDoubleChest(location, expandFacing);
            } else {
                throw new IllegalStateException("Failed to place graveyard: Requires double chest, but not provided expandFacing");
            }

            // Setup chest as graveyard
            int[] ownerUniqueIdArr = UUIDUtil.uuidToIntArray(owner.getUniqueId());
            long expirationTimestamp = System.currentTimeMillis() + expiresAfter;

            chests.first().getPersistentDataContainer().set(OWNER_DATA_KEY, PersistentDataType.INTEGER_ARRAY, ownerUniqueIdArr);
            chests.first().getPersistentDataContainer().set(EXPIRATION_DATA_KEY, PersistentDataType.LONG, expirationTimestamp);
            chests.first().update();

            if (chests.second() != null) {
                chests.second().getPersistentDataContainer().set(OWNER_DATA_KEY, PersistentDataType.INTEGER_ARRAY, ownerUniqueIdArr);
                chests.second().getPersistentDataContainer().set(EXPIRATION_DATA_KEY, PersistentDataType.LONG, expirationTimestamp);
                chests.second().update();
            }

            // Log Chest Placement
            if (CoreProtect.isAPIEnabled()) {
                Pair<Block, Block> chestBlocks = Pair.of(chests.first().getLocation().getBlock(), chests.second() != null ? chests.second().getLocation().getBlock() : null);

                CoreProtect.getAPI().logPlacement("#graveyard", chestBlocks.first().getLocation(), chestBlocks.first().getType(), chestBlocks.first().getBlockData());
                if (chestBlocks.second() != null) {
                    CoreProtect.getAPI().logPlacement("#graveyard", chestBlocks.second().getLocation(), chestBlocks.second().getType(), chestBlocks.second().getBlockData());
                }
            }

            // Add Items
            Inventory inventory = chests.first().getInventory(); // If double chest, this will be DoubleChestInventory

            if (CoreProtect.isAPIEnabled()) {
                CoreProtect.getAPI().logContainerTransaction("#graveyard", chests.first().getLocation());
                if (chests.second() != null) {
                    CoreProtect.getAPI().logContainerTransaction("#graveyard", chests.second().getLocation());
                }
            }

            HashMap<Integer, ItemStack> overflows = inventory.addItem(items.toArray(new ItemStack[0]));
            items.removeIf(item -> !overflows.containsValue(item));
            return location.getBlock().getType() == Material.CHEST && overflows.isEmpty();
        } catch(Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static Pair<Chest, Chest> placeDoubleChest(Location center, BlockFace expandDirection) {
        if (Arrays.stream(CHEST_EXPAND_DIRECTIONS).noneMatch(expandDirection::equals)) {
            throw new IllegalArgumentException("expandDirection must be one of " + Arrays.toString(CHEST_EXPAND_DIRECTIONS));
        }

        Block blockA = center.getBlock();
        Block blockB = blockA.getRelative(expandDirection);

        BlockFace chestFacing = switch(expandDirection) {
            case WEST -> BlockFace.SOUTH;
            case EAST -> BlockFace.NORTH;
            case SOUTH -> BlockFace.EAST;
            case NORTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };

        blockA.setType(Material.CHEST);
        blockB.setType(Material.CHEST);

        org.bukkit.block.data.type.Chest dataA = (org.bukkit.block.data.type.Chest) blockA.getBlockData();
        dataA.setFacing(chestFacing);
        dataA.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
        blockA.setBlockData(dataA);

        org.bukkit.block.data.type.Chest dataB = (org.bukkit.block.data.type.Chest) blockB.getBlockData();
        dataB.setFacing(chestFacing);
        dataB.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
        blockB.setBlockData(dataB);

        Chest chestA = (Chest) blockA.getState();
        Chest chestB = (Chest) blockB.getState();

        return Pair.of(chestA, chestB);
    }

    private static void destroyGraveyard(Location location) {
        destroyGraveyard(location.getBlock());
    }

    private static void destroyGraveyard(Block block) {
        if (getGraveyardOwnerUUID(block) == null) return; // Not a graveyard
        Pair<Chest, Chest> chests = getGraveyard(block);
        if (chests == null) return; // Not a graveyard - but should not happen

        // Clear Items
        if (CoreProtect.isAPIEnabled()) {
            CoreProtect.getAPI().logContainerTransaction("#graveyard", chests.first().getLocation());
            if (chests.second() != null) CoreProtect.getAPI().logContainerTransaction("#graveyard", chests.second().getLocation());
        }
        chests.first().getBlockInventory().clear();
        if (chests.second() != null) chests.second().getBlockInventory().clear();

        // Remove Chest(s)
        if (CoreProtect.isAPIEnabled()) {
            CoreProtect.getAPI().logRemoval("#graveyard", chests.first());
            if (chests.second() != null) CoreProtect.getAPI().logRemoval("#graveyard", chests.second());
        }
        chests.first().getBlock().setType(Material.AIR);
        if (chests.second() != null) chests.second().getBlock().setType(Material.AIR);
    }

    private static UUID getGraveyardOwnerUUID(Location location) {
        return getGraveyardOwnerUUID(location.getBlock());
    }

    private static UUID getGraveyardOwnerUUID(Block block) {
        return getGraveyardOwnerUUID(block.getState());
    }

    private static UUID getGraveyardOwnerUUID(BlockState blockStateA) {
        Block blockB = getGraveyardPart(blockStateA.getBlock());

        Container container;
        if (blockStateA instanceof Container containerA && containerA.getPersistentDataContainer().has(OWNER_DATA_KEY, PersistentDataType.INTEGER_ARRAY)) {
            container = containerA;
        } else if (blockB != null && blockB.getState() instanceof Container containerB && containerB.getPersistentDataContainer().has(OWNER_DATA_KEY, PersistentDataType.INTEGER_ARRAY)) {
            container = containerB;
        } else {
            return null;
        }

        int[] ownerUniqueIdArr = container.getPersistentDataContainer().get(OWNER_DATA_KEY, PersistentDataType.INTEGER_ARRAY);
        if (ownerUniqueIdArr == null) return null; // ここにはおそらく到達しない

        return UUIDUtil.uuidFromIntArray(ownerUniqueIdArr);
    }

    private static long getExpirationTimestamp(Location location) {
        return getExpirationTimestamp(location.getBlock());
    }

    private static long getExpirationTimestamp(Block block) {
        return getExpirationTimestamp(block.getState());
    }

    private static long getExpirationTimestamp(BlockState blockState) {
        if (!(blockState instanceof Container container)) return -1;
        Long expirationTimestamp = container.getPersistentDataContainer().get(EXPIRATION_DATA_KEY, PersistentDataType.LONG);
        if (expirationTimestamp == null) return -1;
        return expirationTimestamp;
    }

    private record GraveyardPlacement(Location location, @Nullable BlockFace chestExpandFacing) {

    }
}
