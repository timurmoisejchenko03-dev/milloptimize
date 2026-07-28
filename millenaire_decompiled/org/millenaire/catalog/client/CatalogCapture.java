/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.platform.NativeImage
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Screenshot
 *  org.slf4j.Logger
 */
package org.millenaire.catalog.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.minecraft.client.Screenshot;
import org.slf4j.Logger;

public final class CatalogCapture {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CatalogCapture() {
    }

    public static boolean capture(RenderTarget target, Path out) {
        try {
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent(), new FileAttribute[0]);
            }
            try (NativeImage image = Screenshot.takeScreenshot((RenderTarget)target);){
                image.writeToFile(out);
            }
            return true;
        }
        catch (IOException e) {
            LOGGER.error("Catalog capture failed for {}: {}", (Object)out, (Object)e.toString());
            return false;
        }
    }

    public static TransparentCapture captureTransparent(RenderTarget target, Path out) {
        TransparentCapture transparentCapture;
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent(), new FileAttribute[0]);
        }
        NativeImage image = new NativeImage(target.width, target.height, false);
        try {
            RenderSystem.bindTexture((int)target.getColorTextureId());
            image.downloadTexture(0, false);
            image.flipY();
            double ratio = CatalogCapture.missingTextureRatio(image);
            image.writeToFile(out);
            transparentCapture = new TransparentCapture(true, ratio);
        }
        catch (Throwable throwable) {
            try {
                try {
                    image.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException e) {
                LOGGER.error("Catalog transparent capture failed for {}: {}", (Object)out, (Object)e.toString());
                return new TransparentCapture(false, 0.0);
            }
        }
        image.close();
        return transparentCapture;
    }

    static double missingTextureRatio(NativeImage image) {
        int opaque = 0;
        int magenta = 0;
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int p = image.getPixelRGBA(x, y);
                int a = p >> 24 & 0xFF;
                if (a <= 16) continue;
                ++opaque;
                int b = p >> 16 & 0xFF;
                int g = p >> 8 & 0xFF;
                int r = p & 0xFF;
                if (r <= 180 || g >= 80 || b <= 180) continue;
                ++magenta;
            }
        }
        return opaque == 0 ? 0.0 : (double)magenta / (double)opaque;
    }

    public record TransparentCapture(boolean ok, double missingTextureRatio) {
    }
}

