package com.nightfall.core;

import com.nightfall.NightfallClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every Nightfall module. Subclasses declare their settings as fields
 * (using {@link #bool}, {@link #num}, {@link #mode}) and override the lifecycle hooks.
 */
public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;

    protected final Minecraft mc = Minecraft.getInstance();

    /** Toggle key (rebindable in Options > Controls > Nightfall). Set by NightfallClient. */
    public KeyMapping keyMapping;

    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<>();

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    // ---- state -------------------------------------------------------

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        try {
            if (value) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (Exception e) {
            NightfallClient.LOGGER.error("Module {} failed during {}", name, value ? "enable" : "disable", e);
        }
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    // ---- hooks -------------------------------------------------------

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Called every client tick while enabled and in a world. */
    public void onTick() {
    }

    /** Called every HUD frame while enabled and in a world. */
    public void onHud(GuiGraphics graphics) {
    }

    /** Short text shown next to the name in the HUD module list. */
    public String getSuffix() {
        return "";
    }

    // ---- settings ----------------------------------------------------

    public final List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public final Setting<?> getSetting(String settingName) {
        for (Setting<?> s : settings) {
            if (s.name.equalsIgnoreCase(settingName)) {
                return s;
            }
        }
        return null;
    }

    protected final Setting.Bool bool(String settingName, boolean defaultValue) {
        Setting.Bool s = new Setting.Bool(settingName, defaultValue);
        settings.add(s);
        return s;
    }

    protected final Setting.Num num(String settingName, double defaultValue, double min, double max, double step) {
        Setting.Num s = new Setting.Num(settingName, defaultValue, min, max, step);
        settings.add(s);
        return s;
    }

    protected final Setting.Mode mode(String settingName, String defaultValue, String... modes) {
        Setting.Mode s = new Setting.Mode(settingName, defaultValue, modes);
        settings.add(s);
        return s;
    }
}
