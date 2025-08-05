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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class StringUtil {

    private static final Pattern UUID_PATTERN = Pattern.compile("(?i)^[\\dA-F]{8}-[\\dA-F]{4}-4[\\dA-F]{3}-[89AB][\\dA-F]{3}-[\\dA-F]{12}");

    public static boolean isValidUUID(String s) {
        return isValidUUID(s, false);
    }

    public static boolean isValidUUID(String s, boolean tryParseAsObject) {
        boolean matched = UUID_PATTERN.matcher(s).matches();
        boolean parsed = false;
        if (tryParseAsObject) {
            try {
                UUID.fromString(s);
                parsed = true;
            } catch(Throwable ignored) {}
        }

        return tryParseAsObject ? matched && parsed : matched;
    }

    public static String shuffle(String s) {
        String[] array = s.split("");
        for (int i = 0; i < array.length; i++) {
            int random = (int) (Math.random() * array.length);
            String tmp = array[i];
            array[i] = array[random];
            array[random] = tmp;
        }
        return String.join("", array);
    }

    public static String shuffleByCollections(String s) {
        List<String> list = Arrays.asList(s.split(""));
        Collections.shuffle(list);
        return String.join("", list);
    }
}
