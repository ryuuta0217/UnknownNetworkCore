package net.unknown.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormatUtil {
    public static String getScaledString(int scale, double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, RoundingMode.UP);
        return bd.toPlainString();
    }
}
