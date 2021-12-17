/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.configurations;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.logging.Logger;

public class ConfigurationSerializer {
    private static final Logger LOGGER = Logger.getLogger("UNC/ConfigurationSerializer");
    private static final String[] LOCATION_VALUES = new String[] {"world", "x", "y", "z", "yaw", "pitch"};
    private static final String[] COLOR_VALUES = new String[] {"r", "g", "b"};

    public static void setLocationData(FileConfiguration config, String path, Location loc) {
        Arrays.stream(LOCATION_VALUES).forEach(s -> config.set(path + "." + s, ConfigurationSerializer.getLocation(s, loc)));
    }

    @Nullable
    public static Location getLocationData(ConfigurationSection config, String path) {
        String worldName = "world";
        double x = 0.0D, y = 0.0D, z = 0.0D;
        float yaw = 0F, pitch = 0F;

        for (String s : LOCATION_VALUES) {
            if (s.equalsIgnoreCase("world") && config.isSet(path + "." + s))
                worldName = config.getString(path + "." + s);
            else if (s.equalsIgnoreCase("x") && config.isSet(path + "." + s))
                x = config.getDouble(path + "." + s);
            else if (s.equalsIgnoreCase("y") && config.isSet(path + "." + s))
                y = config.getDouble(path + "." + s);
            else if (s.equalsIgnoreCase("z") && config.isSet(path + "." + s))
                z = config.getDouble(path + "." + s);
            else if (s.equalsIgnoreCase("yaw") && config.isSet(path + "." + s))
                yaw = (float) config.getDouble(path + "." + s);
            else if (s.equalsIgnoreCase("pitch") && config.isSet(path + "." + s))
                pitch = (float) config.getDouble(path + "." + s);
            else if (!config.isSet(path + "." + s))
                LOGGER.warning(path + "." + s + " is not set, using default value!");
        }

        if (worldName == null || Bukkit.getWorld(worldName) == null) return null;
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    @Nullable
    private static Object getLocation(String s, Location loc) {
        if (s.equalsIgnoreCase("world")) return loc.getWorld().getName();
        else if (s.equalsIgnoreCase("x")) return loc.getX();
        else if (s.equalsIgnoreCase("y")) return loc.getY();
        else if (s.equalsIgnoreCase("z")) return loc.getZ();
        else if (s.equalsIgnoreCase("yaw")) return loc.getYaw();
        else if (s.equalsIgnoreCase("pitch")) return loc.getPitch();
        else return null;
    }

    public static void setColorData(FileConfiguration config, String path, Color color) {
        Arrays.stream(COLOR_VALUES).forEach(s -> config.set(path + "." + s, ConfigurationSerializer.getColor(s, color)));
    }

    public static Color getColorData(FileConfiguration config, String path) {
        int r = 0, g = 0, b = 0;
        if (config.isSet(path + ".r")) r = config.getInt(path + ".r");
        if (config.isSet(path + ".g")) g = config.getInt(path + ".g");
        if (config.isSet(path + ".b")) b = config.getInt(path + ".b");
        return Color.fromRGB(r, g, b);
    }

    @Nullable
    private static Object getColor(String s, Color color) {
        if (s.equalsIgnoreCase("r")) return color.getRed();
        else if (s.equalsIgnoreCase("g")) return color.getGreen();
        else if (s.equalsIgnoreCase("b")) return color.getBlue();
        else return null;
    }
}
