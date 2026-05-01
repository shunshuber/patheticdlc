package com.pathdlc.digger.gui;

public class ModuleButton {
    private final Module module;
    public float hoverAmount;

    public ModuleButton(Module module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public void updateHover(boolean isHovered) {
        float target = isHovered ? 1.0f : 0.0f;
        hoverAmount += (target - hoverAmount) * 0.15f;
    }
}
