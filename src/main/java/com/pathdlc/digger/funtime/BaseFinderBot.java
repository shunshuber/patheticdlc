package com.pathdlc.digger.funtime;

import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.util.Chat;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BaseFinderBot {
    private boolean running;
    private int scanCooldown;
    private final Set<BlockPos> reportedPositions = new HashSet<>();

    private static final Block[] BASE_INDICATORS = {
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.ENDER_CHEST,
            Blocks.BARREL,
            Blocks.FURNACE,
            Blocks.BLAST_FURNACE,
            Blocks.SMOKER,
            Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE,
            Blocks.ANVIL,
            Blocks.BEACON,
            Blocks.SHULKER_BOX,
            Blocks.WHITE_SHULKER_BOX,
            Blocks.ORANGE_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX,
            Blocks.LIGHT_BLUE_SHULKER_BOX,
            Blocks.YELLOW_SHULKER_BOX,
            Blocks.LIME_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX,
            Blocks.GRAY_SHULKER_BOX,
            Blocks.LIGHT_GRAY_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX,
            Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX,
            Blocks.BROWN_SHULKER_BOX,
            Blocks.GREEN_SHULKER_BOX,
            Blocks.RED_SHULKER_BOX,
            Blocks.BLACK_SHULKER_BOX,
            Blocks.WHITE_BED,
            Blocks.CRAFTING_TABLE,
            Blocks.HOPPER,
    };

    public void start() {
        running = true;
        scanCooldown = 0;
        reportedPositions.clear();
        Chat.info("BaseFinder started - scanning for player bases...");
    }

    public void stop() {
        running = false;
        reportedPositions.clear();
        Chat.info("BaseFinder stopped");
    }

    public void tick(MinecraftClient client) {
        if (!running) return;
        if (!ModuleManager.isEnabled("BaseFinder")) {
            stop();
            return;
        }
        if (client.player == null || client.world == null) return;

        scanCooldown++;
        if (scanCooldown < 40) return;
        scanCooldown = 0;

        Module mod = ModuleManager.get("BaseFinder");
        if (mod == null) return;

        ModuleSetting radiusSetting = mod.getSetting("Radius");
        int radius = radiusSetting != null ? (int) radiusSetting.getFloat() : 64;

        scanArea(client, radius);
    }

    private void scanArea(MinecraftClient client, int radius) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return;

        BlockPos center = player.getBlockPos();
        List<BlockPos> found = new ArrayList<>();

        for (int x = -radius; x <= radius; x += 2) {
            for (int z = -radius; z <= radius; z += 2) {
                for (int y = world.getBottomY(); y < world.getTopYInclusive(); y += 2) {
                    BlockPos pos = center.add(x, y - center.getY(), z);

                    if (!world.isChunkLoaded(pos)) continue;

                    Block block = world.getBlockState(pos).getBlock();

                    for (Block indicator : BASE_INDICATORS) {
                        if (block == indicator) {
                            BlockPos chunkPos = new BlockPos(
                                    pos.getX() >> 4 << 4,
                                    pos.getY(),
                                    pos.getZ() >> 4 << 4);
                            if (!reportedPositions.contains(chunkPos)) {
                                found.add(pos);
                                reportedPositions.add(chunkPos);
                            }
                            break;
                        }
                    }
                }
            }
        }

        for (BlockPos pos : found) {
            Block block = world.getBlockState(pos).getBlock();
            String blockName = block.getTranslationKey()
                    .replace("block.minecraft.", "");
            int dist = (int) Math.sqrt(player.getBlockPos().getSquaredDistance(pos));
            Chat.info("Base found: " + blockName + " at "
                    + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                    + " (" + dist + "m)");
        }
    }

    public boolean isRunning() {
        return running;
    }
}
