package dev.konsti.factorygaugefix;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;

import java.lang.reflect.Field;

public final class DeployerSelectionHelper {
    private static boolean resolved;
    private static boolean available;
    private static Field selectedConnectionField;
    private static Field selectedSourceField;

    private DeployerSelectionHelper() {}

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    public static Object getSelectedConnection() {
        resolve();
        if (!available) {
            return null;
        }

        try {
            return selectedConnectionField.get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static void setSelectedConnection(FactoryPanelConnection connection, FactoryPanelPosition source) {
        resolve();
        if (!available) {
            return;
        }

        try {
            selectedConnectionField.set(null, connection);
            selectedSourceField.set(null, source);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        resolved = true;

        try {
            Class<?> clientClass = Class.forName("net.liukrast.deployer.lib.DeployerClient");
            selectedConnectionField = clientClass.getDeclaredField("SELECTED_CONNECTION");
            selectedSourceField = clientClass.getDeclaredField("SELECTED_SOURCE");
            selectedConnectionField.setAccessible(true);
            selectedSourceField.setAccessible(true);
            available = true;
        } catch (ReflectiveOperationException ignored) {
            available = false;
        }
    }
}
