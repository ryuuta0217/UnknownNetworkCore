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

package net.unknown.core.fireworks.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.fireworks.ProgrammedFireworks;
import net.unknown.core.fireworks.ref.ColorReference;
import net.unknown.core.fireworks.ref.LocationReference;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftFirework;

import java.util.*;
import java.util.stream.Stream;

/**
 * プログラム化された花火の打ち上げ内容を表すクラスです。
 */
public class Firework {
    private final Program parent;
    private LocationReference launchLocation;
    private int locationYOffset;
    private boolean randomizeLaunchLocation;

    private int lifetime;
    private boolean shotAtAngle;
    private final Map<String, Explosion> explosions;

    public Firework(Program parent, LocationReference launchLocation, int launchYOffset, boolean randomizeLaunchLocation, int lifetime, boolean shotAtAngle, Map<String, Explosion> explosions) {
        this.parent = parent;
        this.launchLocation = launchLocation;
        this.locationYOffset = launchYOffset;
        this.randomizeLaunchLocation = randomizeLaunchLocation;
        this.lifetime = lifetime;
        this.shotAtAngle = shotAtAngle;
        this.explosions = new HashMap<>(explosions);
    }

    public Program getParent() {
        return this.parent;
    }

    public LocationReference getLaunchLocationRef() {
        return this.launchLocation;
    }

    public Location getPureLaunchLocation() {
        return this.launchLocation.get();
    }

    public Location getLaunchLocation() {
        Location location = this.getPureLaunchLocation();

        if (location != null && this.locationYOffset != 0) {
            location = location.clone();
            location.add(0, this.locationYOffset, 0);
        }

        if (location != null && this.randomizeLaunchLocation) {
            location = location.clone();
            location.add((Math.random() * 6) - 3, 0, (Math.random() * 6) - 3);
        }

        return location;
    }

    public void setLaunchLocation(LocationReference launchLocation) {
        this.launchLocation = launchLocation;
        this.getParent().saveAsync();
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
        this.getParent().saveAsync();
    }

    public boolean isShotAtAngle() {
        return this.shotAtAngle;
    }

    public void setShotAtAngle(boolean shotAtAngle) {
        this.shotAtAngle = shotAtAngle;
        this.getParent().saveAsync();
    }

    public Map<String, Explosion> getExplosions() {
        return new HashMap<>(this.explosions);
    }

    public void addExplosion(String id, Explosion explosion) {
        if (hasExplosion(id)) throw new IllegalArgumentException("Firework already contains an explosion with id " + id + " (id must be unique!)");
        this.explosions.put(id, explosion);
        this.getParent().saveAsync();
    }

    public void removeExplosion(String id) {
        this.explosions.remove(id);
        this.getParent().saveAsync();
    }

    public boolean hasExplosion(String id) {
        return this.explosions.containsKey(id);
    }

    public ItemStack buildItemStack() {
        ItemStack item = new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET);

        // Format: {Fireworks:{Explosions:[{Type:1,Trail:1,Colors:[I;15204362,15204362]}],Flight:3,}}
        CompoundTag tag = new CompoundTag();
        CompoundTag fireworks = new CompoundTag();
        ListTag explosions = new ListTag();
        this.getExplosions().forEach((id, explosion) -> {
            explosions.add(explosion.buildCompoundTag());
        });
        fireworks.put("Explosions", explosions);
        tag.put("Fireworks", fireworks);
        item.setTag(tag);
        return item;
    }

    public org.bukkit.inventory.ItemStack buildBukkitItemStack() {
        return MinecraftAdapter.ItemStack.itemStack(buildItemStack());
    }

    public FireworkRocketEntity buildEntity() {
        Location launchLocation = this.getLaunchLocation();
        FireworkRocketEntity entity = new FireworkRocketEntity(MinecraftAdapter.level(launchLocation.getWorld()), launchLocation.getX(), launchLocation.getY(), launchLocation.getZ(), ItemStack.EMPTY);
        entity.lifetime = this.lifetime;
        entity.getEntityData().set(FireworkRocketEntity.DATA_ID_FIREWORKS_ITEM, this.buildItemStack());
        entity.getEntityData().set(FireworkRocketEntity.DATA_SHOT_AT_ANGLE, this.shotAtAngle);
        return entity;
    }

    public org.bukkit.entity.Firework buildBukkitEntity() {
        return new CraftFirework((CraftServer) Bukkit.getServer(), buildEntity());
    }

    public void launch() {
        FireworkRocketEntity firework = this.buildEntity();
        firework.level().addFreshEntity(firework);
    }

    public void write(ConfigurationSection section) {
        section.set("launch-location", this.launchLocation.toString());
        section.set("launch-y-offset", this.locationYOffset);
        section.set("randomize-launch-location", this.randomizeLaunchLocation);
        section.set("lifetime", this.lifetime);
        section.set("shot-at-angle", this.shotAtAngle);
        ConfigurationSection explosionsSection = section.createSection("explosions");
        this.explosions.forEach((id, explosion) -> {
            explosion.write(explosionsSection.createSection(id));
        });
    }

    public static Firework read(ConfigurationSection section) {
        LocationReference launchLocation = LocationReference.parse(section.getString("launch-location", null));
        int launchYOffset = section.getInt("launch-y-offset", 0);
        boolean randomizeLaunchLocation = section.getBoolean("randomize-launch-location", false);
        int lifetime = section.getInt("lifetime");
        boolean shotAtAngle = section.getBoolean("shot-at-angle");

        Map<String, Explosion> explosions = new HashMap<>();
        ConfigurationSection explosionsSection = section.getConfigurationSection("explosions");
        if (explosionsSection != null) {
            explosionsSection.getKeys(false).forEach(id -> {
                ConfigurationSection explosionSection = explosionsSection.getConfigurationSection(id);
                if (explosionSection != null) {
                    explosions.put(id, Explosion.read(explosionSection));
                }
            });
        }

        return new Firework(null, launchLocation, launchYOffset, randomizeLaunchLocation, lifetime, shotAtAngle, explosions);
    }

    public static Firework buildObject(CompoundTag tag, Program parent, LocationReference launchLocation, int launchYOffset, boolean randomizeLaunchLocation) {
        // entity tag input:
        // {
        //  LifeTime:30,
        //  FireworksItem: {
        //      id:firework_rocket,
        //      Count:1,
        //      tag: {
        //          Fireworks:{Explosions:[{Type:1,Trail:1,Colors:[I;15204362,15204362]}],Flight:3,}
        //      }
        //  }
        // }

        // item tag input:
        // {
        // Fireworks: {
        //      Explosions:[{Type:1,Trail:1,Colors:[I;15204362,15204362]}],
        //      Flight:3,
        //  }
        // }
        boolean entityTag = tag.contains("FireworksItem");

        // if item tag input, calculate lifetime from Flight tag, 10*1+flight = lifetime
        int lifeTime = entityTag ? tag.getInt("LifeTime") : 10 * (1 + tag.getCompound("Fireworks").getInt("Flight"));
        boolean shotAtAngle = entityTag ? (tag.contains("ShotAtAngle") ? tag.getBoolean("ShotAtAngle") : false) : false;

        CompoundTag fireworks = entityTag ? tag.getCompound("FireworksItem").getCompound("tag").getCompound("Fireworks") : tag.getCompound("Fireworks");
        ListTag explosions = fireworks.getList("Explosions", Tag.TAG_COMPOUND);
        Map<String, Explosion> explosionsMap = new HashMap<>();
        for (int i = 0; i < explosions.size(); i++) {
            CompoundTag explosion = explosions.getCompound(i);
            String id = String.valueOf(i);
            explosionsMap.put(id, Explosion.buildObject(explosion, parent.getId()));
        }
        return new Firework(parent, launchLocation, launchYOffset, randomizeLaunchLocation, lifeTime, shotAtAngle, explosionsMap);
    }

    /**
     * Represents Minecraft's Firework Explosion.
     */
    public static class Explosion {
        private final FireworkRocketItem.Shape type;
        private final boolean flicker;
        private final boolean trail;
        private final ArrayList<ColorReference> colors;
        private final ArrayList<ColorReference> fadeColors;

        public Explosion(FireworkRocketItem.Shape type, boolean flicker, boolean trail, List<ColorReference> colors, List<ColorReference> fadeColors) {
            this.type = type;
            this.flicker = flicker;
            this.trail = trail;
            this.colors = new ArrayList<>(colors);
            this.fadeColors = new ArrayList<>(fadeColors);
        }

        public FireworkRocketItem.Shape getType() {
            return this.type;
        }

        public boolean isFlicker() {
            return this.flicker;
        }

        public boolean isTrail() {
            return this.trail;
        }

        public List<ColorReference> getColorRefs() {
            return this.colors;
        }

        public List<Color> getColors() {
            return this.colors.stream().map(ColorReference::get).toList();
        }

        public List<ColorReference> getFadeColorRefs() {
            return this.fadeColors;
        }

        public List<Color> getFadeColors() {
            return this.fadeColors.stream().map(ColorReference::get).toList();
        }

        // Format: {Type: 0, Flicker: 0b, Trail: 0b, Colors: [I;0, 0, 0], FadeColors: [I;0, 0, 0]}
        public CompoundTag buildCompoundTag() {
            CompoundTag explosion = new CompoundTag();
            explosion.putInt("Type", this.type.ordinal());
            explosion.putBoolean("Flicker", this.flicker);
            explosion.putBoolean("Trail", this.trail);
            explosion.putIntArray("Colors", this.getColors().stream().map(Color::asRGB).toList());
            explosion.putIntArray("FadeColors", this.getFadeColors().stream().map(Color::asRGB).toList());
            return explosion;
        }

        public static Explosion buildObject(CompoundTag tag, String programId) {
            FireworkRocketItem.Shape type = FireworkRocketItem.Shape.values()[tag.getInt("Type")];
            boolean flicker = tag.getBoolean("Flicker");
            boolean trail = tag.getBoolean("Trail");
            int[] colors = tag.getIntArray("Colors");
            List<ColorReference> colorRefs = Arrays.stream(colors).mapToObj(rgb -> {
                Map<String, Color> defColors = null;

                if (programId != null) {
                    Program program = ProgrammedFireworks.getProgram(programId);
                    if (program != null) {
                        defColors = program.getColors();
                    }
                } else {
                    defColors = ProgrammedFireworks.getColors();
                }

                if (defColors == null) {
                    return null;
                }

                Map.Entry<String, Color> defColor = defColors.entrySet().stream().filter((e) -> e.getValue().asRGB() == rgb).findFirst().orElse(null);
                if (defColor != null) {
                    return new ColorReference(programId, defColor.getKey());
                }

                return null;
            }).toList();

            int[] fadeColors = tag.getIntArray("FadeColors");
            List<ColorReference> fadeColorRefs = Arrays.stream(fadeColors).mapToObj(rgb -> {
                Map<String, Color> defColors = null;

                if (programId != null) {
                    Program program = ProgrammedFireworks.getProgram(programId);
                    if (program != null) {
                        defColors = program.getColors();
                    }
                } else {
                    defColors = ProgrammedFireworks.getColors();
                }

                if (defColors == null) {
                    return null;
                }

                Map.Entry<String, Color> defColor = defColors.entrySet().stream().filter((e) -> e.getValue().asRGB() == rgb).findFirst().orElse(null);
                if (defColor != null) {
                    return new ColorReference(programId, defColor.getKey());
                }

                return null;
            }).toList();

            return new Explosion(type, flicker, trail, colorRefs, fadeColorRefs);
        }

        public void write(ConfigurationSection section) {
            section.set("type", this.type.name());
            section.set("flicker", this.flicker);
            section.set("trail", this.trail);
            section.set("colors", this.colors.stream().map(ColorReference::toString).toList());
            section.set("fade-colors", this.fadeColors.stream().map(ColorReference::toString).toList());
        }

        public static Explosion read(ConfigurationSection section) {
            FireworkRocketItem.Shape type = FireworkRocketItem.Shape.valueOf(section.getString("type"));
            boolean flicker = section.getBoolean("flicker");
            boolean trail = section.getBoolean("trail");
            List<ColorReference> colors = section.getStringList("colors").stream().map(ColorReference::parse).toList();
            List<ColorReference> fadeColors = section.getStringList("fade-colors").stream().map(ColorReference::parse).toList();
            return new Explosion(type, flicker, trail, colors, fadeColors);
        }
    }
}
