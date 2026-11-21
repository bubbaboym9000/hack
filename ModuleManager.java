package com.nightfall.core;

import com.nightfall.NightfallClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> byClass = new HashMap<>();

    private ModuleManager() {
    }

    public void add(Module module) {
        modules.add(module);
        byClass.put(module.getClass(), module);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) byClass.get(type);
    }

    public List<Module> all() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> inCategory(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) {
            if (m.category == category) {
                out.add(m);
            }
        }
        return out;
    }

    public Module find(String name) {
        for (Module m : modules) {
            if (m.name.equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public void disableAll() {
        for (Module m : modules) {
            m.setEnabled(false);
        }
    }

    /** Runs on END_CLIENT_TICK. Handles toggle keys, then ticks every enabled module. */
    public void tick(Minecraft mc) {
        boolean inWorld = mc.player != null && mc.level != null;

        for (Module m : modules) {
            if (m.keyMapping == null) {
                continue;
            }
            while (m.keyMapping.consumeClick()) {
                if (inWorld) {
                    m.toggle();
                }
            }
        }

        if (!inWorld) {
            return;
        }

        for (Module m : new ArrayList<>(modules)) {
            if (!m.isEnabled()) {
                continue;
            }
            try {
                m.onTick();
            } catch (Exception e) {
                NightfallClient.LOGGER.error("Module {} threw during tick and was disabled", m.name, e);
                m.setEnabled(false);
            }
        }
    }

    public void renderHud(GuiGraphics graphics, Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        for (Module m : new ArrayList<>(modules)) {
            if (!m.isEnabled()) {
                continue;
            }
            try {
                m.onHud(graphics);
            } catch (Exception e) {
                NightfallClient.LOGGER.error("Module {} threw during HUD render and was disabled", m.name, e);
                m.setEnabled(false);
            }
        }
    }
}
