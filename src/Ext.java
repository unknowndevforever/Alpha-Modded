import java.io.*;
import java.util.Properties;

/** Helper code called from the patched game classes. */
public class Ext {
    public static float fov = 70.0F;
    private static File cfg;

    // ---- creative mode (per world, see world settings below) ----
    public static boolean creative = false;
    public static boolean cheats = false;
    public static boolean flying = false;
    private static boolean lastJump = false;
    private static int jumpTimer = 0;

    /** F4: only works when the world was made (or edited) with cheats on. */
    public static void toggleCreative() {
        if (worldDir == null || !cheats) return;
        creative = !creative;
        if (!creative) flying = false;
        saveWorld();
    }
    public static String title(String base) { return base + (creative ? " - Creative" : "") + (cheats ? " (F4)" : ""); }
    public static bh screen(bh survival) {
        if (!creative) return survival;
        try { return new CreativeScreen(); } catch (Throwable t) { t.printStackTrace(); return survival; }
    }
    public static double grav(ge e) { return (creative && flying && e instanceof bi) ? 0.0D : 0.08D; }
    public static boolean noHurt(dm e) { return creative && e instanceof bi; }
    public static void tick(bi p) {
        if (!creative) { flying = false; return; }
        boolean jump = p.a.d, sneak = p.a.e;
        if (jump && !lastJump) {
            if (jumpTimer > 0) { flying = !flying; jumpTimer = 0; } else jumpTimer = 7;
        }
        lastJump = jump;
        if (jumpTimer > 0) jumpTimer--;
        if (flying) {
            p.ao = jump ? 0.4D : (sneak ? -0.4D : 0.0D);
            p.a.a *= 2.0F; p.a.b *= 2.0F;
        }
    }

    // ---- block registration / recipes ----
    public static void init() {
        new PoweredRail(PoweredRail.OFF, 129, false).c(0.7F).a(ly.j);
        new PoweredRail(PoweredRail.ON, 130, true).c(0.7F).a(ly.j);
    }

    public static void recipes(dw m) {
        // 1 rail + 1 redstone -> 1 powered rail
        m.a(new ev(ly.n[PoweredRail.OFF], 1), new Object[] { "#", "R",
            Character.valueOf('#'), ly.aH, Character.valueOf('R'), di.aA });
    }

    // ---- rail ids ----
    public static int canon(int id) { return (id == PoweredRail.OFF || id == PoweredRail.ON) ? 66 : id; }
    public static boolean isRail(int id) { return id == 66 || id == PoweredRail.OFF || id == PoweredRail.ON; }

    /** Used instead of world.setMetadata inside the normal-rail shaper so it can never bend a powered rail. */
    public static void setMeta(cn w, int x, int y, int z, int meta) {
        int id = w.a(x, y, z);
        if ((id == PoweredRail.OFF || id == PoweredRail.ON) && meta > 5) return;
        w.b(x, y, z, meta);
    }

    // ---- minecart on powered rails ----
    public static void cartRail(oc c, cn w, int x, int y, int z, int meta) {
        int id = w.a(x, y, z);
        if (id == PoweredRail.ON) {
            double s = Math.sqrt(c.an * c.an + c.ap * c.ap);
            if (s > 0.01D) {
                double k = 0.06D / s;
                c.an += c.an * k;
                c.ap += c.ap * k;
            } else if (meta == 1) {
                if (w.g(x - 1, y, z)) c.an = 0.02D; else if (w.g(x + 1, y, z)) c.an = -0.02D;
            } else if (meta == 0) {
                if (w.g(x, y, z - 1)) c.ap = 0.02D; else if (w.g(x, y, z + 1)) c.ap = -0.02D;
            }
        } else if (id == PoweredRail.OFF) {
            double s = Math.sqrt(c.an * c.an + c.ap * c.ap);
            if (s < 0.03D) { c.an = 0.0D; c.ao = 0.0D; c.ap = 0.0D; }
            else { c.an *= 0.5D; c.ao *= 0.0D; c.ap *= 0.5D; }
        }
    }

    // ---- FOV (30..110, default 70) ----
    public static float fov() { return fov; }
    public static float fovNorm() { return (fov - 30.0F) / 80.0F; }
    public static void setFovNorm(float v) {
        fov = 30.0F + Math.round(v * 80.0F);
        save();
    }
    public static String fovLabel() { return "FOV: " + (fov == 70.0F ? "Normal" : String.valueOf((int) fov)); }

    public static void load(File optionsTxt) {
        try {
            cfg = new File(optionsTxt.getParentFile(), "fov.txt");
            if (cfg.exists()) {
                BufferedReader r = new BufferedReader(new FileReader(cfg));
                String l = r.readLine(); r.close();
                float f = Float.parseFloat(l.trim());
                if (f >= 30.0F && f <= 110.0F) fov = f;
            }
        } catch (Exception e) { }
    }
    private static void save() {
        try {
            if (cfg == null) return;
            PrintWriter w = new PrintWriter(new FileWriter(cfg));
            w.println((int) fov); w.close();
        } catch (Exception e) { }
    }

    // ---- world settings: saves/WorldN/recreatecraft.txt ----
    private static File worldDir;

    public static File worldDir(int slot) { return new File(new File(net.minecraft.client.Minecraft.b(), "saves"), "World" + slot); }

    /** [name, cheats, creative] for a world slot; defaults when the file is missing. */
    public static Properties readWorld(int slot) {
        Properties p = new Properties();
        p.setProperty("name", "World " + slot);
        p.setProperty("cheats", "false");
        p.setProperty("creative", "false");
        try {
            File f = new File(worldDir(slot), "recreatecraft.txt");
            if (f.exists()) { FileInputStream in = new FileInputStream(f); p.load(in); in.close(); }
        } catch (Exception e) { }
        return p;
    }
    public static void writeWorld(int slot, Properties p) {
        try {
            File d = worldDir(slot);
            d.mkdirs();
            FileOutputStream out = new FileOutputStream(new File(d, "recreatecraft.txt"));
            p.store(out, "recreatecraft world settings"); out.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Called right before a world is loaded / generated. */
    public static void enterWorld(int slot, Properties p) {
        worldDir = worldDir(slot);
        currentSlot = slot;
        current = p;
        cheats = "true".equals(p.getProperty("cheats"));
        creative = "true".equals(p.getProperty("creative"));
        flying = false;
    }
    private static int currentSlot;
    private static Properties current;
    public static void saveWorld() {
        if (worldDir == null || current == null) return;
        current.setProperty("cheats", String.valueOf(cheats));
        current.setProperty("creative", String.valueOf(creative));
        writeWorld(currentSlot, current);
    }
    /** Title screen: no world, so no creative/cheats. */
    public static void leaveWorld() { worldDir = null; current = null; creative = false; cheats = false; flying = false; httpBase = null; }

    // ---- sorting ----
    // The game sorts its chunk renderers with comparators that break Java's sort contract (distances change
    // while sorting). Java 7+ then throws "Comparison method violates its general contract", which crashed
    // the game when switching Fancy/Fast graphics. This merge sort never throws, whatever the comparator does.
    private static int sortTick;

    public static void sort(Object[] a, java.util.Comparator c) {
        if (a.length < 2) return;
        Object[] tmp = new Object[a.length];
        mergeSort(a, tmp, 0, a.length, c);
    }
    private static void mergeSort(Object[] a, Object[] tmp, int lo, int hi, java.util.Comparator c) {
        if (hi - lo < 7) {
            for (int i = lo + 1; i < hi; i++) {
                Object x = a[i]; int j = i - 1;
                while (j >= lo && c.compare(a[j], x) > 0) { a[j + 1] = a[j]; j--; }
                a[j + 1] = x;
            }
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(a, tmp, lo, mid, c);
        mergeSort(a, tmp, mid, hi, c);
        if (c.compare(a[mid - 1], a[mid]) <= 0) return;
        System.arraycopy(a, lo, tmp, lo, hi - lo);
        int i = lo, j = mid, k = lo;
        while (i < mid && j < hi) a[k++] = c.compare(tmp[j], tmp[i]) < 0 ? tmp[j++] : tmp[i++];
        while (i < mid) a[k++] = tmp[i++];
        while (j < hi) a[k++] = tmp[j++];
    }
    public static void sortList(java.util.List l, java.util.Comparator c) {
        if (l.size() < 2) return;
        Object[] a = l.toArray();
        sort(a, c);
        for (int i = 0; i < a.length; i++) l.set(i, a[i]);
    }
    /** Chunk rebuild queue: re-sorting it by distance every frame is wasteful, every 4th frame looks identical. */
    public static void sortDirty(java.util.List l, java.util.Comparator c) {
        if ((sortTick++ & 3) != 0) return;
        sortList(l, c);
    }

    // ---- FPS (F3 screen) ----
    private static long fpsStart = System.currentTimeMillis();
    private static int fpsFrames, fpsShown;
    public static String fps() {
        fpsFrames++;
        long now = System.currentTimeMillis();
        if (now - fpsStart >= 500) { fpsShown = (int) (fpsFrames * 1000L / (now - fpsStart)); fpsFrames = 0; fpsStart = now; }
        return "FPS: " + fpsShown;
    }

    // ---- multiplayer: replaces new Socket(InetAddress.getByName(host), port) in gy.<init> ----
    /** Base URL of the HTTP server we're connected to (skins come from there), null for TCP / singleplayer. */
    public static volatile String httpBase;

    /** Typed address -> host[:port]: drops http(s):// and any /path; https:// without a port means 443, http:// means 80. */
    public static String cleanAddress(String a) {
        a = a.trim();
        String low = a.toLowerCase();
        String def = null;
        if (low.startsWith("https://")) { a = a.substring(8); def = "443"; }
        else if (low.startsWith("http://")) { a = a.substring(7); def = "80"; }
        int sl = a.indexOf('/');
        if (sl >= 0) a = a.substring(0, sl);
        if (def != null && a.indexOf(':') < 0) a = a + ":" + def;
        return a;
    }

    public static java.net.Socket connect(String host, int port, net.minecraft.client.Minecraft mc) throws IOException {
        String base = HttpSocket.baseFor(host, port);
        httpBase = base;
        if (base == null) return new java.net.Socket(java.net.InetAddress.getByName(host), port);
        java.net.Socket s = new HttpSocket(base);
        final File skin = SkinScreen.skinFile();
        final String name = mc.i == null ? null : mc.i.b, b = base;
        if (skin.exists() && name != null) new Thread("skin upload") { public void run() { HttpSocket.uploadSkin(b, name, skin); } }.start();
        return s;
    }

    // ---- coordinates (F3 screen) ----
    public static String pos(double x, double y, double z) {
        return "x: " + f(x) + "  y: " + f(y) + "  z: " + f(z);
    }
    private static String f(double v) { return String.format("%.3f", v); }
}
