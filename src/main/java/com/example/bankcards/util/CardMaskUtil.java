package com.example.bankcards.util;

public final class CardMaskUtil {

    private CardMaskUtil() {
    }

    public static String mask(String lastFourDigits) {
        return "**** **** **** " + lastFourDigits;
    }
}
