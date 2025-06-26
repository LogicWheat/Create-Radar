package com.happysg.radar.compat.cbc;

import com.dsvv.cbcat.cannon.RifledBarrelBlockEntity;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.mixin.AbstractCannonAccessor;
import com.happysg.radar.mixin.AutoCannonAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.IAutocannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterialProperties;
import rbasamoyai.createbigcannons.cannons.big_cannons.BigCannonBehavior;
import rbasamoyai.createbigcannons.cannons.big_cannons.IBigCannonBlockEntity;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock;
import rbasamoyai.createbigcannons.munitions.big_cannon.propellant.BigCannonPropellantBlock;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import riftyboi.cbcmodernwarfare.cannon_control.compact_mount.CompactCannonMountBlockEntity;
import riftyboi.cbcmodernwarfare.cannon_control.contraption.MountedMediumcannonContraption;
import riftyboi.cbcmodernwarfare.cannon_control.contraption.MountedRotarycannonContraption;
import riftyboi.cbcmodernwarfare.cannons.medium_cannon.MediumcannonBlock;
import riftyboi.cbcmodernwarfare.cannons.medium_cannon.MediumcannonBlockEntity;
import riftyboi.cbcmodernwarfare.cannons.medium_cannon.breech.MediumcannonBreechBlockEntity;
import riftyboi.cbcmodernwarfare.cannons.medium_cannon.material.MediumcannonMaterial;
import riftyboi.cbcmodernwarfare.cannons.rotarycannon.RotarycannonBlock;
import riftyboi.cbcmodernwarfare.cannons.rotarycannon.RotarycannonBlockEntity;
import riftyboi.cbcmodernwarfare.cannons.rotarycannon.material.RotarycannonMaterial;
import riftyboi.cbcmodernwarfare.forge.cannons.RotarycannonBreechBlockEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static riftyboi.cbcmodernwarfare.cannon_control.compact_mount.CompactCannonMountBlock.HORIZONTAL_FACING;

public class CannonUtil {
    public static int getBarrelLength(AbstractMountedCannonContraption cannon) {
        if (cannon == null)
            return 0;
        if(cannon.initialOrientation() == Direction.WEST || cannon.initialOrientation() == Direction.NORTH){
            return ((AbstractCannonAccessor) cannon).getBackExtensionLength();
        }
        else{
            return ((AbstractCannonAccessor) cannon).getFrontExtensionLength();
        }
    }
    public static Vec3 getCannonMountOffset(Level level, BlockPos pos) {
        return getCannonMountOffset(level.getBlockEntity(pos));
    }

    public static Vec3 getCannonMountOffset(BlockEntity mount) {
        Vec3 offset = null;
        if(Mods.CBCMODERNWARFARE.isLoaded() && mount instanceof CompactCannonMountBlockEntity){
            Direction direction = mount.getBlockState().getValue(HORIZONTAL_FACING);
            if(direction == Direction.EAST){
                offset = new Vec3(0,0,1);
            }
            else if(direction == Direction.SOUTH){
                offset = new Vec3(-1,0,0);
            }
            else if(direction == Direction.WEST){
                offset = new Vec3(0,0,-1);
            }
            else if(direction == Direction.NORTH){
                offset = new Vec3(1,0,0);
            }
        }
        else{
            if(isUp(mount)){
                offset = new Vec3(0,2,0);
            }
            else {

                offset = new Vec3(0,-2,0);
            }
        }
        return offset;
    }

    public static float getRotarySpeed( AbstractMountedCannonContraption contraptionEntity) {
        if(!Mods.CBCMODERNWARFARE.isLoaded()) return 0f;
        if(contraptionEntity == null) return 0f;
        Map<BlockPos, BlockEntity> presentBlockEntities = contraptionEntity.entity.getContraption().presentBlockEntities;
        if(presentBlockEntities.isEmpty()) return 0f;
        int barrelCount = 0;
        RotarycannonMaterial material = null;
        List<BlockEntity> blocks = presentBlockEntities.values().stream().toList();
        for (BlockEntity entity : blocks){
            if(entity instanceof RotarycannonBlockEntity blockEntity && !(entity instanceof RotarycannonBreechBlockEntity)){
                barrelCount++;
                if(material == null){
                    material = ((RotarycannonBlock) blockEntity.getBlockState().getBlock()).getRotarycannonMaterial();
                }
            }
        }
        if(material == null) return 0;
        float baseSpeed = material.properties().baseSpeed();
        int speedIncrease = Math.min(barrelCount, material.properties().maxSpeedIncreases());
        return baseSpeed+speedIncrease*material.properties().speedIncreasePerBarrel();
    }

    public static float getMediumCannonSpeed(AbstractMountedCannonContraption contraptionEntity) {
        if(!Mods.CBCMODERNWARFARE.isLoaded()) return 0f;
        if(contraptionEntity == null) return 0f;
        Map<BlockPos, BlockEntity> presentBlockEntities = contraptionEntity.entity.getContraption().presentBlockEntities;
        if(presentBlockEntities.isEmpty()) return 0f;
        int barrelCount = 0;
        MediumcannonMaterial material = null;
        List<BlockEntity> blocks = presentBlockEntities.values().stream().toList();
        for (BlockEntity entity : blocks){
            if(entity instanceof MediumcannonBlockEntity blockEntity && !(entity instanceof MediumcannonBreechBlockEntity)){
                barrelCount++;
                if(material == null){
                    material = ((MediumcannonBlock) blockEntity.getBlockState().getBlock()).getMediumcannonMaterial();
                }
            }
        }
        if(material == null) return 0;
        float baseSpeed = material.properties().baseSpeed();
        int speedIncrease = Math.min(barrelCount, material.properties().maxSpeedIncreases());
        return baseSpeed+speedIncrease*material.properties().speedIncreasePerBarrel();
    }

    public static int getBigCannonSpeed(ServerLevel level, PitchOrientedContraptionEntity contraptionEntity) {
        if(contraptionEntity == null) return 0;
        Map<BlockPos, BlockEntity> presentBlockEntities = contraptionEntity.getContraption().presentBlockEntities;
        int speeed = 0;
        for (BlockEntity blockEntity : presentBlockEntities.values()) {
            if (!(blockEntity instanceof IBigCannonBlockEntity cannonBlockEntity)) continue;
            BigCannonBehavior behavior = cannonBlockEntity.cannonBehavior();
            StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();

            Block block = containedBlockInfo.state().getBlock();
            if (block instanceof BigCannonPropellantBlock propellantBlock) {
                speeed += (int) propellantBlock.getChargePower(containedBlockInfo);
            } else if (block instanceof ProjectileBlock<?> projectileBlock) {
                AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, Collections.singletonList(containedBlockInfo));
                speeed += (int) projectile.addedChargePower();
            }
        }
        return speeed;
    }

    public static float getInitialVelocity(AbstractMountedCannonContraption cannon, ServerLevel level) {
        if (isBigCannon(cannon)) {
            return getBigCannonSpeed(level, (PitchOrientedContraptionEntity)cannon.entity);
        } else if (isAutoCannon(cannon)) {
            return getACSpeed((MountedAutocannonContraption) cannon);
        }
        else if(isRotaryCannon(cannon)){
            return getRotarySpeed(cannon);
        }
        else if(isMediumCannon(cannon)){
            return getMediumCannonSpeed(cannon);
        }
        return 0;
    }

    public static double getProjectileGravity(AbstractMountedCannonContraption cannon, ServerLevel level) {
        if (isAutoCannon(cannon) || isRotaryCannon(cannon) || isMediumCannon(cannon)) return -0.025;
        Map<BlockPos, BlockEntity> presentBlockEntities = cannon.presentBlockEntities;
        for (BlockEntity blockEntity : presentBlockEntities.values()) {
            if (!(blockEntity instanceof IBigCannonBlockEntity cannonBlockEntity)) continue;
            BigCannonBehavior behavior = cannonBlockEntity.cannonBehavior();
            StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();

            Block block = containedBlockInfo.state().getBlock();
            if (block instanceof ProjectileBlock<?> projectileBlock) {
                AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, Collections.singletonList(containedBlockInfo));
                BallisticPropertiesComponent ballisticProperties;
                try {

                    Method method = projectile.getClass().getDeclaredMethod("getBallisticProperties");
                    method.setAccessible(true);
                    ballisticProperties = (BallisticPropertiesComponent) method.invoke(projectile);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                         ClassCastException e) {
                    return 0.05;
                }
                return ballisticProperties.gravity();
            }
        }
        return 0.05;
    }

    public static double getProjectileDrag(AbstractMountedCannonContraption cannon, ServerLevel level) {
        Map<BlockPos, BlockEntity> presentBlockEntities = cannon.presentBlockEntities;
        double drag = 0.01;
        int rifledBarrelAmount = 0;
        for (BlockEntity blockEntity : presentBlockEntities.values()) {
            if (!(blockEntity instanceof IBigCannonBlockEntity cannonBlockEntity)) continue;
            if(Mods.CBC_AT.isLoaded() && blockEntity instanceof RifledBarrelBlockEntity){
                rifledBarrelAmount+=1;
            }
            BigCannonBehavior behavior = cannonBlockEntity.cannonBehavior();
            StructureTemplate.StructureBlockInfo containedBlockInfo = behavior.block();

            Block block = containedBlockInfo.state().getBlock();
            if (block instanceof ProjectileBlock<?> projectileBlock) {
                AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, Collections.singletonList(containedBlockInfo));
                BallisticPropertiesComponent ballisticProperties;
                try {
                    Method method = projectile.getClass().getDeclaredMethod("getBallisticProperties");
                    method.setAccessible(true);
                    ballisticProperties = (BallisticPropertiesComponent) method.invoke(projectile);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                         ClassCastException e) {
                    return drag;
                }
                drag = ballisticProperties.drag();
            }
        }
        return drag;
    }

    public static boolean isBigCannon(AbstractMountedCannonContraption cannon) {
        return cannon instanceof MountedBigCannonContraption;
    }

    public static boolean isAutoCannon(AbstractMountedCannonContraption cannon) {
        return cannon instanceof MountedAutocannonContraption;
    }
    public static boolean isRotaryCannon(AbstractMountedCannonContraption cannonContraption){
        if(!Mods.CBCMODERNWARFARE.isLoaded()) return false;
        return cannonContraption instanceof MountedRotarycannonContraption;
    }
    public static boolean isMediumCannon(AbstractMountedCannonContraption cannonContraption){
        if(!Mods.CBCMODERNWARFARE.isLoaded()) return false;
        return cannonContraption instanceof MountedMediumcannonContraption;
    }


    public static float getACSpeed(MountedAutocannonContraption autocannon) {
        if (autocannon == null)
            return 0;

        if (((AutoCannonAccessor) autocannon).getMaterial() == null)
            return 0;

        AutocannonMaterialProperties properties = ((AutoCannonAccessor) autocannon).getMaterial().properties();
        float speed = properties.baseSpeed();
        BlockPos currentPos = autocannon.getStartPos().relative(autocannon.initialOrientation());
        int barrelTravelled = 0;

        while (autocannon.presentBlockEntities.get(currentPos) instanceof IAutocannonBlockEntity) {
            ++barrelTravelled;
            if (barrelTravelled <= properties.maxSpeedIncreases())
                speed += properties.speedIncreasePerBarrel();
            if (barrelTravelled > properties.maxBarrelLength()) {
                break;
            }
            currentPos = currentPos.relative(autocannon.initialOrientation());
        }
        return speed;
    }

    public static boolean isUp(Level level , Vec3 mountPos){
        BlockEntity blockEntity =  level.getBlockEntity(new BlockPos( (int) mountPos.x, (int) mountPos.y, (int) mountPos.z));
        return isUp(blockEntity);
    }

    public static boolean isUp(BlockEntity blockEntity) {
        if(!(blockEntity instanceof CannonMountBlockEntity cannonMountBlockEntity)) return true;
        if(cannonMountBlockEntity.getContraption() == null) return true;
        return !(cannonMountBlockEntity.getContraption().position().y < blockEntity.getBlockPos().getY());
    }

}
