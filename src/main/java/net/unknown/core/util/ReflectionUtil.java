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

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionUtil {
    /*private static final VarHandle MOD_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
            MOD_HANDLE = lookup.findVarHandle(Field.class, "modifiers", int.class);
        } catch(IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void makeNonFinal(Field targetField) {
        int beforeMods = targetField.getModifiers();
        if(Modifier.isFinal(beforeMods)) {
            MOD_HANDLE.set(targetField, beforeMods & ~Modifier.FINAL);

            if(Modifier.isFinal(targetField.getModifiers())) {
                throw new RuntimeException("Failed to make non-final.");
            }
        }
    }*/

    public static void setStaticFinalObject(Field targetField, Object newValue) {
        try {
            if (!Modifier.isStatic(targetField.getModifiers())) {
                throw new RuntimeException("Only it works static fields!");
            }

            Class<Unsafe> classUnsafe = Unsafe.class;
            Field fieldTheUnsafe = classUnsafe.getDeclaredField("theUnsafe");
            if (!fieldTheUnsafe.trySetAccessible()) {
                throw new RuntimeException("Failed to Unsafe.theUnsafe #trySetAccessible");
            }
            Unsafe unsafe = (Unsafe) fieldTheUnsafe.get(null);

            Object fieldBase = unsafe.staticFieldBase(targetField);
            long fieldOffset = unsafe.staticFieldOffset(targetField);

            unsafe.putObject(fieldBase, fieldOffset, newValue);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
