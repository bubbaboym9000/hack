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
 * One panel per category. Left click a module to toggle it, right click to expand its settings.
 * Click a setting to change it (left = next / up, right = previous / down).
 */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_W = 112;
    private static final int HEADER_H = 16;
    private static final int ROW_H = 14;
    private static final int SETTING_H = 12;
    private static final int GAP = 8;
    private static final int TOP = 24;
    private static final int ACCENT = 0xFF8B5CF6;

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

        g.drawCenteredString(this.font, "Nightfall  -  left click: toggle / change   right click: settings   wheel: scroll",
                this.width / 2, 8, 0xFFB388FF);

        final String[] hovered = {null};

        layout((category, module, setting, x, y, w, h) -> {
            boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

            if (module == null) {
                g.fill(x, y, x + w, y + h, 0xFF120A22);
                g.fill(x, y + h - 1, x + w, y + h, ACCENT);
                g.drawString(this.font, category.label, x + 6, y + 4, 0xFFFFFFFF);
            } else if (setting == null) {
                int background = module.isEnabled() ? 0xE0522A9C : (over ? 0xE0302848 : 0xE01A1426);
                g.fill(x, y, x + w, y + h, background);
                g.drawString(this.font, module.name, x + 6, y + 3, module.isEnabled() ? 0xFFFFFFFF : 0xFFB8B0C8);
                if (!module.getSettings().isEmpty()) {
                    g.drawString(this.font, EXPANDED.contains(module) ? "-" : "+", x + w - 10, y + 3, 0xFF9088A8);
                }
                if (over) {
                    hovered[0] = module.description;
                }
            } else {
                g.fill(x, y, x + w, y + h, over ? 0xE0241C38 : 0xE0150F22);
                g.drawString(this.font, setting.name, x + 10, y + 2, 0xFFC8C0D8);
                String value = setting.display();
                g.drawString(this.font, value, x + w - this.font.width(value) - 6, y + 2, 0xFFB388FF);
            }
        });

        if (hovered[0] != null) {
            g.drawString(this.font, hovered[0], 6, this.height - 12, 0xFFFFFFFF);
        }
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
