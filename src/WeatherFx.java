import org.lwjgl.opengl.GL11;

/**
 * Rain (Alpha 1.1.2 has no weather). Switched by PacketWeather (0x46), cleared on every world change.
 *  - cn.b(F) sky colour and cn.e(F) fog colour are greyed / darkened (hooks before their ARETURN)
 *  - cn.a(F) sky light subtraction gets up to +3 (like Beta's rain), so blocks lit by the sky get dimmer
 *  - render(): falling rain streaks around the player (GL11 lines), splash particles (vanilla nf = rain
 *    splash particle) and the ambient.weather.rain sound when the sound pool has it
 * Everything runs on the game thread; every entry point is wrapped so it can never crash the game.
 */
public class WeatherFx {
    public static volatile boolean raining = false;
    private static float level = 0.0F;          // 0..1 smooth intensity
    private static long last = 0L;
    private static final float FADE_SECONDS = 6.0F;
    private static int errors = 0;

    public static void setRaining(boolean on) { raining = on; }

    /** Clear weather right now (no fade): new world / disconnect / title screen. */
    public static synchronized void reset() { raining = false; level = 0.0F; last = 0L; heightAge = 1000; }

    /** Current intensity, advanced by real time so the fade speed doesn't depend on fps or ticks. */
    public static synchronized float level() {
        long now = System.nanoTime();
        float dt = last == 0L ? 0.0F : (now - last) / 1.0e9F;
        last = now;
        if (dt < 0.0F) dt = 0.0F;
        if (dt > 0.25F) dt = 0.25F;
        float target = raining ? 1.0F : 0.0F, step = dt / FADE_SECONDS;
        if (level < target) level = Math.min(target, level + step);
        else if (level > target) level = Math.max(target, level - step);
        return level;
    }

    // ---- colour hooks (cn) ----

    /** cn.b(F): sky colour. Greyed towards its own luminance and darkened. */
    public static aj sky(aj c) {
        try {
            float f = level();
            if (c == null || f <= 0.0F) return c;
            double lum = (c.a * 0.3D + c.b * 0.59D + c.c * 0.11D) * 0.6D;
            double k = f * 0.75D;
            c.a = c.a * (1.0D - k) + lum * k;
            c.b = c.b * (1.0D - k) + lum * k;
            c.c = c.c * (1.0D - k) + lum * k;
        } catch (Throwable t) { }
        return c;
    }

    /** cn.e(F): fog colour (also the clear colour). */
    public static aj fog(aj c) {
        try {
            float f = level();
            if (c == null || f <= 0.0F) return c;
            double lum = (c.a * 0.3D + c.b * 0.59D + c.c * 0.11D) * 0.6D;
            double k = f * 0.6D;
            c.a = (c.a * (1.0D - k) + lum * k) * (1.0D - f * 0.25D);
            c.b = (c.b * (1.0D - k) + lum * k) * (1.0D - f * 0.25D);
            c.c = (c.c * (1.0D - k) + lum * k) * (1.0D - f * 0.2D);
        } catch (Throwable t) { }
        return c;
    }

    /** cn.a(F): sky light subtracted (0 = day, 11 = night). Rain takes away up to 5/16 of the remaining daylight. */
    public static int skyDark(int r) {
        try {
            float f = level();
            if (f <= 0.0F) return r;
            float bright = 1.0F - r / 11.0F;
            bright *= 1.0F - f * 0.3125F;
            int n = (int) ((1.0F - bright) * 11.0F);
            return n < r ? r : (n > 11 ? 11 : n);
        } catch (Throwable t) { return r; }
    }

    // ---- rendering (iq.c(F), called right where the vanilla snow renderer is checked) ----

    private static final int R = 9, D = 2 * R + 1;
    private static final int[] heights = new int[D * D];
    private static int hx = Integer.MIN_VALUE, hz = Integer.MIN_VALUE, heightAge = 1000;
    private static long lastSplash = 0L, lastSound = 0L;
    private static final java.util.Random rnd = new java.util.Random();

    /** Called with the vanilla "is snow covered" flag; returns it unchanged. */
    public static boolean render(boolean snow, net.minecraft.client.Minecraft mc, float partial) {
        if (errors > 20) return snow;
        try {
            float f = level();
            if (f <= 0.001F || mc == null || mc.e == null || mc.g == null) return snow;
            draw(mc, f, partial);
            effects(mc, f);
        } catch (Throwable t) {
            if (errors++ < 3) t.printStackTrace();
        }
        return snow;
    }

    private static void updateHeights(cn w, int px, int pz) {
        if (px == hx && pz == hz && heightAge++ < 20) return;
        hx = px; hz = pz; heightAge = 0;
        for (int i = 0; i < D; i++) for (int k = 0; k < D; k++) heights[i * D + k] = w.e(px - R + i, pz - R + k);
    }

    private static void draw(net.minecraft.client.Minecraft mc, float f, float partial) {
        bi p = mc.g;
        cn w = mc.e;
        double ex = p.aI + (p.ak - p.aI) * partial;
        double ey = p.aJ + (p.al - p.aJ) * partial;
        double ez = p.aK + (p.am - p.aK) * partial;
        int px = (int) Math.floor(ex), py = (int) Math.floor(ey), pz = (int) Math.floor(ez);
        updateHeights(w, px, pz);

        double time = (System.nanoTime() / 1.0e9D) % 10000.0D;
        boolean pushed = false;
        try {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
            pushed = true;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            GL11.glLineWidth(1.5F);
            GL11.glBegin(GL11.GL_LINES);
            float alpha = 0.45F * f;
            for (int i = 0; i < D; i++) {
                for (int k = 0; k < D; k++) {
                    int dx = i - R, dz = k - R;
                    int d2 = dx * dx + dz * dz;
                    if (d2 > R * R) continue;
                    int x = px + dx, z = pz + dz;
                    int ground = heights[i * D + k];
                    int top = py + 14, bottom = Math.max(ground, py - 10);
                    if (bottom >= top) continue;               // roof over this column above the view
                    int span = top - bottom;
                    int h = (x * 73428767) ^ (z * 912931);
                    h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
                    int drops = d2 <= 25 ? 2 : 1;              // denser close to the player
                    float a = alpha * (1.0F - d2 / (float) (R * R + 1) * 0.6F);
                    for (int n = 0; n < drops; n++) {
                        int hn = h + n * 0x9e3779b9;
                        double ox = ((hn & 0xff) / 255.0D) * 0.9D + 0.05D;
                        double oz = (((hn >>> 8) & 0xff) / 255.0D) * 0.9D + 0.05D;
                        double speed = 14.0D + ((hn >>> 16) & 7);
                        double phase = ((hn >>> 19) & 0x3ff) / 1024.0D * span;
                        double y = top - ((time * speed + phase) % span);
                        double len = 0.8D + ((hn >>> 24) & 3) * 0.15D;
                        double y2 = Math.max(bottom, y - len);
                        if (y2 >= y) continue;
                        double wx = x + ox - ex, wz = z + oz - ez;
                        GL11.glColor4f(0.62F, 0.68F, 0.85F, a);
                        GL11.glVertex3d(wx, y - ey, wz);
                        GL11.glColor4f(0.62F, 0.68F, 0.85F, a * 0.35F);
                        GL11.glVertex3d(wx - 0.06D, y2 - ey, wz + 0.03D);
                    }
                }
            }
            GL11.glEnd();
        } finally {
            if (pushed) GL11.glPopAttrib();
        }
    }

    /** Splash particles on exposed ground and the rain ambience. */
    private static void effects(net.minecraft.client.Minecraft mc, float f) {
        long now = System.currentTimeMillis();
        bi p = mc.g;
        cn w = mc.e;
        int px = (int) Math.floor(p.ak), py = (int) Math.floor(p.al), pz = (int) Math.floor(p.am);
        if (now - lastSplash >= 50L) {
            lastSplash = now;
            int n = (int) (f * 5.0F);
            for (int i = 0; i < n; i++) {
                int x = px + rnd.nextInt(R * 2 + 1) - R, z = pz + rnd.nextInt(R * 2 + 1) - R;
                int y = w.e(x, z);
                if (y > py + 10 || y < py - 10) continue;
                int id = w.a(x, y - 1, z);
                if (id <= 0) continue;
                mc.h.a(new nf(w, x + rnd.nextFloat(), y + 0.1D, z + rnd.nextFloat()));
            }
        }
        if (now - lastSound >= 900L && rnd.nextFloat() < f) {
            lastSound = now;
            int x = px + rnd.nextInt(9) - 4, z = pz + rnd.nextInt(9) - 4;
            int y = w.e(x, z);
            if (y <= py + 10) {
                boolean above = y > py + 1;   // under a roof: quieter and lower
                w.a(x + 0.5D, y + 0.1D, z + 0.5D, "ambient.weather.rain", (above ? 0.1F : 0.2F) * f, above ? 0.5F : 1.0F);
            }
        }
    }
}
