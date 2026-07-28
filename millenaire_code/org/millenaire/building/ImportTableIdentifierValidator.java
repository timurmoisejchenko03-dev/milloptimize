/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.building;

import java.util.regex.Pattern;

public final class ImportTableIdentifierValidator {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Pattern CULTURE_KEY_COMPONENT = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern CULTURE_KEY_NAMESPACED = Pattern.compile("[a-z0-9_-]+:[a-z0-9_-]+");
    public static final int MAX_IDENTIFIER_LENGTH = 64;
    public static final int MAX_CULTURE_KEY_LENGTH = 96;

    private ImportTableIdentifierValidator() {
    }

    public static boolean isValidIdentifier(String s) {
        if (s == null || s.isEmpty() || s.length() > 64) {
            return false;
        }
        return IDENTIFIER.matcher(s).matches();
    }

    public static boolean isValidOptionalIdentifier(String s) {
        if (s == null) {
            return false;
        }
        if (s.isEmpty()) {
            return true;
        }
        return ImportTableIdentifierValidator.isValidIdentifier(s);
    }

    public static boolean isValidCultureKey(String s) {
        if (s == null) {
            return false;
        }
        if (s.isEmpty()) {
            return true;
        }
        if (s.length() > 96) {
            return false;
        }
        return CULTURE_KEY_COMPONENT.matcher(s).matches() || CULTURE_KEY_NAMESPACED.matcher(s).matches();
    }
}

