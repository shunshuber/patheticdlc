package com.pathdlc.digger.funtime;

import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.util.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoFishBot {
    private boolean running;
    private int castDelay;
    private int reelDelay;
    private boolean waitingForBite;

    public void start() {
        running = true;
        castDelay = 20;
        reelDelay = 0;
        waitingForBite = false;
        Chat.info("AutoFish started - hold a fishing rod");
    }

    public void stop() {
        running = false;
        Chat.info("AutoFish stopped");
    }

    public void tick(MinecraftClient client) {
        if (!running) return;
        if (!ModuleManager.isEnabled("AutoFish")) {
            stop();
            return;
        }
        if (client.player == null || client.world == null) return;
        if (client.interactionManager == null) return;

        ClientPlayerEntity player = client.player;

        boolean hasRod = player.getMainHandStack().getItem() == Items.FISHING_ROD;
        if (!hasRod) return;

        FishingBobberEntity bobber = player.fishHook;

        if (bobber == null) {
            if (castDelay > 0) {
                castDelay--;
                return;
            }
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            waitingForBite = true;
            castDelay = 30;
            return;
        }

        if (waitingForBite) {
            boolean caught = bobber.getVelocity().y < -0.04
                    && bobber.getVelocity().y > -0.5
                    && !bobber.isOnGround();

            if (caught) {
                if (reelDelay > 0) {
                    reelDelay--;
                    return;
                }
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                waitingForBite = false;
                castDelay = 20 + (int)(Math.random() * 20);
                reelDelay = 3;
            }
        }
    }

    public boolean isRunning() {
        return running;
    }
}
