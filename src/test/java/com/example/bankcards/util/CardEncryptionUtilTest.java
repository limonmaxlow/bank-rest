package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardEncryptionUtilTest {

    private final CardEncryptionUtil encryptionUtil = new CardEncryptionUtil("test-secret-key-32-characters!!");

    @Test
    void encryptThenDecrypt_returnsOriginalValue() {
        String cardNumber = "1234567812345678";

        String encrypted = encryptionUtil.encrypt(cardNumber);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo(cardNumber);
        assertThat(decrypted).isEqualTo(cardNumber);
    }

    @Test
    void encrypt_sameInputTwice_producesDifferentCiphertext() {
        String cardNumber = "1234567812345678";

        String encrypted1 = encryptionUtil.encrypt(cardNumber);
        String encrypted2 = encryptionUtil.encrypt(cardNumber);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }
}
