package com.pathdlc.digger.farm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

public final class InventoryUtil {
    public static boolean hasItem(MinecraftClient mc, Item item) {
        return findInventorySlot(mc, stack -> stack.isOf(item)) >= 0;
    }

    public static boolean hasMatching(MinecraftClient mc, Predicate<ItemStack> predicate) {
        return findInventorySlot(mc, predicate) >= 0;
    }

    public static boolean selectItem(MinecraftClient mc, Item item, int preferredHotbarSlot) {
        return selectMatching(mc, stack -> stack.isOf(item), preferredHotbarSlot);
    }

    public static boolean selectMatching(MinecraftClient mc, Predicate<ItemStack> predicate, int preferredHotbarSlot) {
        if (mc.player == null || mc.interactionManager == null) {
            return false;
        }

        preferredHotbarSlot = clampHotbar(preferredHotbarSlot);

        int hotbarSlot = findHotbarSlot(mc, predicate);

        if (hotbarSlot >= 0) {
            mc.player.getInventory().selectedSlot = hotbarSlot;
            return true;
        }

        int inventorySlot = findInventorySlot(mc, predicate);

        if (inventorySlot < 0) {
            return false;
        }

        int screenSlot = inventorySlotToScreenSlot(inventorySlot);

        if (screenSlot < 0) {
            return false;
        }

        /*
         * SWAP moves the inventory slot into the selected hotbar slot without needing
         * to open the inventory screen. Slot ids 36-44 are hotbar slots in the player
         * screen handler; main inventory is 9-35.
         */
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                screenSlot,
                preferredHotbarSlot,
                SlotActionType.SWAP,
                mc.player
        );

        mc.player.getInventory().selectedSlot = preferredHotbarSlot;
        return true;
    }

    public static int countItem(MinecraftClient mc, Item item) {
        if (mc.player == null) {
            return 0;
        }

        int count = 0;

        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);

            if (!stack.isEmpty() && stack.isOf(item)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private static int findHotbarSlot(MinecraftClient mc, Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);

            if (!stack.isEmpty() && predicate.test(stack)) {
                return slot;
            }
        }

        return -1;
    }

    private static int findInventorySlot(MinecraftClient mc, Predicate<ItemStack> predicate) {
        if (mc.player == null) {
            return -1;
        }

        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);

            if (!stack.isEmpty() && predicate.test(stack)) {
                return slot;
            }
        }

        return -1;
    }

    private static int inventorySlotToScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot <= 8) {
            return 36 + inventorySlot;
        }

        if (inventorySlot >= 9 && inventorySlot <= 35) {
            return inventorySlot;
        }

        // Armor/offhand slots are intentionally ignored for farm tools/items.
        return -1;
    }

    private static int clampHotbar(int slot) {
        if (slot < 0) {
            return 0;
        }

        if (slot > 8) {
            return 8;
        }

        return slot;
    }

    private InventoryUtil() {
    }
}
