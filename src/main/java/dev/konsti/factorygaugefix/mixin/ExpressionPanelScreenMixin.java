package dev.konsti.factorygaugefix.mixin;

import dev.konsti.factorygaugefix.ExpressionGaugeCompatHelper;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Pseudo
@Mixin(targets = "net.liukrast.eg.content.logistics.board.ExpressionPanelScreen", remap = false)
public abstract class ExpressionPanelScreenMixin {
    @Shadow
    private EditBox expressionBox;

    @Shadow
    private char[] variables;

    @Shadow
    private String error;

    @Inject(method = "init", at = @At("TAIL"))
    private void sublevelGaugeCompat$expandExpressionBox(CallbackInfo ci) {
        if (expressionBox != null) {
            expressionBox.setMaxLength(ExpressionGaugeCompatHelper.MAX_EXPRESSION_LENGTH);
        }
    }

    @Inject(method = "evaluateExpression", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$validateExtendedExpressions(CallbackInfo ci) {
        if (expressionBox == null) {
            ci.cancel();
            return;
        }

        try {
            Map<String, Double> previewVariables = IntStream.range(0, variables.length)
                .mapToObj(index -> variables[index])
                .collect(Collectors.toMap(
                    variable -> Character.toString(variable),
                    variable -> 1d
                ));
            ExpressionGaugeCompatHelper.validate(expressionBox.getValue(), previewVariables);
            error = null;
        } catch (Exception e) {
            error = e.getMessage();
        }

        ci.cancel();
    }
}
