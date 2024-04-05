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

package com.ryuuta0217.util;

import javax.annotation.Nonnull;
import java.util.*;

public class ListUtil {
    @Nonnull
    public static <V> List<Set<V>> splitListAsSet(@Nonnull Collection<V> baseList, int splitSize) {
        List<Set<V>> splitList = new ArrayList<>();
        Set<V> split = new HashSet<>();
        for (V value : baseList) {
            if (split.size() >= splitSize) {
                splitList.add(Collections.unmodifiableSet(split));
                split = new HashSet<>();
            }
            split.add(value);
        }
        splitList.add(split);
        return Collections.unmodifiableList(splitList);
    }

    @Nonnull
    public static <V> List<Set<V>> splitListAsLinkedSet(@Nonnull Collection<V> baseList, int splitSize) {
        List<Set<V>> splitList = new ArrayList<>();
        Set<V> split = new LinkedHashSet<>();
        for (V value : baseList) {
            if (split.size() >= splitSize) {
                splitList.add(Collections.unmodifiableSet(split));
                split = new LinkedHashSet<>();
            }
            split.add(value);
        }
        splitList.add(split);
        return Collections.unmodifiableList(splitList);
    }
}