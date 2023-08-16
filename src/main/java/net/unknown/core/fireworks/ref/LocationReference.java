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

package net.unknown.core.fireworks.ref;

import net.unknown.core.fireworks.ProgrammedFireworks;
import net.unknown.core.fireworks.model.Program;
import org.bukkit.Location;

import javax.annotation.Nullable;

public record LocationReference(@Nullable String programId, String id) {
    @Nullable
    public Location get() {
        if (this.programId != null) {
            // Program defined location
            if (!ProgrammedFireworks.hasProgram(this.programId)) return null;
            Program program = ProgrammedFireworks.getProgram(this.programId);
            if (program == null || !program.hasLocation(this.id)) return null;
            Location loc = program.getLocation(this.id);
            return loc != null ? loc.clone() : null;
        } else {
            // Globally defined location
            if (!ProgrammedFireworks.hasLocation(this.id)) return null;
            Location loc = ProgrammedFireworks.getLocation(this.id);;
            return loc != null ? loc.clone() : null;
        }
    }

    public String toString() {
        if (this.programId != null) {
            return this.programId + ":" + this.id;
        }

        return this.id;
    }

    public static LocationReference ofProgramDefined(Program program, String id) {
        return new LocationReference(program.getId(), id);
    }

    public static LocationReference ofGloballyDefined(String id) {
        return new LocationReference(null, id);
    }

    public static LocationReference parse(@Nullable String input) {
        if (input == null) return null;

        if (input.contains(":")) {
            String[] split = input.split(":");
            if (split.length != 2) return null;
            return ofProgramDefined(ProgrammedFireworks.getProgram(split[0]), split[1]);
        } else {
            return ofGloballyDefined(input);
        }
    }
}
