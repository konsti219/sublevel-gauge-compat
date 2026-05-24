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
@Mixin(targets = "dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity", remap = false)
public abstract class NavTableBlockEntityMixin extends SmartBlockEntity {
    @Shadow
    public abstract float getRelativeAngle();

    @Unique
    private double sublevelGaugeCompat$lastRelativeAngle = Double.NaN;

    protected NavTableBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sublevelGaugeCompat$notifyLinkedPanels(CallbackInfo ci) {
        if (level == null || level.isClientSide) {
            return;
        }

        double current = getRelativeAngle();
        if (Double.isNaN(sublevelGaugeCompat$lastRelativeAngle) || Math.abs(sublevelGaugeCompat$lastRelativeAngle - current) > 1e-4) {
            sublevelGaugeCompat$lastRelativeAngle = current;
            FactoryPanelSupportBehaviour support = BlockEntityBehaviour.get(level, worldPosition, FactoryPanelSupportBehaviour.TYPE);
            if (support != null) {
                support.notifyPanels();
            }
        }
    }
}
