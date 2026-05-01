package com.pathdlc.digger.funtime;

import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.util.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoBuy — auction house sniper for FunTime (/ah).
 * Periodically opens /ah, scans items, and buys anything
 * matching the target item name below the max price.
 */
public class AutoBuyBot {
    private boolean running;
    private int tickCounter;
    private int scanDelay;
    private boolean guiOpened;
    private boolean scanning;
    private int scanSlot;

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?:Цена|Price|Стоимость|Cost)[:\\s]*([\\d,.]+)");
    private static final Pattern PRICE_NUMBER = Pattern.compile(
            "([\\d]+[.,]?[\\d]*)");

    public void start() {
        running = true;
        tickCounter = 0;
        scanDelay = 0;
        guiOpened = false;
        scanning = false;
        scanSlot = 0;
        Chat.info("AutoBuy ON - sniping /ah auction");
    }

    public void stop() {
        running = false;
        guiOpened = false;
        scanning = false;
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

        ModuleSetting intervalSetting = mod.getSetting("Interval");
        int interval = intervalSetting != null
                ? (int)(intervalSetting.getFloat() * 20) : 200;

        ModuleSetting maxPriceSetting = mod.getSetting("Max Price");
        int maxPrice = maxPriceSetting != null
                ? (int) maxPriceSetting.getFloat() : 10000;

        ModuleSetting itemSetting = mod.getSetting("Item");
        int itemIndex = itemSetting != null ? itemSetting.getChoiceIndex() : 0;
        String targetName = getTargetName(itemIndex).toLowerCase();

        if (client.currentScreen instanceof GenericContainerScreen containerScreen) {
            GenericContainerScreenHandler handler = containerScreen.getScreenHandler();
            int containerSlots = handler.getRows() * 9;

            if (scanDelay > 0) {
                scanDelay--;
                return;
            }

            if (!scanning) {
                scanning = true;
                scanSlot = 0;
                scanDelay = 5;
                return;
            }

            while (scanSlot < containerSlots) {
                ItemStack stack = handler.getSlot(scanSlot).getStack();

                if (!stack.isEmpty()) {
                    String itemName = stack.getName().getString().toLowerCase();
                    int price = extractPrice(stack);

                    if (itemName.contains(targetName) && price > 0 && price <= maxPrice) {
                        client.interactionManager.clickSlot(
                                handler.syncId, scanSlot,
                                0, SlotActionType.PICKUP, client.player);
                        Chat.info("AutoBuy: bought \"" + stack.getName().getString()
                                + "\" for " + price);
                        scanSlot++;
                        scanDelay = 3;
                        return;
                    }
                }
                scanSlot++;
            }

            scanning = false;
            guiOpened = false;
            client.player.closeHandledScreen();
            return;
        }

        guiOpened = false;
        scanning = false;

        tickCounter++;
        if (tickCounter >= interval) {
            tickCounter = 0;

            ModuleSetting searchSetting = mod.getSetting("Search");
            int searchMode = searchSetting != null ? searchSetting.getChoiceIndex() : 0;

            if (searchMode == 1) {
                String searchTarget = getTargetName(itemIndex);
                client.player.networkHandler.sendChatMessage("/ah search " + searchTarget);
            } else {
                client.player.networkHandler.sendChatMessage("/ah");
            }
            guiOpened = true;
        }
    }

    private int extractPrice(ItemStack stack) {
        List<Text> lore = List.of();

        var loreComponent = stack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            lore = loreComponent.lines();
        }

        for (Text line : lore) {
            String text = line.getString();
            Matcher matcher = PRICE_PATTERN.matcher(text);
            if (matcher.find()) {
                return parseNumber(matcher.group(1));
            }
        }

        for (Text line : lore) {
            String text = line.getString();
            if (text.contains("$") || text.contains("монет")
                    || text.contains("coin") || text.contains("руб")) {
                Matcher numMatcher = PRICE_NUMBER.matcher(text);
                if (numMatcher.find()) {
                    return parseNumber(numMatcher.group(1));
                }
            }
        }

        String name = stack.getName().getString();
        Matcher nameMatcher = PRICE_PATTERN.matcher(name);
        if (nameMatcher.find()) {
            return parseNumber(nameMatcher.group(1));
        }

        return -1;
    }

    private int parseNumber(String str) {
        try {
            String clean = str.replace(",", "").replace(".", "").trim();
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String getTargetName(int index) {
        return switch (index) {
            case 0 -> "diamond";
            case 1 -> "emerald";
            case 2 -> "netherite";
            case 3 -> "enchanted golden apple";
            case 4 -> "elytra";
            case 5 -> "totem";
            case 6 -> "shulker";
            case 7 -> "beacon";
            default -> "diamond";
        };
    }

    public boolean isRunning() {
        return running;
    }
}
