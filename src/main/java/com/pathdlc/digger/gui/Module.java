package com.pathdlc.digger.gui;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Module {
    private final String name;
    private final Identifier icon;
    private boolean enabled;
    private Runnable onEnable;
    private Runnable onDisable;
    private final List<ModuleSetting> settings = new ArrayList<>();
    private boolean settingsExpanded;

    public Module(String name) {
        this.name = name;
        this.icon = Identifier.of("pathdlc_digger",
                "textures/gui/icons/" + name.toLowerCase() + ".png");
    }

    public Module(String name, Runnable onEnable, Runnable onDisable) {
        this.name = name;
        this.icon = Identifier.of("pathdlc_digger",
                "textures/gui/icons/" + name.toLowerCase() + ".png");
        this.onEnable = onEnable;
        this.onDisable = onDisable;
    }

    public String getName() {
        return name;
    }

    public Identifier getIcon() {
        return icon;
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

    public Module addSetting(ModuleSetting setting) {
        settings.add(setting);
        return this;
    }

    public List<ModuleSetting> getSettings() {
        return settings;
    }

    public boolean hasSettings() {
        return !settings.isEmpty();
    }

    public boolean isSettingsExpanded() {
        return settingsExpanded;
    }

    public void toggleSettingsExpanded() {
        settingsExpanded = !settingsExpanded;
    }

    public ModuleSetting getSetting(String name) {
        for (ModuleSetting s : settings) {
            if (s.getName().equals(name)) return s;
        }
        return null;
    }
}
