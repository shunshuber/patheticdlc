package com.pathdlc.digger.funtime;

import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.util.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * AutoBuy — automatically clicks/buys items in server shop GUIs.
 * When a chest-based shop menu opens (e.g. /shop, /buyer on FunTime),
 * it scans the GUI slots and clicks on non-empty items to buy them.
 * Configurable delay between clicks and auto-close.
 */
public class AutoBuyBot {
    private boolean running;
    private int clickDelay;
    private int currentSlot;
    private boolean waitingForGui;
    private boolean processingGui;

    public void start() {
        running = true;
        clickDelay = 0;
        currentSlot = 0;
        waitingForGui = true;
        processingGui = false;
        Chat.info("AutoBuy ON - open a shop GUI to start buying");
    }

    public void stop() {
        running = false;
        waitingForGui = false;
        processingGui = false;
        Chat.info("AutoBuy OFF");
    }

    public void tick(MinecraftClient client) {
        if (!running) return;
        if (!ModuleManager.isEnabled("AutoBuy")) {
            stop();
            return;
        }
        if (client.player == null) return;
        if (client.interactionManager == null) return;

        Module mod = ModuleManager.get("AutoBuy");
        if (mod == null) return;

        ModuleSetting delaySetting = mod.getSetting("Delay");
        int delayTicks = delaySetting != null ? (int) delaySetting.getFloat() : 2;

        if (!(client.currentScreen instanceof GenericContainerScreen containerScreen)) {
            if (processingGui) {
                processingGui = false;
                currentSlot = 0;
            }
            return;
        }

        GenericContainerScreenHandler handler = containerScreen.getScreenHandler();
        int containerSlots = handler.getRows() * 9;

        if (!processingGui) {
            processingGui = true;
            currentSlot = 0;
            clickDelay = 5;
            return;
        }

        if (clickDelay > 0) {
            clickDelay--;
            return;
        }

        ModuleSetting modeSetting = mod.getSetting("Mode");
        int mode = modeSetting != null ? modeSetting.getChoiceIndex() : 0;

        while (currentSlot < containerSlots) {
            Slot slot = handler.getSlot(currentSlot);
            ItemStack stack = slot.getStack();

            if (!stack.isEmpty()) {
                boolean shouldClick = switch (mode) {
                    case 0 -> true;
                    case 1 -> {
                        String name = stack.getName().getString().toLowerCase();
                        yield !name.contains("back") && !name.contains("close")
                                && !name.contains("return") && !name.contains("exit")
                                && !name.contains("назад") && !name.contains("закрыть")
                                && !name.contains("выход");
                    }
                    case 2 -> {
                        String name = stack.getName().getString().toLowerCase();
                        yield name.contains("buy") || name.contains("купить")
                                || name.contains("purchase") || name.contains("trade")
                                || name.contains("торг");
                    }
                    default -> true;
                };

                if (shouldClick) {
                    client.interactionManager.clickSlot(
                            handler.syncId, currentSlot,
                            0, SlotActionType.PICKUP, client.player);
                    currentSlot++;
                    clickDelay = delayTicks;
                    return;
                }
            }
            currentSlot++;
        }

        ModuleSetting autoCloseSetting = mod.getSetting("AutoClose");
        boolean autoClose = autoCloseSetting != null && autoCloseSetting.getBool();
        if (autoClose) {
            client.player.closeHandledScreen();
        }

        processingGui = false;
        currentSlot = 0;
    }

    public boolean isRunning() {
        return running;
    }
}
