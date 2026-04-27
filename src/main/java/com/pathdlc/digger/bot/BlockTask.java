package com.pathdlc.digger.bot;

import net.minecraft.util.math.BlockPos;

public class BlockTask {
    public enum Type {
        MINE,
        PLACE
    }

    public final Type type;
    public final BlockPos pos;

    public boolean pathRequested;
    public int retries;

    public BlockTask(Type type, BlockPos pos) {
        this.type = type;
        this.pos = pos.toImmutable();
        this.pathRequested = false;
        this.retries = 0;
    }
}
