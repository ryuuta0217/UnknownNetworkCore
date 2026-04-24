/*
 * Copyright (C) 2023 Ryuta Iwakura (ryuuta0217)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ryuuta0217.util.mc;

public class MinecraftUtil {
    /**
     * Get version parts from version id.
     *
     * @param version Version id (e.g. 1.12.2, 1.13.2, 1.14.4, 1.15.2, 1.16.5, 1.17.1, 1.18.1, 1.19.1, 1.20.1)
     * @return Version parts (major, minor, patch)
     */
    public static int[] getVersionParts(String version) {
        String[] versionParts = version.split("\\.");
        if (versionParts.length >= 2) {
            int majorVersion = Integer.parseInt(versionParts[0]);
            int minorVersion = Integer.parseInt(versionParts[1]);
            int patchVersion = versionParts.length >= 3 ? Integer.parseInt(versionParts[2]) : 0;
            return new int[] {majorVersion, minorVersion, patchVersion};
        }
        return new int[] {0, 0, 0};
    }

    /**
     * Check if the version is modern (1.13+)
     *
     * @param version Version id (e.g. 1.12.2, 1.13.2, 1.14.4, 1.15.2, 1.16.5, 1.17.1, 1.18.1, 1.19.1, 1.20.1, 26.1)
     * @return True if the version is modern (1.13+)
     */
    public static boolean isModernVersion(String version) {
        int[] versionParts = getVersionParts(version);
        boolean isAfter1_13 = versionParts[0] >= 1 && versionParts[1] >= 13;
        boolean isAfter26_1 = versionParts[0] >= 26 && versionParts[1] >= 1;
        return isAfter1_13 || isAfter26_1;
    }
}
