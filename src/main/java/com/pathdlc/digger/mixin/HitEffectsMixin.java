package com.pathdlc.digger.mixin;

import com.pathdlc.digger.gui.ModuleManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class HitEffectsMixin {
    private static final Random RANDOM = new Random();

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (!ModuleManager.isEnabled("HitEffects")) return;
        if (!(player.getWorld() instanceof ClientWorld world)) return;

        double x = target.getX();
        double y = target.getY() + target.getHeight() / 2.0;
        double z = target.getZ();

        for (int i = 0; i < 12; i++) {
            double ox = (RANDOM.nextDouble() - 0.5) * 0.8;
            double oy = (RANDOM.nextDouble() - 0.5) * 0.8;
            double oz = (RANDOM.nextDouble() - 0.5) * 0.8;
            world.addParticle(ParticleTypes.CRIT, x + ox, y + oy, z + oz,
                    ox * 0.5, oy * 0.5, oz * 0.5);
        }

        for (int i = 0; i < 6; i++) {
            double ox = (RANDOM.nextDouble() - 0.5) * 0.6;
            double oy = (RANDOM.nextDouble() - 0.5) * 0.6;
            double oz = (RANDOM.nextDouble() - 0.5) * 0.6;
            world.addParticle(ParticleTypes.ENCHANTED_HIT, x + ox, y + oy, z + oz,
                    ox * 0.3, oy * 0.3, oz * 0.3);
        }
    }
}
