package com.pathdlc.digger.warden;

import com.pathdlc.digger.baritone.BaritoneBridge;
import com.pathdlc.digger.util.Chat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoWardenBot {
    private static final double OPEN_REACH = 4.65;
    private static final int DEFAULT_RADIUS = 48;
    private static final int DEFAULT_VERTICAL_RADIUS = 16;
    private static final int SCAN_EVERY_TICKS = 18;
    private static final int PATH_EVERY_TICKS = 45;
    private static final int CHEST_OPEN_WAIT_TICKS = 10;
    private static final int LOOT_BURST_PER_TICK = 18;

    private final BaritoneBridge baritone;

    private boolean running;
    private int radius = DEFAULT_RADIUS;
    private int verticalRadius = DEFAULT_VERTICAL_RADIUS;

    private int scanCooldown;
    private int pathCooldown;
    private int openWaitTicks;
    private int lootedChests;
    private int failed;

    private WardenChestTarget target;

    public AutoWardenBot(BaritoneBridge baritone) {
        this.baritone = baritone;
    }

    public void start() {
        running = true;
        scanCooldown = 0;
        pathCooldown = 0;
        openWaitTicks = 0;
        lootedChests = 0;
        failed = 0;
        target = null;

        Chat.info("AutoWarden started. Radius=" + radius + ", vertical=" + verticalRadius);
    }

    public void stop() {
        running = false;
        target = null;
        baritone.cancel();
        Chat.info("AutoWarden stopped.");
    }

    public void tick(MinecraftClient mc) {
        if (!running) {
            return;
        }

        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (scanCooldown > 0) {
            scanCooldown--;
        }

        if (pathCooldown > 0) {
            pathCooldown--;
        }

        if (openWaitTicks > 0) {
            openWaitTicks--;
            return;
        }

        if (isContainerOpen(mc)) {
            lootOpenContainer(mc);
            return;
        }

        if (target == null || scanCooldown <= 0 || !isChestStillValid(mc, target.pos())) {
            target = scanBestChest(mc);
            scanCooldown = SCAN_EVERY_TICKS;
        }

        if (target == null) {
            return;
        }

        if (!inReach(mc, target.pos(), OPEN_REACH)) {
            requestPath(target.pos());
            return;
        }

        baritone.cancel();

        /*
         * If a timer is visible and still not ready, wait at the best-timer chest.
         * Unknown timers are treated as openable.
         */
        if (target.hasTimer() && target.timerSeconds() > 1.25) {
            return;
        }

        openChest(mc, target.pos());
    }

    private WardenChestTarget scanBestChest(MinecraftClient mc) {
        BlockPos origin = mc.player.getBlockPos();
        List<WardenChestTarget> targets = new ArrayList<>();

        int minY = Math.max(mc.world.getBottomY(), origin.getY() - verticalRadius);
        int maxY = Math.min(mc.world.getTopYInclusive(), origin.getY() + verticalRadius);

        for (int y = minY; y <= maxY; y++) {
            for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);

                    if (!isLootChestBlock(state.getBlock())) {
                        continue;
                    }

                    double timer = readTimerNear(mc, pos);
                    boolean hasTimer = timer >= 0.0;
                    double distance = origin.getSquaredDistance(pos);

                    /*
                     * Timer is the main priority. Distance only breaks ties.
                     * Chests without visible timers are lower priority.
                     */
                    double score = (hasTimer ? timer : 9999.0) * 100000.0 + distance;
                    targets.add(new WardenChestTarget(pos.toImmutable(), timer, hasTimer, score));
                }
            }
        }

        return targets.stream()
                .min(Comparator.comparingDouble(WardenChestTarget::score))
                .orElse(null);
    }

    private double readTimerNear(MinecraftClient mc, BlockPos chestPos) {
        double best = -1.0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity) && !entity.hasCustomName()) {
                continue;
            }

            if (entity.squaredDistanceTo(Vec3d.ofCenter(chestPos)) > 25.0) {
                continue;
            }

            String text = entity.getDisplayName().getString();
            double parsed = WardenTimerParser.parseSeconds(text);

            if (parsed < 0.0) {
                continue;
            }

            if (best < 0.0 || parsed < best) {
                best = parsed;
            }
        }

        return best;
    }

    private boolean isLootChestBlock(Block block) {
        return block == Blocks.CHEST
                || block == Blocks.TRAPPED_CHEST
                || block == Blocks.BARREL;
    }

    private boolean isChestStillValid(MinecraftClient mc, BlockPos pos) {
        return pos != null && isLootChestBlock(mc.world.getBlockState(pos).getBlock());
    }

    private void requestPath(BlockPos pos) {
        if (pathCooldown > 0) {
            return;
        }

        baritone.gotoBlock(pos);
        pathCooldown = PATH_EVERY_TICKS;
    }

    private void openChest(MinecraftClient mc, BlockPos pos) {
        Vec3d hitVec = Vec3d.ofCenter(pos);
        lookAt(mc, hitVec);

        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        openWaitTicks = CHEST_OPEN_WAIT_TICKS;
    }

    private boolean isContainerOpen(MinecraftClient mc) {
        if (mc.player == null) {
            return false;
        }

        return mc.player.currentScreenHandler != mc.player.playerScreenHandler;
    }

    private void lootOpenContainer(MinecraftClient mc) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        int totalSlots = handler.slots.size();
        int containerSlots = Math.max(0, totalSlots - 36);

        int moved = 0;

        for (int slot = 0; slot < containerSlots && moved < LOOT_BURST_PER_TICK; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();

            if (stack.isEmpty()) {
                continue;
            }

            mc.interactionManager.clickSlot(
                    handler.syncId,
                    slot,
                    0,
                    SlotActionType.QUICK_MOVE,
                    mc.player
            );

            moved++;
        }

        if (moved == 0) {
            mc.player.closeHandledScreen();
            lootedChests++;
            target = null;
            scanCooldown = 0;
        }
    }

    private boolean inReach(MinecraftClient mc, BlockPos pos, double reach) {
        return mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= reach * reach;
    }

    private void lookAt(MinecraftClient mc, Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;

        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    public boolean isRunning() {
        return running;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = Math.max(8, Math.min(96, radius));
        Chat.info("AutoWarden radius set to " + this.radius);
    }

    public int getVerticalRadius() {
        return verticalRadius;
    }

    public void setVerticalRadius(int verticalRadius) {
        this.verticalRadius = Math.max(4, Math.min(32, verticalRadius));
        Chat.info("AutoWarden vertical radius set to " + this.verticalRadius);
    }

    public String status() {
        return "autoWarden running=" + running
                + ", radius=" + radius
                + ", vertical=" + verticalRadius
                + ", target=" + (target == null ? "none" : target.pos().toShortString() + " timer=" + (target.hasTimer() ? target.timerSeconds() : "unknown"))
                + ", looted=" + lootedChests
                + ", failed=" + failed;
    }
}
