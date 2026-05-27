package dev.konsti.sublevelgaugecompat.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import dev.konsti.sublevelgaugecompat.ExpressionGaugeCompatHelper;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.PanelType;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnectionBuilder;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Pseudo
@Mixin(targets = "net.liukrast.eg.content.logistics.board.ExpressionPanelBehaviour", remap = false)
public abstract class ExpressionPanelBehaviourMixin extends AbstractPanelBehaviour {
    @Shadow
    private float output;

    @Shadow
    private String numberExpression;

    protected ExpressionPanelBehaviourMixin(PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot);
    }

    @Inject(method = "addConnections", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$registerRedstoneOutput(PanelConnectionBuilder builder, CallbackInfo ci) {
        builder.registerBoth(DeployerPanelConnections.NUMBERS, () -> output);
        builder.registerOutput(DeployerPanelConnections.REDSTONE.get(), this::sublevelGaugeCompat$hasRedstoneOutput);
        builder.registerInput(DeployerPanelConnections.REDSTONE);
        builder.registerOutput(DeployerPanelConnections.STRING.get(), () -> getDisplayLinkComponent(false).getString());
        ci.cancel();
    }

    @Inject(method = "notifiedFromInput", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$evaluateExtendedExpressions(CallbackInfo ci) {
        super.notifiedFromInput();
        List<Boolean> flagList = getAllValues(DeployerPanelConnections.REDSTONE.get());
        if (flagList == null) {
            ci.cancel();
            return;
        }

        boolean flagged = flagList.stream().anyMatch(Boolean::booleanValue);

        float result;
        if (!flagged) {
            List<ConnectionValue<Float>> inputs = getAllValuesWithSource(DeployerPanelConnections.NUMBERS.get());
            if (inputs == null) {
                ci.cancel();
                return;
            }

            try {
                Map<String, Double> variables = inputs.stream().collect(Collectors.toMap(
                    value -> String.valueOf((char) ('a' + value.connection().amount)),
                    value -> (double) value.value(),
                    Double::sum
                ));
                result = (float) ExpressionGaugeCompatHelper.evaluate(numberExpression, variables);
            } catch (Exception ignored) {
                result = 0;
            }
        } else {
            result = 0;
        }

        if (Math.abs(result - output) >= 1e-6f) {
            redstonePowered = flagged;
            output = result;
            blockEntity.notifyUpdate();
            notifyOutputs();
        }

        ci.cancel();
    }

    @Inject(method = "setFilter", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$clampExpressionLength(String expression, CallbackInfo ci) {
        numberExpression = ExpressionGaugeCompatHelper.clampExpression(expression);
        blockEntity.notifyUpdate();
        notifiedFromInput();
        ci.cancel();
    }

    private boolean sublevelGaugeCompat$hasRedstoneOutput() {
        return Math.abs(output) >= 1e-6f;
    }
}
