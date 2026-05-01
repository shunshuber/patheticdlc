package com.pathdlc.digger.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

public final class BlockOverlayRenderer {
    private static final int ANIMATION_FRAMES = 8;
    private static final int TICKS_PER_FRAME = 3;
    private static int tick;

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.crosshairTarget == null
                || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = hit.getBlockPos();

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null || context.camera() == null) {
            return;
        }

        Vec3d camera = context.camera().getPos();

        tick++;
        float pulse = 0.5f + 0.3f * (float) Math.sin(tick * 0.08);
        float r = 0.4f;
        float g = 0.7f + 0.15f * (float) Math.sin(tick * 0.05);
        float b = 1.0f;

        Box box = new Box(pos).expand(0.003);

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexRendering.drawBox(
                matrices,
                consumers.getBuffer(RenderLayer.getLines()),
                box,
                r, g, b, pulse
        );

        VertexRendering.drawBox(
                matrices,
                consumers.getBuffer(RenderLayer.getLines()),
                box.expand(0.01),
                r * 0.6f, g * 0.6f, b * 0.6f, pulse * 0.4f
        );

        matrices.pop();
    }

    private BlockOverlayRenderer() {}
}
