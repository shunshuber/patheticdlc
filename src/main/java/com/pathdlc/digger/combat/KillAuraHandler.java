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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KillAuraHandler {
    private static LivingEntity currentTarget;
    private static int attackTimer;
    private static int switchTimer;

    public static void tick(MinecraftClient client) {
        if (!ModuleManager.isEnabled("KillAura")) {
            currentTarget = null;
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
            return;
        }

        switchTimer++;
        if (currentTarget == null || !currentTarget.isAlive()
                || player.distanceTo(currentTarget) > range
                || switchTimer > 40) {
            currentTarget = targets.get(0);
            switchTimer = 0;
        }

        attackTimer++;

        boolean cooldownReady = player.getAttackCooldownProgress(0) >= 0.9f;

        if (critOnly) {
            boolean falling = !player.isOnGround() && player.getVelocity().y < 0
                    && player.fallDistance > 0.0f;
            if (!falling) {
                if (player.isOnGround() && cooldownReady && attackTimer > 2) {
                    player.jump();
                }
                return;
            }
        }

        if (cooldownReady && attackTimer >= 1) {
            float[] angles = getRotation(player, currentTarget);

            player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(
                            angles[0], angles[1],
                            player.isOnGround(),
                            player.horizontalCollision));

            client.interactionManager.attackEntity(player, currentTarget);
            player.swingHand(Hand.MAIN_HAND);
            attackTimer = 0;
        }
    }

    private static float[] getRotation(ClientPlayerEntity player,
                                        LivingEntity target) {
        Vec3d playerEyes = player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.85, 0);

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
            double dist = player.distanceTo(e);
            double healthPenalty = e.getHealth() / 20.0 * 0.5;
            return dist + healthPenalty;
        }));

        return result;
    }

    public static LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    private KillAuraHandler() {}
}
