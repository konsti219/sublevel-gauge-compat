package dev.konsti.factorygaugefix.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.konsti.factorygaugefix.SimulatedGaugesHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour", remap = false)
public abstract class AbstractPanelBehaviourMixin extends FactoryPanelBehaviour {
    @Unique
    private double sublevelGaugeCompat$lastDynamicOutput = Double.NaN;

    protected AbstractPanelBehaviourMixin(com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity be,
                                          com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot slot) {
        super(be, slot);
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
        ((AbstractPanelBehaviour) (Object) this).notifyOutputs();
    }

    @Inject(method = "writeSafe", at = @At("TAIL"))
    private void sublevelGaugeCompat$writeCustomPanelDataToSafeNbt(CompoundTag nbt, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!active) {
            return;
        }

        String slotKey = CreateLang.asId(slot.name());
        CompoundTag panelTag = nbt.getCompound(slotKey);
        if (panelTag.isEmpty()) {
            return;
        }

        ((AbstractPanelBehaviour) (Object) this).easyWrite(panelTag, registries, false);
        nbt.put(slotKey, panelTag);
    }
}
