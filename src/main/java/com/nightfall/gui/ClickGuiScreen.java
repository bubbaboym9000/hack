package com.nightfall.gui;

import com.nightfall.core.Category;
import com.nightfall.core.ConfigManager;
import com.nightfall.core.Module;
import com.nightfall.core.ModuleManager;
import com.nightfall.core.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * One panel per category on a night-sky backdrop. Left click a module to toggle it, right click to
 * expand its settings. Click a setting to change it (left = next / up, right = previous / down).
 */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_W = 112;
    private static final int HEADER_H = 16;
    private static final int ROW_H = 14;
    private static final int SETTING_H = 12;
    private static final int GAP = 8;
    private static final int TOP = 24;

    /** Kept static so panels stay expanded between openings. */
    private static final Set<Module> EXPANDED = new HashSet<>();

    private double scroll;
    private int contentBottom;

    private interface RowVisitor {
        /** module == null: category header. setting == null: module row. Otherwise: setting row. */
        void row(Category category, Module module, Setting<?> setting, int x, int y, int w, int h);
    }

    public ClickGuiScreen() {
        super(Component.literal("Nightfall"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void layout(RowVisitor visitor) {
        Category[] categories = Category.values();
        contentBottom = 0;
        int total = categories.length * PANEL_W + (categories.length - 1) * GAP;
        int x = (this.width - total) / 2;

        for (Category category : categories) {
            int y = TOP + (int) scroll;
            visitor.row(category, null, null, x, y, PANEL_W, HEADER_H);
            y += HEADER_H;

            for (Module module : ModuleManager.INSTANCE.inCategory(category)) {
                visitor.row(category, module, null, x, y, PANEL_W, ROW_H);
                y += ROW_H;
                if (EXPANDED.contains(module)) {
                    for (Setting<?> setting : module.getSettings()) {
                        visitor.row(category, module, setting, x, y, PANEL_W, SETTING_H);
                        y += SETTING_H;
                    }
                }
            }
            contentBottom = Math.max(contentBottom, y - (int) scroll);
            x += PANEL_W + GAP;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        Theme.sky(g, this.width, this.height);

        // Title with a little crescent moon to its left.
        int titleWidth = this.font.width("Nightfall");
        Theme.moon(g, this.width / 2 - titleWidth / 2 - 9, 12, 5, Theme.MOON);
        g.drawCenteredString(this.font, "Nightfall", this.width / 2, 8, Theme.ACCENT_SOFT);

        final String[] hovered = {null};

        layout((category, module, setting, x, y, w, h) -> {
            boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

            if (module == null) {
                g.fill(x, y, x + w, y + h, Theme.HEADER);
                g.fill(x, y + h - 1, x + w, y + h, Theme.ACCENT);
                g.drawString(this.font, category.label, x + 6, y + 4, Theme.TEXT);
            } else if (setting == null) {
                boolean on = module.isEnabled();
                g.fill(x, y, x + w, y + h, on ? Theme.ROW_ON : (over ? Theme.ROW_HOVER : Theme.ROW));
                if (on) {
                    g.fill(x, y, x + 2, y + h, Theme.ACCENT);
                }
                g.drawString(this.font, module.name, x + 6, y + 3, on ? Theme.TEXT : Theme.TEXT_MUTED);
                if (!module.getSettings().isEmpty()) {
                    g.drawString(this.font, EXPANDED.contains(module) ? "-" : "+", x + w - 10, y + 3, Theme.TEXT_DIM);
                }
                if (over) {
                    hovered[0] = module.description;
                }
            } else {
                g.fill(x, y, x + w, y + h, over ? Theme.SETTING_HOVER : Theme.SETTING);
                g.drawString(this.font, setting.name, x + 10, y + 2, Theme.TEXT_MUTED);
                String value = setting.display();
                g.drawString(this.font, value, x + w - this.font.width(value) - 6, y + 2, Theme.ACCENT);
            }
        });

        if (hovered[0] != null) {
            g.drawString(this.font, hovered[0], 6, this.height - 12, Theme.TEXT);
        }

        String hint = "left click: toggle   right click: settings   wheel: scroll";
        g.drawString(this.font, hint, this.width - this.font.width(hint) - 6, this.height - 12, Theme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        final double mx = event.x();
        final double my = event.y();
        final int button = event.button();
        final boolean[] handled = {false};

        layout((category, module, setting, x, y, w, h) -> {
            if (handled[0] || module == null) {
                return;
            }
            if (mx < x || mx >= x + w || my < y || my >= y + h) {
                return;
            }
            handled[0] = true;
            if (setting != null) {
                setting.click(button);
            } else if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                if (!EXPANDED.remove(module)) {
                    EXPANDED.add(module);
                }
            }
        });

        return handled[0] || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double min = Math.min(0, this.height - 8 - contentBottom);
        scroll = Math.max(min, Math.min(0, scroll + scrollY * 14));
        return true;
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }
}
