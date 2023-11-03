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

    public static boolean isValidUUID(String s, boolean tryParse) {
        boolean matched = UUID_PATTERN.matcher(s).matches();
        boolean parsed = false;
        if (tryParse) {
            try {
                UUID.fromString(s);
                parsed = true;
            } catch(Throwable ignored) {}
        }

        return tryParse ? matched && parsed : matched;
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
