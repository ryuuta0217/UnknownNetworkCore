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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;

public class ReflectionUtil {
    public static void makeNonFinal(Field targetField) {
        try {
            /*Field theUnsafeF = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeF.trySetAccessible();
            Unsafe unsafe = (Unsafe) theUnsafeF.get(null);
            long offSet = unsafe.objectFieldOffset(targetField);
            unsafe.getBoolean()*/

            Method internalMethod = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            //Method internalMethod = Stream.of(java.lang.Class.class.getDeclaredMethods()).filter(m -> m.getName().equalsIgnoreCase("getDeclaredFields0")).toList().get(0);
            if (!internalMethod.trySetAccessible()) {
                throw new RuntimeException("Failed to #trySetAccessible");
            }
            Field[] fields = (Field[]) internalMethod.invoke(targetField, false);
            List<Field> filteredFields = Stream.of(fields).filter(f -> f.getName().equals("modifiers")).toList();
            if (filteredFields.size() != 1) {
                throw new RuntimeException("Cannot Find Field: modifiers");
            }
            Field modifiersField = filteredFields.get(0);
            modifiersField.trySetAccessible();

            int modifiers = targetField.getModifiers();
            if (Modifier.isFinal(modifiers)) {
                modifiersField.set(targetField, modifiers & ~Modifier.FINAL);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
