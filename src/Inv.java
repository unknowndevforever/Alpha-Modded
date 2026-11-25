import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Extra slot controls for every container screen (ee): inventory, crafting table, chest, furnace.
 * Shift click: crafting output = craft as many as fit into the inventory, inventory slot = move the stack
 * to the other part (hotbar <-> main, or into the chest), other slot = move it into the inventory.
 * Right click + drag while holding a stack = drop one item into every slot the mouse passes over.
 */
public class Inv {
    private static boolean dragging;
    private static ee dragScreen;
    private static final Set<dk> visited = new HashSet<dk>();

    private static boolean shift() { return Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54); }
    private static boolean same(ev a, ev b) { return a != null && b != null && a.c == b.c && a.d == b.d; }

    private static mm slotAt(ee s, int mx, int my) {
        for (int k = 0; k < s.i.size(); k++) {
            mm m = (mm) s.i.get(k);
            if (m.a(mx, my)) return m;
        }
        return null;
    }

    /** Called first thing in ee.a(int,int,int) (mouse down). true = handled, skip the vanilla click. */
    public static boolean click(ee s, int mx, int my, int btn) {
        try {
            eu inv = s.b.g.b;
            mm slot = slotAt(s, mx, my);
            if (btn == 1 && !shift()) {
                // vanilla puts one item into the clicked slot; the drag carries on from there
                dragging = inv.e != null;
                dragScreen = s;
                visited.clear();
                if (slot != null) visited.add(slot);
                return false;
            }
            if (btn != 0 || !shift() || slot == null || inv.e != null || slot.b() == null) return false;
            if (slot instanceof an) craftAll(s, slot);
            else moveStack(s, slot);
            return true;
        } catch (Throwable t) { t.printStackTrace(); return false; }
    }

    /** Called at the start of ee.a(int,int,float) (every frame). */
    public static void frame(ee s, int mx, int my) {
        try {
            if (!dragging || dragScreen != s) return;
            eu inv = s.b.g.b;
            if (!Mouse.isButtonDown(1) || inv.e == null) { dragging = false; visited.clear(); return; }
            mm slot = slotAt(s, mx, my);
            if (slot == null || visited.contains(slot)) return;
            visited.add(slot);
            ev held = inv.e, cur = slot.b();
            if (!slot.a(held)) return;
            if (cur == null) slot.b(held.a(1));
            else if (same(cur, held) && cur.a < Math.min(cur.c(), slot.b.e())) { cur.a++; held.a--; slot.d(); }
            else return;
            if (held.a <= 0) inv.e = null;
        } catch (Throwable t) { t.printStackTrace(); dragging = false; }
    }

    private static boolean isPlayer(mm m, eu inv) { return m.b == inv && m.a < 36; }

    /** Slots a stack from `from` should go to, in fill order. */
    private static List<mm> targets(ee s, mm from, boolean crafted) {
        eu inv = s.b.g.b;
        List<mm> hot = new ArrayList<mm>(), main = new ArrayList<mm>(), other = new ArrayList<mm>();
        for (int k = 0; k < s.i.size(); k++) {
            mm m = (mm) s.i.get(k);
            if (m == from) continue;
            if (isPlayer(m, inv)) (m.a < 9 ? hot : main).add(m);
            else if (!(m instanceof an) && !(m.b instanceof gu) && m.b != inv) other.add(m);  // chest / furnace, not crafting grid or armor
        }
        List<mm> out = new ArrayList<mm>();
        if (!isPlayer(from, inv)) {
            if (crafted) { out.addAll(hot); out.addAll(main); } else { out.addAll(main); out.addAll(hot); }
        } else if (!other.isEmpty()) out.addAll(other);
        else out.addAll(from.a < 9 ? main : hot);
        return out;
    }

    private static int room(List<mm> slots, ev st) {
        int n = 0;
        for (mm m : slots) {
            ev c = m.b();
            int max = Math.min(st.c(), m.b.e());
            if (c == null) { if (m.a(st)) n += max; }
            else if (same(c, st)) n += Math.max(0, max - c.a);
        }
        return n;
    }

    /** Merge into matching stacks first, then empty slots. Leaves whatever didn't fit in st.a. */
    private static void put(List<mm> slots, ev st) {
        for (mm m : slots) {
            if (st.a <= 0) return;
            ev c = m.b();
            if (!same(c, st)) continue;
            int move = Math.min(st.a, Math.min(st.c(), m.b.e()) - c.a);
            if (move > 0) { c.a += move; st.a -= move; m.d(); }
        }
        for (mm m : slots) {
            if (st.a <= 0) return;
            if (m.b() != null || !m.a(st)) continue;
            int move = Math.min(st.a, Math.min(st.c(), m.b.e()));
            m.b(st.a(move));
        }
    }

    private static void moveStack(ee s, mm from) {
        ev st = from.b();
        put(targets(s, from, false), st);
        if (st.a <= 0) from.b(null); else from.d();
    }

    /** Shift click on a crafting output: keep crafting while the recipe gives the same item and it fits. */
    private static void craftAll(ee s, mm out) {
        List<mm> dest = targets(s, out, true);
        ev first = out.b();
        for (int n = 0; n < 64 * 9; n++) {
            ev r = out.b();
            if (r == null || !same(r, first) || room(dest, r) < r.a) return;
            ev taken = out.b.a(out.a, r.a);
            out.a();                       // uses up one of each ingredient, recipe is recalculated
            put(dest, taken);
            if (taken.a > 0) { s.b.g.a(taken); return; }  // can't happen after the room check, but never lose items
        }
    }
}
