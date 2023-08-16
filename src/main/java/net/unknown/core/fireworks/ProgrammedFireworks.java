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

package net.unknown.core.fireworks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.fireworks.model.Firework;
import net.unknown.core.fireworks.model.Program;
import net.unknown.core.fireworks.ref.LocationReference;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProgrammedFireworks {
    private static final Map<String, Program> PROGRAMS = new HashMap<>();
    private static final Map<String, Location> LOCATIONS = new HashMap<>();
    private static final Map<String, Color> COLORS = new HashMap<>();

    public static Map<String, Program> getPrograms() {
        return Collections.unmodifiableMap(PROGRAMS);
    }

    @Nullable
    public static Program getProgram(String id) {
        if (!hasProgram(id)) return null;
        return PROGRAMS.get(id);
    }

    public static void addProgram(String id, Program program) {
        if (hasProgram(id)) throw new IllegalArgumentException("Program already exists with id " + id);
        PROGRAMS.put(id, program);
    }

    public static void removeProgram(String id) {
        PROGRAMS.remove(id);
    }

    public static boolean hasProgram(String id) {
        return PROGRAMS.containsKey(id);
    }

    public static Map<String, Location> getLocations() {
        return Collections.unmodifiableMap(LOCATIONS);
    }

    public static boolean hasLocation(String id) {
        return LOCATIONS.containsKey(id);
    }

    public static Location getLocation(String id) {
        return LOCATIONS.get(id);
    }

    public static void addLocation(String id, Location location) {
        if (hasLocation(id)) throw new IllegalArgumentException("Location already exists with id " + id);
        LOCATIONS.put(id, location);
    }

    public static void removeLocation(String id) {
        LOCATIONS.remove(id);
    }

    public static Map<String, Color> getColors() {
        return Collections.unmodifiableMap(COLORS);
    }

    public static boolean hasColor(String id) {
        return COLORS.containsKey(id);
    }

    public static Color getColor(String id) {
        return COLORS.get(id);
    }

    public static void addColor(String id, Color color) {
        if (hasColor(id)) throw new IllegalArgumentException("Color already exists with id " + id);
        COLORS.put(id, color);
    }

    public static void removeColor(String id) {
        COLORS.remove(id);
    }

    public static void execImport(String fileName) {
        debug("花火のプログラムをインポートしています...", false);
        long start = System.nanoTime();
        try {
            File file = new File(fileName);
            if (file.exists()) {
                Program useProgram = null;

                boolean parsingScene = false;
                Map<String, String> locationMap = new HashMap<>();
                String prevScene = null;
                String currentScene = null;
                long baseTick = 0;

                for (String line : Files.readAllLines(file.toPath())) {
                    debug("行を処理中: " + line, true);
                    if (line.isEmpty() || line.isBlank() || line.startsWith("#")) continue;

                    if (!parsingScene) {
                        String[] command = line.split(" ");

                        if (command[0].equalsIgnoreCase("CREATE")) {
                            if (command[1].equalsIgnoreCase("PROGRAM")) {
                                String[] args = new String[command.length - 2];
                                System.arraycopy(command, 2, args, 0, command.length - 2);
                                String programId = args[0];
                                if (ProgrammedFireworks.hasProgram(programId)) throw new IllegalArgumentException("Program already exists with id " + programId);
                                ProgrammedFireworks.addProgram(programId, new Program(programId, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
                                continue;
                            }
                        }

                        if (command[0].equalsIgnoreCase("USE")) {
                            if (command[1].equalsIgnoreCase("PROGRAM")) {
                                String[] args = new String[command.length - 2];
                                System.arraycopy(command, 2, args, 0, command.length - 2);

                                String programId = args[0];
                                if (!ProgrammedFireworks.hasProgram(programId)) throw new IllegalArgumentException("Program does not exist with id " + programId);
                                useProgram = ProgrammedFireworks.getProgram(args[0]);
                                continue;
                            }
                        }

                        if (command[0].equalsIgnoreCase("DEFINE")) {
                            if (command[1].equalsIgnoreCase("COLOR")) {
                                String[] args = new String[command.length - 2];
                                System.arraycopy(command, 2, args, 0, command.length - 2);

                                switch (args[0]) {
                                    case "GLOBALLY", "GLOBAL" -> {
                                        String colorId = args[1];
                                        Color color = Color.fromRGB(Integer.parseInt(args[2]));
                                        if (ProgrammedFireworks.hasColor(colorId))
                                            throw new IllegalArgumentException("Color already exists with id " + colorId);
                                        ProgrammedFireworks.addColor(colorId, color);
                                    }

                                    case "PROGRAM" -> {
                                        String programId = args[1];
                                        if (!ProgrammedFireworks.hasProgram(programId)) throw new IllegalArgumentException("Program does not exist with id " + programId);
                                        String colorId = args[2];
                                        Color color = Color.fromRGB(Integer.parseInt(args[3]));
                                        ProgrammedFireworks.getProgram(programId).addColor(colorId, color);
                                    }

                                    default -> {
                                        if (useProgram != null) {
                                            String colorId = args[0];
                                            Color color = Color.fromRGB(Integer.parseInt(args[1]));
                                            useProgram.addColor(colorId, color);
                                        } else {
                                            throw new IllegalArgumentException("No program is currently being used. Use \"USE PROGRAM\" to use a program.");
                                        }
                                    }
                                }

                                continue;
                            }

                            if (command[1].equalsIgnoreCase("LOCATION")) {
                                String[] args = new String[command.length - 2];
                                System.arraycopy(command, 2, args, 0, command.length - 2);

                                switch (args[0]) {
                                    case "GLOBALLY", "GLOBAL" -> {
                                        String locationId = args[1];
                                        Location location = new Location(Bukkit.getWorld(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]));
                                        if (ProgrammedFireworks.hasLocation(locationId))
                                            throw new IllegalArgumentException("Location already exists with id " + locationId);
                                        ProgrammedFireworks.addLocation(locationId, location);
                                    }

                                    case "PROGRAM" -> {
                                        String programId = args[1];
                                        if (!ProgrammedFireworks.hasProgram(programId)) throw new IllegalArgumentException("Program does not exist with id " + programId);
                                        String locationId = args[2];
                                        Location location = new Location(Bukkit.getWorld(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]));
                                        ProgrammedFireworks.getProgram(programId).addLocation(locationId, location);
                                    }

                                    default -> {
                                        if (useProgram != null) {
                                            String locationId = args[0];
                                            Location location = new Location(Bukkit.getWorld(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
                                            useProgram.addLocation(locationId, location);
                                        } else {
                                            throw new IllegalArgumentException("No program is currently being used. Use \"USE PROGRAM\" to use a program.");
                                        }
                                    }
                                }

                                continue;
                            }
                        }

                        if (command[0].equalsIgnoreCase("MAP")) {
                            if (command[1].equalsIgnoreCase("LOCATION")) {
                                String[] args = new String[command.length - 2];
                                System.arraycopy(command, 2, args, 0, command.length - 2);
                                String key = args[0];
                                String value = args[2];
                                locationMap.put(key, value);
                                continue;
                            }
                        }

                        if (command[0].equalsIgnoreCase("START")) {
                            if (command[1].equalsIgnoreCase("SCENE")) {
                                parsingScene = true;
                                continue;
                            }
                        }

                        if (command[0].equalsIgnoreCase("END")) {
                            if (command[1].equalsIgnoreCase("SCENE")) {
                                parsingScene = false;
                                continue;
                            }
                        }
                    } else {
                        boolean sceneSwitched = false;

                        String[] cols = line.split("\\t");

                        String scene = cols[0];
                        if (!scene.equals(currentScene)) {
                            if (prevScene != null) sceneSwitched = true;
                            prevScene = currentScene;
                            currentScene = scene;
                        }

                        if (sceneSwitched) {
                            if (useProgram != null) {
                                long oldBaseTick = baseTick;
                                baseTick = useProgram.getPrograms().keySet().stream().max(Comparator.naturalOrder()).orElse(0L);
                                debug("起点となるtickを " + oldBaseTick + " から " + baseTick + " に変更しました", true);
                            } else {
                                throw new IllegalArgumentException("No program defined.");
                            }
                        }
                        long tick = baseTick + Long.parseLong(cols[1]);

                        int launchYOffset = cols[2].isEmpty() || cols[2].isBlank() ? 0 : Integer.parseInt(cols[2]);
                        boolean randomizeLaunchLocation = cols[3].equals("1");

                        String launchLocationDisplayName = cols[4];
                        String launchLocationId = locationMap.get(launchLocationDisplayName);
                        LocationReference launchLocation = useProgram != null && useProgram.hasLocation(launchLocationId) ? LocationReference.ofProgramDefined(useProgram, launchLocationId) : null;

                        String minecraftCommand = cols[15];
                        String[] minecraftCommandParts = minecraftCommand.split(" ", 16);
                        String rawCompoundTag = minecraftCommandParts[15];
                        CompoundTag compoundTag = TagParser.parseTag(rawCompoundTag);
                        if (useProgram != null) {
                            useProgram.addFirework(tick, Firework.buildObject(compoundTag, useProgram, launchLocation, launchYOffset, randomizeLaunchLocation));
                            debug("プログラムをインポートしました: [" + scene + ", " + tick + ", " + cols[4] + ", " + cols[5] + ", " + cols[6] + "]", true);
                        } else {
                            throw new IllegalArgumentException("No program defined.");
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("File not found: " + fileName);
            }
        } catch(Throwable t) {
            t.printStackTrace();
            debug("エラーが発生しました: " + t.getMessage(), false);
        }
        long end = System.nanoTime();
        long elapsed = end - start;
        debug("処理が終了しました (Elapsed " + elapsed + "ns (" + TimeUnit.NANOSECONDS.toMillis(elapsed) + "ms))", false);
    }

    private static void debug(String message, boolean actionbar) {
        RunnableManager.runAsync(() -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (player.hasPermission("unknown.core.debug") || player.isOp()) {
                    if (actionbar) player.sendActionBar(Component.text(message, DefinedTextColor.GRAY, TextDecoration.ITALIC));
                    else player.sendMessage(Component.text("[DEBUG] " + message, DefinedTextColor.GRAY, TextDecoration.ITALIC));
                }
            });
        });
    }
}
