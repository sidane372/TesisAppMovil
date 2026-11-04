package com.example.santaceciliatesis.util;

import java.math.BigDecimal;

public final class MoneyUtils {
    private MoneyUtils() {}

    public static String formatPen(long centimos) {
        return "S/ " + BigDecimal.valueOf(centimos, 2).toPlainString();
    }
}
