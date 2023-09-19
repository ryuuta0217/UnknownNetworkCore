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

import net.unknown.core.managers.RunnableManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * プログラム化された花火のメイン・データ・モデルです。
 */
public class Program {
    private final String id; // not needed?
    private final Map<Long, Set<Firework>> programs;
    private final Map<String, Location> locations;
    private final Map<String, Color> colors;

    public Program(String id, Map<Long, Set<Firework>> programs, Map<String, Location> locations, Map<String, Color> colors) {
        this.id = id;
        this.programs = new HashMap<>(programs);
        this.locations = new HashMap<>(locations);
        this.colors = new HashMap<>(colors);
    }

    public String getId() {
        return this.id;
    }

    public Map<Long, Set<Firework>> getPrograms() {
        return new HashMap<>(this.programs);
    }

    public Set<Firework> getFireworks(long tick) {
        return this.programs.get(tick);
    }

    public boolean hasFirework(long tick) {
        return this.programs.containsKey(tick) && !this.programs.get(tick).isEmpty();
    }

    public void addFirework(long tick, Firework firework) {
        if (!this.programs.containsKey(tick)) this.programs.put(tick, new HashSet<>());
        this.programs.get(tick).add(firework);
    }

    public void removeFireworks(long tick) {
        programs.remove(tick);
    }

    public void removeFirework(long tick, Firework firework) {
        if (!this.programs.containsKey(tick)) return;
        this.programs.get(tick).remove(firework);
    }

    public void removeFirework(Firework firework) {
        this.programs.forEach((tick, fireworks) -> {
            if (fireworks.contains(firework)) {
                this.removeFirework(tick, firework);
            }
        });
    }

    public void removeFireworks() {
        this.programs.clear();
    }

    public Map<String, Location> getLocations() {
        return new HashMap<>(this.locations);
    }

    public Location getLocation(String id) {
        return this.locations.get(id).clone();
    }

    public boolean hasLocation(String id) {
        return this.locations.containsKey(id);
    }

    public void addLocation(String id, Location location) {
        if (hasLocation(id)) throw new IllegalArgumentException("Location ID " + id + " is already defined.");
        this.locations.put(id, location);
    }

    public void removeLocation(String id) {
        this.locations.remove(id);
    }

    public void removeLocations() {
        this.locations.clear();
    }

    public Map<String, Color> getColors() {
        return new HashMap<>(this.colors);
    }

    public Color getColor(String id) {
        return this.colors.get(id);
    }

    public boolean hasColor(String id) {
        return this.colors.containsKey(id);
    }

    public void addColor(String id, Color color) {
        if (hasColor(id)) throw new IllegalArgumentException("Color ID " + id + " is already defined.");
        this.colors.put(id, color);
    }

    public void removeColor(String id) {
        this.colors.remove(id);
    }

    public void removeColors() {
        this.colors.clear();
    }

    public void save() {
        // ProgrammedFireworks.save(this.id, this);
    }

    public void saveAsync() {
        RunnableManager.runAsync(this::save);
    }

    public void write(ConfigurationSection section) {
        section.set("id", this.id);
        ConfigurationSection locationsSection = section.createSection("locations");
        this.locations.forEach((id, location) -> {
            locationsSection.set(id, location);
        });
        ConfigurationSection programsSection = section.createSection("programs");
        this.programs.forEach((tick, fireworks) -> {
            int i = 0;
            for (Firework firework : fireworks) {
                firework.write(programsSection.createSection(tick.toString() + "." + i));
            }
        });
    }
}
