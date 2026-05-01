package com.pathdlc.digger.render;

/**
 * Automatic performance scaling.
 * Tracks FPS and adjusts rendering quality so the mod
 * doesn't lag on weak PCs.
 */
public final class PerformanceSettings {
    public enum Quality { LOW, MEDIUM, HIGH }

    private static Quality quality = Quality.HIGH;
    private static Quality userOverride = null;

    private static long lastFrameTime = System.nanoTime();
    private static float smoothFps = 60f;
    private static int frameCount = 0;
    private static long fpsAccum = 0;

    private PerformanceSettings() {}

    public static void onFrameEnd() {
        long now = System.nanoTime();
        long delta = now - lastFrameTime;
        lastFrameTime = now;

        if (delta <= 0) return;

        float instantFps = 1_000_000_000f / delta;
        smoothFps += (instantFps - smoothFps) * 0.05f;

        frameCount++;
        if (frameCount >= 60) {
            if (userOverride != null) {
                quality = userOverride;
            } else {
                if (smoothFps < 25f) {
                    quality = Quality.LOW;
                } else if (smoothFps < 45f) {
                    quality = Quality.MEDIUM;
                } else {
                    quality = Quality.HIGH;
                }
            }
            frameCount = 0;
        }
    }

    public static Quality getQuality() {
        return quality;
    }

    public static float getSmoothFps() {
        return smoothFps;
    }

    public static void setUserOverride(Quality q) {
        userOverride = q;
        if (q != null) quality = q;
    }

    public static Quality getUserOverride() {
        return userOverride;
    }

    public static void cycleQuality() {
        if (userOverride == null) {
            userOverride = Quality.HIGH;
        } else {
            userOverride = switch (userOverride) {
                case HIGH -> Quality.MEDIUM;
                case MEDIUM -> Quality.LOW;
                case LOW -> null;
            };
        }
        if (userOverride != null) quality = userOverride;
    }

    public static String getQualityLabel() {
        if (userOverride == null) return "Auto (" + quality.name().charAt(0) + quality.name().substring(1).toLowerCase() + ")";
        return quality.name().charAt(0) + quality.name().substring(1).toLowerCase();
    }

    public static int getBlurIterations() {
        return switch (quality) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH -> 3;
        };
    }

    public static boolean useShaders() {
        return quality != Quality.LOW;
    }

    public static boolean useGlassEffect() {
        return quality == Quality.HIGH;
    }

    public static boolean useRoundedShader() {
        return quality != Quality.LOW;
    }

    public static int getParticleCount() {
        return switch (quality) {
            case LOW -> 0;
            case MEDIUM -> 15;
            case HIGH -> 40;
        };
    }

    public static float getBlurSpread() {
        return switch (quality) {
            case LOW -> 0f;
            case MEDIUM -> 2.0f;
            case HIGH -> 2.5f;
        };
    }
}
