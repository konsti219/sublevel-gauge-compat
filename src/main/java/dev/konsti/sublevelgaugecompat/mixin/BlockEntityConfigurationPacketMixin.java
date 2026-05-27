package dev.konsti.sublevelgaugecompat.mixin;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityConfigurationPacket.class)
public class BlockEntityConfigurationPacketMixin {
    @Redirect(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isLoaded(Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean createSableFactoryGaugeFix$treatSublevelBlockEntitiesAsLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos) || level.getBlockEntity(pos) instanceof SyncedBlockEntity;
    }
}
