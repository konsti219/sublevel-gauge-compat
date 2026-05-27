package dev.konsti.sublevelgaugecompat.mixin;

import dev.konsti.sublevelgaugecompat.ExpressionGaugeCompatHelper;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Pseudo
@Mixin(targets = "net.liukrast.eg.content.logistics.board.ExpressionPanelScreen", remap = false)
public abstract class ExpressionPanelScreenMixin {
    private static final String BASIC_PANEL_SCREEN_CLASS = "net.liukrast.deployer.lib.logistics.board.screen.BasicPanelScreen";

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
            String storedExpression = sublevelGaugeCompat$getStoredExpression();
            if (storedExpression != null && !storedExpression.equals(expressionBox.getValue())) {
                expressionBox.setValue(storedExpression);
            }
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
    private AbstractPanelBehaviour sublevelGaugeCompat$getBehaviour() {
        try {
            Class<?> screenClass = Class.forName(BASIC_PANEL_SCREEN_CLASS);
            Field behaviourField = screenClass.getField("behaviour");
            return (AbstractPanelBehaviour) behaviourField.get(this);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private String sublevelGaugeCompat$getStoredExpression() {
        AbstractPanelBehaviour behaviour = sublevelGaugeCompat$getBehaviour();
        if (behaviour == null) {
            return null;
        }

        try {
            Method getExpression = behaviour.getClass().getMethod("getExpression");
            Object value = getExpression.invoke(behaviour);
            return value instanceof String string ? string : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
