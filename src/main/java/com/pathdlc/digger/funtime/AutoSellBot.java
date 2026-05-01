package com.pathdlc.digger.funtime;

import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.util.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class AutoSellBot {
    private boolean running;
    private int tickCounter;
    private int sellCooldown;

    private static final int SELL_INTERVAL = 600;

    public void start() {
        running = true;
        tickCounter = 0;
        sellCooldown = 20;
        Chat.info("AutoSell started - selling via /buyer");
    }

    public void stop() {
        running = false;
        Chat.info("AutoSell stopped");
    }

    public void tick(MinecraftClient client) {
        if (!running) return;
        if (!ModuleManager.isEnabled("AutoSell")) {
            stop();
            return;
        }
        if (client.player == null) return;

        tickCounter++;

        Module mod = ModuleManager.get("AutoSell");
        if (mod == null) return;

        ModuleSetting modeSetting = mod.getSetting("Mode");
        int mode = modeSetting != null ? modeSetting.getChoiceIndex() : 0;

        ModuleSetting intervalSetting = mod.getSetting("Interval");
        int interval = intervalSetting != null
                ? (int)(intervalSetting.getFloat() * 20) : SELL_INTERVAL;

        if (tickCounter < interval) return;
        tickCounter = 0;

        ClientPlayerEntity player = client.player;

        switch (mode) {
            case 0 -> sellViaBuyer(player);
            case 1 -> sellJunk(player);
            case 2 -> sellAll(player);
        }
    }

    private void sellViaBuyer(ClientPlayerEntity player) {
        player.networkHandler.sendChatMessage("/buyer");
        Chat.info("Opening /buyer...");
    }

    private void sellJunk(ClientPlayerEntity player) {
        boolean hasJunk = false;

        for (int i = 0; i < player.getInventory().size(); i++) {
            Item item = player.getInventory().getStack(i).getItem();
            if (isJunk(item)) {
                hasJunk = true;
                break;
            }
        }

        if (hasJunk) {
            player.networkHandler.sendChatMessage("/buyer");
            Chat.info("Selling junk items...");
        }
    }

    private void sellAll(ClientPlayerEntity player) {
        player.networkHandler.sendChatMessage("/buyer");
        Chat.info("Selling all via /buyer...");
    }

    private boolean isJunk(Item item) {
        return item == Items.COBBLESTONE
                || item == Items.COBBLED_DEEPSLATE
                || item == Items.DIRT
                || item == Items.GRAVEL
                || item == Items.SAND
                || item == Items.ANDESITE
                || item == Items.DIORITE
                || item == Items.GRANITE
                || item == Items.TUFF
                || item == Items.NETHERRACK
                || item == Items.ROTTEN_FLESH
                || item == Items.SPIDER_EYE
                || item == Items.STRING
                || item == Items.BONE
                || item == Items.GUNPOWDER
                || item == Items.FEATHER
                || item == Items.FLINT;
    }

    public boolean isRunning() {
        return running;
    }
}
