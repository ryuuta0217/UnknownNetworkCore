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

package net.unknown.survival.feature.gnarms.module;

import net.minecraft.resources.ResourceLocation;
import net.unknown.survival.feature.gnarms.module.drive.FlyModule;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GNModules {
    private static final Map<ResourceLocation, GNModule> ALL_MODULES = new HashMap<>();

    public static final GNModule FLY = FlyModule.INSTANCE;

    public static void registerMapping(ResourceLocation id, GNModule module) {
        if(ALL_MODULES.containsKey(id)) {
            LoggerFactory.getLogger("GNModules").warn("Overwriting already mapped Module " + id + " from " + ALL_MODULES.get(id) + " to " + module);
        }
        ALL_MODULES.put(id, module);
    }

    public static boolean isModule(ResourceLocation id) {
        return ALL_MODULES.containsKey(id);
    }

    public static GNModule getModule(ResourceLocation id) {
        return ALL_MODULES.get(id);
    }
}
