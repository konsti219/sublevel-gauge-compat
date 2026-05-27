package dev.konsti.sublevelgaugecompat.mixin;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.AbstractPanelSupportBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnectionBuilder;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RedstoneLinkBlockEntity.class)
public abstract class RedstoneLinkBlockEntityMixin {
    @Shadow
    private LinkBehaviour link;

    @Shadow
    public FactoryPanelSupportBehaviour panelSupport;

    @Shadow
    public abstract int getReceivedSignal();

    @Inject(method = "addBehaviours", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$addAnalogPanelSupport(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        RedstoneLinkBlockEntity self = (RedstoneLinkBlockEntity) (Object) this;
        behaviours.add(panelSupport = new AbstractPanelSupportBehaviour(
            self,
            () -> link != null && link.isListening(),
            () -> AllBlocks.REDSTONE_LINK.get().updateTransmittedSignal(self.getBlockState(), self.getLevel(), self.getBlockPos())
        ) {
            @Override
            public void addConnections(PanelConnectionBuilder builder) {
                builder.registerInput(DeployerPanelConnections.NUMBERS.get());
                builder.registerInput(DeployerPanelConnections.REDSTONE.get());
                builder.registerOutput(DeployerPanelConnections.NUMBERS.get(), () -> (float) getReceivedSignal());
                builder.registerOutput(DeployerPanelConnections.REDSTONE.get(), () -> getReceivedSignal() > 0);
            }
        });
        ci.cancel();
    }
}
