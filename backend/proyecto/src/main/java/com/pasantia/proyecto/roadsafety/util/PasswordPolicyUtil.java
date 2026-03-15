package com.pasantia.proyecto.roadsafety.util;


import java.util.regex.Pattern;

public final class PasswordPolicyUtil {

    private static final Pattern POLICY = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private PasswordPolicyUtil() {}

    public static boolean isValid(String password) {
        if (password == null) return false;
        return POLICY.matcher(password).matches();
    }
}
