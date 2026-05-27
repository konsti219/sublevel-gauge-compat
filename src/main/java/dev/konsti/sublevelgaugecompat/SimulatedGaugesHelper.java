package dev.konsti.sublevelgaugecompat;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;

public final class SimulatedGaugesHelper {
    private static final String ALTITUDE_PANEL = "io.github.techtastic.simulated_gauges.content.logistics.board.AltitudeSensorPanelBehaviour";
    private static final String VECTOR_PANEL = "io.github.techtastic.simulated_gauges.content.logistics.board.AbstractVector3PanelBehaviour";

    private static Class<?> altitudePanelClass;
    private static Class<?> vectorPanelClass;
    private static Method altitudeOutputMethod;
    private static Method vectorValueMethod;
    private static boolean resolvedPanels;

    private SimulatedGaugesHelper() {
    }

    public static Double getDynamicPanelOutput(Object panel) {
        resolvePanels(panel.getClass().getClassLoader());

        try {
            if (altitudePanelClass != null && altitudePanelClass.isInstance(panel)) {
                return ((Number) altitudeOutputMethod.invoke(panel)).doubleValue();
            }
            if (vectorPanelClass != null && vectorPanelClass.isInstance(panel)) {
                return ((Number) vectorValueMethod.invoke(panel)).doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    public static boolean valueChanged(double previous, double current) {
        return Double.isNaN(previous) || Math.abs(previous - current) > 1e-4;
    }

    private static void resolvePanels(ClassLoader classLoader) {
        if (resolvedPanels) {
            return;
        }

        resolvedPanels = true;

        try {
            altitudePanelClass = Class.forName(ALTITUDE_PANEL, false, classLoader);
            altitudeOutputMethod = altitudePanelClass.getMethod("getOutput");
        } catch (ReflectiveOperationException ignored) {
            altitudePanelClass = null;
            altitudeOutputMethod = null;
        }

        try {
            vectorPanelClass = Class.forName(VECTOR_PANEL, false, classLoader);
            vectorValueMethod = vectorPanelClass.getMethod("getVectorValue");
        } catch (ReflectiveOperationException ignored) {
            vectorPanelClass = null;
            vectorValueMethod = null;
        }
    }

    public static boolean shouldNotifySupport(BlockEntity blockEntity, double previous, double current) {
        return !blockEntity.getLevel().isClientSide && valueChanged(previous, current);
    }
}
