package com.pathdlc.digger.clan;

import com.pathdlc.digger.farm.FarmInteraction;
import com.pathdlc.digger.farm.InventoryUtil;
import com.pathdlc.digger.util.Chat;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ClanRedstoneBot {
    private boolean running;
    private int actions;
    private int failed;
    private BlockPos lastWirePos;

    public void start() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (!hasRequiredRedstone(mc)) {
            return;
        }

        running = true;
        actions = 0;
        failed = 0;
        lastWirePos = null;

        Chat.info("Clan redstone started. No artificial delay.");
    }

    public void stop() {
        running = false;
        lastWirePos = null;
        Chat.info("Clan redstone stopped.");
    }

    public void tick(MinecraftClient mc) {
        if (!running) {
            return;
        }

        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (!hasRequiredRedstone(mc)) {
            stop();
            return;
        }

        /*
         * Fast mode:
         * - if redstone wire is already under the player, break it immediately;
         * - otherwise place it immediately.
         * No custom timer/delay is used here.
         */
        BlockPos wirePos = lastWirePos != null ? lastWirePos : mc.player.getBlockPos();

        if (mc.world.getBlockState(wirePos).isOf(Blocks.REDSTONE_WIRE)) {
            FarmInteraction.breakBlock(mc, wirePos);
            actions++;
            lastWirePos = null;
            return;
        }

        BlockPos playerFeet = mc.player.getBlockPos();
        BlockPos support = playerFeet.down();

        if (!mc.world.getBlockState(playerFeet).isAir()) {
            failed++;
            return;
        }

        if (mc.world.getBlockState(support).isAir() || !mc.world.getBlockState(support).getFluidState().isEmpty()) {
            failed++;
            return;
        }

        if (!InventoryUtil.selectItem(mc, Items.REDSTONE, 4)) {
            Chat.error("Clan redstone needs redstone dust in inventory.");
            stop();
            return;
        }

        FarmInteraction.interactBlock(mc, support, Direction.UP, Items.REDSTONE);
        lastWirePos = playerFeet.toImmutable();
        actions++;
    }

    public boolean isRunning() {
        return running;
    }

    public String status() {
        return "clanRedstone running=" + running
                + ", actions=" + actions
                + ", failed=" + failed
                + ", mode=fast-no-delay";
    }

    private boolean hasRequiredRedstone(MinecraftClient mc) {
        if (mc == null || mc.player == null) {
            return false;
        }

        if (!InventoryUtil.hasItem(mc, Items.REDSTONE)) {
            Chat.error("Clan redstone needs redstone dust in inventory.");
            return false;
        }

        return true;
    }
}
