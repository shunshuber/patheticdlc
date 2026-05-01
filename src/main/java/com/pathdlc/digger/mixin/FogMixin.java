package com.pathdlc.digger.mixin;

import com.pathdlc.digger.gui.FogSettings;
import com.pathdlc.digger.gui.ModuleManager;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public abstract class FogMixin {
    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void onApplyFog(Camera camera,
                                    BackgroundRenderer.FogType fogType,
                                    Vector4f color,
                                    float viewDistance,
                                    boolean thickFog,
                                    float tickDelta,
                                    CallbackInfoReturnable<Fog> cir) {
        if (!ModuleManager.isEnabled("Fog")) return;

        FogSettings.FogColor fogColor = FogSettings.getColor();
        FogSettings.FogDensity density = FogSettings.getDensity();

        float start = viewDistance * density.startMul;
        float end = viewDistance * density.endMul;

        cir.setReturnValue(new Fog(
                start, end, FogShape.SPHERE,
                fogColor.r, fogColor.g, fogColor.b, 1.0f
        ));
    }
}
