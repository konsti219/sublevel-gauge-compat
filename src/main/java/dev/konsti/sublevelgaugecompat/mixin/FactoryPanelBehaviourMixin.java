package dev.konsti.sublevelgaugecompat.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FactoryPanelBehaviour.class)
public class FactoryPanelBehaviourMixin {
    @Inject(
        method = "at(Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;)Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void createSableFactoryGaugeFix$resolveSublevelGauge(
        BlockAndTintGetter world, FactoryPanelPosition pos, CallbackInfoReturnable<FactoryPanelBehaviour> cir
    ) {
        if (!(world.getBlockEntity(pos.pos()) instanceof FactoryPanelBlockEntity blockEntity)) {
            return;
        }

        FactoryPanelBehaviour behaviour = blockEntity.panels.get(pos.slot());
        if (behaviour != null && behaviour.isActive()) {
            cir.setReturnValue(behaviour);
        }
    }

    @Inject(
        method = "linkAt(Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;)Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelSupportBehaviour;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void createSableFactoryGaugeFix$resolveSublevelLinkSupport(
        BlockAndTintGetter world, FactoryPanelPosition pos, CallbackInfoReturnable<FactoryPanelSupportBehaviour> cir
    ) {
        FactoryPanelSupportBehaviour behaviour =
            BlockEntityBehaviour.get(world, pos.pos(), FactoryPanelSupportBehaviour.TYPE);
        if (behaviour != null) {
            cir.setReturnValue(behaviour);
        }
    }
}
