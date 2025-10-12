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

package net.unknown.shared.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EmbeddedResources {
    private static final Set<JarEntry> ENTRIES = new HashSet<>();

    public static void init(final boolean excludeMetaInf, final boolean excludeClasses) {
        try {
            JarFile jarFile = new JarFile(EmbeddedResources.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            Set<String> IGNORE_DIRECTORIES = new HashSet<>();
            jarFile.entries().asIterator().forEachRemaining(e -> {
                if (e.isDirectory() && IGNORE_DIRECTORIES.contains(e.getName())) {
                    return;
                }

                if (excludeMetaInf && e.getName().startsWith("META-INF")) return;
                if (excludeClasses && e.getName().endsWith(".class")) {
                    String[] path = e.getName().split("/");
                    String[] dirPaths = new String[path.length - 1];
                    System.arraycopy(path, 0, dirPaths, 0, path.length - 1);
                    for (int i = 0; i < dirPaths.length; i++) {
                        String dirPath = String.join("/", Arrays.copyOfRange(path, 0, i + 1)) + "/";
                        if (!IGNORE_DIRECTORIES.contains(dirPath)) {
                            IGNORE_DIRECTORIES.add(dirPath);
                        }
                    }
                    return;
                }
                ENTRIES.add(e);
            });
            ENTRIES.removeIf(e -> e.isDirectory() && IGNORE_DIRECTORIES.contains(e.getName()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load", e);
        }
    }

    public static Set<URL> listResources(String path) {
        if (path.startsWith(".")) path = path.replaceFirst("^\\.", "");
        if (path.startsWith("/")) path = path.replaceFirst("^/", "");

        final String finalPath = path;
        Set<URL> resources = new HashSet<>();
        ENTRIES.stream().filter(e -> e.getName().startsWith(finalPath)).forEach(e -> {
            try {
                resources.add(new URL("jar:file:" + EmbeddedResources.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "!/" + e.getName()));
            } catch (IOException ex) {
                throw new RuntimeException("Failed to load resource: " + e.getName(), ex);
            }
        });
        return resources;
    }

    public static URL getResource(String path) throws FileNotFoundException {
        if (path.startsWith(".")) path = path.replaceFirst("^\\.", "");
        if (path.startsWith("/")) path = path.replaceFirst("^/", "");

        final String finalPath = path;
        return ENTRIES.stream().filter(e -> e.getName().equals(finalPath)).findFirst().map(e -> {
            try {
                return new URL("jar:file:" + EmbeddedResources.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "!/" + e.getName());
            } catch (IOException ex) {
                throw new RuntimeException("Failed to load resource: " + e.getName(), ex);
            }
        }).orElseThrow(() -> new FileNotFoundException("Resource not found: " + finalPath));
    }
}