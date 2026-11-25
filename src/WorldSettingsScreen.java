import java.util.Properties;
import org.lwjgl.input.Keyboard;

/** "Create New World" (empty slot) and "Edit World" screen: world name, game mode, allow cheats. */
public class WorldSettingsScreen extends bh {
    private final bh back;
    private final WorldsScreen worlds;
    private final int slot;
    private final boolean create;
    private final Properties p;
    private String name;
    private boolean cheats, creative;
    private int ticks;

    public WorldSettingsScreen(bh back, WorldsScreen worlds, int slot, boolean create) {
        this.back = back;
        this.worlds = worlds;
        this.slot = slot;
        this.create = create;
        this.p = Ext.readWorld(slot);
        this.name = create ? "New World" : p.getProperty("name");
        this.cheats = !create && "true".equals(p.getProperty("cheats"));
        this.creative = !create && "true".equals(p.getProperty("creative"));
    }

    private String modeLabel() { return "Game Mode: " + (creative ? "Creative" : "Survival"); }
    private String cheatsLabel() { return "Allow Cheats: " + (cheats ? "ON" : "OFF"); }

    public void a() {
        Keyboard.enableRepeatEvents(true);
        this.e.clear();
        int y = this.d / 4;
        this.e.add(new fk(0, this.c / 2 - 100, y + 44, modeLabel()));
        this.e.add(new fk(1, this.c / 2 - 100, y + 68, cheatsLabel()));
        this.e.add(new fk(2, this.c / 2 - 100, y + 120, 98, 20, create ? "Create New World" : "Save"));
        this.e.add(new fk(3, this.c / 2 + 2, y + 120, 98, 20, "Cancel"));
    }

    public void h() { Keyboard.enableRepeatEvents(false); }
    public void g() { ticks++; }

    protected void a(fk b) {
        if (!b.g) return;
        if (b.f == 0) { creative = !creative; b.e = modeLabel(); }
        else if (b.f == 1) { cheats = !cheats; b.e = cheatsLabel(); }
        else if (b.f == 2) done();
        else if (b.f == 3) this.b.a(back);
    }

    private void done() {
        String n = name.trim();
        p.setProperty("name", n.length() == 0 ? "World " + slot : n);
        p.setProperty("cheats", String.valueOf(cheats));
        p.setProperty("creative", String.valueOf(creative));
        if (create) worlds.start(slot, p);
        else { Ext.writeWorld(slot, p); this.b.a(back); }
    }

    protected void a(char ch, int key) {
        if (key == 1) { this.b.a(back); return; }
        if (key == 28) { done(); return; }
        if (key == 14) { if (name.length() > 0) name = name.substring(0, name.length() - 1); return; }
        if (ch >= 32 && ch < 127 && name.length() < 32 && this.g.a(name + ch) < 190) name += ch;
    }

    public void a(int mx, int my, float f) {
        this.i();
        int y = this.d / 4, x = this.c / 2 - 100;
        this.a(this.g, create ? "Create New World" : "Edit World", this.c / 2, 20, 0xFFFFFF);
        this.b(this.g, "World Name", x, y, 0xA0A0A0);
        this.a(x - 1, y + 11, x + 201, y + 33, 0xFFA0A0A0);
        this.a(x, y + 12, x + 200, y + 32, 0xFF000000);
        this.b(this.g, name + ((ticks / 6) % 2 == 0 ? "_" : ""), x + 4, y + 18, 0xE0E0E0);
        String hint = cheats ? "F4 switches between Survival and Creative" : "Turn on cheats to switch modes with F4";
        this.a(this.g, hint, this.c / 2, y + 94, 0xA0A0A0);
        if (!create) this.a(this.g, "Saved in: saves/World" + slot, this.c / 2, y + 106, 0x707070);
        super.a(mx, my, f);
    }
}
