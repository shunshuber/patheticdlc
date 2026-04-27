package com.pathdlc.digger.farm;

import com.pathdlc.digger.baritone.BaritoneBridge;
import com.pathdlc.digger.util.Chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public abstract class AbstractFarmBot implements FarmBot {
    protected final BaritoneBridge baritone;

    protected boolean running;
    protected int radius = 16;
    protected int pathCooldown;
    protected int waitTicks;
    protected int actions;
    protected int failed;

    protected AbstractFarmBot(BaritoneBridge baritone) {
        this.baritone = baritone;
    }

    @Override
    public void start() {
        running = true;
        pathCooldown = 0;
        waitTicks = 0;
        actions = 0;
        failed = 0;
        Chat.info(name() + " started. Radius: " + radius);
    }

    @Override
    public void stop() {
        running = false;
        baritone.cancel();
        Chat.info(name() + " stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void setRadius(int radius) {
        this.radius = Math.max(4, Math.min(64, radius));
    }

    public int getRadius() {
        return radius;
    }

    protected void gotoIfNeeded(BlockPos pos) {
        if (pos == null || pathCooldown > 0) {
            return;
        }

        baritone.gotoBlock(pos);
        pathCooldown = 45;
    }

    protected void cooldownTick() {
        if (pathCooldown > 0) {
            pathCooldown--;
        }

        if (waitTicks > 0) {
            waitTicks--;
        }
    }

    @Override
    public String status() {
        return name() + ": running=" + running + ", radius=" + radius + ", actions=" + actions + ", failed=" + failed;
    }

    protected boolean worldReady(MinecraftClient mc) {
        return mc.player != null && mc.world != null && mc.interactionManager != null;
    }
}
