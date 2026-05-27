package dev.konsti.sublevelgaugecompat.mixin;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
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
        FactoryPanelBehaviour behaviour = createSableFactoryGaugeFix$findPanelBehaviour(world, pos);
        if (behaviour == null) {
            return;
        }

        cir.setReturnValue(behaviour);
    }

    @Inject(
        method = "linkAt(Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;)Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelSupportBehaviour;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void createSableFactoryGaugeFix$resolveSublevelLinkSupport(
        BlockAndTintGetter world, FactoryPanelPosition pos, CallbackInfoReturnable<FactoryPanelSupportBehaviour> cir
    ) {
        FactoryPanelSupportBehaviour behaviour = createSableFactoryGaugeFix$findLinkSupport(world, pos);
        if (behaviour != null) {
            cir.setReturnValue(behaviour);
        }
    }

    private static FactoryPanelBehaviour createSableFactoryGaugeFix$findPanelBehaviour(
        BlockAndTintGetter world, FactoryPanelPosition pos
    ) {
        if (!(world instanceof Level level)) {
            return world.getBlockEntity(pos.pos()) instanceof FactoryPanelBlockEntity blockEntity
                ? createSableFactoryGaugeFix$getActivePanel(blockEntity, pos)
                : null;
        }

        if (world.getBlockEntity(pos.pos()) instanceof FactoryPanelBlockEntity blockEntity) {
            FactoryPanelBehaviour behaviour = createSableFactoryGaugeFix$getActivePanel(blockEntity, pos);
            if (behaviour != null) {
                return behaviour;
            }
        }

        return Sable.HELPER.runIncludingSubLevels(
            level,
            Vec3.atCenterOf(pos.pos()),
            true,
            Sable.HELPER.getContaining(level, pos.pos()),
            (subLevel, internalPos) -> {
                if (!(level.getBlockEntity(internalPos) instanceof FactoryPanelBlockEntity blockEntity)) {
                    return null;
                }
                return createSableFactoryGaugeFix$getActivePanel(blockEntity, pos);
            }
        );
    }

    private static FactoryPanelSupportBehaviour createSableFactoryGaugeFix$findLinkSupport(
        BlockAndTintGetter world, FactoryPanelPosition pos
    ) {
        if (!(world instanceof Level level)) {
            return BlockEntityBehaviour.get(world, pos.pos(), FactoryPanelSupportBehaviour.TYPE);
        }

        FactoryPanelSupportBehaviour behaviour =
            BlockEntityBehaviour.get(world, pos.pos(), FactoryPanelSupportBehaviour.TYPE);
        if (behaviour != null) {
            return behaviour;
        }

        return Sable.HELPER.runIncludingSubLevels(
            level,
            Vec3.atCenterOf(pos.pos()),
            true,
            Sable.HELPER.getContaining(level, pos.pos()),
            (subLevel, internalPos) -> BlockEntityBehaviour.get(level, internalPos, FactoryPanelSupportBehaviour.TYPE)
        );
    }

    private static FactoryPanelBehaviour createSableFactoryGaugeFix$getActivePanel(
        FactoryPanelBlockEntity blockEntity, FactoryPanelPosition pos
    ) {
        FactoryPanelBehaviour behaviour = blockEntity.panels.get(pos.slot());
        return behaviour != null && behaviour.isActive() ? behaviour : null;
    }
}
