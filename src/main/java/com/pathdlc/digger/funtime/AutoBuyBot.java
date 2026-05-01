package com.pathdlc.digger.funtime;

import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.util.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class AutoBuyBot {
    private boolean running;
    private int tickCounter;

    public void start() {
        running = true;
        tickCounter = 0;
        Chat.info("AutoBuy started - auto purchasing from /shop");
    }

    public void stop() {
        running = false;
        Chat.info("AutoBuy stopped");
    }

    public void tick(MinecraftClient client) {
        if (!running) return;
        if (!ModuleManager.isEnabled("AutoBuy")) {
            stop();
            return;
        }
        if (client.player == null) return;

        tickCounter++;

        Module mod = ModuleManager.get("AutoBuy");
        if (mod == null) return;

        ModuleSetting intervalSetting = mod.getSetting("Interval");
        int interval = intervalSetting != null
                ? (int)(intervalSetting.getFloat() * 20) : 1200;

        if (tickCounter < interval) return;
        tickCounter = 0;

        ClientPlayerEntity player = client.player;

        ModuleSetting itemSetting = mod.getSetting("Item");
        int itemIndex = itemSetting != null ? itemSetting.getChoiceIndex() : 0;

        String command = getShopCommand(itemIndex);
        player.networkHandler.sendChatMessage(command);

        String itemName = getItemName(itemIndex);
        Chat.info("AutoBuy: purchasing " + itemName + "...");
    }

    private String getShopCommand(int index) {
        return "/shop";
    }

    private String getItemName(int index) {
        return switch (index) {
            case 0 -> "Food";
            case 1 -> "Blocks";
            case 2 -> "Tools";
            case 3 -> "Weapons";
            case 4 -> "Potions";
            default -> "Items";
        };
    }

    public boolean isRunning() {
        return running;
    }
}
