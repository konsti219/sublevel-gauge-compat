package dev.konsti.factorygaugefix.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import dev.konsti.factorygaugefix.SimulatedGaugesHelper;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.PanelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractPanelBehaviour.class)
public abstract class AbstractPanelBehaviourMixin extends AbstractPanelBehaviour {
    @Unique
    private double sublevelGaugeCompat$lastDynamicOutput = Double.NaN;

    protected AbstractPanelBehaviourMixin(PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sublevelGaugeCompat$notifyDynamicSimulatedOutputs(CallbackInfo ci) {
        if (getWorld().isClientSide()) {
            return;
        }

        Double currentOutput = SimulatedGaugesHelper.getDynamicPanelOutput(this);
        if (currentOutput == null || !SimulatedGaugesHelper.valueChanged(sublevelGaugeCompat$lastDynamicOutput, currentOutput)) {
            return;
        }

        sublevelGaugeCompat$lastDynamicOutput = currentOutput;
        notifyOutputs();
    }
}
