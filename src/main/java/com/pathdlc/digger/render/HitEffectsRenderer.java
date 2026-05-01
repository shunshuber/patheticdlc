package com.pathdlc.digger.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class HitEffectsRenderer {
    private static final Identifier HIT_STARS = Identifier.of("pathdlc_digger",
            "textures/gui/hit_stars.png");
    private static final int FRAME_COUNT = 8;
    private static final int FRAME_SIZE = 16;
    private static final int SHEET_WIDTH = 128;
    private static final int SHEET_HEIGHT = 16;

    private static final List<HitStar> stars = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static void spawnAt(double screenX, double screenY) {
        int count = 4 + RANDOM.nextInt(4);
        for (int i = 0; i < count; i++) {
            double vx = (RANDOM.nextDouble() - 0.5) * 6.0;
            double vy = (RANDOM.nextDouble() - 0.5) * 6.0 - 2.0;
            float scale = 0.8f + RANDOM.nextFloat() * 1.2f;
            stars.add(new HitStar(screenX + vx * 4, screenY + vy * 4, vx, vy, scale));
        }
    }

    public static void renderHud(DrawContext context) {
        if (stars.isEmpty()) return;

        Iterator<HitStar> it = stars.iterator();
        while (it.hasNext()) {
            HitStar s = it.next();
            s.tick();
            if (s.isDead()) {
                it.remove();
                continue;
            }

            int frame = Math.min(s.frame(), FRAME_COUNT - 1);
            float u = frame * FRAME_SIZE;
            int size = (int) (FRAME_SIZE * s.scale);
            int alpha = (int) (255 * s.alpha());

            if (alpha > 10 && size > 0) {
                context.drawTexture(
                        RenderLayer::getGuiTextured,
                        HIT_STARS,
                        (int) s.x - size / 2, (int) s.y - size / 2,
                        u, 0,
                        FRAME_SIZE, FRAME_SIZE,
                        SHEET_WIDTH, SHEET_HEIGHT
                );
            }
        }
    }

    public static boolean hasActiveEffects() {
        return !stars.isEmpty();
    }

    private static class HitStar {
        double x, y, vx, vy;
        float scale;
        int age;
        static final int MAX_AGE = 20;

        HitStar(double x, double y, double vx, double vy, float scale) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.scale = scale;
        }

        void tick() {
            x += vx;
            y += vy;
            vy += 0.3;
            vx *= 0.92;
            vy *= 0.92;
            age++;
        }

        boolean isDead() {
            return age >= MAX_AGE;
        }

        int frame() {
            return age * FRAME_COUNT / MAX_AGE;
        }

        float alpha() {
            return Math.max(0, 1.0f - (float) age / MAX_AGE);
        }
    }

    private HitEffectsRenderer() {}
}
