package com.nightfall.gui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/** The Nightfall look: midnight blues, moonlight accents, a crescent moon and a field of stars. */
public final class Theme {
    // ---- backgrounds ----
    public static final int BG_TOP = 0xD0040716;
    public static final int BG_BOTTOM = 0xD01B1448;
    public static final int HEADER = 0xFF0A0F2E;
    public static final int ROW = 0xE00E1534;
    public static final int ROW_HOVER = 0xE01A2450;
    public static final int ROW_ON = 0xE02A4694;
    public static final int SETTING = 0xE00A1026;
    public static final int SETTING_HOVER = 0xE014204A;
    public static final int HUD_BG = 0x90050A1A;

    // ---- accents ----
    public static final int ACCENT = 0xFF7FA8FF;
    public static final int ACCENT_ALT = 0xFFB69CFF;
    public static final int ACCENT_SOFT = 0xFFB8CBFF;
    public static final int ACCENT_PRESSED = 0xB07FA8FF;
    public static final int MOON = 0xFFF6F1CF;

    // ---- text ----
    public static final int TEXT = 0xFFE8EDFF;
    public static final int TEXT_MUTED = 0xFFB4BEE0;
    public static final int TEXT_DIM = 0xFF8D99C6;

    private static final int STAR_COUNT = 90;
    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final float[] STAR_PHASE = new float[STAR_COUNT];
    private static final float[] STAR_SPEED = new float[STAR_COUNT];
    private static final boolean[] STAR_BIG = new boolean[STAR_COUNT];
    private static final boolean[] STAR_BLUE = new boolean[STAR_COUNT];

    static {
        Random random = new Random(20261121L);
        for (int i = 0; i < STAR_COUNT; i++) {
            STAR_X[i] = random.nextFloat();
            STAR_Y[i] = random.nextFloat();
            STAR_PHASE[i] = random.nextFloat() * 6.2831f;
            STAR_SPEED[i] = 0.6f + random.nextFloat() * 1.4f;
            STAR_BIG[i] = random.nextFloat() < 0.15f;
            STAR_BLUE[i] = random.nextBoolean();
        }
    }

    private Theme() {
    }

    // ------------------------------------------------------------------
    // colour helpers
    // ------------------------------------------------------------------

    public static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** rgb with the given opacity (0..1). */
    public static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Slowly drifting blue-to-violet gradient used for the module list. Index offsets each row. */
    public static int twilight(int index) {
        double wave = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.0015 - index * 0.35);
        return lerp(ACCENT, ACCENT_ALT, (float) wave);
    }

    // ------------------------------------------------------------------
    // shapes
    // ------------------------------------------------------------------

    public static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.floor(Math.sqrt((double) r * r - (double) dy * dy));
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }

    /** A crescent moon: a disc of radius r with a smaller, offset disc cut out of it. */
    public static void moon(GuiGraphics g, int cx, int cy, int r, int color) {
        int r2 = Math.max(1, (int) (r * 0.82));
        int ox = (int) (r * 0.5);
        int oy = (int) (-r * 0.25);

        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.floor(Math.sqrt((double) r * r - (double) dy * dy));
            int x0 = -w;
            int x1 = w;
            int iy = dy - oy;

            if (Math.abs(iy) <= r2) {
                int w2 = (int) Math.floor(Math.sqrt((double) r2 * r2 - (double) iy * iy));
                int a = ox - w2;
                int b = ox + w2;
                if (a - 1 >= x0) {
                    g.fill(cx + x0, cy + dy, cx + Math.min(x1, a - 1) + 1, cy + dy + 1, color);
                }
                if (b + 1 <= x1) {
                    g.fill(cx + Math.max(x0, b + 1), cy + dy, cx + x1 + 1, cy + dy + 1, color);
                }
            } else {
                g.fill(cx + x0, cy + dy, cx + x1 + 1, cy + dy + 1, color);
            }
        }
    }

    // ------------------------------------------------------------------
    // full-screen backdrop
    // ------------------------------------------------------------------

    /** Dark night-sky gradient, twinkling stars, an occasional shooting star and a big moon. */
    public static void sky(GuiGraphics g, int width, int height) {
        g.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);

        long now = System.currentTimeMillis();
        for (int i = 0; i < STAR_COUNT; i++) {
            double twinkle = 0.5 + 0.5 * Math.sin(now * 0.002 * STAR_SPEED[i] + STAR_PHASE[i]);
            float alpha = (float) (0.25 + 0.75 * twinkle);
            int x = (int) (STAR_X[i] * width);
            int y = (int) (STAR_Y[i] * height);
            int color = withAlpha(STAR_BLUE[i] ? 0xCFE0FF : 0xFFFFFF, alpha);
            int size = STAR_BIG[i] ? 2 : 1;
            g.fill(x, y, x + size, y + size, color);
        }

        // Shooting star: one streak every nine seconds, in a slightly different place each time.
        long cycle = now / 9000L;
        long t = now % 9000L;
        if (t < 900L) {
            float p = t / 900.0f;
            float startX = 0.10f + ((cycle * 37L) % 55L) / 100.0f;
            float startY = 0.04f + ((cycle * 19L) % 25L) / 100.0f;
            int headX = (int) (width * (startX + 0.30f * p));
            int headY = (int) (height * (startY + 0.18f * p));
            for (int i = 0; i < 9; i++) {
                float fade = (1.0f - i / 9.0f) * (1.0f - p * 0.6f);
                int px = headX - i * 3;
                int py = headY - (int) (i * 1.5f);
                g.fill(px, py, px + 2, py + 1, withAlpha(0xFFFFFF, fade));
            }
        }

        // Moon with a soft halo, top right.
        int mx = width - 44;
        int my = 40;
        disc(g, mx, my, 22, withAlpha(MOON, 0.04f));
        disc(g, mx, my, 18, withAlpha(MOON, 0.06f));
        moon(g, mx, my, 14, withAlpha(MOON, 0.92f));
    }
}
