/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.language;

public final class SpeechRefCodec {
    private SpeechRefCodec() {
    }

    public static String encodeTargetName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name.replace("%", "%25").replace(":", "%3A");
    }

    public static String decodeTargetName(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        return encoded.replace("%3A", ":").replace("%25", "%");
    }

    public static String applyDialogueSubstitutions(String text, String playerName, String targetFirstName) {
        String out = text.replace("$name", playerName);
        if (targetFirstName != null && !targetFirstName.isEmpty()) {
            out = out.replace("$targetfirstname", targetFirstName);
        } else {
            out = out.replace("$targetfirstname, ", "");
            out = out.replace("$targetfirstname,", "");
            out = out.replace(" $targetfirstname", "");
            out = out.replace("$targetfirstname", "");
        }
        return out;
    }
}

