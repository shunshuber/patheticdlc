package com.pathdlc.digger.combat;

import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class KillAuraHandler {
    private static LivingEntity currentTarget;
    private static int switchTimer;
    private static int nextAttackDelay;
    private static int ticksSinceAttack;
    private static boolean isAiming;

    private static final float MAX_YAW_SPEED = 35f;
    private static final float MAX_PITCH_SPEED = 25f;
    private static final float AIM_THRESHOLD = 8f;

    public static void tick(MinecraftClient client) {
        if (!ModuleManager.isEnabled("KillAura")) {
            currentTarget = null;
            isAiming = false;
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        if (client.interactionManager == null) return;

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

        List<LivingEntity> targets = findTargets(client, player, range,
                hitMobs, hitPlayers);

        if (targets.isEmpty()) {
            currentTarget = null;
            isAiming = false;
            return;
        }

        switchTimer++;
        if (currentTarget == null || !currentTarget.isAlive()
                || player.distanceTo(currentTarget) > range
                || switchTimer > 60) {
            currentTarget = targets.get(0);
            switchTimer = 0;
        }

        float[] targetAngles = getRotation(player, currentTarget);
        float yawDiff = MathHelper.wrapDegrees(targetAngles[0] - player.getYaw());
        float pitchDiff = targetAngles[1] - player.getPitch();

        float jitterYaw = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 2f;
        float jitterPitch = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 1f;

        float yawStep = MathHelper.clamp(yawDiff + jitterYaw,
                -MAX_YAW_SPEED, MAX_YAW_SPEED);
        float pitchStep = MathHelper.clamp(pitchDiff + jitterPitch,
                -MAX_PITCH_SPEED, MAX_PITCH_SPEED);

        player.setYaw(player.getYaw() + yawStep);
        player.setPitch(MathHelper.clamp(player.getPitch() + pitchStep, -90f, 90f));

        isAiming = Math.abs(yawDiff) < AIM_THRESHOLD
                && Math.abs(pitchDiff) < AIM_THRESHOLD;

        ticksSinceAttack++;

        if (player.getAttackCooldownProgress(0) < 1.0f) return;
        if (ticksSinceAttack < nextAttackDelay) return;
        if (!isAiming) return;

        if (critOnly) {
            boolean falling = !player.isOnGround() && player.getVelocity().y < 0
                    && player.fallDistance > 0.0f;
            if (!falling) return;
        }

        client.interactionManager.attackEntity(player, currentTarget);
        player.swingHand(Hand.MAIN_HAND);

        ticksSinceAttack = 0;
        nextAttackDelay = ThreadLocalRandom.current().nextInt(1, 4);
    }

    private static float[] getRotation(ClientPlayerEntity player,
                                        LivingEntity target) {
        Vec3d playerEyes = player.getEyePos();
        double targetY = target.getY() + target.getHeight() * 0.4
                + ThreadLocalRandom.current().nextDouble() * target.getHeight() * 0.4;
        Vec3d targetPos = new Vec3d(target.getX(), targetY, target.getZ());

        double dx = targetPos.x - playerEyes.x;
        double dy = targetPos.y - playerEyes.y;
        double dz = targetPos.z - playerEyes.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, dist));

        pitch = MathHelper.clamp(pitch, -90f, 90f);

        return new float[]{yaw, pitch};
    }

    private static List<LivingEntity> findTargets(MinecraftClient client,
                                                    ClientPlayerEntity player,
                                                    float range,
                                                    boolean hitMobs,
                                                    boolean hitPlayers) {
        List<LivingEntity> result = new ArrayList<>();

        for (Entity entity : client.world.getEntities()) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.isAlive() || living.getHealth() <= 0) continue;

            double dist = player.distanceTo(living);
            if (dist > range) continue;

            boolean isMob = entity instanceof Monster;
            boolean isAnimal = entity instanceof AnimalEntity;
            boolean isPlayer = entity instanceof PlayerEntity;

            if (isMob && !hitMobs) continue;
            if (isAnimal && !hitMobs) continue;
            if (isPlayer && !hitPlayers) continue;
            if (!isMob && !isAnimal && !isPlayer) continue;

            result.add(living);
        }

        result.sort(Comparator.comparingDouble(e -> {
            double d = player.distanceTo(e);
            double healthPenalty = e.getHealth() / 20.0 * 0.5;
            return d + healthPenalty;
        }));

        return result;
    }

    public static LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    private KillAuraHandler() {}
}
