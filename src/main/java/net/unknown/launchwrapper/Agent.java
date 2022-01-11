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

package net.unknown.launchwrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Agent {
    private static Instrumentation INSTRUMENT = null;

    public static void addTransformer(ClassFileTransformer transformer) {
        if (INSTRUMENT != null) INSTRUMENT.addTransformer(transformer);
    }

    public static void addJar(Path path) throws IOException {
        addJar(path.toFile());
    }

    public static void addJar(File jarFile) throws IOException {
        if(!jarFile.exists()) throw new FileNotFoundException(jarFile.getAbsolutePath());
        if(jarFile.isDirectory() || !jarFile.getName().endsWith(".jar")) throw new IOException(jarFile.getName() + " is not a JarFile");
        if(INSTRUMENT != null) {
            INSTRUMENT.appendToSystemClassLoaderSearch(new JarFile(jarFile));
            return;
        }
        throw new IllegalStateException("Failed to inject " + jarFile.getName() + " to SystemClassPath");
    }

    public static void premain(String args, Instrumentation instrumentation) {
        agentmain(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        if (INSTRUMENT == null) INSTRUMENT = instrumentation;
        if (INSTRUMENT == null) throw new NullPointerException("WHY JAPANESE PEOPLE");
    }

    public static void updateSecurity() {
        Set<Module> unnamedModules = Set.of(ClassLoader.getSystemClassLoader().getUnnamedModule());
        INSTRUMENT.redefineModule(Manifest.class.getModule(), Set.of(), Map.of("sun.security.util", unnamedModules), Map.of("java.util.jar", unnamedModules), Set.of(), Map.of()); // modlauncher
    }
}