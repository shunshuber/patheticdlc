package com.pathdlc.digger.farm;

import com.pathdlc.digger.baritone.BaritoneBridge;

import net.minecraft.client.MinecraftClient;

public class FarmManager {
    private final AutoAppleFarm apple;

    public FarmManager(BaritoneBridge baritone) {
        this.apple = new AutoAppleFarm(baritone);
    }

    public AutoAppleFarm apple() {
        return apple;
    }

    public void tick(MinecraftClient mc) {
        apple.tick(mc);
    }

    public void stopAll() {
        if (apple.isRunning()) {
            apple.stop();
        }
    }

    public String status() {
        return apple.status();
    }
}
