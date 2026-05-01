package com.pathdlc.digger.gui;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private final String name;
    private final List<ModuleButton> modules = new ArrayList<>();
    public float x;
    public float y;
    public float hoverAmount;
    private boolean collapsed;
    private float expandProgress = 1.0f;

    public Category(String name, float x, float y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public void addModule(Module module) {
        modules.add(new ModuleButton(module));
    }

    public String getName() {
        return name;
    }

    public List<ModuleButton> getModules() {
        return modules;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void toggleCollapsed() {
        collapsed = !collapsed;
    }

    public void updateExpandProgress() {
        float target = collapsed ? 0f : 1f;
        expandProgress += (target - expandProgress) * 0.18f;
        if (Math.abs(expandProgress - target) < 0.01f) expandProgress = target;
    }

    public float getExpandProgress() {
        return expandProgress;
    }

    public float getHeight() {
        float fullH = 22 + 1 + modules.size() * 20;
        return 22 + (fullH - 22) * expandProgress;
    }
}
