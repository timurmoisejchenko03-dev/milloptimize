/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.net;

import com.mojang.logging.LogUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.millenaire.net.ModApiConfig;
import org.millenaire.net.ModSignature;
import org.millenaire.net.SignatureParts;
import org.slf4j.Logger;

public final class ModApiClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Duration TIMEOUT = Duration.ofSeconds(5L);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Millenaire-ModApi");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).executor(EXECUTOR).build();

    private ModApiClient() {
    }

    public static CompletableFuture<ApiResponse> postJson(String path, String query, String jsonBody) {
        return ModApiClient.send("POST", path, query == null ? "" : query, jsonBody);
    }

    public static CompletableFuture<ApiResponse> get(String path, String query) {
        return ModApiClient.send("GET", path, query == null ? "" : query, "");
    }

    private static CompletableFuture<ApiResponse> send(String method, String path, String query, String body) {
        if (!ModApiConfig.hasSecret()) {
            LOGGER.debug("Mod API {} {} skipped: no HMAC secret in this build", (Object)method, (Object)path);
            return CompletableFuture.completedFuture(null);
        }
        return ModApiClient.attempt(method, path, query, body, ModApiConfig.baseUrls(), 0);
    }

    private static CompletableFuture<ApiResponse> attempt(String method, String path, String query, String body, List<String> bases, int index) {
        String base = bases.get(index);
        boolean hasNext = index + 1 < bases.size();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        String signature = ModSignature.sign(ModApiConfig.secret(), new SignatureParts(method, path, query, timestamp, nonce, body));
        String url = base + path + (String)(query.isEmpty() ? "" : "?" + query);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).header("X-Mod-Signature", signature).header("X-Mod-Timestamp", timestamp).header("X-Mod-Nonce", nonce);
        if (method.equals("POST")) {
            b.header("Content-Type", "application/json; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            b.GET();
        }
        return ((CompletableFuture)((CompletableFuture)CLIENT.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(r -> new ApiResponse(r.statusCode(), (String)r.body()))).exceptionally(e -> {
            LOGGER.debug("Mod API {} {} @ {} failed: {}", new Object[]{method, path, base, e.getMessage()});
            return null;
        })).thenCompose(resp -> {
            if (resp != null && resp.status() == 200) {
                return CompletableFuture.completedFuture(resp);
            }
            if (resp != null) {
                LOGGER.debug("Mod API {} {} @ {} -> {} {}", new Object[]{method, path, base, resp.status(), resp.body()});
            }
            if (hasNext) {
                LOGGER.debug("Mod API {} {} falling back from {} to {}", new Object[]{method, path, base, bases.get(index + 1)});
                return ModApiClient.attempt(method, path, query, body, bases, index + 1);
            }
            return CompletableFuture.completedFuture(resp);
        });
    }

    public record ApiResponse(int status, String body) {
    }
}

