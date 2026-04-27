package com.pathdlc.digger.command;

import com.pathdlc.digger.baritone.BaritoneBridge;
import com.pathdlc.digger.bot.DiggerBot;
import com.pathdlc.digger.selection.SelectionManager;
import com.pathdlc.digger.util.Chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

public class DotCommandHandler {
    private final SelectionManager selection;
    private final DiggerBot digger;
    private final BaritoneBridge baritone;

    public DotCommandHandler(SelectionManager selection, DiggerBot digger, BaritoneBridge baritone) {
        this.selection = selection;
        this.digger = digger;
        this.baritone = baritone;
    }

    public boolean handle(String rawMessage) {
        String message = rawMessage.trim();

        if (!message.startsWith(".")) {
            return false;
        }

        String[] args = message.substring(1).trim().split("\\s+");

        if (args.length == 0 || args[0].isBlank()) {
            return false;
        }

        String root = args[0].toLowerCase(Locale.ROOT);

        return switch (root) {
            case "pos" -> handlePos(args);
            case "fill" -> handleFill();
            case "dig" -> handleDig(args);
            default -> false;
        };
    }

    private boolean handlePos(String[] args) {
        if (args.length < 2) {
            Chat.warn("Use: .pos 1 / .pos 2 / .pos 1 look / .pos clear");
            return true;
        }

        String side = args[1].toLowerCase(Locale.ROOT);

        if (side.equals("clear")) {
            selection.clear();
            Chat.info("Selection cleared.");
            return true;
        }

        BlockPos pos = getCommandPos(args);

        if (pos == null) {
            Chat.error("Cannot set position. Join a world first.");
            return true;
        }

        if (side.equals("1")) {
            selection.setPos1(pos);
            Chat.info("pos1 = " + format(pos));
            printSelectionInfo();
            return true;
        }

        if (side.equals("2")) {
            selection.setPos2(pos);
            Chat.info("pos2 = " + format(pos));
            printSelectionInfo();
            return true;
        }

        Chat.warn("Use: .pos 1 / .pos 2 / .pos 1 look / .pos clear");
        return true;
    }

    private boolean handleFill() {
        if (!selection.isComplete()) {
            Chat.error("Set both positions first: .pos 1 and .pos 2");
            return true;
        }

        digger.startFillOnly();
        return true;
    }

    private boolean handleDig(String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("baritone")) {
            if (!selection.isComplete()) {
                Chat.error("Set both positions first.");
                return true;
            }

            if (baritone.clearArea(selection.min(), selection.max())) {
                Chat.info("Started Baritone BuilderProcess.clearArea for selected area.");
            } else {
                Chat.error("Baritone API clearArea failed. Is Baritone installed and loaded?");
            }

            return true;
        }

        Chat.warn("Only command left here is: .dig baritone");
        return true;
    }

    private BlockPos getCommandPos(String[] args) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            return null;
        }

        if (args.length >= 3 && args[2].equalsIgnoreCase("look")) {
            HitResult hit = mc.crosshairTarget;

            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                return blockHit.getBlockPos().toImmutable();
            }
        }

        return mc.player.getBlockPos().toImmutable();
    }

    private void printSelectionInfo() {
        if (!selection.isComplete()) {
            return;
        }

        Chat.info("Selection ready. Volume: " + selection.volume() + " blocks.");
    }

    private String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
