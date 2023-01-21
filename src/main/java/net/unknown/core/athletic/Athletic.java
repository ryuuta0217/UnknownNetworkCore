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

package net.unknown.core.athletic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Athletic {
    private final UUID uniqueId;
    private final String name;
    private Component displayName;
    private final Set<Checkpoint> checkpoints = new HashSet<>();
    private Location start;
    private Location end;

    public Athletic(UUID uniqueId, String name, Component displayName, Location start, Location end) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.displayName = displayName;
        this.start = start;
        this.end = end;
    }

    public Athletic(UUID uniqueId, String name, Component displayName, Location start, Location end, Set<Checkpoint> checkpoints) {
        this(uniqueId, name, displayName, start, end);
        this.checkpoints.clear();
        this.checkpoints.addAll(checkpoints);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public Component setDisplayName(Component newDisplayName) {
        Component oldDisplayName = this.displayName;
        this.displayName = newDisplayName;
        RunnableManager.runAsync(Athletics::save);
        return oldDisplayName;
    }

    public Checkpoint addCheckpoint(String name, Component displayName, Location location) {
        UUID randomUniqueId = UUID.randomUUID();

        while (this.hasCheckpoint(randomUniqueId)) {
            randomUniqueId = UUID.randomUUID();
        }

        if (!this.hasCheckpoint(name)) {
            Checkpoint createdCheckpoint = new Checkpoint(randomUniqueId, name, displayName, location);
            if (this.checkpoints.add(createdCheckpoint)) {
                RunnableManager.runAsync(Athletics::save);
                return createdCheckpoint;
            }
        } else {
            throw new IllegalArgumentException("Checkpoint \"" + name + "\" is already exists. please try again with other name.");
        }
        return null;
    }

    public void removeCheckpoint(UUID uniqueId) {
        this.checkpoints.removeIf(checkpoint -> checkpoint.getUniqueId().equals(uniqueId));
        RunnableManager.runAsync(Athletics::save);
    }

    public void removeCheckpoint(String name) {
        this.checkpoints.removeIf(checkpoint -> checkpoint.getName().equals(name));
        RunnableManager.runAsync(Athletics::save);
    }

    public void removeCheckpoint(Checkpoint checkpoint) {
        this.checkpoints.remove(checkpoint);
        RunnableManager.runAsync(Athletics::save);
    }

    public boolean hasCheckpoint(UUID uniqueId) {
        return this.getCheckpoint(uniqueId) != null;
    }

    public boolean hasCheckpoint(String name) {
        return this.getCheckpointByName(name) != null;
    }

    public Checkpoint getCheckpoint(UUID uniqueId) {
        return this.checkpoints.stream()
                .filter(checkpoint -> checkpoint.getUniqueId().equals(uniqueId))
                .findFirst()
                .orElse(null);
    }

    public Checkpoint getCheckpointByName(String name) {
        return this.checkpoints.stream()
                .filter(checkpoint -> checkpoint.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public Set<Checkpoint> getCheckpoints() {
        return this.checkpoints;
    }

    public void save(ConfigurationSection athleticsSection) {
        ConfigurationSection athleticSection = athleticsSection.createSection(this.uniqueId.toString());
        athleticSection.set("name", this.name);
        athleticSection.set("display_name", GsonComponentSerializer.gson().serialize(this.displayName));
        ConfigurationSerializer.setLocationData(athleticSection, "start", this.start);
        ConfigurationSerializer.setLocationData(athleticSection, "end", this.end);
        ConfigurationSection checkpointsSection = athleticSection.createSection("checkpoints");
        this.checkpoints.forEach(checkpoint -> checkpoint.save(checkpointsSection));
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }

    public static Athletic load(ConfigurationSection athleticSection) {
        String rawUniqueId = athleticSection.getName();
        if (MessageUtil.isUUID(rawUniqueId)) {
            if (athleticSection.contains("name") && athleticSection.contains("display_name")) {
                UUID uniqueId = UUID.fromString(rawUniqueId);
                String name = athleticSection.getString("name");
                Component displayName = GsonComponentSerializer.gson().deserialize(athleticSection.getString("display_name", "{}"));
                Location start = athleticSection.contains("start") ? ConfigurationSerializer.getLocationData(athleticSection, "start") : null;
                Location end = athleticSection.contains("end") ? ConfigurationSerializer.getLocationData(athleticSection, "end") : null;
                Set<Checkpoint> checkpoints = new HashSet<>();
                if (athleticSection.contains("checkpoints")) {
                    ConfigurationSection checkpointsSection = athleticSection.getConfigurationSection("checkpoints");
                    if (checkpointsSection != null) {
                        checkpointsSection.getKeys(false).forEach(checkpointId -> {
                            ConfigurationSection checkpointSection = checkpointsSection.getConfigurationSection(checkpointId);
                            if (checkpointSection != null) {
                                Checkpoint checkpoint = Checkpoint.load(checkpointSection);
                                checkpoints.add(checkpoint);
                            }
                        });
                    }
                }
                return new Athletic(uniqueId, name, displayName, start, end, checkpoints);
            } else {
                throw new IllegalArgumentException("Failed to parse configuration: Invalid configuration format (require name, display_name)");
            }
        } else {
            throw new IllegalArgumentException("Failed to parse configuration: Invalid key (not a UUID)");
        }
    }

    public static class Checkpoint {
        private final UUID uniqueId;
        private final String name;
        private Component displayName;
        private Location location;

        public Checkpoint(UUID uniqueId, String name, Component displayName, Location location) {
            this.uniqueId = uniqueId;
            this.name = name;
            this.displayName = displayName;
            this.location = location;
        }

        public UUID getUniqueId() {
            return this.uniqueId;
        }

        public String getName() {
            return this.name;
        }

        public Component getDisplayName() {
            return this.displayName;
        }

        public void setDisplayName(Component displayName) {
            this.displayName = displayName;
            RunnableManager.runAsync(Athletics::save);
        }

        public Location getLocation() {
            return this.location;
        }

        public void setLocation(Location location) {
            this.location = location;
            RunnableManager.runAsync(Athletics::save);
        }

        @Override
        public int hashCode() {
            return this.uniqueId.hashCode();
        }

        public void save(ConfigurationSection checkpointsSection) {
            ConfigurationSection checkpointSection = checkpointsSection.createSection(this.uniqueId.toString());
            checkpointSection.set("name", this.name);
            checkpointSection.set("display_name", GsonComponentSerializer.gson().serialize(this.displayName));
            ConfigurationSerializer.setLocationData(checkpointSection, "location", this.location);
        }

        public static Checkpoint load(ConfigurationSection checkpointSection) {
            String rawUniqueId = checkpointSection.getName();
            if (MessageUtil.isUUID(rawUniqueId)) {
                if (checkpointSection.contains("name") && checkpointSection.contains("display_name") && checkpointSection.contains("location")) {
                    UUID uniqueId = UUID.fromString(rawUniqueId);
                    String name = checkpointSection.getString("name");
                    Component displayName = GsonComponentSerializer.gson().deserialize(checkpointSection.getString("display_name", "{}"));
                    Location location = ConfigurationSerializer.getLocationData(checkpointSection," location");
                    return new Checkpoint(uniqueId, name, displayName, location);
                } else {
                    throw new IllegalArgumentException("Failed to parse configuration: Invalid configuration format (require name, display_name, location)");
                }
            } else {
                throw new IllegalArgumentException("Failed to parse configuration: Invalid Key (not a UUID)");
            }
        }
    }
}
