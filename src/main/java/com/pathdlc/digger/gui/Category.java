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

    public float getHeight() {
        if (collapsed) {
            return 20;
        }
        return 20 + 1 + modules.size() * 18;
    }
}
