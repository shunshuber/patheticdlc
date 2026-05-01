package com.pathdlc.digger.gui;

public class Module {
    private final String name;
    private boolean enabled;
    private Runnable onEnable;
    private Runnable onDisable;

    public Module(String name) {
        this.name = name;
    }

    public Module(String name, Runnable onEnable, Runnable onDisable) {
        this.name = name;
        this.onEnable = onEnable;
        this.onDisable = onDisable;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled && onEnable != null) {
            onEnable.run();
        }
        if (!enabled && onDisable != null) {
            onDisable.run();
        }
    }
}
