package com.pathdlc.digger.gui;

public class GuiSettings {
    public enum Layout {
        COLUMNS("Columns"),
        SINGLE("Single Panel");

        private final String label;

        Layout(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public Layout next() {
            Layout[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum AccentColor {
        BLUE("Blue", 0.3f, 0.6f, 1.0f, 0xFF88BBFF),
        PURPLE("Purple", 0.6f, 0.3f, 1.0f, 0xFFAA88FF),
        GREEN("Green", 0.3f, 1.0f, 0.5f, 0xFF88FFAA),
        RED("Red", 1.0f, 0.3f, 0.3f, 0xFFFF8888),
        ORANGE("Orange", 1.0f, 0.6f, 0.2f, 0xFFFFBB66),
        PINK("Pink", 1.0f, 0.3f, 0.7f, 0xFFFF88BB),
        CYAN("Cyan", 0.2f, 0.9f, 0.9f, 0xFF66EEFF);

        private final String label;
        public final float r, g, b;
        public final int textColor;

        AccentColor(String label, float r, float g, float b, int textColor) {
            this.label = label;
            this.r = r;
            this.g = g;
            this.b = b;
            this.textColor = textColor;
        }

        public String getLabel() {
            return label;
        }

        public AccentColor next() {
            AccentColor[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private static Layout layout = Layout.COLUMNS;
    private static AccentColor accentColor = AccentColor.BLUE;
    private static boolean customFontEnabled = false;

    public static Layout getLayout() {
        return layout;
    }

    public static void setLayout(Layout layout) {
        GuiSettings.layout = layout;
    }

    public static void cycleLayout() {
        layout = layout.next();
    }

    public static AccentColor getAccentColor() {
        return accentColor;
    }

    public static void setAccentColor(AccentColor color) {
        accentColor = color;
    }

    public static void cycleAccentColor() {
        accentColor = accentColor.next();
    }

    public static boolean isCustomFontEnabled() {
        return customFontEnabled;
    }

    public static void toggleCustomFont() {
        customFontEnabled = !customFontEnabled;
    }
}
