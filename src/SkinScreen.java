import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * "Change Skin" (title screen, where "Play tutorial level" used to be). The skin is stored as
 * .minecraft/skin.png and used for your player in singleplayer, and uploaded to recreatecraft servers
 * so other players see it too. The old minecraft.net skin server is gone, so nothing else works anyway.
 */
public class SkinScreen extends bh {
    private static int version = (int) (System.currentTimeMillis() / 1000L);
    private final bh parent;
    private String status = "";
    private volatile String picked;     // set by the file dialog thread
    private volatile boolean picking;

    public SkinScreen(bh parent) { this.parent = parent; }

    public static File skinFile() { return new File(net.minecraft.client.Minecraft.b(), "skin.png"); }

    // ---- hooks ----

    /** Title screen init: the disabled "Play tutorial level" button becomes "Change Skin". */
    public static void titleInit(bh title) {
        try {
            fk b = (fk) title.e.get(2);
            b.e = "Change Skin";
            b.g = true;
        } catch (Throwable t) { t.printStackTrace(); }
    }
    public static boolean titleButton(bh title, fk b) {
        if (b.f != 3) return false;
        title.b.a(new SkinScreen(title));
        return true;
    }

    /** Replaces the dead http://www.minecraft.net/skin/NAME.png URL for other players (nt). */
    public static String skinUrl(String name) {
        if (name == null || name.length() == 0 || Ext.httpBase == null) return null;
        try { return Ext.httpBase + "/api/skin?name=" + java.net.URLEncoder.encode(name, "UTF-8"); }
        catch (Throwable t) { return null; }
    }
    /** Same for your own player (bi): the local skin.png if there is one. */
    public static String localSkinUrl(String name) {
        File f = skinFile();
        if (f.exists()) return f.toURI().toString() + "?v=" + version;
        return skinUrl(name);
    }

    /** Start of the texture download thread (kz.run): read file: URLs ourselves, the game only knows HTTP. */
    public static boolean loadLocal(String url, hb fix, cc target) {
        if (url == null || !url.startsWith("file:")) return false;
        try {
            int q = url.indexOf('?');
            BufferedImage img = ImageIO.read(new File(new java.net.URI(q < 0 ? url : url.substring(0, q))));
            if (img != null) target.a = fix == null ? img : fix.a(img);
        } catch (Throwable t) { t.printStackTrace(); }
        return true;
    }

    // ---- screen ----

    public void a() {
        this.e.clear();
        this.e.add(new fk(0, this.c / 2 - 100, this.d / 4 + 72, "Choose skin file..."));
        this.e.add(new fk(1, this.c / 2 - 100, this.d / 4 + 96, "Use default skin"));
        this.e.add(new fk(2, this.c / 2 - 100, this.d / 4 + 132, "Done"));
        ((fk) this.e.get(1)).g = skinFile().exists();
    }

    protected void a(fk b) {
        if (!b.g) return;
        if (b.f == 0 && !picking) {
            picking = true;
            status = "Pick a PNG in the file window...";
            Thread t = new Thread("skin picker") { public void run() {
                try {
                    FileDialog d = new FileDialog((Frame) null, "Choose a Minecraft skin (64x32 or 64x64 PNG)", FileDialog.LOAD);
                    d.setFile("*.png");
                    d.setVisible(true);
                    picked = d.getFile() == null ? "" : new File(d.getDirectory(), d.getFile()).getPath();
                    d.dispose();
                } catch (Throwable e) { picked = ""; status = "Couldn't open a file window: " + e; }
            } };
            t.setDaemon(true);
            t.start();
        }
        if (b.f == 1) {
            if (skinFile().delete()) { version++; status = "Back to the default skin."; }
            this.a();
        }
        if (b.f == 2) this.b.a(parent);
    }

    public void g() {
        String p = picked;
        if (p == null) return;
        picked = null;
        picking = false;
        if (p.length() == 0) { if (status.startsWith("Pick")) status = ""; return; }
        status = apply(new File(p));
        this.a();
    }

    private static String apply(File f) {
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) return "That file isn't an image.";
            if (img.getWidth() != 64 || (img.getHeight() != 32 && img.getHeight() != 64))
                return "Skins must be 64x32 or 64x64 (that one is " + img.getWidth() + "x" + img.getHeight() + ").";
            // this version only knows the old 64x32 layout: keep the top half of new-style skins
            BufferedImage out = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            out.getGraphics().drawImage(img, 0, 0, null);
            ImageIO.write(out, "png", skinFile());
            version++;
            return "Skin changed to " + f.getName() + "!";
        } catch (Throwable t) {
            return "Couldn't use that file: " + t;
        }
    }

    public void a(int mx, int my, float f) {
        this.i();
        this.a(this.g, "Change Skin", this.c / 2, this.d / 4 - 20, 0xFFFFFF);
        this.a(this.g, skinFile().exists() ? "Using your custom skin (.minecraft/skin.png)" : "Using the default skin",
            this.c / 2, this.d / 4 + 10, 0xA0A0A0);
        this.a(this.g, "Other players see it on recreatecraft servers.", this.c / 2, this.d / 4 + 22, 0xA0A0A0);
        if (status.length() > 0) this.a(this.g, status, this.c / 2, this.d / 4 + 46, 0xFFFF55);
        super.a(mx, my, f);
    }
}
