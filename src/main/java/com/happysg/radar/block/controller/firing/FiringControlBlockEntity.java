package com.happysg.radar.block.controller.firing;

import com.happysg.radar.block.controller.pitch.AutoPitchControllerBlockEntity;
import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import com.happysg.radar.block.datalink.screens.TargetingConfig;
import com.happysg.radar.compat.cbc.CannonUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.happysg.radar.compat.cbc.CannonTargeting.calculateProjectileYatX;

public class FiringControlBlockEntity {

    private static final int TARGET_TIMEOUT_TICKS = 20;   // 1 s at 20 TPS

    TargetingConfig          targetingConfig = TargetingConfig.DEFAULT;
    Vec3                     target;
    boolean                  firing;

    public final CannonMountBlockEntity          cannonMount;
    final AutoPitchControllerBlockEntity         pitchController;
    final Level                                  level;

    public List<AABB> safeZones = new ArrayList<>();

    private long lastTargetTick = -1;   // server-time when we last got a target update

    public FiringControlBlockEntity(AutoPitchControllerBlockEntity controller,
                                    CannonMountBlockEntity cannonMount) {
        this.cannonMount     = cannonMount;
        this.pitchController = controller;
        this.level           = cannonMount.getLevel();
    }

    public void setSafeZones(List<AABB> safeZones) {
        this.safeZones = safeZones;
    }

    /**
     * Called every tick by the pitch controller.
     */
    public void tick() {
        // --- invalidate stale targets (e.g. entity died or radar lost lock) ---
        if (target != null && level != null &&
                level.getGameTime() - lastTargetTick > TARGET_TIMEOUT_TICKS) {
            target = null;          // clear target after timeout
        }

        // normal fire-control logic
        if (isTargetInRange() && targetingConfig.autoFire()) {
            tryFireCannon();
        } else {
            stopFireCannon();
        }
    }

    /**
     * Radar updates the gun’s target through this method.
     */
    public void setTarget(Vec3 target, TargetingConfig config) {
        if (target == null) {
            this.target = null;
            stopFireCannon();
            return;
        }

        this.target          = target;
        this.targetingConfig = config;
        this.lastTargetTick  = level != null ? level.getGameTime() : 0;

        if (pitchController != null) {
            pitchController.setTarget(target);   // keep pitch solution fresh
        }
    }

    private boolean isTargetInRange() {
        return target != null && hasCorrectYawPitch() && !passesSafeZone();
    }

    private boolean passesSafeZone() {
        if (!(level instanceof ServerLevel)) return false;

        for (AABB aabb : safeZones) {
            if (aabb.contains(target)) return true;

            Vec3 cannonPos = cannonMount.getBlockPos().getCenter();
            Optional<Vec3> optionalMin = aabb.clip(new Vec3(cannonPos.x, aabb.minY, cannonPos.z),
                    new Vec3(target.x,   aabb.minY, target.z));
            Optional<Vec3> optionalMax = aabb.clip(new Vec3(target.x,   aabb.minY, target.z),
                    new Vec3(cannonPos.x, aabb.minY, cannonPos.z));
            if (optionalMin.isEmpty() || optionalMax.isEmpty()) continue;

            Vec3  minX = optionalMin.get();
            Vec3  maxX = optionalMax.get();

            double yMax = aabb.maxY - cannonMount.getBlockPos().getY();
            double yMin = aabb.minY - cannonMount.getBlockPos().getY();

            PitchOrientedContraptionEntity contraption = cannonMount.getContraption();
            if (contraption == null ||
                    !(contraption.getContraption() instanceof AbstractMountedCannonContraption cannonContraption))
                continue;

            float  speed        = CannonUtil.getInitialVelocity(cannonContraption, (ServerLevel) level);
            double drag         = CannonUtil.getProjectileDrag(cannonContraption, (ServerLevel) level);
            double gravity      = CannonUtil.getProjectileGravity(cannonContraption, (ServerLevel) level);
            int    barrelLength = CannonUtil.getBarrelLength(cannonContraption);

            double thetaRad          = Math.toRadians(cannonMount.getDisplayPitch());
            double projectileYatMinX = calculateProjectileYatX(speed,
                    cannonPos.distanceTo(minX) - barrelLength * Math.cos(thetaRad),
                    thetaRad, drag, gravity);
            double projectileYatMaxX = calculateProjectileYatX(speed,
                    cannonPos.distanceTo(maxX) - barrelLength * Math.cos(thetaRad),
                    thetaRad, drag, gravity);

            if ((projectileYatMinX >= yMin && projectileYatMaxX <= yMax) ||
                    (projectileYatMinX <= yMin && projectileYatMaxX >= yMin))
                return true;
        }
        return false;
    }

    private boolean hasCorrectYawPitch() {
        BlockPos yawControllerPos = cannonMount.getBlockPos().below();
        if (level.getBlockEntity(yawControllerPos) instanceof AutoYawControllerBlockEntity yawController &&
                pitchController != null) {
            return yawController.atTargetYaw() && pitchController.atTargetPitch();
        }
        return false;
    }

    private void stopFireCannon() {
        cannonMount.onRedstoneUpdate(true, true, false, true, 0);
        firing = false;
    }

    private void tryFireCannon() {
        cannonMount.onRedstoneUpdate(true, true, true, false, 15);
        firing = true;
    }
}
