package com.nightfall.modules;

import com.nightfall.core.FriendManager;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.item.Items;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import com.nightfall.core.Category;
import com.nightfall.core.Module;
import com.nightfall.core.ModuleManager;
import com.nightfall.core.Setting;
import com.nightfall.gui.Theme;
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
        m.add(new Keystrokes());
        m.add(new ArmorHud());
        m.add(new PotionHud());
        m.add(new Info());
        m.add(new NoBob());
        m.add(new Pointers());
        m.add(new Compass());
        m.add(new TargetHud());
        m.add(new ItemCounter());
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
            int accent = Theme.ACCENT;

            g.fill(x0, y0, x0 + box, y0 + box, Theme.HUD_BG);
            g.fill(x0, y0, x0 + box, y0 + 1, accent);
            g.fill(x0, y0 + box - 1, x0 + box, y0 + box, accent);
            g.fill(x0, y0, x0 + 1, y0 + box, accent);
            g.fill(x0 + box - 1, y0, x0 + box, y0 + box, accent);

            int cx = x0 + box / 2;
            int cy = y0 + box / 2;
            g.fill(cx - 1, cy - 1, cx + 2, cy + 2, Theme.MOON);

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
                int color = e instanceof Player ? 0xFFFF6B6B : e instanceof Enemy ? 0xFFFFB454 : 0xFF7CE0A5;
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
                Theme.moon(g, 9, 8, 4, Theme.MOON);
                g.drawString(font, "Nightfall", 17, 4, Theme.ACCENT_SOFT);
            }

            if (coords.on()) {
                String text = String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ());
                g.drawString(font, text, 4, height - 12, Theme.TEXT);
            }

            if (arrayList.on()) {
                List<Module> active = new ArrayList<>();
                for (Module m : ModuleManager.INSTANCE.all()) {
                    if (m.isEnabled() && m != this) {
                        active.add(m);
                    }
                }
                active.sort(Comparator.comparingInt((Module m) -> -font.width(label(m))));

                int y = 2;
                int index = 0;
                for (Module m : active) {
                    String text = label(m);
                    int x = width - font.width(text) - 4;
                    int color = Theme.twilight(index);
                    g.fill(x - 2, y - 1, width, y + 10, Theme.HUD_BG);
                    g.fill(width - 1, y - 1, width, y + 10, color);
                    g.drawString(font, text, x, y, color);
                    y += 11;
                    index++;
                }
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Keystrokes extends Module {
        public Keystrokes() {
            super("Keystrokes", "Shows WASD and jump on screen.", Category.RENDER);
        }

        private void key(GuiGraphics g, String label, boolean down, int x, int y, int w) {
            g.fill(x, y, x + w, y + 18, down ? Theme.ACCENT_PRESSED : Theme.HUD_BG);
            g.drawCenteredString(mc.font, label, x + w / 2, y + 5, Theme.TEXT);
        }

        @Override
        public void onHud(GuiGraphics g) {
            var o = mc.options;
            int x = g.guiWidth() - 64;
            int y = g.guiHeight() - 66;
            key(g, "W", o.keyUp.isDown(), x + 20, y, 18);
            key(g, "A", o.keyLeft.isDown(), x, y + 20, 18);
            key(g, "S", o.keyDown.isDown(), x + 20, y + 20, 18);
            key(g, "D", o.keyRight.isDown(), x + 40, y + 20, 18);
            key(g, "_", o.keyJump.isDown(), x, y + 40, 58);
        }
    }

    // ------------------------------------------------------------------

    public static final class ArmorHud extends Module {
        private static final EquipmentSlot[] SLOTS = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        public ArmorHud() {
            super("ArmorHud", "Shows your armor and its durability.", Category.RENDER);
        }

        @Override
        public void onHud(GuiGraphics g) {
            LocalPlayer p = mc.player;
            int x = 4;
            int y = g.guiHeight() - 96;
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = p.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, x, y);
                    g.renderItemDecorations(mc.font, stack, x, y);
                    if (stack.isDamageableItem()) {
                        int left = stack.getMaxDamage() - stack.getDamageValue();
                        g.drawString(mc.font, Integer.toString(left), x + 20, y + 4, 0xFFFFFFFF);
                    }
                }
                y += 18;
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class PotionHud extends Module {
        public PotionHud() {
            super("PotionHud", "Lists your active potion effects with their time left.", Category.RENDER);
        }

        @Override
        public void onHud(GuiGraphics g) {
            int y = g.guiHeight() / 2 - 20;
            for (MobEffectInstance e : mc.player.getActiveEffects()) {
                String name = e.getEffect().value().getDisplayName().getString();
                int seconds = e.getDuration() / 20;
                String time = e.isInfiniteDuration()
                        ? "inf"
                        : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                int color = e.getEffect().value().isBeneficial() ? 0xFF55FF55 : 0xFFFF5555;
                g.drawString(mc.font, name + " " + (e.getAmplifier() + 1) + " \u00a77" + time, 4, y, color);
                y += 10;
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Info extends Module {
        private final Setting.Bool fps = bool("FPS", true);
        private final Setting.Bool ping = bool("Ping", true);
        private final Setting.Bool speed = bool("Speed", true);
        private final Setting.Bool clock = bool("Clock", true);

        private long lastSecond = System.nanoTime();
        private int frames;
        private int shownFps;
        private double lastX;
        private double lastZ;
        private double bps;

        public Info() {
            super("Info", "FPS, ping, speed and the time, centred at the top of the screen.", Category.RENDER);
        }

        @Override
        protected void onEnable() {
            if (mc.player != null) {
                lastX = mc.player.getX();
                lastZ = mc.player.getZ();
            }
        }

        @Override
        public void onTick() {
            LocalPlayer p = mc.player;
            double now = Math.hypot(p.getX() - lastX, p.getZ() - lastZ) * 20.0;
            bps = bps * 0.7 + now * 0.3;
            lastX = p.getX();
            lastZ = p.getZ();
        }

        @Override
        public void onHud(GuiGraphics g) {
            frames++;
            long now = System.nanoTime();
            if (now - lastSecond >= 1_000_000_000L) {
                shownFps = frames;
                frames = 0;
                lastSecond = now;
            }

            List<String> parts = new ArrayList<>();
            if (fps.on()) {
                parts.add(shownFps + " FPS");
            }
            if (ping.on()) {
                var connection = mc.getConnection();
                var info = connection == null ? null : connection.getPlayerInfo(mc.player.getUUID());
                parts.add((info == null ? 0 : info.getLatency()) + " ms");
            }
            if (speed.on()) {
                parts.add(String.format(Locale.ROOT, "%.1f b/s", bps));
            }
            if (clock.on()) {
                parts.add(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            if (!parts.isEmpty()) {
                g.drawCenteredString(mc.font, String.join("  |  ", parts), g.guiWidth() / 2, 4, Theme.TEXT);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class NoBob extends Module {
        private boolean previous;

        public NoBob() {
            super("NoBob", "Turns off the view bobbing while you walk.", Category.RENDER);
        }

        @Override
        protected void onEnable() {
            previous = mc.options.bobView().get();
            mc.options.bobView().set(false);
        }

        @Override
        protected void onDisable() {
            mc.options.bobView().set(previous);
        }
    }

    // ------------------------------------------------------------------

    public static final class Pointers extends Module {
        private final Setting.Num range = num("Range", 60, 10, 150, 5);
        private final Setting.Bool players = bool("Players", true);
        private final Setting.Bool mobs = bool("Mobs", false);

        public Pointers() {
            super("Pointers", "Dots around your crosshair that point towards nearby players and mobs. Green = friend.", Category.RENDER);
        }

        @Override
        public void onHud(GuiGraphics g) {
            LocalPlayer p = mc.player;
            int cx = g.guiWidth() / 2;
            int cy = g.guiHeight() / 2;
            int radius = 48;

            for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(range.d()), x -> true)) {
                boolean isPlayer = e instanceof Player;
                if (!e.isAlive() || !((isPlayer && players.on()) || (e instanceof Enemy && mobs.on()))) {
                    continue;
                }
                double dist = p.distanceTo(e);
                if (dist > range.d()) {
                    continue;
                }

                double dx = e.getX() - p.getX();
                double dz = e.getZ() - p.getZ();
                double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
                double diff = Math.toRadians(Mth.wrapDegrees((float) (targetYaw - p.getYRot())));
                int px = cx + (int) Math.round(Math.sin(diff) * radius);
                int py = cy - (int) Math.round(Math.cos(diff) * radius);

                int color = isPlayer
                        ? (FriendManager.isFriend(e.getName().getString()) ? 0xFF7CE0A5 : 0xFFFF6B6B)
                        : 0xFFFFB454;
                g.fill(px - 2, py - 2, px + 3, py + 3, color);
                if (isPlayer) {
                    g.drawCenteredString(mc.font, Integer.toString((int) dist), px, py + 5, Theme.TEXT_MUTED);
                }
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Compass extends Module {
        private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

        public Compass() {
            super("Compass", "Shows which way you are facing.", Category.RENDER);
        }

        @Override
        public void onHud(GuiGraphics g) {
            float yaw = Mth.wrapDegrees(mc.player.getYRot());
            String name = NAMES[Math.floorMod(Math.round(yaw / 45.0f), 8)];
            g.drawCenteredString(mc.font, name + "  " + Math.round(yaw) + "\u00b0", g.guiWidth() / 2, 16, Theme.ACCENT_SOFT);
        }
    }

    // ------------------------------------------------------------------

    public static final class TargetHud extends Module {
        private LivingEntity last;
        private int seen;

        public TargetHud() {
            super("TargetHud", "Shows the name and health of whatever you last looked at.", Category.RENDER);
        }

        @Override
        protected void onEnable() {
            last = null;
            seen = 0;
        }

        @Override
        public void onTick() {
            if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && living != mc.player) {
                last = living;
                seen = 80;
            } else if (seen > 0 && --seen == 0) {
                last = null;
            }
        }

        @Override
        public void onHud(GuiGraphics g) {
            if (last == null || last.isRemoved()) {
                return;
            }
            int x = g.guiWidth() / 2 - 55;
            int y = g.guiHeight() / 2 + 28;

            float hp = last.getHealth();
            float ratio = Mth.clamp(hp / Math.max(1.0f, last.getMaxHealth()), 0.0f, 1.0f);
            String hpText = String.format(Locale.ROOT, "%.1f", hp);

            g.fill(x, y, x + 110, y + 28, Theme.HUD_BG);
            g.drawString(mc.font, mc.font.plainSubstrByWidth(last.getName().getString(), 66), x + 4, y + 3, Theme.TEXT);
            g.drawString(mc.font, hpText, x + 106 - mc.font.width(hpText), y + 3, Theme.ACCENT_SOFT);
            g.fill(x + 4, y + 16, x + 106, y + 22, 0xFF161C3C);
            g.fill(x + 4, y + 16, x + 4 + (int) (102 * ratio), y + 22, Theme.lerp(0xFFFF5A5A, 0xFF5AE08A, ratio));
        }
    }

    // ------------------------------------------------------------------

    public static final class ItemCounter extends Module {
        // Built on first use: item stacks cannot be created while the game is still loading.
        private List<ItemStack> icons;

        public ItemCounter() {
            super("ItemCounter", "Shows how many totems, golden apples, ender pearls, arrows and XP bottles you carry.", Category.RENDER);
        }

        @Override
        public void onHud(GuiGraphics g) {
            if (icons == null) {
                icons = List.of(
                        new ItemStack(Items.TOTEM_OF_UNDYING),
                        new ItemStack(Items.GOLDEN_APPLE),
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.ARROW),
                        new ItemStack(Items.EXPERIENCE_BOTTLE));
            }
            LocalPlayer p = mc.player;
            int x = g.guiWidth() - 40;
            int y = g.guiHeight() / 2 - 45;
            for (ItemStack icon : icons) {
                int count = 0;
                for (int i = 0; i < 36; i++) {
                    ItemStack s = p.getInventory().getItem(i);
                    if (s.is(icon.getItem())) {
                        count += s.getCount();
                    }
                }
                if (p.getOffhandItem().is(icon.getItem())) {
                    count += p.getOffhandItem().getCount();
                }
                g.renderItem(icon, x, y);
                g.drawString(mc.font, Integer.toString(count), x + 20, y + 4, count > 0 ? Theme.TEXT : Theme.TEXT_DIM);
                y += 18;
            }
        }
    }
}
