package com.pathdlc.digger.funtime;

import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.util.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class AutoCraftBot {
    private boolean running;
    private int tickCounter;
    private int craftCooldown;

    private static final Item[][] RECIPES = {
            {Items.OAK_PLANKS, Items.OAK_LOG},
            {Items.STICK, Items.OAK_PLANKS},
            {Items.TORCH, Items.STICK, Items.COAL},
            {Items.BREAD, Items.WHEAT},
            {Items.GOLDEN_APPLE, Items.GOLD_INGOT, Items.APPLE},
    };

    private static final String[] RECIPE_NAMES = {
            "Planks", "Sticks", "Torches", "Bread", "Golden Apple"
    };

    public void start() {
        running = true;
        tickCounter = 0;
        craftCooldown = 0;
        Chat.info("AutoCraft started");
    }

    public void stop() {
        running = false;
        Chat.info("AutoCraft stopped");
    }

    public void tick(MinecraftClient client) {
        if (!running) return;
        if (!ModuleManager.isEnabled("AutoCraft")) {
            stop();
            return;
        }
        if (client.player == null) return;

        tickCounter++;
        if (tickCounter < 5) return;
        tickCounter = 0;

        if (craftCooldown > 0) {
            craftCooldown--;
            return;
        }

        Module mod = ModuleManager.get("AutoCraft");
        if (mod == null) return;

        ModuleSetting recipeSetting = mod.getSetting("Recipe");
        int recipeIndex = recipeSetting != null ? recipeSetting.getChoiceIndex() : 0;

        ClientPlayerEntity player = client.player;

        if (client.currentScreen instanceof CraftingScreen craftScreen) {
            CraftingScreenHandler handler = craftScreen.getScreenHandler();
            tryCraftInTable(client, handler, recipeIndex);
        } else if (client.currentScreen instanceof InventoryScreen) {
            tryCraftInInventory(client, player, recipeIndex);
        } else if (client.currentScreen == null) {
            tryCraftInInventory(client, player, recipeIndex);
        }
    }

    private void tryCraftInInventory(MinecraftClient client,
                                       ClientPlayerEntity player, int recipeIndex) {
        PlayerScreenHandler handler = player.playerScreenHandler;

        switch (recipeIndex) {
            case 0 -> craftPlanks(client, handler);
            case 1 -> craftSticks(client, handler);
            case 3 -> Chat.info("Bread requires crafting table");
            case 4 -> Chat.info("Golden Apple requires crafting table");
            default -> {}
        }
    }

    private void craftPlanks(MinecraftClient client,
                               PlayerScreenHandler handler) {
        int logSlot = findItem(handler, Items.OAK_LOG, Items.BIRCH_LOG,
                Items.SPRUCE_LOG, Items.DARK_OAK_LOG, Items.JUNGLE_LOG,
                Items.ACACIA_LOG);
        if (logSlot == -1) return;

        client.interactionManager.clickSlot(handler.syncId,
                logSlot, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(handler.syncId,
                1, 0, SlotActionType.PICKUP, client.player);

        if (!handler.getSlot(0).getStack().isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId,
                    0, 0, SlotActionType.QUICK_MOVE, client.player);
        }

        craftCooldown = 3;
    }

    private void craftSticks(MinecraftClient client,
                               PlayerScreenHandler handler) {
        int plankSlot = findItem(handler, Items.OAK_PLANKS, Items.BIRCH_PLANKS,
                Items.SPRUCE_PLANKS, Items.DARK_OAK_PLANKS, Items.JUNGLE_PLANKS,
                Items.ACACIA_PLANKS);
        if (plankSlot == -1) return;

        client.interactionManager.clickSlot(handler.syncId,
                plankSlot, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(handler.syncId,
                1, 0, SlotActionType.PICKUP, client.player);

        client.interactionManager.clickSlot(handler.syncId,
                plankSlot, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(handler.syncId,
                2, 0, SlotActionType.PICKUP, client.player);

        if (!handler.getSlot(0).getStack().isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId,
                    0, 0, SlotActionType.QUICK_MOVE, client.player);
        }

        craftCooldown = 3;
    }

    private void tryCraftInTable(MinecraftClient client,
                                   CraftingScreenHandler handler, int recipeIndex) {
        if (!handler.getSlot(0).getStack().isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId,
                    0, 0, SlotActionType.QUICK_MOVE, client.player);
            craftCooldown = 2;
        }
    }

    private int findItem(PlayerScreenHandler handler, Item... items) {
        for (int i = 9; i < 45; i++) {
            Item slotItem = handler.getSlot(i).getStack().getItem();
            for (Item target : items) {
                if (slotItem == target) return i;
            }
        }
        return -1;
    }

    public boolean isRunning() {
        return running;
    }
}
