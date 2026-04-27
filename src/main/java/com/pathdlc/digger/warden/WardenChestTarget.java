package com.pathdlc.digger.warden;

import net.minecraft.util.math.BlockPos;

public record WardenChestTarget(BlockPos pos, double timerSeconds, boolean hasTimer, double score) {
}
