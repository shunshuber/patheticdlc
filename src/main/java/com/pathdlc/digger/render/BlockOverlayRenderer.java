package com.pathdlc.digger.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class BlockOverlayRenderer {
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
        Box box = new Box(pos).expand(0.002);

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexRendering.drawBox(
                matrices,
                consumers.getBuffer(RenderLayer.getLines()),
                box,
                1.0f, 1.0f, 0.2f, 0.8f
        );

        matrices.pop();
    }

    private BlockOverlayRenderer() {}
}
