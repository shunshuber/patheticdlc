package com.pathdlc.digger.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Queue;

public final class Chat {
    private static final Queue<String> PENDING = new ArrayDeque<>();

    public static void info(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            later(message);
            return;
        }

        mc.player.sendMessage(Text.literal("§f[§bPathDLC§f] §7" + message), false);
    }

    public static void warn(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            later(message);
            return;
        }

        mc.player.sendMessage(Text.literal("§f[§bPathDLC§f] §e" + message), false);
    }

    public static void error(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            later(message);
            return;
        }

        mc.player.sendMessage(Text.literal("§f[§bPathDLC§f] §c" + message), false);
    }

    public static void later(String message) {
        PENDING.add(message);
    }

    public static void flush() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) {
            return;
        }

        while (!PENDING.isEmpty()) {
            info(PENDING.poll());
        }
    }

    private Chat() {
    }
}
