package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardMaskUtilTest {

    @Test
    void mask_returnsMaskedFormat() {
        assertThat(CardMaskUtil.mask("1234")).isEqualTo("**** **** **** 1234");
    }
}
