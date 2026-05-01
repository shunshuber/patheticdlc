package com.pathdlc.digger.gui;

public class ModuleSetting {
    public enum Type { TOGGLE, SLIDER, CHOICE }

    private final String name;
    private final Type type;

    private boolean boolValue;

    private float floatValue;
    private final float min;
    private final float max;
    private final float step;

    private final String[] choices;
    private int choiceIndex;

    public static ModuleSetting toggle(String name, boolean defaultValue) {
        ModuleSetting s = new ModuleSetting(name, Type.TOGGLE, 0, 1, 1, null);
        s.boolValue = defaultValue;
        return s;
    }

    public static ModuleSetting slider(String name, float defaultValue,
                                        float min, float max, float step) {
        ModuleSetting s = new ModuleSetting(name, Type.SLIDER, min, max, step, null);
        s.floatValue = defaultValue;
        return s;
    }

    public static ModuleSetting choice(String name, String[] choices, int defaultIndex) {
        ModuleSetting s = new ModuleSetting(name, Type.CHOICE, 0, choices.length - 1, 1, choices);
        s.choiceIndex = defaultIndex;
        return s;
    }

    private ModuleSetting(String name, Type type, float min, float max,
                           float step, String[] choices) {
        this.name = name;
        this.type = type;
        this.min = min;
        this.max = max;
        this.step = step;
        this.choices = choices;
    }

    public String getName() { return name; }
    public Type getType() { return type; }

    public boolean getBool() { return boolValue; }
    public void setBool(boolean v) { boolValue = v; }
    public void toggleBool() { boolValue = !boolValue; }

    public float getFloat() { return floatValue; }
    public void setFloat(float v) {
        floatValue = Math.max(min, Math.min(max, v));
        floatValue = Math.round(floatValue / step) * step;
    }
    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getStep() { return step; }
    public float getNormalized() {
        return (max == min) ? 0 : (floatValue - min) / (max - min);
    }
    public void setFromNormalized(float norm) {
        setFloat(min + norm * (max - min));
    }

    public String[] getChoices() { return choices; }
    public int getChoiceIndex() { return choiceIndex; }
    public String getChoiceValue() {
        return choices != null ? choices[choiceIndex] : "";
    }
    public void cycleChoice() {
        if (choices != null) {
            choiceIndex = (choiceIndex + 1) % choices.length;
        }
    }

    public String getDisplayValue() {
        return switch (type) {
            case TOGGLE -> boolValue ? "ON" : "OFF";
            case SLIDER -> String.format("%.1f", floatValue);
            case CHOICE -> getChoiceValue();
        };
    }
}
