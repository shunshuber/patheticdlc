package com.pathdlc.digger.bot;

import com.pathdlc.digger.baritone.BaritoneBridge;
import com.pathdlc.digger.selection.SelectionManager;
import com.pathdlc.digger.util.Chat;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class DiggerBot {
    private static final double MINE_REACH = 4.65;
    private static final double PLACE_REACH = 4.65;

    /*
     * Several placements per tick make shell repair feel instant. 4 is conservative:
     * fast enough for border repair, but not so high that most servers instantly reject every click.
     */
    private static final int PLACE_BURST_PER_TICK = 4;
    private static final int PATH_COOLDOWN_TICKS = 34;
    private static final int MAX_PLACE_RETRIES = 10;

    private final SelectionManager selection;
    private final BaritoneBridge baritone;

    private final Queue<BlockTask> tasks = new ArrayDeque<>();
    private BlockTask currentTask;

    private boolean running;
    private boolean bulldozer2;
    private int taskTicks;
    private int completedTasks;
    private int failedTasks;
    private int pathCooldown;

    public DiggerBot(SelectionManager selection, BaritoneBridge baritone) {
        this.selection = selection;
        this.baritone = baritone;
    }

    public void startDigAndFill() {
        tasks.clear();
        tasks.addAll(TaskPlanner.planDig(selection, bulldozer2));
        tasks.addAll(TaskPlanner.planFillShell(selection, bulldozer2));
        start("Fast dig + fast shell repair planned. Tasks: " + tasks.size());
    }

    public void startDigOnly() {
        tasks.clear();
        tasks.addAll(TaskPlanner.planDig(selection, bulldozer2));
        start("Dig planned. Tasks: " + tasks.size());
    }

    public void startFillOnly() {
        tasks.clear();
        tasks.addAll(TaskPlanner.planFillShell(selection, bulldozer2));
        start("Fast shell repair planned. Tasks: " + tasks.size());
    }

    public void stop() {
        running = false;
        tasks.clear();
        currentTask = null;
        taskTicks = 0;
        pathCooldown = 0;
    }

    public void tick(MinecraftClient mc) {
        Chat.flush();

        if (!running) {
            return;
        }

        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (pathCooldown > 0) {
            pathCooldown--;
        }

        /*
         * Before doing slow path logic, place every reachable repair block near the player.
         * This removes the old behavior where the bot waited on one unreachable place task.
         */
        int placed = placeReachableBurst(mc);

        if (placed > 0 && currentTask != null && currentTask.type == BlockTask.Type.PLACE) {
            currentTask = null;
        }

        if (currentTask == null) {
            currentTask = pollNextSmartTask(mc);
            taskTicks = 0;

            if (currentTask == null) {
                running = false;
                baritone.cancel();
                Chat.info("Done. Completed: " + completedTasks + ", failed/skipped: " + failedTasks);
                return;
            }
        }

        taskTicks++;

        if (taskTicks > 520) {
            failedTasks++;
            requeueOrDrop(currentTask);
            currentTask = null;
            baritone.cancel();
            return;
        }

        if (currentTask.type == BlockTask.Type.PLACE) {
            tickPlaceTask(mc, currentTask);
        } else {
            tickMineTask(mc, currentTask);
        }
    }

    private void start(String message) {
        currentTask = null;
        running = true;
        completedTasks = 0;
        failedTasks = 0;
        taskTicks = 0;
        pathCooldown = 0;

        if (tasks.isEmpty()) {
            running = false;
            Chat.warn("No tasks generated. Check selection.");
            return;
        }

        Chat.info(message);
        Chat.info("Bulldozer 2 logic: " + (bulldozer2 ? "ON" : "OFF"));
        Chat.info("Baritone API: " + (baritone.isAvailable() ? "found" : "not found"));
    }

    private BlockTask pollNextSmartTask(MinecraftClient mc) {
        removeAlreadyDoneTasks(mc);

        BlockTask reachablePlace = removeReachablePlaceTask(mc);

        if (reachablePlace != null) {
            return reachablePlace;
        }

        BlockTask reachableMine = removeReachableMineTask(mc);

        if (reachableMine != null) {
            return reachableMine;
        }

        return removeNearestTask(mc);
    }

    private void removeAlreadyDoneTasks(MinecraftClient mc) {
        Iterator<BlockTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            BlockTask task = iterator.next();

            if (isTaskAlreadyDone(mc, task)) {
                failedTasks++;
                iterator.remove();
            }
        }
    }

    private boolean isTaskAlreadyDone(MinecraftClient mc, BlockTask task) {
        BlockState state = mc.world.getBlockState(task.pos);

        if (task.type == BlockTask.Type.MINE) {
            return state.isAir() || state.getBlock() == Blocks.BEDROCK;
        }

        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private BlockTask removeReachablePlaceTask(MinecraftClient mc) {
        Iterator<BlockTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            BlockTask task = iterator.next();

            if (task.type != BlockTask.Type.PLACE) {
                continue;
            }

            if (isInReach(mc, task.pos, PLACE_REACH) && findPlacementFace(mc, task.pos) != null) {
                iterator.remove();
                return task;
            }
        }

        return null;
    }

    private BlockTask removeReachableMineTask(MinecraftClient mc) {
        Iterator<BlockTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            BlockTask task = iterator.next();

            if (task.type != BlockTask.Type.MINE) {
                continue;
            }

            if (isInReach(mc, task.pos, MINE_REACH)) {
                iterator.remove();
                return task;
            }
        }

        return null;
    }

    private BlockTask removeNearestTask(MinecraftClient mc) {
        Iterator<BlockTask> iterator = tasks.iterator();
        BlockTask best = null;
        double bestDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            BlockTask task = iterator.next();
            double distance = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(task.pos));

            if (distance < bestDistance) {
                bestDistance = distance;
                best = task;
            }
        }

        if (best == null) {
            return null;
        }

        tasks.remove(best);
        return best;
    }

    private int placeReachableBurst(MinecraftClient mc) {
        int placed = 0;

        for (int i = 0; i < PLACE_BURST_PER_TICK; i++) {
            BlockTask task = removeReachablePlaceTask(mc);

            if (task == null) {
                break;
            }

            if (tryPlace(mc, task)) {
                placed++;
            } else {
                requeueOrDrop(task);
            }
        }

        return placed;
    }

    private void tickMineTask(MinecraftClient mc, BlockTask task) {
        BlockState state = mc.world.getBlockState(task.pos);

        if (state.isAir() || state.getBlock() == Blocks.BEDROCK) {
            completedTasks++;
            currentTask = null;
            return;
        }

        if (!isInReach(mc, task.pos, MINE_REACH)) {
            requestPathTo(task.pos);
            return;
        }

        baritone.cancel();
        lookAt(mc, Vec3d.ofCenter(task.pos));

        Direction side = bestSide(mc, task.pos);
        mc.interactionManager.updateBlockBreakingProgress(task.pos, side);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void tickPlaceTask(MinecraftClient mc, BlockTask task) {
        if (isTaskAlreadyDone(mc, task)) {
            completedTasks++;
            currentTask = null;
            return;
        }

        if (!selectFillBlock(mc)) {
            Chat.error("No cobblestone/stone in hotbar. Put blocks in hotbar and run .fill again.");
            stop();
            return;
        }

        if (!isInReach(mc, task.pos, PLACE_REACH)) {
            requestPathTo(findNearbyGoal(task.pos));
            return;
        }

        if (tryPlace(mc, task)) {
            currentTask = null;
            return;
        }

        // No face or server did not accept yet. Do not wait on it forever.
        requeueOrDrop(task);
        currentTask = null;
    }

    private boolean tryPlace(MinecraftClient mc, BlockTask task) {
        if (!selectFillBlock(mc)) {
            return false;
        }

        if (isTaskAlreadyDone(mc, task)) {
            completedTasks++;
            return true;
        }

        PlacementFace face = findPlacementFace(mc, task.pos);

        if (face == null) {
            return false;
        }

        Vec3d hitVec = Vec3d.ofCenter(face.neighbor).add(Vec3d.of(face.side.getVector()).multiply(0.5));
        lookAt(mc, hitVec);

        BlockHitResult hit = new BlockHitResult(hitVec, face.side, face.neighbor, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        completedTasks++;
        return true;
    }

    private void requestPathTo(BlockPos pos) {
        if (pathCooldown > 0 || pos == null) {
            return;
        }

        baritone.gotoBlock(pos);
        pathCooldown = PATH_COOLDOWN_TICKS;
    }

    private void requeueOrDrop(BlockTask task) {
        if (task == null) {
            return;
        }

        task.retries++;

        if (task.type == BlockTask.Type.PLACE && task.retries <= MAX_PLACE_RETRIES) {
            tasks.add(task);
            return;
        }

        failedTasks++;
    }

    private BlockPos findNearbyGoal(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null) {
            return pos;
        }

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos candidate = pos.offset(direction);
            BlockPos feet = candidate;
            BlockPos head = candidate.up();

            if (mc.world.getBlockState(feet).isAir() && mc.world.getBlockState(head).isAir()) {
                double dist = mc.player == null ? 0.0 : mc.player.getBlockPos().getSquaredDistance(candidate);

                if (dist < bestDist) {
                    bestDist = dist;
                    best = candidate;
                }
            }
        }

        if (best != null) {
            return best;
        }

        BlockPos above = pos.up();

        if (mc.world.getBlockState(above).isAir()) {
            return above;
        }

        return pos;
    }

    private boolean isInReach(MinecraftClient mc, BlockPos pos, double reach) {
        return mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= reach * reach;
    }

    private boolean selectFillBlock(MinecraftClient mc) {
        int slot = findHotbarBlock(mc, Items.COBBLESTONE);

        if (slot < 0) {
            slot = findHotbarBlock(mc, Items.STONE);
        }

        if (slot < 0) {
            return false;
        }

        mc.player.getInventory().selectedSlot = slot;
        return true;
    }

    private int findHotbarBlock(MinecraftClient mc, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);

            if (!stack.isEmpty() && stack.isOf(item)) {
                return slot;
            }
        }

        return -1;
    }

    private PlacementFace findPlacementFace(MinecraftClient mc, BlockPos target) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = target.offset(direction);
            BlockState neighborState = mc.world.getBlockState(neighbor);

            if (!neighborState.isAir() && neighborState.getFluidState().isEmpty()) {
                return new PlacementFace(neighbor, direction.getOpposite());
            }
        }

        return null;
    }

    private Direction bestSide(MinecraftClient mc, BlockPos pos) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d diff = center.subtract(eye);

        return Direction.getFacing(diff.x, diff.y, diff.z).getOpposite();
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

    public boolean isBulldozer2() {
        return bulldozer2;
    }

    public void setBulldozer2(boolean bulldozer2) {
        this.bulldozer2 = bulldozer2;
    }

    public String statusLine() {
        return "running=" + running
                + ", queued=" + tasks.size()
                + ", completed=" + completedTasks
                + ", failed/skipped=" + failedTasks
                + ", bulldozer2=" + bulldozer2
                + ", current=" + (currentTask == null ? "none" : currentTask.type + " " + currentTask.pos.toShortString() + " retries=" + currentTask.retries);
    }

    private record PlacementFace(BlockPos neighbor, Direction side) {
    }
}
