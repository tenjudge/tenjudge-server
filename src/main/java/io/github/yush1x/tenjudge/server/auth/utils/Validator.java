package io.github.yush1x.tenjudge.server.auth.utils;

import java.util.Set;
import java.util.regex.Pattern;

public class Validator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{2,19}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> VALID_ROLES = Set.of("super_admin", "admin", "user");

    public static boolean isUsernameValid(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isPasswordValid(String password) {
        return password != null && password.length() >= 8 && password.length() <= 20;
    }

    public static boolean isEmailValid(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isRoleValid(String role) {
        return role != null && VALID_ROLES.contains(role);
    }
}
