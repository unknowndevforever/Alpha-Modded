import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * Creative inventory, drawn with the vanilla chest texture: a scrollable 9x5 item grid on top,
 * the player's real inventory + hotbar below (inv.a[9..35] and inv.a[0..8], so it saves with the world).
 * Grid: left click = full stack on the cursor, right click = 1, shift click = straight into the inventory,
 * click the grid while holding something = delete it. 1-9 over a slot = copy/swap it into that hotbar slot.
 */
public class CreativeScreen extends bh {
    private static final int COLS = 9, ROWS = 5, W = 176, H = 114 + ROWS * 18;
    private static final int INV_Y = 103 + (ROWS - 4) * 18, HOT_Y = 161 + (ROWS - 4) * 18;
    private static final int BAR_W = 14;
    private static List<ev> all;
    private static final ab items = new ab();
    private int scroll = 0;
    private boolean dragging = false;

    private static List<ev> build() {
        if (all != null) return all;
        List<ev> l = new ArrayList<ev>();
        Set<Integer> banned = new HashSet<Integer>(Arrays.asList(0, 8, 9, 10, 11, 26, 28, 34, 36, 51, 55, 59, 60, 62, 63, 64, 68, 71, 74, 75, 83, 93, 94));
        for (int id = 1; id < 256; id++)
            if (ly.n[id] != null && !banned.contains(id)) l.add(new ev(id, 1, 0));
        for (int id = 256; id < di.c.length; id++)
            if (di.c[id] != null) l.add(new ev(id, 1, 0));
        all = l;
        return l;
    }

    private int maxScroll() { return Math.max(0, (build().size() + COLS - 1) / COLS - ROWS); }
    private int left() { return (this.c - W - BAR_W) / 2; }
    private int top() { return (this.d - H) / 2; }
    private eu inv() { return this.b.g.b; }

    public boolean b() { return false; }

    public void a(int mx, int my, float f) {
        try { draw(mx, my, f); }
        catch (Throwable t) {
            t.printStackTrace();
            try { GL11.glDisable(32826); i.a(); } catch (Throwable t2) { }
            try { this.a(this.g, "Creative screen error (press E):", this.c / 2, 40, 0xFF5555);
                  String err = t.toString();
                  this.a(this.g, err.length() > 70 ? err.substring(0, 70) : err, this.c / 2, 56, 0xFFFFFF);
                  StackTraceElement[] st = t.getStackTrace();
                  for (int k = 0; k < Math.min(4, st.length); k++) this.a(this.g, st[k].toString(), this.c / 2, 72 + k * 12, 0xAAAAAA);
            } catch (Throwable t3) { }
        }
    }

    private void draw(int mx, int my, float f) {
        int w = Mouse.getDWheel();
        if (w > 0) scroll = Math.max(0, scroll - 1);
        else if (w < 0) scroll = Math.min(maxScroll(), scroll + 1);
        if (dragging) { if (Mouse.isButtonDown(0)) dragTo(my); else dragging = false; }

        this.i();
        int x = left(), y = top();
        // chest background: top part with 5 rows, then the player inventory part
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.b.n.b(this.b.n.a("/gui/container.png"));
        this.b(x, y, 0, 0, W, ROWS * 18 + 17);
        this.b(x, y + ROWS * 18 + 17, 0, 126, W, 96);
        drawScrollbar(x + W - 3, y + 12);
        this.g.b("Item Selection", x + 8, y + 6, 0x404040);
        this.g.b("Inventory", x + 8, y + H - 96 + 2, 0x404040);

        List<ev> list = build();
        eu inv = inv();
        // text and filled rects leave their colour (dark grey) set in GL, which made the items dark
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPushMatrix();
        GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
        i.b();
        GL11.glPopMatrix();
        GL11.glEnable(32826);
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                int n = (r + scroll) * COLS + c;
                if (n < list.size()) items.a(this.g, this.b.n, list.get(n), x + 8 + c * 18, y + 18 + r * 18);
            }
        for (int k = 0; k < 36; k++) {
            ev s = inv.a[k];
            if (s == null) continue;
            int sx = x + slotX(k), sy = y + slotY(k);
            items.a(this.g, this.b.n, s, sx, sy);
            items.b(this.g, this.b.n, s, sx, sy);
        }
        // hover highlight, same as the vanilla container screens
        int g = gridAt(mx, my), k = invAt(mx, my), hx = -1, hy = -1;
        if (g >= 0) { int idx = g - scroll * COLS; hx = x + 8 + (idx % COLS) * 18; hy = y + 18 + (idx / COLS) * 18; }
        else if (k >= 0) { hx = x + slotX(k); hy = y + slotY(k); }
        if (hx >= 0) {
            GL11.glDisable(2896); GL11.glDisable(2929);
            this.a(hx, hy, hx + 16, hy + 16, 0x80FFFFFF, 0x80FFFFFF);
            GL11.glEnable(2896); GL11.glEnable(2929);
        }
        if (inv.e != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, 32.0F);
            items.a(this.g, this.b.n, inv.e, mx - 8, my - 8);
            items.b(this.g, this.b.n, inv.e, mx - 8, my - 8);
            GL11.glPopMatrix();
        }
        GL11.glDisable(32826);
        i.a();
        GL11.glDisable(2896); GL11.glDisable(2929);

        // tooltip
        ev hover = null;
        if (inv.e == null) {
            if (g >= 0 && g < list.size()) hover = list.get(g);
            else if (k >= 0) hover = inv.a[k];
        }
        if (hover != null) {
            String name = Names.of(hover.c);
            int tx = mx + 12, ty = my - 12, tw = this.g.a(name);
            this.a(tx - 3, ty - 3, tx + tw + 3, ty + 8 + 3, 0xC0000000, 0xC0000000);
            this.g.a(name, tx, ty, 0xFFFFFF);
        }
        GL11.glEnable(2896); GL11.glEnable(2929);
        super.a(mx, my, f);
    }

    /** Beveled side tab with the scroll track and thumb, in the vanilla GUI colours. */
    private void drawScrollbar(int bx, int by) {
        int bw = BAR_W + 3, bh = ROWS * 18 + 10;
        this.a(bx, by, bx + bw, by + bh, 0xFF000000);
        this.a(bx, by + 1, bx + bw - 1, by + bh - 1, 0xFFC6C6C6);
        this.a(bx, by + 1, bx + bw - 2, by + 2, 0xFFFFFFFF);
        this.a(bx + bw - 2, by + 2, bx + bw - 1, by + bh - 1, 0xFF555555);
        this.a(bx, by + bh - 2, bx + bw - 1, by + bh - 1, 0xFF555555);
        int tx = trackX(), ty = top() + 18, th = ROWS * 18 - 2;
        this.a(tx, ty, tx + 12, ty + th, 0xFF8B8B8B);
        this.a(tx, ty, tx + 12, ty + 1, 0xFF373737);
        this.a(tx, ty, tx + 1, ty + th, 0xFF373737);
        this.a(tx + 11, ty + 1, tx + 12, ty + th, 0xFFFFFFFF);
        this.a(tx + 1, ty + th - 1, tx + 12, ty + th, 0xFFFFFFFF);
        int knobY = thumbY();
        boolean on = maxScroll() > 0;
        this.a(tx + 1, knobY, tx + 11, knobY + 15, on ? 0xFFC6C6C6 : 0xFF9C9C9C);
        this.a(tx + 1, knobY, tx + 10, knobY + 1, 0xFFFFFFFF);
        this.a(tx + 1, knobY, tx + 2, knobY + 14, 0xFFFFFFFF);
        this.a(tx + 2, knobY + 14, tx + 11, knobY + 15, 0xFF555555);
        this.a(tx + 10, knobY + 1, tx + 11, knobY + 15, 0xFF555555);
    }
    private int trackX() { return left() + W + 1; }
    private int thumbY() {
        int travel = ROWS * 18 - 2 - 2 - 15, ms = maxScroll();
        return top() + 18 + 1 + (ms == 0 ? 0 : scroll * travel / ms);
    }
    private void dragTo(int my) {
        int travel = ROWS * 18 - 2 - 2 - 15, ms = maxScroll();
        if (ms == 0) return;
        float t = (my - (top() + 19) - 7) / (float) travel;
        scroll = Math.max(0, Math.min(ms, Math.round(t * ms)));
    }

    /** Inventory slot k (0-8 hotbar, 9-35 main) relative to the panel. */
    private static int slotX(int k) { return 8 + (k % 9) * 18; }
    private static int slotY(int k) { return k < 9 ? HOT_Y : INV_Y + (k / 9 - 1) * 18; }

    /** Index into the item list under the mouse, or -1. */
    private int gridAt(int mx, int my) {
        int cx = mx - left() - 7, cy = my - top() - 17;
        if (cx < 0 || cy < 0 || cx >= COLS * 18 || cy >= ROWS * 18) return -1;
        int n = (cy / 18 + scroll) * COLS + cx / 18;
        return n < build().size() ? n : -1;
    }
    private int invAt(int mx, int my) {
        for (int k = 0; k < 36; k++) {
            int sx = left() + slotX(k) - 1, sy = top() + slotY(k) - 1;
            if (mx >= sx && my >= sy && mx < sx + 18 && my < sy + 18) return k;
        }
        return -1;
    }
    private boolean inPanel(int mx, int my) {
        return mx >= left() && my >= top() && mx < left() + W + BAR_W + 3 && my < top() + H;
    }

    private static boolean shift() { return Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54); }
    private static boolean same(ev a, ev b) { return a != null && b != null && a.c == b.c && a.d == b.d; }

    protected void a(int mx, int my, int btn) {
        eu inv = inv();
        int tx = trackX();
        if (btn == 0 && mx >= tx && mx < tx + 12 && my >= top() + 18 && my < top() + 18 + ROWS * 18 - 2) {
            dragging = true; dragTo(my); return;
        }
        int g = gridAt(mx, my);
        if (g >= 0) {
            ev it = build().get(g);
            if (inv.e != null) { inv.e = null; return; }       // drop the held stack into the "trash"
            ev stack = new ev(it.c, 1, it.d);
            int max = stack.c();
            if (shift()) { stack.a = max; inv.a(stack); }
            else { stack.a = btn == 1 ? 1 : max; inv.e = stack; }
            return;
        }
        int k = invAt(mx, my);
        if (k >= 0) { clickSlot(inv, k, btn); return; }
        if (!inPanel(mx, my) && inv.e != null) { inv.e = null; return; }
        super.a(mx, my, btn);
    }

    /** Normal inventory slot behaviour (pick up / place / merge / swap, right click = half / one). */
    private void clickSlot(eu inv, int k, int btn) {
        ev slot = inv.a[k], held = inv.e;
        if (shift()) { inv.a[k] = null; return; }
        if (held == null) {
            if (slot == null) return;
            if (btn == 1) {
                int take = (slot.a + 1) / 2;
                inv.e = new ev(slot.c, take, slot.d);
                slot.a -= take;
                if (slot.a <= 0) inv.a[k] = null;
            } else { inv.e = slot; inv.a[k] = null; }
        } else if (slot == null) {
            if (btn == 1) { inv.a[k] = new ev(held.c, 1, held.d); if (--held.a <= 0) inv.e = null; }
            else { inv.a[k] = held; inv.e = null; }
        } else if (same(slot, held)) {
            int room = slot.c() - slot.a;
            int move = Math.min(room, btn == 1 ? 1 : held.a);
            slot.a += move; held.a -= move;
            if (held.a <= 0) inv.e = null;
        } else { inv.a[k] = held; inv.e = slot; }
    }

    protected void b(int mx, int my, int btn) {
        if (btn == 0) dragging = false;
        super.b(mx, my, btn);
    }

    protected void a(char ch, int key) {
        if (key == 1 || key == this.b.y.o.b) { this.b.a((bh) null); return; }
        if (key == 200) scroll = Math.max(0, scroll - 1);
        else if (key == 208) scroll = Math.min(maxScroll(), scroll + 1);
        else if (key >= 2 && key <= 10) numberKey(key - 2);
    }

    /** 1-9 over a grid item copies a full stack into that hotbar slot; over an inventory slot it swaps. */
    private void numberKey(int hot) {
        int mx = Mouse.getX() * this.c / this.b.c, my = this.d - Mouse.getY() * this.d / this.b.d - 1;
        eu inv = inv();
        int g = gridAt(mx, my), k = invAt(mx, my);
        if (g >= 0) { ev it = build().get(g); ev s = new ev(it.c, 1, it.d); s.a = s.c(); inv.a[hot] = s; }
        else if (k >= 0 && k != hot) { ev t = inv.a[hot]; inv.a[hot] = inv.a[k]; inv.a[k] = t; }
        else inv.d = hot;
    }

    /** Closing: put whatever is on the cursor back into the inventory (or bin it if full). */
    public void h() {
        eu inv = inv();
        if (inv.e != null) { inv.a(inv.e); inv.e = null; }
    }
}
