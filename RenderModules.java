package com.nightfall.modules;

import com.nightfall.core.Category;
import com.nightfall.core.Module;
import com.nightfall.core.ModuleManager;
import com.nightfall.core.Setting;
import com.nightfall.util.TargetUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RenderModules {
    private RenderModules() {
    }

    public static void register(ModuleManager m) {
        m.add(new Fullbright());
        m.add(new Esp());
        m.add(new XRay());
        m.add(new Zoom());
        m.add(new Radar());
        m.add(new Hud());
    }

    // ------------------------------------------------------------------

    public static final class Fullbright extends Module {
        public Fullbright() {
            super("Fullbright", "See everything at full brightness (client-side night vision).", Category.RENDER);
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            MobEffectInstance current = p.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 400) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false, false));
            }
        }

        @Override
        protected void onDisable() {
            if (mc.player != null) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    // ------------------------------------------------------------------

    /** Outlines entities through walls using the vanilla glowing effect (see EntityMixin). */
    public static final class Esp extends Module {
        private final Setting.Bool players = bool("Players", true);
        private final Setting.Bool mobs = bool("Mobs", true);
        private final Setting.Bool animals = bool("Animals", false);
        private final Setting.Bool items = bool("Items", false);

        public Esp() {
            super("ESP", "Outlines entities through walls.", Category.RENDER);
        }

        public boolean shouldGlow(Entity e) {
            if (e == mc.player) {
                return false;
            }
            if (items.on() && e instanceof ItemEntity) {
                return true;
            }
            return TargetUtil.isValid(e, players.on(), mobs.on(), animals.on());
        }
    }

    // ------------------------------------------------------------------

    /** Hides everything except ores and valuable blocks (see BlockMixin). */
    public static final class XRay extends Module {
        private static final Set<String> EXTRA = Set.of(
                "ancient_debris", "spawner", "trial_spawner", "vault",
                "diamond_block", "emerald_block", "gold_block", "iron_block",
                "netherite_block", "lapis_block", "redstone_block", "coal_block",
                "copper_block", "budding_amethyst", "amethyst_cluster");

        // Chunk meshing runs on several threads, so the cache must be thread-safe.
        private final Map<Block, Boolean> cache = new ConcurrentHashMap<>();

        public XRay() {
            super("XRay", "Shows only ores and valuables. Pair with Fullbright to see in caves.", Category.RENDER);
        }

        public boolean isVisible(Block block) {
            return cache.computeIfAbsent(block, b -> {
                var key = BuiltInRegistries.BLOCK.getKey(b);
                String path = key.getPath();
                return path.endsWith("_ore") || EXTRA.contains(path);
            });
        }

        private void rebuildChunks() {
            if (mc.levelRenderer != null) {
                mc.levelRenderer.allChanged();
            }
        }

        @Override
        protected void onEnable() {
            rebuildChunks();
        }

        @Override
        protected void onDisable() {
            rebuildChunks();
        }
    }

    // ------------------------------------------------------------------

    public static final class Zoom extends Module {
        private final Setting.Num fov = num("FOV", 30, 30, 90, 5);
        private int previous = -1;

        public Zoom() {
            super("Zoom", "Narrows your field of view. Lower is further.", Category.RENDER);
        }

        @Override
        protected void onEnable() {
            previous = mc.options.fov().get();
            mc.options.fov().set(fov.i());
        }

        @Override
        public void onTick() {
            if (mc.options.fov().get() != fov.i()) {
                mc.options.fov().set(fov.i());
            }
        }

        @Override
        protected void onDisable() {
            if (previous != -1) {
                mc.options.fov().set(previous);
                previous = -1;
            }
        }
    }

    // ------------------------------------------------------------------

    /** Small top-down minimap of nearby entities. Red = players, orange = hostile, green = other. */
    public static final class Radar extends Module {
        private final Setting.Num range = num("Range", 40, 10, 100, 5);
        private final Setting.Num size = num("Size", 90, 60, 160, 10);

        public Radar() {
            super("Radar", "2D radar showing nearby entities.", Category.RENDER);
        }

        @Override
        public void onHud(GuiGraphics g) {
            LocalPlayer p = mc.player;
            int box = size.i();
            int x0 = 6;
            int y0 = 30;
            int accent = 0xFF8B5CF6;

            g.fill(x0, y0, x0 + box, y0 + box, 0x90000000);
            g.fill(x0, y0, x0 + box, y0 + 1, accent);
            g.fill(x0, y0 + box - 1, x0 + box, y0 + box, accent);
            g.fill(x0, y0, x0 + 1, y0 + box, accent);
            g.fill(x0 + box - 1, y0, x0 + box, y0 + box, accent);

            int cx = x0 + box / 2;
            int cy = y0 + box / 2;
            g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);

            double yaw = Math.toRadians(p.getYRot());
            double sin = Math.sin(yaw);
            double cos = Math.cos(yaw);
            double scale = (box / 2.0 - 3) / range.d();

            for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(range.d()), x -> true)) {
                if (!(e instanceof LivingEntity) || e == p) {
                    continue;
                }
                double dx = e.getX() - p.getX();
                double dz = e.getZ() - p.getZ();
                double right = -dx * cos - dz * sin;
                double forward = -dx * sin + dz * cos;
                int px = cx + (int) Math.round(right * scale);
                int py = cy - (int) Math.round(forward * scale);
                if (px < x0 + 2 || px > x0 + box - 3 || py < y0 + 2 || py > y0 + box - 3) {
                    continue;
                }
                int color = e instanceof Player ? 0xFFFF5555 : e instanceof Enemy ? 0xFFFFAA00 : 0xFF55FF55;
                g.fill(px - 1, py - 1, px + 1, py + 1, color);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Hud extends Module {
        private final Setting.Bool watermark = bool("Watermark", true);
        private final Setting.Bool arrayList = bool("ArrayList", true);
        private final Setting.Bool coords = bool("Coordinates", true);

        public Hud() {
            super("HUD", "Watermark, enabled-module list and coordinates.", Category.RENDER);
            setEnabled(true);
        }

        private static String label(Module m) {
            String suffix = m.getSuffix();
            return suffix.isEmpty() ? m.name : m.name + " §7" + suffix;
        }

        @Override
        public void onHud(GuiGraphics g) {
            Font font = mc.font;
            LocalPlayer p = mc.player;
            int width = g.guiWidth();
            int height = g.guiHeight();

            if (watermark.on()) {
                g.drawString(font, "Nightfall", 4, 4, 0xFFB388FF);
            }

            if (coords.on()) {
                String text = String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ());
                g.drawString(font, text, 4, height - 12, 0xFFFFFFFF);
            }

            if (arrayList.on()) {
                List<Module> active = new ArrayList<>();
                for (Module m : ModuleManager.INSTANCE.all()) {
                    if (m.isEnabled() && m != this) {
                        active.add(m);
                    }
                }
                active.sort(Comparator.comparingInt((Module m) -> -font.width(label(m))));

                float hueBase = (System.currentTimeMillis() % 6000L) / 6000.0f;
                int y = 2;
                int index = 0;
                for (Module m : active) {
                    String text = label(m);
                    int x = width - font.width(text) - 4;
                    int color = 0xFF000000 | Mth.hsvToRgb((hueBase + index * 0.04f) % 1.0f, 0.55f, 1.0f);
                    g.fill(x - 2, y - 1, width, y + 10, 0x80000000);
                    g.fill(width - 1, y - 1, width, y + 10, color);
                    g.drawString(font, text, x, y, color);
                    y += 11;
                    index++;
                }
            }
        }
    }
}
