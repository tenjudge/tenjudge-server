package io.github.yush1x.tenjudge.server.auth.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @Test
    void isUsernameValid_shouldAcceptValidUsername() {
        assertTrue(Validator.isUsernameValid("abc_123"));
    }

    @Test
    void isUsernameValid_shouldRejectInvalidUsername() {
        assertFalse(Validator.isUsernameValid(null));
        assertFalse(Validator.isUsernameValid("ab"));
        assertFalse(Validator.isUsernameValid("1abc"));
        assertFalse(Validator.isUsernameValid("ab-c"));
    }

    @Test
    void isPasswordValid_shouldValidateLengthOnly() {
        assertTrue(Validator.isPasswordValid("12345678"));
        assertTrue(Validator.isPasswordValid("12345678901234567890"));
        assertFalse(Validator.isPasswordValid(null));
        assertFalse(Validator.isPasswordValid("1234567"));
        assertFalse(Validator.isPasswordValid("123456789012345678901"));
    }

    @Test
    void isEmailValid_shouldFollowConfiguredRegex() {
        assertTrue(Validator.isEmailValid("user.name+1@test-domain.com"));
        assertFalse(Validator.isEmailValid(null));
        assertFalse(Validator.isEmailValid("invalid-email"));
        assertFalse(Validator.isEmailValid("user@test"));
    }

    @Test
    void isRoleValid_shouldOnlyAcceptAllowedRoles() {

        assertTrue(Validator.isRoleValid("user"));
        assertTrue(Validator.isRoleValid("admin"));
        assertTrue(Validator.isRoleValid("super_admin"));
        assertFalse(Validator.isRoleValid(null));
        assertFalse(Validator.isRoleValid("USER"));
        assertFalse(Validator.isRoleValid("guest"));
    }
}
