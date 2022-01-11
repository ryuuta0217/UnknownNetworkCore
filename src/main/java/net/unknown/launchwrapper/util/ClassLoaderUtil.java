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

package net.unknown.launchwrapper.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;

public class ClassLoaderUtil {
    public static URL[] getSystemClassPathURLs() {
        ClassLoader cl = ClassLoaderUtil.class.getClassLoader();
        if(cl instanceof URLClassLoader ucl) return ucl.getURLs();

        if(cl.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
            try {
                Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                if(theUnsafeField.trySetAccessible()) {
                    Unsafe unSafe = (Unsafe) theUnsafeField.get(null);

                    // URLClassLoaderは使われなくなったが内部的にURLClassPathは使われているのでそれを入手する
                    Field urlClassPathField;
                    if(Arrays.stream(cl.getClass().getFields()).anyMatch(f -> f.getName().equals("ucp"))) {
                        urlClassPathField = cl.getClass().getDeclaredField("ucp");
                    } else {
                        urlClassPathField = cl.getClass().getSuperclass().getDeclaredField("ucp");
                    }

                    long ucpFieldOffset = unSafe.objectFieldOffset(urlClassPathField);
                    Object urlClassPath = unSafe.getObject(cl, ucpFieldOffset);

                    // URLClassPath ArrayList<URL> path; (URLClassPath)
                    Field pathField = urlClassPathField.getType().getDeclaredField("path");
                    long pathFieldOffset = unSafe.objectFieldOffset(pathField);
                    ArrayList<URL> path = (ArrayList<URL>) unSafe.getObject(urlClassPath, pathFieldOffset);

                    return path.toArray(new URL[0]);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to get SystemClassPath URLs", e);
            }
        }
        throw new RuntimeException("Incompatible");
    }
}
