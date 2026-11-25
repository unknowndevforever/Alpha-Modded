import java.util.Random;

/** Powered rail. Two blocks share this class: OFF (id 27) and ON (id 28). Metadata = rail shape 0-5. */
public class PoweredRail extends ly {
    public static final int OFF = 27, ON = 28;
    private final boolean on;

    public PoweredRail(int id, int tex, boolean on) {
        super(id, tex, gb.n);
        this.on = on;
        this.a(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    public cf d(cn w, int x, int y, int z) { return null; }
    public boolean b() { return false; }
    public boolean c() { return false; }
    public int f() { return 9; }
    public int a(Random r) { return 1; }
    public int a(int meta, Random r) { return OFF; }
    public int a(int side, int meta) { return this.bb; }

    public mf a(cn w, int x, int y, int z, aj from, aj to) {
        this.a((nm) w, x, y, z);
        return super.a(w, x, y, z, from, to);
    }

    public void a(nm w, int x, int y, int z) {
        int m = w.e(x, y, z);
        if (m >= 2 && m <= 5) this.a(0.0F, 0.0F, 0.0F, 1.0F, 0.625F, 1.0F);
        else this.a(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    public boolean a(cn w, int x, int y, int z) { return w.g(x, y - 1, z); }

    public void e(cn w, int x, int y, int z) {
        int cur = w.e(x, y, z);
        int s = shapeFor(w, x, y, z);
        if (s != cur && (s >= 2 || (cur <= 1 && !connectsAlong(w, x, y, z, cur)))) {
            w.b(x, y, z, w.a(x, y, z), s);
            return; // the metadata change re-enters here / notifies neighbours
        }
        refresh(w, x, y, z);
    }

    public void a(cn w, int x, int y, int z, int nid) {
        int m = w.e(x, y, z);
        boolean gone = !w.g(x, y - 1, z);
        if (m == 2 && !w.g(x + 1, y, z)) gone = true;
        if (m == 3 && !w.g(x - 1, y, z)) gone = true;
        if (m == 4 && !w.g(x, y, z - 1)) gone = true;
        if (m == 5 && !w.g(x, y, z + 1)) gone = true;
        if (gone) {
            this.b_(w, x, y, z, m);
            w.d(x, y, z, 0);
            return;
        }
        // keep a flat rail pointing at its neighbours
        if (m <= 1 && !connectsAlong(w, x, y, z, m)) {
            int s = shapeFor(w, x, y, z);
            if (s != m) { w.b(x, y, z, w.a(x, y, z), s); return; }
        }
        refresh(w, x, y, z);
    }

    // ---- helpers ----
    private static boolean rail(cn w, int x, int y, int z) { return Ext.isRail(w.a(x, y, z)); }

    private static boolean connectsAlong(cn w, int x, int y, int z, int m) {
        if (m == 1) return rail(w, x + 1, y, z) || rail(w, x - 1, y, z) || rail(w, x + 1, y - 1, z) || rail(w, x - 1, y - 1, z);
        return rail(w, x, y, z + 1) || rail(w, x, y, z - 1) || rail(w, x, y - 1, z + 1) || rail(w, x, y - 1, z - 1);
    }

    private static int shapeFor(cn w, int x, int y, int z) {
        if (rail(w, x + 1, y + 1, z)) return 2;
        if (rail(w, x - 1, y + 1, z)) return 3;
        if (rail(w, x, y + 1, z - 1)) return 4;
        if (rail(w, x, y + 1, z + 1)) return 5;
        boolean ex = connectsAlong(w, x, y, z, 1), ez = connectsAlong(w, x, y, z, 0);
        if (ex && !ez) return 1;
        return 0;
    }

    private static boolean chain(cn w, int x, int y, int z, int meta, int dir) {
        boolean xAxis = (meta == 1 || meta == 2 || meta == 3);
        int dx = xAxis ? dir : 0, dz = xAxis ? 0 : dir;
        int cx = x, cy = y, cz = z;
        for (int i = 0; i < 8; i++) {
            cx += dx; cz += dz;
            int found = Integer.MIN_VALUE;
            int[] dys = { 0, 1, -1 };
            for (int k = 0; k < 3; k++) {
                int id = w.a(cx, cy + dys[k], cz);
                if (id == OFF || id == ON) { found = cy + dys[k]; break; }
            }
            if (found == Integer.MIN_VALUE) return false;
            cy = found;
            int m = w.e(cx, cy, cz);
            boolean mx = (m == 1 || m == 2 || m == 3);
            if (mx != xAxis) return false;
            if (w.o(cx, cy, cz)) return true;
        }
        return false;
    }

    private void refresh(cn w, int x, int y, int z) {
        int m = w.e(x, y, z);
        boolean p = w.o(x, y, z) || chain(w, x, y, z, m, -1) || chain(w, x, y, z, m, 1);
        int want = p ? ON : OFF;
        if (want != this.bc) w.b(x, y, z, want, m);
    }
}
