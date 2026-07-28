/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.net;

public record SignatureParts(String method, String path, String query, String timestamp, String nonce, String body) {
}

