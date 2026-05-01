package com.pathdlc.digger.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public final class BlockESPRenderer {
    private static final int SCAN_RADIUS = 32;
    private static final int SCAN_VERTICAL = 16;
    private static final int SCAN_COOLDOWN = 10;

    private static final Set<Block> INTERESTING_BLOCKS = Set.of(
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.ANCIENT_DEBRIS,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST,
            Blocks.SPAWNER,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE
    );

    private static BlockPos[] cachedPositions = new BlockPos[0];
    private static int tickCounter;

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        if (tickCounter >= SCAN_COOLDOWN) {
            tickCounter = 0;
            cachedPositions = scan(mc);
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null || context.camera() == null) return;

        Vec3d camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        for (BlockPos pos : cachedPositions) {
            BlockState state = mc.world.getBlockState(pos);
            float[] color = colorFor(state.getBlock());
            Box box = new Box(pos).expand(0.002);

            VertexRendering.drawBox(
                    matrices,
                    consumers.getBuffer(RenderLayer.getLines()),
                    box,
                    color[0], color[1], color[2], 0.7f
            );
        }

        matrices.pop();
    }

    private static BlockPos[] scan(MinecraftClient mc) {
        BlockPos origin = mc.player.getBlockPos();
        int minY = Math.max(mc.world.getBottomY(), origin.getY() - SCAN_VERTICAL);
        int maxY = Math.min(mc.world.getTopYInclusive(), origin.getY() + SCAN_VERTICAL);

        BlockPos[] buffer = new BlockPos[512];
        int count = 0;

        for (int y = minY; y <= maxY && count < buffer.length; y++) {
            for (int x = origin.getX() - SCAN_RADIUS; x <= origin.getX() + SCAN_RADIUS && count < buffer.length; x++) {
                for (int z = origin.getZ() - SCAN_RADIUS; z <= origin.getZ() + SCAN_RADIUS && count < buffer.length; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (INTERESTING_BLOCKS.contains(mc.world.getBlockState(pos).getBlock())) {
                        buffer[count++] = pos.toImmutable();
                    }
                }
            }
        }

        BlockPos[] result = new BlockPos[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }

    private static float[] colorFor(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)
            return new float[]{0.2f, 0.9f, 0.95f};
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)
            return new float[]{0.1f, 0.95f, 0.3f};
        if (block == Blocks.ANCIENT_DEBRIS)
            return new float[]{0.6f, 0.3f, 0.15f};
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST)
            return new float[]{1.0f, 0.85f, 0.2f};
        if (block == Blocks.SPAWNER)
            return new float[]{0.9f, 0.2f, 0.2f};
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE)
            return new float[]{1.0f, 0.85f, 0.0f};
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)
            return new float[]{0.85f, 0.7f, 0.6f};
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE)
            return new float[]{0.15f, 0.2f, 0.9f};
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE)
            return new float[]{0.9f, 0.1f, 0.1f};
        return new float[]{1.0f, 1.0f, 1.0f};
    }

    private BlockESPRenderer() {}
}
