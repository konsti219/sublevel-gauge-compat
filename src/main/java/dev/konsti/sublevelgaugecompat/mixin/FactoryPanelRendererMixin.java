package dev.konsti.sublevelgaugecompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelRenderer;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.konsti.sublevelgaugecompat.DeployerSelectionHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Optional;

@Mixin(value = FactoryPanelRenderer.class, priority = 1500)
public class FactoryPanelRendererMixin {
    private static final float HITBOX_THICKNESS = 0.25f;
    private static final float HITBOX_NORMAL_THICKNESS = 0.05f;
    private static final float HITBOX_PLANE_EXPANSION = 1 / 32f;

    @Inject(
        method = "renderPath",
        at = @At(
            value = "INVOKE",
            target = "Lnet/createmod/catnip/render/SuperByteBuffer;color(I)Lnet/createmod/catnip/render/SuperByteBuffer;"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void createSableFactoryGaugeFix$pickDeployerConnectionsOnSublevels(
        FactoryPanelBehaviour behaviour, FactoryPanelConnection connection, float partialTicks, PoseStack ms,
        MultiBufferSource buffer, int light, int overlay, CallbackInfo ci,
        BlockState blockState, List<Direction> path, float xRot, float yRot, float glow,
        FactoryPanelSupportBehaviour sbe, boolean displayLinkMode, boolean redstoneLinkMode, boolean pathReversed,
        int color, float yOffset, boolean success, boolean dots, float currentX, float currentZ, int i,
        Direction direction, boolean isArrowSegment, PartialModel partial, SuperByteBuffer connectionSprite
    ) {
        if (!DeployerSelectionHelper.isAvailable() || DeployerSelectionHelper.getSelectedConnection() != null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.getMainHandItem().is(AllItems.WRENCH.get())) {
            return;
        }

        float y = (yOffset + (direction.get2DDataValue() % 2) * 0.125f) / 512f;

        Vector3f pos = new Vector3f(
            currentX + behaviour.slot.xOffset * .5f + .25f,
            y,
            currentZ + behaviour.slot.yOffset * .5f + .25f
        ).add(-.5f, -.5f, -.5f);
        Quaternionf rotation = new Quaternionf()
            .rotationY((float) (yRot + Math.PI))
            .rotateX(-xRot);
        rotation.transform(pos);
        pos = pos.add(.5f, .5f, .5f);

        Direction.Axis axis = FactoryPanelBlock.connectedDirection(blockState).getAxis();
        float thickX = axis == Direction.Axis.X ? HITBOX_NORMAL_THICKNESS : HITBOX_THICKNESS + HITBOX_PLANE_EXPANSION;
        float thickY = axis == Direction.Axis.Y ? HITBOX_NORMAL_THICKNESS : HITBOX_THICKNESS + HITBOX_PLANE_EXPANSION;
        float thickZ = axis == Direction.Axis.Z ? HITBOX_NORMAL_THICKNESS : HITBOX_THICKNESS + HITBOX_PLANE_EXPANSION;

        AABB localAabb = new AABB(
            pos.x - thickX,
            pos.y - thickY,
            pos.z - thickZ,
            pos.x + thickX,
            pos.y + thickY,
            pos.z + thickZ
        ).move(behaviour.blockEntity.getBlockPos());

        Vec3 eyePos = mc.player.getEyePosition(partialTicks);
        Vec3 rayEnd = eyePos.add(mc.player.getViewVector(partialTicks).scale(mc.player.blockInteractionRange()));

        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(behaviour.blockEntity);
        Optional<Vec3> hit;
        Vec3 worldHit;

        if (subLevel != null) {
            float pt = AnimationTickHolder.getPartialTicks(mc.level);
            Vec3 localEyePos = subLevel.renderPose(pt).transformPositionInverse(eyePos);
            Vec3 localRayEnd = subLevel.renderPose(pt).transformPositionInverse(rayEnd);
            hit = localAabb.clip(localEyePos, localRayEnd);
            if (hit.isEmpty()) {
                return;
            }
            worldHit = subLevel.renderPose(pt).transformPosition(hit.get());
        } else {
            hit = localAabb.clip(eyePos, rayEnd);
            if (hit.isEmpty()) {
                return;
            }
            worldHit = hit.get();
        }

        if (mc.hitResult != null && mc.hitResult.getLocation().distanceToSqr(eyePos) <= worldHit.distanceToSqr(eyePos)) {
            return;
        }

        DeployerSelectionHelper.setSelectedConnection(connection, behaviour.getPanelPosition());
    }
}
