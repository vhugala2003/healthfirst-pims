package com.healthfirst.pims;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {
    @Test
    void verifiesSeededDemoAccounts() {
        assertTrue(PasswordUtil.verify("admin123".toCharArray(),
                "pbkdf2_sha256$120000$aGVhbHRoZmlyc3QtYWRtaW4tc2VlZA==$NlNMkN9sm7h9e0zMOGyoK22VWNrkf5jJkUsK59r2K5E="));
        assertTrue(PasswordUtil.verify("cash123".toCharArray(),
                "pbkdf2_sha256$120000$aGVhbHRoZmlyc3QtY2FzaGllci1zZWVk$6yjFnuWgWatKF4wNrTsnxh/rFxfRLRLuFmr4l/28dL0="));
    }

    @Test
    void rejectsWrongAndMalformedPasswords() {
        assertFalse(PasswordUtil.verify("wrong-password".toCharArray(),
                "pbkdf2_sha256$120000$aGVhbHRoZmlyc3QtYWRtaW4tc2VlZA==$NlNMkN9sm7h9e0zMOGyoK22VWNrkf5jJkUsK59r2K5E="));
        assertFalse(PasswordUtil.verify("anything".toCharArray(), "not-a-password-hash"));
    }

    @Test
    void generatedPasswordHashCanBeVerified() {
        char[] password = "fresh-cashier-password".toCharArray();
        String encoded = PasswordUtil.hash(password);
        assertTrue(PasswordUtil.verify(password, encoded));
        assertFalse(PasswordUtil.verify("another-password".toCharArray(), encoded));
    }
}