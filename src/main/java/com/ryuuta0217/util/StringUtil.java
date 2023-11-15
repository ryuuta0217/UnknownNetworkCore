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
