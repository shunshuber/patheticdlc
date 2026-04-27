package com.pathdlc.digger.farm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public final class FarmScanner {
    public static BlockPos closestBlock(MinecraftClient mc, int radius, Predicate<BlockPos> predicate) {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        BlockPos origin = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.add(x, y, z);

                    if (!predicate.test(pos)) {
                        continue;
                    }

                    double distance = origin.getSquaredDistance(pos);

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.toImmutable();
                    }
                }
            }
        }

        return best;
    }

    public static BlockPos closestGroundSpot(MinecraftClient mc, int radius, Predicate<BlockPos> groundPredicate) {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        BlockPos origin = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int y = -2; y <= 2; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos ground = origin.add(x, y, z);
                    BlockPos plant = ground.up();

                    if (!groundPredicate.test(ground)) {
                        continue;
                    }

                    if (!mc.world.getBlockState(plant).isAir()) {
                        continue;
                    }

                    if (!mc.world.getBlockState(plant.up()).isAir()) {
                        continue;
                    }

                    double distance = origin.getSquaredDistance(ground);

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = ground.toImmutable();
                    }
                }
            }
        }

        return best;
    }

    private FarmScanner() {
    }
}
