package com.pathdlc.digger.combat;

import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class KillAuraHandler {
    private static int cooldownTicks;

    public static void tick(MinecraftClient client) {
        if (!ModuleManager.isEnabled("KillAura")) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        Module mod = ModuleManager.get("KillAura");
        if (mod == null) return;

        ModuleSetting rangeSetting = mod.getSetting("Range");
        ModuleSetting onlyCrit = mod.getSetting("Only Crit");
        ModuleSetting attackMobs = mod.getSetting("Attack Mobs");
        ModuleSetting attackPlayers = mod.getSetting("Attack Players");

        float range = rangeSetting != null ? rangeSetting.getFloat() : 4.0f;
        boolean critOnly = onlyCrit != null && onlyCrit.getBool();
        boolean hitMobs = attackMobs != null && attackMobs.getBool();
        boolean hitPlayers = attackPlayers != null && attackPlayers.getBool();

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (player.getAttackCooldownProgress(0) < 1.0f) return;

        if (critOnly && (player.isOnGround() || player.getVelocity().y >= 0)) {
            return;
        }

        LivingEntity target = findTarget(client, player, range, hitMobs, hitPlayers);
        if (target == null) return;

        rotateTo(player, target);

        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);

        cooldownTicks = 2;
    }

    private static LivingEntity findTarget(MinecraftClient client,
                                             ClientPlayerEntity player,
                                             float range, boolean hitMobs,
                                             boolean hitPlayers) {
        LivingEntity best = null;
        double bestDist = range;

        for (Entity entity : client.world.getEntities()) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.isAlive()) continue;

            boolean isMob = entity instanceof Monster;
            boolean isPlayer = entity instanceof PlayerEntity;

            if (isMob && !hitMobs) continue;
            if (isPlayer && !hitPlayers) continue;
            if (!isMob && !isPlayer) continue;

            double dist = player.distanceTo(living);
            if (dist < bestDist) {
                bestDist = dist;
                best = living;
            }
        }
        return best;
    }

    private static void rotateTo(ClientPlayerEntity player, LivingEntity target) {
        Vec3d playerEyes = player.getEyePos();
        Vec3d targetCenter = target.getPos().add(0, target.getHeight() / 2.0, 0);

        double dx = targetCenter.x - playerEyes.x;
        double dy = targetCenter.y - playerEyes.y;
        double dz = targetCenter.z - playerEyes.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, dist));

        targetPitch = MathHelper.clamp(targetPitch, -90f, 90f);

        float yawDiff = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float pitchDiff = targetPitch - player.getPitch();

        float rotSpeed = 0.6f;
        player.setYaw(player.getYaw() + yawDiff * rotSpeed);
        player.setPitch(player.getPitch() + pitchDiff * rotSpeed);
    }

    private KillAuraHandler() {}
}
