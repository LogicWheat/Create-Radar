package com.happysg.radar.block.datalink.screens;

import com.happysg.radar.block.radar.track.TrackCategory;
import net.minecraft.nbt.CompoundTag;

public record TargetingConfig(boolean player, boolean contraption, boolean mob, boolean animal, boolean projectile,
                              boolean autoTarget, boolean autoFire) {

    public static final TargetingConfig DEFAULT = new TargetingConfig(false, false, true, true, false, false, true);

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("player", player);
        tag.putBoolean("contraption", contraption);
        tag.putBoolean("mob", mob);
        tag.putBoolean("animal", animal);
        tag.putBoolean("projectile", projectile);
        tag.putBoolean("autoTarget", autoTarget);
        tag.putBoolean("autoFire", autoFire);
        return tag;
    }

    public static TargetingConfig fromTag(CompoundTag tag) {
        if (!tag.contains("targeting"))
            return DEFAULT;
        CompoundTag targeting = tag.getCompound("targeting");
        return new TargetingConfig(
                targeting.getBoolean("player"),
                targeting.getBoolean("contraption"),
                targeting.getBoolean("mob"),
                targeting.getBoolean("animal"),
                targeting.getBoolean("projectile"),
                targeting.getBoolean("autoTarget"),
                targeting.getBoolean("autoFire")
        );
    }

    public boolean test(TrackCategory trackCategory) {
        return switch (trackCategory) {
            case PLAYER -> player;
            case CONTRAPTION -> contraption;
            case HOSTILE -> mob;
            case ANIMAL -> animal;
            case PROJECTILE -> projectile;
            default -> false;
        };
    }
}
