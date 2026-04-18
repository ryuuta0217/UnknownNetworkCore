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

package net.unknown.core.util;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public interface CustomPersistentDataTypes {
    PersistentDataType<String[], String[]> STRING_ARRAY = new PrimitivePersistentDataType<>(String[].class);

    /**
     * A default implementation that simply exists to pass on the retrieved or
     * inserted value to the next layer.
     * <p>
     * This implementation does not add any kind of logic, but is used to
     * provide default implementations for the primitive types.
     *
     * @param <P> the generic type of the primitive objects
     */
    // Obtained from bukkit org.bukkit.persistence.PersistentDataType 2026/03/25, version = 1.21.11
    class PrimitivePersistentDataType<P> implements PersistentDataType<P, P> {

        private final Class<P> primitiveType;

        PrimitivePersistentDataType(@NotNull Class<P> primitiveType) {
            this.primitiveType = primitiveType;
        }

        @NotNull
        @Override
        public Class<P> getPrimitiveType() {
            return primitiveType;
        }

        @NotNull
        @Override
        public Class<P> getComplexType() {
            return primitiveType;
        }

        @NotNull
        @Override
        public P toPrimitive(@NotNull P complex, @NotNull PersistentDataAdapterContext context) {
            return complex;
        }

        @NotNull
        @Override
        public P fromPrimitive(@NotNull P primitive, @NotNull PersistentDataAdapterContext context) {
            return primitive;
        }
    }
}
