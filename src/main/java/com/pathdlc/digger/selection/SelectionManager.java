package com.pathdlc.digger.selection;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class SelectionManager {
    private BlockPos pos1;
    private BlockPos pos2;

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1 == null ? null : pos1.toImmutable();
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2 == null ? null : pos2.toImmutable();
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public BlockPos min() {
        if (!isComplete()) {
            return null;
        }

        return new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
    }

    public BlockPos max() {
        if (!isComplete()) {
            return null;
        }

        return new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
    }

    public int volume() {
        if (!isComplete()) {
            return 0;
        }

        BlockPos min = min();
        BlockPos max = max();

        return (max.getX() - min.getX() + 1)
                * (max.getY() - min.getY() + 1)
                * (max.getZ() - min.getZ() + 1);
    }

    public Box outlineBox() {
        if (!isComplete()) {
            return null;
        }

        BlockPos min = min();
        BlockPos max = max();

        return new Box(
                min.getX(),
                min.getY(),
                min.getZ(),
                max.getX() + 1.0,
                max.getY() + 1.0,
                max.getZ() + 1.0
        );
    }
}
