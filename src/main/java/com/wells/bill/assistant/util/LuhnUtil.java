package com.wells.bill.assistant.util;

public class LuhnUtil {
    public static boolean isValid(String card) {
        int sum = 0;
        boolean alt = false;
        for (int i = card.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(card.substring(i, i + 1));
            if (alt) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }
}
