package com.pathdlc.digger.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class HitEffectsRenderer {
    private static final Identifier HIT_STAR = Identifier.of("pathdlc_digger",
            "textures/gui/hit_star.png");
    private static final int TEX_SIZE = 32;

    private static final List<HitStar> stars = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static void spawnAt(double screenX, double screenY) {
        int count = 5 + RANDOM.nextInt(4);
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double speed = 2.0 + RANDOM.nextDouble() * 5.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 1.5;
            float scale = 0.4f + RANDOM.nextFloat() * 0.8f;
            float rotation = RANDOM.nextFloat() * 360f;
            float rotSpeed = (RANDOM.nextFloat() - 0.5f) * 15f;
            stars.add(new HitStar(
                    screenX + (RANDOM.nextDouble() - 0.5) * 20,
                    screenY + (RANDOM.nextDouble() - 0.5) * 20,
                    vx, vy, scale, rotation, rotSpeed));
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

            int drawSize = (int) (TEX_SIZE * s.scale);
            if (drawSize < 2) continue;

            int x = (int) s.x - drawSize / 2;
            int y = (int) s.y - drawSize / 2;

            context.getMatrices().push();
            context.getMatrices().translate(s.x, s.y, 0);
            context.getMatrices().scale(s.scale * s.alpha(), s.scale * s.alpha(), 1f);
            context.getMatrices().translate(-s.x, -s.y, 0);

            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    HIT_STAR,
                    (int) s.x - TEX_SIZE / 2, (int) s.y - TEX_SIZE / 2,
                    0, 0,
                    TEX_SIZE, TEX_SIZE,
                    TEX_SIZE, TEX_SIZE
            );

            context.getMatrices().pop();
        }
    }

    public static boolean hasActiveEffects() {
        return !stars.isEmpty();
    }

    private static class HitStar {
        double x, y, vx, vy;
        float scale, rotation, rotSpeed;
        int age;
        static final int MAX_AGE = 25;

        HitStar(double x, double y, double vx, double vy,
                float scale, float rotation, float rotSpeed) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.scale = scale;
            this.rotation = rotation;
            this.rotSpeed = rotSpeed;
        }

        void tick() {
            x += vx;
            y += vy;
            vy += 0.25;
            vx *= 0.95;
            vy *= 0.95;
            rotation += rotSpeed;
            age++;
        }

        boolean isDead() {
            return age >= MAX_AGE;
        }

        float alpha() {
            if (age < 3) return age / 3.0f;
            return Math.max(0, 1.0f - (float) (age - 3) / (MAX_AGE - 3));
        }
    }

    private HitEffectsRenderer() {}
}
