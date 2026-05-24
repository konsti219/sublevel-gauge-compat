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
@Mixin(targets = "dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity", remap = false)
public abstract class AltitudeSensorBlockEntityMixin extends SmartBlockEntity {
    @Shadow
    public abstract float getValue();

    @Shadow
    public abstract int getSignal();

    @Unique
    private double sublevelGaugeCompat$lastAltitudeValue = Double.NaN;

    protected AltitudeSensorBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sublevelGaugeCompat$notifyLinkedPanels(CallbackInfo ci) {
        if (level == null || level.isClientSide) {
            return;
        }

        double current = getValue() + getSignal() / 1000d;
        if (Double.isNaN(sublevelGaugeCompat$lastAltitudeValue) || Math.abs(sublevelGaugeCompat$lastAltitudeValue - current) > 1e-4) {
            sublevelGaugeCompat$lastAltitudeValue = current;
            FactoryPanelSupportBehaviour support = BlockEntityBehaviour.get(level, worldPosition, FactoryPanelSupportBehaviour.TYPE);
            if (support != null) {
                support.notifyPanels();
            }
        }
    }
}
