package com.pathdlc.digger.gui;

public final class FogSettings {
    public enum FogColor {
        WHITE("White", 1.0f, 1.0f, 1.0f),
        LIGHT_BLUE("Light Blue", 0.7f, 0.85f, 1.0f),
        PURPLE("Purple", 0.6f, 0.3f, 0.8f),
        RED("Red", 0.8f, 0.2f, 0.15f),
        GREEN("Green", 0.2f, 0.7f, 0.3f),
        DARK("Dark", 0.15f, 0.1f, 0.2f),
        GOLDEN("Golden", 0.9f, 0.75f, 0.3f);

        public final String label;
        public final float r, g, b;

        FogColor(String label, float r, float g, float b) {
            this.label = label;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public FogColor next() {
            FogColor[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    public enum FogDensity {
        LIGHT("Light", 0.6f, 1.0f),
        MEDIUM("Medium", 0.3f, 0.7f),
        HEAVY("Heavy", 0.1f, 0.4f),
        ULTRA("Ultra", 0.02f, 0.15f);

        public final String label;
        public final float startMul;
        public final float endMul;

        FogDensity(String label, float startMul, float endMul) {
            this.label = label;
            this.startMul = startMul;
            this.endMul = endMul;
        }

        public FogDensity next() {
            FogDensity[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    private static FogColor color = FogColor.WHITE;
    private static FogDensity density = FogDensity.MEDIUM;

    public static FogColor getColor() { return color; }
    public static void cycleColor() { color = color.next(); }

    public static FogDensity getDensity() { return density; }
    public static void cycleDensity() { density = density.next(); }

    private FogSettings() {}
}
