package dev.konsti.factorygaugefix;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SableDistanceHelper {
    private SableDistanceHelper() {}

    public static boolean playerAndBlockWithin(Level level, Player player, BlockPos pos, double maxDistance) {
        Vec3 playerPos = Vec3.atCenterOf(player.blockPosition());
        Vec3 blockPos = Vec3.atCenterOf(pos);
        return Sable.HELPER.distanceSquaredWithSubLevels(level, playerPos, blockPos) < maxDistance * maxDistance;
    }

    public static boolean blocksWithin(Level level, BlockPos first, BlockPos second, double maxDistance) {
        Vec3 firstPos = Vec3.atCenterOf(first);
        Vec3 secondPos = Vec3.atCenterOf(second);
        return Sable.HELPER.distanceSquaredWithSubLevels(level, firstPos, secondPos) < maxDistance * maxDistance;
    }
}
