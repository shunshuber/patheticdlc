package com.pathdlc.digger.farm;

import com.pathdlc.digger.baritone.BaritoneBridge;
import com.pathdlc.digger.util.Chat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public class AutoAppleFarm extends AbstractFarmBot {
    /*
     * AutoApple intentionally does not chase high leaves/logs anymore.
     * It only breaks blocks that are actually reachable from the current player position.
     */
    private static final double TREE_REACH = 4.55;
    private static final int TREE_SCAN_HORIZONTAL = 4;
    private static final int TREE_SCAN_UP = 9;

    private boolean bonemeal = true;
    private BlockPos farmGround;
    private BlockPos currentBreakTarget;

    private final Set<BlockPos> skippedThisCycle = new HashSet<>();

    public AutoAppleFarm(BaritoneBridge baritone) {
        super(baritone);
    }

    @Override
    public String name() {
        return "AutoApple";
    }

    @Override
    public void start() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player != null && farmGround == null) {
            farmGround = mc.player.getBlockPos().down().toImmutable();
            Chat.info("AutoApple spot auto-set to block under you: " + shortPos(farmGround));
        }

        if (!validateRequiredItems(mc)) {
            running = false;
            return;
        }

        if (!validateFarmSpot(mc)) {
            running = false;
            return;
        }

        resetTreeState();
        super.start();
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (!running || !worldReady(mc)) {
            return;
        }

        cooldownTick();

        if (waitTicks > 0) {
            return;
        }

        if (!validateRequiredItems(mc)) {
            stop();
            return;
        }

        if (!validateFarmSpot(mc)) {
            stop();
            return;
        }

        BlockPos saplingPos = farmGround.up();

        if (breakReachableTreeBlocks(mc)) {
            return;
        }

        BlockState plantState = mc.world.getBlockState(saplingPos);

        if (plantState.isOf(Blocks.OAK_SAPLING)) {
            if (bonemeal) {
                growSapling(mc, saplingPos);
                return;
            }

            waitTicks = 20;
            return;
        }

        if (plantState.isAir()) {
            /*
             * Remaining high leaves are ignored. Start a new cycle in the same spot.
             */
            resetTreeState();
            plantSapling(mc);
            return;
        }

        if (isOakLog(plantState) || isOakLeaf(plantState)) {
            /*
             * If the trunk/leaf is exactly in the planting cell but not reachable,
             * move back to the farm spot. Otherwise do not chase high blocks.
             */
            if (!FarmInteraction.inReach(mc, saplingPos, TREE_REACH)) {
                gotoIfNeeded(farmGround);
            }

            waitTicks = 8;
            return;
        }

        Chat.warn("AutoApple spot is blocked at " + shortPos(saplingPos) + ". Clear it or set another spot with .apple set.");
        waitTicks = 60;
    }

    public void setSpotAtPlayer() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            Chat.error("Join a world first.");
            return;
        }

        farmGround = mc.player.getBlockPos().down().toImmutable();
        resetTreeState();
        Chat.info("AutoApple spot = " + shortPos(farmGround) + " / plant at " + shortPos(farmGround.up()));
    }

    public void setSpotAtLook() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.crosshairTarget == null) {
            Chat.error("Join a world first.");
            return;
        }

        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            Chat.warn("Look at a block first.");
            return;
        }

        farmGround = hit.getBlockPos().toImmutable();
        resetTreeState();
        Chat.info("AutoApple spot = " + shortPos(farmGround) + " / plant at " + shortPos(farmGround.up()));
    }

    public void clearSpot() {
        farmGround = null;
        resetTreeState();
        Chat.info("AutoApple spot cleared.");
    }

    public BlockPos getFarmGround() {
        return farmGround;
    }

    private boolean breakReachableTreeBlocks(MinecraftClient mc) {
        if (currentBreakTarget == null
                || mc.world.getBlockState(currentBreakTarget).isAir()
                || skippedThisCycle.contains(currentBreakTarget)
                || !FarmInteraction.inReach(mc, currentBreakTarget, TREE_REACH)) {
            currentBreakTarget = findReachableTreeTarget(mc);
        }

        if (currentBreakTarget == null) {
            return false;
        }

        BlockState targetState = mc.world.getBlockState(currentBreakTarget);
        boolean leaf = isOakLeaf(targetState);
        boolean log = isOakLog(targetState);

        if (!leaf && !log) {
            markSkipped(currentBreakTarget);
            currentBreakTarget = null;
            return true;
        }

        if (!FarmInteraction.inReach(mc, currentBreakTarget, TREE_REACH)) {
            /*
             * Critical fix: do not try to path/chase high leaves. Skip immediately
             * and continue farming the same spot.
             */
            markSkipped(currentBreakTarget);
            currentBreakTarget = null;
            return true;
        }

        baritone.cancel();

        if (log) {
            if (!InventoryUtil.selectMatching(mc, stack -> stack.getItem() instanceof AxeItem, 0)) {
                Chat.error("AutoApple needs an axe in inventory.");
                stop();
                return true;
            }
        } else {
            if (!InventoryUtil.selectMatching(mc, stack -> stack.getItem() instanceof HoeItem, 1)) {
                Chat.error("AutoApple needs a hoe in inventory.");
                stop();
                return true;
            }
        }

        boolean done = FarmInteraction.breakBlock(mc, currentBreakTarget);

        if (done || mc.world.getBlockState(currentBreakTarget).isAir()) {
            actions++;
            currentBreakTarget = null;
        }

        return true;
    }

    private void growSapling(MinecraftClient mc, BlockPos saplingPos) {
        if (!InventoryUtil.selectItem(mc, Items.BONE_MEAL, 3)) {
            Chat.error("AutoApple needs bone meal in inventory.");
            stop();
            return;
        }

        if (!FarmInteraction.inReach(mc, saplingPos, 4.6)) {
            gotoIfNeeded(saplingPos);
            return;
        }

        baritone.cancel();

        for (int i = 0; i < 4; i++) {
            FarmInteraction.interactBlock(mc, saplingPos, Direction.UP, Items.BONE_MEAL);
        }

        actions++;
        waitTicks = 5;
    }

    private void plantSapling(MinecraftClient mc) {
        if (!InventoryUtil.selectItem(mc, Items.OAK_SAPLING, 2)) {
            Chat.error("AutoApple needs oak saplings in inventory.");
            stop();
            return;
        }

        if (!FarmInteraction.inReach(mc, farmGround, 4.6)) {
            gotoIfNeeded(farmGround);
            return;
        }

        baritone.cancel();
        FarmInteraction.interactBlock(mc, farmGround, Direction.UP, Items.OAK_SAPLING);
        actions++;
        waitTicks = 8;
    }

    private BlockPos findReachableTreeTarget(MinecraftClient mc) {
        if (farmGround == null || mc.world == null || mc.player == null) {
            return null;
        }

        BlockPos plant = farmGround.up();

        BlockPos log = scanReachableAroundSpot(mc, plant, true);

        if (log != null) {
            return log;
        }

        return scanReachableAroundSpot(mc, plant, false);
    }

    private BlockPos scanReachableAroundSpot(MinecraftClient mc, BlockPos plant, boolean logsOnly) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int y = 0; y <= TREE_SCAN_UP; y++) {
            for (int x = -TREE_SCAN_HORIZONTAL; x <= TREE_SCAN_HORIZONTAL; x++) {
                for (int z = -TREE_SCAN_HORIZONTAL; z <= TREE_SCAN_HORIZONTAL; z++) {
                    BlockPos pos = plant.add(x, y, z).toImmutable();

                    if (skippedThisCycle.contains(pos)) {
                        continue;
                    }

                    if (!FarmInteraction.inReach(mc, pos, TREE_REACH)) {
                        continue;
                    }

                    BlockState state = mc.world.getBlockState(pos);
                    boolean match = logsOnly ? isOakLog(state) : isOakLeaf(state);

                    if (!match) {
                        continue;
                    }

                    /*
                     * Prefer lower blocks. High leaves usually cause unreachable loops,
                     * so reachable low blocks are processed first.
                     */
                    double heightPenalty = y * 3.0;
                    double distance = mc.player.getEyePos().squaredDistanceTo(net.minecraft.util.math.Vec3d.ofCenter(pos));
                    double score = distance + heightPenalty;

                    if (score < bestScore) {
                        bestScore = score;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private void markSkipped(BlockPos pos) {
        if (pos == null) {
            return;
        }

        skippedThisCycle.add(pos.toImmutable());
        failed++;
    }

    private boolean validateRequiredItems(MinecraftClient mc) {
        if (!worldReady(mc)) {
            return false;
        }

        if (!InventoryUtil.hasMatching(mc, stack -> stack.getItem() instanceof AxeItem)) {
            Chat.error("AutoApple will not start: axe missing in inventory.");
            return false;
        }

        if (!InventoryUtil.hasMatching(mc, stack -> stack.getItem() instanceof HoeItem)) {
            Chat.error("AutoApple will not start: hoe missing in inventory.");
            return false;
        }

        if (!InventoryUtil.hasItem(mc, Items.OAK_SAPLING)) {
            Chat.error("AutoApple will not start: oak sapling missing in inventory.");
            return false;
        }

        if (bonemeal && !InventoryUtil.hasItem(mc, Items.BONE_MEAL)) {
            Chat.error("AutoApple will not start: bone meal missing in inventory.");
            return false;
        }

        return true;
    }

    private boolean validateFarmSpot(MinecraftClient mc) {
        if (farmGround == null) {
            Chat.error("AutoApple spot is not set. Use .apple set or stand on the spot and run .apple start.");
            return false;
        }

        if (mc.world == null) {
            return false;
        }

        BlockState ground = mc.world.getBlockState(farmGround);

        if (!isValidSaplingGround(ground)) {
            Chat.error("AutoApple spot ground is invalid: " + shortPos(farmGround) + ". Use dirt/grass/podzol/coarse dirt.");
            return false;
        }

        return true;
    }

    private boolean isValidSaplingGround(BlockState state) {
        Block block = state.getBlock();

        return block == Blocks.GRASS_BLOCK
                || block == Blocks.DIRT
                || block == Blocks.COARSE_DIRT
                || block == Blocks.PODZOL
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.MOSS_BLOCK;
    }

    private boolean isOakLog(BlockState state) {
        Block block = state.getBlock();

        return block == Blocks.OAK_LOG
                || block == Blocks.OAK_WOOD
                || block == Blocks.STRIPPED_OAK_LOG
                || block == Blocks.STRIPPED_OAK_WOOD;
    }

    private boolean isOakLeaf(BlockState state) {
        return state.isOf(Blocks.OAK_LEAVES);
    }

    private void resetTreeState() {
        currentBreakTarget = null;
        skippedThisCycle.clear();
    }

    public boolean isBonemeal() {
        return bonemeal;
    }

    public void setBonemeal(boolean bonemeal) {
        this.bonemeal = bonemeal;
    }

    @Override
    public String status() {
        return super.status()
                + ", bonemeal=" + bonemeal
                + ", spot=" + (farmGround == null ? "not set" : shortPos(farmGround))
                + ", skipped=" + skippedThisCycle.size()
                + ", saplings=" + InventoryUtil.countItem(MinecraftClient.getInstance(), Items.OAK_SAPLING)
                + ", bonemealCount=" + InventoryUtil.countItem(MinecraftClient.getInstance(), Items.BONE_MEAL);
    }

    private String shortPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
