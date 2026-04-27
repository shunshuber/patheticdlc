package com.pathdlc.digger.baritone;

import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

public class BaritoneBridge {
    private long lastErrorAt = 0L;

    public boolean isAvailable() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean execute(String command) {
        try {
            Object baritone = getPrimaryBaritone();
            Object manager = invokeNoArgs(baritone, "getCommandManager");

            Method execute = manager.getClass().getMethod("execute", String.class);
            execute.invoke(manager, command);

            return true;
        } catch (Throwable throwable) {
            rememberError();
            return false;
        }
    }

    public boolean gotoBlock(BlockPos pos) {
        if (pos == null) {
            return false;
        }

        return execute("goto " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    public void cancel() {
        execute("cancel");
    }

    public boolean clearArea(BlockPos a, BlockPos b) {
        try {
            Object baritone = getPrimaryBaritone();
            Object builder = invokeNoArgs(baritone, "getBuilderProcess");

            Method clearArea = builder.getClass().getMethod(
                    "clearArea",
                    BlockPos.class,
                    BlockPos.class
            );

            clearArea.invoke(builder, a, b);
            return true;
        } catch (Throwable throwable) {
            rememberError();
            return false;
        }
    }


    public boolean buildSchematic(String name, Object schematic, BlockPos origin) {
        if (schematic == null || origin == null) {
            return false;
        }

        try {
            Object baritone = getPrimaryBaritone();
            Object builder = invokeNoArgs(baritone, "getBuilderProcess");

            for (Method method : builder.getClass().getMethods()) {
                if (!method.getName().equals("build")) {
                    continue;
                }

                Class<?>[] parameters = method.getParameterTypes();

                if (parameters.length != 3) {
                    continue;
                }

                if (!parameters[0].isAssignableFrom(String.class)) {
                    continue;
                }

                if (!parameters[1].isInstance(schematic)) {
                    continue;
                }

                if (!parameters[2].isAssignableFrom(origin.getClass())) {
                    continue;
                }

                method.invoke(builder, name, schematic, origin);
                return true;
            }

            rememberError();
            return false;
        } catch (Throwable throwable) {
            rememberError();
            return false;
        }
    }

    public boolean hadRecentError() {
        return System.currentTimeMillis() - lastErrorAt < 5000L;
    }

    private Object getPrimaryBaritone() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
        Method getProvider = apiClass.getMethod("getProvider");
        Object provider = getProvider.invoke(null);

        return invokeNoArgs(provider, "getPrimaryBaritone");
    }

    private Object invokeNoArgs(Object owner, String methodName) throws ReflectiveOperationException {
        Method method = owner.getClass().getMethod(methodName);
        return method.invoke(owner);
    }

    private void rememberError() {
        lastErrorAt = System.currentTimeMillis();
    }
}
