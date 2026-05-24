package dev.konsti.factorygaugefix.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity", remap = false)
public abstract class VelocitySensorBlockEntityMixin extends SmartBlockEntity {
    @Shadow
    public abstract float getAdjustedVelocity();

    @Shadow
    public abstract int getRedstoneStrength();

    @Unique
    private double sublevelGaugeCompat$lastVelocity = Double.NaN;

    protected VelocitySensorBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sublevelGaugeCompat$notifyLinkedPanels(CallbackInfo ci) {
        if (level == null || level.isClientSide) {
            return;
        }

        double current = getAdjustedVelocity() + getRedstoneStrength() / 1000d;
        if (Double.isNaN(sublevelGaugeCompat$lastVelocity) || Math.abs(sublevelGaugeCompat$lastVelocity - current) > 1e-4) {
            sublevelGaugeCompat$lastVelocity = current;
            FactoryPanelSupportBehaviour support = BlockEntityBehaviour.get(level, worldPosition, FactoryPanelSupportBehaviour.TYPE);
            if (support != null) {
                support.notifyPanels();
            }
        }
    }
}
