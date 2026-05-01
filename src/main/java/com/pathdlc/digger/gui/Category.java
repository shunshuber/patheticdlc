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

    public float getExpandProgress() {
        float target = collapsed ? 0f : 1f;
        expandProgress += (target - expandProgress) * 0.18f;
        if (Math.abs(expandProgress - target) < 0.01f) expandProgress = target;
        return expandProgress;
    }

    public boolean isFullyCollapsed() {
        return collapsed && expandProgress < 0.01f;
    }

    public float getHeight() {
        float ep = getExpandProgress();
        float fullH = 20 + 1 + modules.size() * 18;
        return 20 + (fullH - 20) * ep;
    }
}
