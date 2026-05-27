package dev.konsti.sublevelgaugecompat.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import net.liukrast.deployer.lib.logistics.board.connection.AbstractPanelSupportBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.ProvidesConnection;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RedstoneLinkBlock.class)
public abstract class RedstoneLinkBlockMixin {
    @Inject(method = "updateTransmittedSignal", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$acceptAnalogPanelSignals(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (level.isClientSide || state.getValue(RedstoneLinkBlock.RECEIVER)) {
            return;
        }

        int power = level.getBestNeighborSignal(pos);
        RedstoneLinkBlockEntity blockEntity = ((RedstoneLinkBlock) (Object) this).getBlockEntityOptional(level, pos).orElse(null);
        if (blockEntity != null && blockEntity.panelSupport instanceof AbstractPanelSupportBehaviour support) {
            int panelPower = 0;
            boolean booleanPower = false;

            for (var panelPos : support.getLinkedPanels()) {
                if (!level.isLoaded(panelPos.pos())) {
                    ci.cancel();
                    return;
                }

                FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(level, panelPos);
                if (!(behaviour instanceof ProvidesConnection provider)) {
                    continue;
                }

                if (provider.getOutputConnections().contains(DeployerPanelConnections.NUMBERS.get())) {
                    panelPower += provider.getConnectionValue(DeployerPanelConnections.NUMBERS.get())
                        .map(value -> (int) (float) value)
                        .orElse(0);
                    continue;
                }

                if (provider.getOutputConnections().contains(DeployerPanelConnections.REDSTONE.get())) {
                    booleanPower |= provider.getConnectionValue(DeployerPanelConnections.REDSTONE.get()).orElse(false);
                }
            }

            panelPower = Mth.clamp(panelPower, 0, 15);
            if (panelPower == 0 && booleanPower) {
                panelPower = 15;
            }
            power = Math.max(power, panelPower);
        }

        boolean previouslyPowered = state.getValue(RedstoneLinkBlock.POWERED);
        if (previouslyPowered != power > 0) {
            level.setBlock(pos, state.cycle(RedstoneLinkBlock.POWERED), 2);
        }

        if (blockEntity != null) {
            blockEntity.transmit(power);
        }
        ci.cancel();
    }
}
