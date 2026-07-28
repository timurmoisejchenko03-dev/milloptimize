/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.net;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.millenaire.net.SignatureParts;

public final class ModSignature {
    private ModSignature() {
    }

    static String canonical(SignatureParts p) {
        return String.join((CharSequence)"\n", p.method().toUpperCase(Locale.ROOT), p.path(), p.query(), p.timestamp(), p.nonce(), p.body());
    }

    public static String sign(String secret, SignatureParts parts) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(ModSignature.canonical(parts).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}

