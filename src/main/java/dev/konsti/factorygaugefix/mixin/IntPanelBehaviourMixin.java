package dev.konsti.factorygaugefix.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import net.liukrast.deployer.lib.logistics.board.AbstractPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.PanelType;
import net.liukrast.deployer.lib.logistics.board.ScrollPanelBehaviour;
import net.liukrast.deployer.lib.logistics.board.connection.PanelConnectionBuilder;
import net.liukrast.deployer.lib.registry.DeployerPanelConnections;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.DecimalFormat;
import java.util.List;

@Pseudo
@Mixin(targets = "net.liukrast.eg.content.logistics.board.IntPanelBehaviour", remap = false)
public abstract class IntPanelBehaviourMixin extends AbstractPanelBehaviour {
    @Unique
    private static final int sublevelGaugeCompat$memoryModeIndex = 3;

    @Unique
    private static final float sublevelGaugeCompat$floatEpsilon = 1e-6f;

    @Unique
    private float sublevelGaugeCompat$memoryValue;

    protected IntPanelBehaviourMixin(PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot);
    }

    @Inject(method = "addConnections", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$registerFloatMemoryOutput(PanelConnectionBuilder builder, CallbackInfo ci) {
        builder.registerBoth(DeployerPanelConnections.NUMBERS, this::sublevelGaugeCompat$getOutputValue);
        builder.registerInput(DeployerPanelConnections.REDSTONE);
        builder.registerOutput(DeployerPanelConnections.STRING.get(), () -> getDisplayLinkComponent(false).getString());
        ci.cancel();
    }

    @Inject(method = "easyWrite", at = @At("TAIL"))
    private void sublevelGaugeCompat$writeMemoryValue(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        nbt.putFloat("MemoryValue", sublevelGaugeCompat$memoryValue);
    }

    @Inject(method = "easyRead", at = @At("TAIL"))
    private void sublevelGaugeCompat$readMemoryValue(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        sublevelGaugeCompat$memoryValue = nbt.contains("MemoryValue") ? nbt.getFloat("MemoryValue") : count;
    }

    @Inject(method = "notifiedFromInput", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$allowFloatMemoryMode(CallbackInfo ci) {
        if (!active) {
            ci.cancel();
            return;
        }

        List<Boolean> flagList = getAllValues(DeployerPanelConnections.REDSTONE.get());
        if (flagList == null) {
            ci.cancel();
            return;
        }

        boolean flagged = flagList.stream().anyMatch(Boolean::booleanValue);

        if (sublevelGaugeCompat$isMemoryMode()) {
            if (flagged) {
                if (redstonePowered != flagged) {
                    redstonePowered = true;
                    blockEntity.notifyUpdate();
                }
                ci.cancel();
                return;
            }

            List<Float> countList = getAllValues(DeployerPanelConnections.NUMBERS.get());
            if (countList == null) {
                ci.cancel();
                return;
            }

            float result = countList.stream().reduce(0f, Float::sum);
            if (Math.abs(result - sublevelGaugeCompat$memoryValue) >= sublevelGaugeCompat$floatEpsilon || redstonePowered != flagged) {
                redstonePowered = false;
                sublevelGaugeCompat$memoryValue = result;
                count = (int) result;
                blockEntity.notifyUpdate();
                notifyOutputs();
            }

            ci.cancel();
            return;
        }

        int result = flagged ? 0 : sublevelGaugeCompat$evaluateIntegerMode();
        if (result != count || flagged != redstonePowered) {
            redstonePowered = flagged;
            count = result;
            sublevelGaugeCompat$memoryValue = result;
            blockEntity.notifyUpdate();
            notifyOutputs();
        }

        ci.cancel();
    }

    @Inject(method = "getDisplayLinkComponent", at = @At("HEAD"), cancellable = true)
    private void sublevelGaugeCompat$displayFloatMemoryValue(boolean shortened, CallbackInfoReturnable<MutableComponent> cir) {
        if (!sublevelGaugeCompat$isMemoryMode()) {
            return;
        }

        cir.setReturnValue(Component.literal(sublevelGaugeCompat$formatFloat(sublevelGaugeCompat$memoryValue, shortened)));
    }

    @Unique
    private boolean sublevelGaugeCompat$isMemoryMode() {
        return sublevelGaugeCompat$getScrollValue() == sublevelGaugeCompat$memoryModeIndex;
    }

    @Unique
    private int sublevelGaugeCompat$evaluateIntegerMode() {
        List<Float> countList = getAllValues(DeployerPanelConnections.NUMBERS.get());
        if (countList == null) {
            return count;
        }

        return switch (sublevelGaugeCompat$getScrollValue()) {
            case 0 -> countList.stream().mapToInt(number -> (int) (float) number).sum();
            case 1 -> -countList.stream().mapToInt(number -> (int) (float) number).sum();
            case 2 -> countList.stream().mapToInt(number -> (int) (float) number).reduce(1, (left, right) -> left * right);
            default -> count;
        };
    }

    @Unique
    private float sublevelGaugeCompat$getOutputValue() {
        return sublevelGaugeCompat$isMemoryMode() ? sublevelGaugeCompat$memoryValue : count;
    }

    @Unique
    private int sublevelGaugeCompat$getScrollValue() {
        return ((ScrollPanelBehaviour) (Object) this).getValue();
    }

    @Unique
    private static String sublevelGaugeCompat$formatFloat(float number, boolean shortened) {
        if (shortened) {
            float abs = Math.abs(number);
            if (abs >= 1_000_000f) {
                return String.format("%.1fM", number / 1_000_000f);
            }
            if (abs >= 1_000f) {
                return String.format("%.1fK", number / 1_000f);
            }
        }

        return new DecimalFormat("0.##").format(number);
    }
}
