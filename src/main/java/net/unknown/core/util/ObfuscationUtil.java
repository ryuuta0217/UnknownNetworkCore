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

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.util.ObfHelper;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObfuscationUtil {
    public static final ClassMapping OBF_STATE;
    public static final Pattern DESCRIPTOR_PARAMS_PATTERN = Pattern.compile("\\[*(L[^;]+;|[ZBCSIFDJ])");
    // (Ljava/util/function/Function;Ljava/util/function/Predicate;)Ljava/lang/Object;
    public static final Pattern DESCRIPTOR_PATTERN = Pattern.compile("^\\((.*)\\)(.*)$");
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/ObfuscationUtil");
    private static final Pattern CLASS_OBF_PATTERN = Pattern.compile("^([a-zA-Z_$0-9.]+) -> ([a-z$]+):$");
    private static final Pattern FIELD_OBF_PATTERN = Pattern.compile("^    ([a-zA-Z_$0-9.\\[\\]]+) ([a-zA-Z_0-9$]+) -> ([a-zA-Z_]+)$");
    private static final Pattern METHOD_OBF_PATTERN = Pattern.compile("^    (\\d+:\\d+):([a-zA-Z_$0-9.]+) ([a-zA-Z_0-9]+)(\\(.*\\)) -> ([a-z]+)$");
    private static final Map<String, Class> CLASSES = new HashMap<>();

    static {
        ClassMapping state;
        try {
            java.lang.Class.forName("net.minecraft.server.Bootstrap");
            state = ClassMapping.MOJANG;
        } catch (ClassNotFoundException ignored) {
            try {
                java.lang.Class.forName("net.minecraft.server.DispenserRegistry");
                state = ClassMapping.SPIGOT;
            } catch (ClassNotFoundException ignored2) {
                state = ClassMapping.UNKNOWN;
            }
        }
        OBF_STATE = state;
    }

    public static void loadAllMappings() {
        loadMojangMappings();
        loadSpigotMappings();
        loadTinyMappings();
    }

    public static void loadMojangMappings() {
        LOGGER.info("Loading mojang mappings...");
        CLASSES.clear();
        try {
            List<String> mojangMapping = new BufferedReader(new InputStreamReader(ObfuscationUtil.class.getClassLoader().getResourceAsStream("mapping-mojang.txt"), StandardCharsets.UTF_8)).lines().toList();

            Class currentClass = null;

            for (String line : mojangMapping) {
                Matcher classMatcher = CLASS_OBF_PATTERN.matcher(line);
                if (classMatcher.matches()) {
                    String originalClass = classMatcher.group(1);
                    boolean subClass = originalClass.contains("$");
                    String obfuscatedClass = classMatcher.group(2);

                    if (subClass) {
                        String[] classes = originalClass.split("\\$");
                        String[] obfClasses = obfuscatedClass.split("\\$");
                        Class parent = CLASSES.getOrDefault(classes[0], null);
                        if (parent != null) {
                            for (int i = 1; i < classes.length; i++) {
                                String subClassStr = classes[i];
                                String obfSubClassStr = obfClasses[i];
                                SubClass child = parent.getSubClass(subClassStr, obfSubClassStr);
                                if (child == null) {
                                    SubClass newChild = new SubClass(parent, subClassStr, obfSubClassStr);
                                    parent.addSubClass(newChild);
                                    parent = newChild;
                                } else {
                                    parent = child;
                                }
                            }
                            currentClass = parent;
                            continue;
                        }
                    }

                    currentClass = new Class(originalClass, obfuscatedClass);
                    CLASSES.put(originalClass, currentClass);
                    continue;
                }

                Matcher fieldMatcher = FIELD_OBF_PATTERN.matcher(line);
                if (fieldMatcher.matches() && currentClass != null) {
                    currentClass.addField(new Field(
                            currentClass,
                            fieldMatcher.group(1),
                            fieldMatcher.group(2),
                            fieldMatcher.group(3)));
                    continue;
                }

                Matcher methodMatcher = METHOD_OBF_PATTERN.matcher(line);
                if (methodMatcher.matches() && currentClass != null) {
                    currentClass.addMethod(new Method(
                            currentClass,
                            methodMatcher.group(2),
                            methodMatcher.group(3),
                            methodMatcher.group(4).replaceAll("[()]", "").split(","),
                            methodMatcher.group(5)));
                }
            }
            LOGGER.info("Mojang mappings was successfully loaded. Classes: " + CLASSES.size());
        } catch (Throwable e) {
            LOGGER.error("Failed to load mojang mappings", e);
        }
    }

    public static void loadSpigotMappings() {
        LOGGER.info("Loading spigot mappings...");
        try {
            List<String> spigotMapping = new BufferedReader(new InputStreamReader(ObfuscationUtil.class.getClassLoader().getResourceAsStream("mapping-spigot.txt"), StandardCharsets.UTF_8)).lines().toList();

            for (String line : spigotMapping) {
                if (line.startsWith("#")) continue;
                String[] spl = line.split(" ", 2);
                String obfuscatedClass = spl[0];
                String spigotClass = spl[1].replace("/", ".");

                String[] obfSplitClass = obfuscatedClass.split("\\$");
                String[] spigotSplitClass = spigotClass.split("\\$");

                Class parent = getClassByObfuscatedName(obfSplitClass[0]);
                if (parent != null) {
                    parent.setSpigotName(spigotSplitClass[0]);
                    for (int i = 1; i < obfSplitClass.length; i++) {
                        Class child = parent.getSubClassByObfuscatedName(obfSplitClass[i]);
                        if (child != null) {
                            child.setSpigotName(spigotSplitClass[i]);
                            parent = child;
                        }
                    }
                }
            }
            LOGGER.info("Spigot mappings was successfully loaded.");
        } catch (Throwable e) {
            LOGGER.error("Failed to load spigot mappings", e);
        }
    }

    public static void loadTinyMappings() {
        //CLASSES.clear();
        LOGGER.info("Loading tiny mappings...");
        Map<String, Class> tinyClasses = new HashMap<>();
        try (InputStream mappingRaw = ObfHelper.class.getClassLoader().getResourceAsStream("META-INF/mappings/reobf.tiny")) {
            if (mappingRaw == null) {
                LOGGER.warn("Failed to load tiny mapping.");
                return;
            }

            MemoryMappingTree mapping = new MemoryMappingTree();
            MappingReader.read(new InputStreamReader(mappingRaw, StandardCharsets.UTF_8), MappingFormat.TINY_2, mapping);
            for (MappingTree.ClassMapping classMapping : mapping.getClasses()) {
                String mojangClassName = classMapping.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE).replace("/", ".");
                String spigotClassName = classMapping.getName(ObfHelper.SPIGOT_NAMESPACE).replace("/", ".");

                boolean isSubClass = classMapping.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE).contains("$");

                Class clazz;

                if (!isSubClass) {
                    clazz = new Class(mojangClassName, spigotClassName);
                    //clazz = tinyClasses.getOrDefault(mojangClassName, new Class(mojangClassName, spigotClassName));
                } else {
                    String[] mojangNames = mojangClassName.split("\\$");
                    String[] spigotNames = spigotClassName.split("\\$");
                    /*if(!tinyClasses.containsKey(mojangNames[0])) {
                        tinyClasses.put(mojangNames[0], new Class(mojangNames[0], spigotNames[0]))
                    }*/

                    Class parent = tinyClasses.get(mojangNames[0]);
                    for (int i = 1; i < mojangNames.length; i++) {
                        SubClass subClazz = parent.getSubClass(mojangNames[i], spigotNames[i]);
                        if (subClazz == null) {
                            subClazz = new SubClass(parent, mojangNames[i], spigotNames[i]);
                            parent.addSubClass(subClazz);
                        }
                        parent = subClazz;
                    }

                    clazz = parent;
                }

                for (MappingTree.MethodMapping methodMapping : classMapping.getMethods()) {
                    String descriptor = methodMapping.getDesc(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE);
                    Matcher descMatcher = DESCRIPTOR_PATTERN.matcher(descriptor);
                    if (descMatcher.matches()) {
                        String argsRaw = descMatcher.group(1);
                        String returnTypeRaw = descMatcher.group(2);

                        Matcher argsMatcher = DESCRIPTOR_PARAMS_PATTERN.matcher(argsRaw);
                        List<String> params = new ArrayList<>();
                        while (argsMatcher.find()) {
                            params.add(argsMatcher.group());
                        }
                        clazz.addMethod(new Method(
                                clazz,
                                ObfuscationUtil.formatTypeDescriptor(returnTypeRaw),
                                methodMapping.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE),
                                params.stream()
                                        .map(ObfuscationUtil::formatTypeDescriptor)
                                        .toArray(String[]::new),
                                methodMapping.getName(ObfHelper.SPIGOT_NAMESPACE)));
                    }
                }

                for (MappingTree.FieldMapping fieldMapping : classMapping.getFields()) {
                    clazz.addField(new Field(
                            clazz,
                            ObfuscationUtil.formatTypeDescriptor(fieldMapping.getDesc(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE)),
                            fieldMapping.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE),
                            fieldMapping.getName(ObfHelper.SPIGOT_NAMESPACE)));
                }

                if (!isSubClass) tinyClasses.put(clazz.getName(), clazz);

            }

            tinyClasses.entrySet().stream()
                    .filter(e -> !CLASSES.containsKey(e.getKey()))
                    .forEach(e -> {
                        System.out.println("Unknown class detected: " + e.getKey());
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to load tiny mappings", e);
        }
    }

    public static Map<String, Class> getMapping() {
        return CLASSES;
    }

    public static Class getClassByMojangName(String mojangClassName) {
        String[] classes = mojangClassName.split("\\$");
        if (CLASSES.containsKey(classes[0])) {
            Class clazz = CLASSES.get(classes[0]);
            for (int i = 1; i < classes.length; i++) {
                clazz = clazz.getSubClassByMojangName(classes[i]);
            }
            return clazz;
        }
        return null;
    }

    public static Class getClassByObfuscatedName(String obfuscatedClassName) {
        String[] classes = obfuscatedClassName.split("\\$");
        Class parent = CLASSES.values()
                .stream()
                .filter(clazz -> clazz.getObfuscatedName().equals(classes[0]))
                .findAny()
                .orElse(null);

        if (parent != null) {
            for (int i = 1; i < classes.length; i++) {
                Class subClass = parent.getSubClassByObfuscatedName(classes[i]);
                if (subClass != null) parent = subClass;
                else return null;
            }
            return parent;
        }
        return null;
    }

    /**
     * format type descriptor to pattern "[Lsome.package.ClassName;", "some.package.ClassName"
     * for primitive: I -> int | Z -> boolean
     *
     * @param input as known Lsome/package/ClassName, IFFZ format
     * @return formatted pattern
     */
    private static String formatTypeDescriptor(String input) {
        StringReader reader = new StringReader(input);
        boolean isArray = false;
        StringBuilder result = new StringBuilder();
        while (reader.canRead()) {
            switch (reader.read()) {
                case '[' -> {
                    result.append("[");
                    isArray = true;
                }
                case 'L' -> {
                    String val;
                    try {
                        val = reader.readStringUntil(';').replace("/", ".");
                    } catch (CommandSyntaxException e) {
                        break;
                    }
                    if (!isArray) result = new StringBuilder(val.replace(";", ""));
                    else result.append("L").append(val);
                }
                case 'B' -> {
                    if (!isArray) result = new StringBuilder("byte");
                    else result.append("B");
                }
                case 'C' -> {
                    if (!isArray) result = new StringBuilder("char");
                    else result.append("C");
                }
                case 'D' -> {
                    if (!isArray) result = new StringBuilder("double");
                    else result.append("D");
                }
                case 'F' -> {
                    if (!isArray) result = new StringBuilder("float");
                    else result.append("F");
                }
                case 'I' -> {
                    if (!isArray) result = new StringBuilder("int");
                    else result.append("I");
                }
                case 'J' -> {
                    if (!isArray) result = new StringBuilder("long");
                    else result.append("J");
                }
                case 'S' -> {
                    if (!isArray) result = new StringBuilder("short");
                    else result.append("S");
                }
                case 'Z' -> {
                    if (!isArray) result = new StringBuilder("boolean");
                    else result.append("Z");
                }
                case 'V' -> {
                    if (!isArray) result = new StringBuilder("void");
                    else result.append("V");
                }
            }
        }

        return result.toString();
    }

    private static String obfuscateTypeDescriptor(String typeDescriptor) {
        boolean isArray = false;
        String array = "";
        if (typeDescriptor.matches("^\\[+L.*;")) {
            isArray = true;
            array = typeDescriptor.substring(0, typeDescriptor.indexOf("L"));
            typeDescriptor = typeDescriptor.substring(typeDescriptor.indexOf("L") + 1, typeDescriptor.indexOf(";") - 1);
        }
        Class clazz = ObfuscationUtil.getClassByMojangName(typeDescriptor);
        if (clazz != null) {
            return (isArray ? array : "") + clazz.getObfuscatedName() + (isArray ? ";" : "");
        }
        return typeDescriptor;
    }

    private static java.lang.Class<?> getClassOrPrimitive(String className) throws ClassNotFoundException {
        return switch (className) {
            case "int" -> int.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "long" -> long.class;
            case "short" -> short.class;
            case "boolean" -> boolean.class;
            case "void" -> void.class;
            default -> java.lang.Class.forName(className);
        };
    }

    public enum ClassMapping {
        SPIGOT,
        MOJANG,
        UNKNOWN
    }

    public static class Class {
        private final String name;
        private final String obfuscatedName;
        private final Set<Field> fields = new HashSet<>();
        private final Set<Method> methods = new HashSet<>();
        private final Set<SubClass> subClasses = new HashSet<>();
        private String spigotName = null;

        public Class(String name, String obfuscatedName) {
            this.name = name;
            this.obfuscatedName = obfuscatedName;
        }

        public String getName() {
            return this.name;
        }

        public String getObfuscatedName() {
            return this.obfuscatedName;
        }

        public String getSpigotName() {
            if (this.spigotName == null) return this.obfuscatedName;
            return this.spigotName;
        }

        private void setSpigotName(String spigotName) {
            this.spigotName = spigotName;
        }

        public String getEffectiveClassName() {
            if (OBF_STATE == ClassMapping.SPIGOT) {
                if (this.hasSpigotName()) return this.getSpigotName();
                else return this.getName();
            }

            return this.getName();
        }

        public boolean hasSpigotName() {
            return this.spigotName != null;
        }

        public Set<SubClass> getSubClasses() {
            return this.subClasses;
        }

        public SubClass getSubClassByMojangName(String mojangSubClassName) {
            return subClasses.stream().filter(subClass -> subClass.getOriginalName().equals(mojangSubClassName)).findFirst().orElse(null);
        }

        public SubClass getSubClassByObfuscatedName(String obfuscatedSubClassName) {
            return subClasses.stream().filter(subClass -> subClass.getOriginalSpigotName().equals(obfuscatedSubClassName)).findFirst().orElse(null);
        }

        public SubClass getSubClass(String mojangSubClassName, String obfuscatedSubClassName) {
            return subClasses.stream()
                    .filter(subClass -> subClass.getOriginalName().equals(mojangSubClassName) && subClass.getOriginalObfuscatedName().equals(obfuscatedSubClassName))
                    .findFirst().orElse(null);
        }

        public Field getFieldByMojangName(String mojangFieldName) {
            return fields.stream().filter(field -> field.getName().equals(mojangFieldName)).findFirst().orElse(null);
        }

        public Field getFieldByObfuscatedName(String obfuscatedFieldName) {
            return fields.stream().filter(field -> field.getObfuscatedName().equals(obfuscatedFieldName)).findFirst().orElse(null);
        }

        public Field getField(String mojangFieldName, String obfuscatedFieldName) {
            return fields.stream()
                    .filter(field -> field.getName().equals(mojangFieldName) && field.getObfuscatedName().equals(obfuscatedFieldName))
                    .findFirst().orElse(null);
        }

        public Set<Field> getFields() {
            return this.fields;
        }

        public Method getMethodByMojangName(String mojangMethodName) {
            return methods.stream().filter(method -> method.getName().equals(mojangMethodName)).findFirst().orElse(null);
        }

        public Method getMethodByObfuscatedName(String obfuscatedMethodName) {
            return methods.stream().filter(method -> method.getObfuscatedName().equals(obfuscatedMethodName)).findFirst().orElse(null);
        }

        public Method getMethod(String mojangMethodName, String obfuscatedMethodName) {
            return methods.stream()
                    .filter(method -> method.getName().equals(mojangMethodName) && method.getObfuscatedName().equals(obfuscatedMethodName))
                    .findFirst().orElse(null);
        }

        public Set<Method> getMethods() {
            return this.methods;
        }

        @Nullable
        public java.lang.Class<?> asClass() {
            try {
                return java.lang.Class.forName(this.getName());
            } catch (ClassNotFoundException ignored) {
                try {
                    return java.lang.Class.forName(this.getObfuscatedName());
                } catch (ClassNotFoundException ignored2) {
                    if (this.hasSpigotName()) {
                        try {
                            return java.lang.Class.forName(this.getSpigotName());
                        } catch (ClassNotFoundException ignored3) {
                            return null;
                        }
                    }
                    return null;
                }
            }
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        private void addField(Field field) {
            this.fields.add(field);
        }

        private void addMethod(Method method) {
            this.methods.add(method);
        }

        private void addSubClass(SubClass subClass) {
            this.subClasses.add(subClass);
        }
    }

    public static class SubClass extends Class {
        private final Class parent;

        public SubClass(Class parent, String name, String obfuscatedName) {
            super(name, obfuscatedName);
            this.parent = parent;
        }

        public String getOriginalName() {
            return super.getName();
        }

        @Override
        public String getName() {
            return this.parent.getName() + "$" + super.getName();
        }

        public String getOriginalObfuscatedName() {
            return super.getObfuscatedName();
        }

        @Override
        public String getObfuscatedName() {
            return this.parent.getObfuscatedName() + "$" + super.getObfuscatedName();
        }

        public String getOriginalSpigotName() {
            return super.getSpigotName();
        }

        @Override
        public String getSpigotName() {
            return this.parent.getSpigotName() + "$" + super.getSpigotName();
        }

        public Class getParent() {
            if (this.parent instanceof SubClass subClass) {
                return subClass.getParent();
            } else {
                return this.parent;
            }
        }
    }

    public static class Method {
        private final Class parent;
        private final String returnType;
        private final String mojangMethodName;
        private final String[] argTypes;
        private final String obfuscatedMethodName;
        private String[] obfuscatedArgTypes;

        public Method(Class parent, String returnType, String mojangMethodName, String[] argTypes, String obfuscatedMethodName) {
            this.parent = parent;
            this.returnType = returnType;
            this.mojangMethodName = mojangMethodName;
            this.argTypes = argTypes;
            this.obfuscatedMethodName = obfuscatedMethodName;
        }

        public Class getParent() {
            return this.parent;
        }

        public String getReturnType() {
            return this.returnType;
        }

        public String getObfuscatedReturnType() {
            return ObfuscationUtil.obfuscateTypeDescriptor(this.returnType);
        }

        public String getName() {
            return this.mojangMethodName;
        }

        public String getObfuscatedName() {
            return this.obfuscatedMethodName;
        }

        public String[] getArgTypes() {
            return this.argTypes;
        }

        public String[] getObfuscatedArgTypes() {
            if (this.obfuscatedArgTypes == null) {
                this.obfuscatedArgTypes = Arrays.stream(this.argTypes)
                        .map(ObfuscationUtil::obfuscateTypeDescriptor)
                        .toArray(String[]::new);
            }

            return this.obfuscatedArgTypes;
        }

        public String[] getEffectiveArgTypes() {
            if (OBF_STATE == ClassMapping.MOJANG) return this.getArgTypes();
            else return this.getObfuscatedArgTypes();
        }

        public String getEffectiveName() {
            if (OBF_STATE == ClassMapping.MOJANG) return this.getName();
            else return this.getObfuscatedName();
        }

        public java.lang.reflect.Method getMethod() throws NoSuchMethodException {
            // Input examples: int, float, [I, [[D, [Lnet.minecraft.core.Class;, net.minecraft.core.Class2
            java.lang.Class<?>[] argTypeClasses = Arrays.stream(this.getEffectiveArgTypes())
                    .map(className -> {
                        try {
                            return ObfuscationUtil.getClassOrPrimitive(className);
                        } catch (ClassNotFoundException e) {
                            return null;
                        }
                    }).toArray(java.lang.Class[]::new);
            return this.getParent().asClass().getMethod(this.getEffectiveName(), argTypeClasses);
        }
    }

    public static class Field {
        private final Class parent;
        private final String type;
        private final String mojangFieldName;
        private final String obfuscatedFieldName;

        public Field(Class parent, String type, String mojangFieldName, String obfuscatedFieldName) {
            this.parent = parent;
            this.type = type;
            this.mojangFieldName = mojangFieldName;
            this.obfuscatedFieldName = obfuscatedFieldName;
        }

        public Class getParent() {
            return this.parent;
        }

        public String getType() {
            return this.type;
        }

        public String getObfuscatedType() {
            return ObfuscationUtil.obfuscateTypeDescriptor(this.type);
        }

        public String getName() {
            return this.mojangFieldName;
        }

        public String getObfuscatedName() {
            return this.obfuscatedFieldName;
        }

        public String getEffectiveName() {
            if (OBF_STATE == ClassMapping.MOJANG) return this.getName();
            else return this.getObfuscatedName();
        }

        public java.lang.reflect.Field getField() throws NoSuchFieldException {
            return this.getParent().asClass().getDeclaredField(this.getEffectiveName());
        }
    }
}
