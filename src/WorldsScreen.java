import java.util.Properties;
import net.minecraft.client.Minecraft;

/**
 * Replaces the vanilla "Select world" screen (jq). Same five slots, but:
 * empty slot -> Create New World screen (game mode + allow cheats), slots show the world's custom name,
 * and there is an "Edit world..." button next to "Delete world...".
 * The same class is reused for the delete and edit pickers (mode).
 */
public class WorldsScreen extends jq {
    static final int PLAY = 0, DELETE = 1, EDIT = 2;
    private final int mode;

    public WorldsScreen(bh parent) { this(parent, PLAY); }
    public WorldsScreen(bh parent, int mode) {
        super(parent);
        this.mode = mode;
        if (mode == DELETE) this.h = "Delete world";
        if (mode == EDIT) this.h = "Edit world";
    }

    public void a() {
        this.e.clear();
        for (int n = 0; n < 5; n++) {
            int slot = n + 1;
            hm tag = cn.a(Minecraft.b(), "World" + slot);
            String label = "- empty -";
            if (tag != null) {
                long size = tag.f("SizeOnDisk");
                label = Ext.readWorld(slot).getProperty("name") + " (" + (float) (size / 1024L * 100L / 1024L) / 100.0F + " MB)";
            }
            fk b = new fk(n, this.c / 2 - 100, this.d / 6 + 24 * n, label);
            if (tag == null && mode != PLAY) b.g = false;
            this.e.add(b);
        }
        this.j();
    }

    public void j() {
        if (mode == PLAY) {
            this.e.add(new fk(5, this.c / 2 - 100, this.d / 6 + 132, 98, 20, "Delete world..."));
            this.e.add(new fk(7, this.c / 2 + 2, this.d / 6 + 132, 98, 20, "Edit world..."));
        }
        this.e.add(new fk(6, this.c / 2 - 100, this.d / 6 + 168, "Cancel"));
    }

    protected void a(fk b) {
        if (!b.g) return;
        if (b.f < 5) pick(b.f + 1);
        else if (b.f == 5) this.b.a(new WorldsScreen(this, DELETE));
        else if (b.f == 7) this.b.a(new WorldsScreen(this, EDIT));
        else if (b.f == 6) this.b.a(this.a);
    }

    private void pick(int slot) {
        boolean exists = this.d(slot) != null;
        if (mode == DELETE) {
            if (exists) this.b.a(new ja(this, "Are you sure you want to delete this world?",
                    "'" + Ext.readWorld(slot).getProperty("name") + "' will be lost forever!", slot));
        } else if (mode == EDIT) {
            if (exists) this.b.a(new WorldSettingsScreen(this.a, null, slot, false));
        } else if (exists) {
            start(slot, Ext.readWorld(slot));
        } else {
            this.b.a(new WorldSettingsScreen(this, this, slot, true));
        }
    }

    /** Delete confirmation callback from ja. */
    public void a(boolean yes, int slot) {
        if (yes) cn.b(Minecraft.b(), "World" + slot);
        this.b.a(this.a);
    }

    /** Load (or generate) the world with these settings, then write them next to level.dat. */
    void start(int slot, Properties p) {
        Ext.enterWorld(slot, p);
        this.c(slot);
        // jq.c shows the title screen for a moment (which calls Ext.leaveWorld), so set the world again
        Ext.enterWorld(slot, p);
        Ext.saveWorld();
    }
}
