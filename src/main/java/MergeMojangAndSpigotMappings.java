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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MergeMojangAndSpigotMappings {
    private static final Pattern CLASS_OBF_PATTERN = Pattern.compile("^([a-zA-Z_\\$0-9\\.]+) -> ([a-z\\$]+):$");
    private static final Pattern FIELD_OBF_PATTERN = Pattern.compile("^    ([a-zA-Z_\\$0-9\\.]+) ([a-zA-Z_0-9]+) -> ([a-z]+)$");
    private static final Pattern METHOD_OBF_PATTERN = Pattern.compile("^    (\\d+:\\d+):([a-zA-Z_\\$0-9\\.]+) ([a-zA-Z_0-9]+)(\\(.*\\)) -> ([a-z]+)$");

    public static void main(String[] args) throws IOException {
        Map<String, String> mojang2obf = new LinkedHashMap<>();
        Map<String, String> obf2mojang = new LinkedHashMap<>();

        Map<String, List<FieldInfo>> mojang2fields = new LinkedHashMap<>();
        Map<String, List<MethodInfo>> mojang2methods = new LinkedHashMap<>();

        System.out.println("Loading Mojang Mapping");
        List<String> mojangMapping = Files.readAllLines(Path.of("1.19.4-server-mojang.txt"));

        String currentClass = "";

        for (String line : mojangMapping) {
            Matcher classMatcher = CLASS_OBF_PATTERN.matcher(line);
            if (classMatcher.matches()) {
                String originalClass = classMatcher.group(1);
                String obfuscatedClass = classMatcher.group(2);
                mojang2obf.put(originalClass, obfuscatedClass);
                obf2mojang.put(obfuscatedClass, originalClass);

                currentClass = originalClass;
                continue;
            }

            Matcher fieldMatcher = FIELD_OBF_PATTERN.matcher(line);
            if (fieldMatcher.matches()) {
                if (!mojang2fields.containsKey(currentClass)) mojang2fields.put(currentClass, new LinkedList<>());
                mojang2fields.get(currentClass)
                        .add(new FieldInfo(
                                fieldMatcher.group(1),
                                fieldMatcher.group(2),
                                fieldMatcher.group(3)
                        ));
                continue;
            }

            Matcher methodMatcher = METHOD_OBF_PATTERN.matcher(line);
            if (methodMatcher.matches()) {
                if (!mojang2methods.containsKey(currentClass)) mojang2methods.put(currentClass, new LinkedList<>());
                mojang2methods.get(currentClass)
                        .add(new MethodInfo(
                                methodMatcher.group(2),
                                methodMatcher.group(3),
                                methodMatcher.group(4).replaceAll("(\\(|\\))", "").split(","),
                                methodMatcher.group(5)
                        ));
            }
        }

        System.out.println("Mojang Mapping Loaded");

        System.out.println("Loading Spigot Mapping");
        List<String> spigotMapping = Files.readAllLines(Path.of("1.19.4-server-spigot.txt"));

        Map<String, String> obf2spigot = new LinkedHashMap<>();
        Map<String, String> mojang2spigot = new LinkedHashMap<>();
        spigotMapping.forEach(line -> {
            if (line.startsWith("#")) return;

            String[] spl = line.split(" ", 2);
            String obfuscatedClass = spl[0];
            String spigotClass = spl[1].replace("/", ".");
            obf2spigot.put(obfuscatedClass, spigotClass);
            mojang2spigot.put(obf2mojang.get(obfuscatedClass), spigotClass);
        });
        System.out.println("Spigot Mapping Loaded");

        System.out.println("Fixing Sub Classes Mapping");
        /* Not de-obfuscated Sub-Classes
         * ClassExample -> a -> ExampleClass
         * ClassExample$SubClass -> a$a -> a$a <- FIX THIS!
         */
        mojang2obf.forEach((mojang, obf) -> {
            if (obf.contains("$") && !obf2spigot.containsKey(obf)) {
                String[] spl = obf.split("\\$", 2);
                String parentClass = spl[0];
                if (obf2spigot.containsKey(parentClass)) {
                    obf2spigot.put(obf, obf2spigot.get(parentClass) + "$" + spl[1]);
                }
            }
        });
        System.out.println("Sub Classes Fixed");

        System.out.println("Fixing \"Used Mojang Mapping\"");
        /* Not de-obfuscated Classes (Spigot used Mojang mapping!) */
        mojang2obf.forEach((mojang, obf) -> {
            if (!obf2spigot.containsKey(obf)) {
                if (mojang.contains("$") && obf.contains("$")) {
                    String[] mspl = mojang.split("\\$", 2);
                    String[] ospl = obf.split("\\$", 2);
                    obf2spigot.put(obf, mspl[0] + "$" + ospl[1]);
                } else {
                    obf2spigot.put(obf, mojang);
                }
            }
        });
        System.out.println("Fixed");

        System.out.println("Writing to file");
        File writeTarget = new File("1.19.4-mojang-spigot.new.txt");
        if (writeTarget.exists() || !writeTarget.exists() && writeTarget.createNewFile()) {
            FileWriter fWriter = new FileWriter(writeTarget, false);
            PrintWriter pWriter = new PrintWriter(new BufferedWriter(fWriter));
            mojang2obf.forEach((mojang, obf) -> {
                pWriter.println(mojang + " -> " + obf + " -> " + obf2spigot.getOrDefault(obf, "UNKNOWN"));

                mojang2fields.getOrDefault(mojang, Collections.emptyList()).forEach(field -> {
                    String type = field.type();
                    String originalName = field.name();
                    String obfuscatedName = field.obfuscatedName();
                    String spigotType = mojang2spigot.getOrDefault(type, type);

                    pWriter.println("    F:" + spigotType + " " + originalName + " -> " + obfuscatedName);
                });

                mojang2methods.getOrDefault(mojang, Collections.emptyList()).forEach(method -> {
                    String returnType = method.returnType();
                    String methodName = method.methodName();
                    String obfuscatedMethodName = method.obfuscatedMethodName();
                    String[] methodArgTypes = method.methodArgs();
                    String spigotReturnType = mojang2spigot.getOrDefault(returnType, returnType);
                    String[] spigotMethodArgTypes = Arrays.stream(methodArgTypes)
                            .map(type -> mojang2spigot.getOrDefault(type, type))
                            .toArray(String[]::new);

                    pWriter.println("    M:" + spigotReturnType + " " + methodName + "(" + String.join(",", spigotMethodArgTypes) + ") -> " + obfuscatedMethodName);
                });
            });
            pWriter.close();
        }
        System.out.println("Complete");
    }

    public record FieldInfo(String type, String name, String obfuscatedName) {
    }

    public record MethodInfo(String returnType, String methodName, String[] methodArgs, String obfuscatedMethodName) {
    }
}