package com.pathdlc.digger.mixin;

import com.pathdlc.digger.gui.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class AspectRatioMixin {
    @Shadow @Final
    private MinecraftClient client;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov,
                          CallbackInfoReturnable<Float> cir) {
        if (!ModuleManager.isEnabled("AspectRatio")) return;

        int w = client.getWindow().getWidth();
        int h = client.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;

        float currentAspect = (float) w / h;
        float targetAspect = 4.0f / 3.0f;

        if (currentAspect > targetAspect) {
            float scale = targetAspect / currentAspect;
            cir.setReturnValue(cir.getReturnValue() * scale);
        }
    }
}
