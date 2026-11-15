package dev.coco.cocovis;

import java.util.Locale;

public final class Fmt {
    private Fmt() {}

    public static String num(int n) {
        return String.format(Locale.ROOT, "%,d", n).replace(',', ' ');
    }

    /** Russian plural: 1 алмаз, 2 алмаза, 5 алмазов. */
    public static String diamonds(int n) {
        int a = Math.abs(n);
        int m100 = a % 100;
        int m10 = a % 10;
        if (m100 >= 11 && m100 <= 14) {
            return "алмазов";
        }
        if (m10 == 1) {
            return "алмаз";
        }
        if (m10 >= 2 && m10 <= 4) {
            return "алмаза";
        }
        return "алмазов";
    }
}
