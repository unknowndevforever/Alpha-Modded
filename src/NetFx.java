/**
 * Handlers for the recreatecraft server -> client packets (PacketHurt 0x08, PacketSound 0x3e, PacketWeather 0x46).
 * The game's network reader thread (ii) only queues packets; ii.a() runs fn.a(lb) on the game thread
 * (Minecraft tick -> gy.a() -> ii.a()), the same thread every vanilla gy handler touches the world on,
 * so these methods may use the world / player directly. Everything is still wrapped so a bad packet can never crash the game.
 */
public class NetFx {
    /** Set by the Minecraft.a(cn, String) hook (every world change) and by Ext.connect. */
    public static volatile net.minecraft.client.Minecraft mc;

    /** Hook at the start of Minecraft.a(cn, String): new world, leaving a world or disconnecting -> clear weather. */
    public static void worldChange(net.minecraft.client.Minecraft m) {
        if (m != null) mc = m;
        WeatherFx.reset();
    }

    /** 0x08: damage the local player through the game's own attackEntityFrom path (dm.a(kh, int) -> ge.a(kh, int)). */
    public static void hurt(int damage) {
        try {
            net.minecraft.client.Minecraft m = mc;
            if (m == null || m.e == null) return;
            bi p = m.g;
            if (p == null || Ext.creative || p.E <= 0) return;
            if (damage < 1) damage = 1;
            if (damage > 20) damage = 20;
            // null attacker = no knockback, random hurt tilt; armor, hurt flash, invulnerability frames,
            // hurt/death sound and death (-> game over screen via Minecraft's health check) are all vanilla
            p.a((kh) null, damage);
        } catch (Throwable t) { t.printStackTrace(); }
    }

    /** 0x3e: World.playSound(x, y, z, name, volume, pitch). Unknown names find no sound in the pool and play nothing. */
    public static void sound(String name, int x, int y, int z, float volume, float pitch) {
        try {
            net.minecraft.client.Minecraft m = mc;
            if (m == null || m.e == null || m.g == null) return;
            if (name == null || name.length() == 0 || name.length() > 64) return;
            if (!(volume > 0.0F)) return;              // also drops NaN
            if (volume > 16.0F) volume = 16.0F;
            if (!(pitch > 0.0F)) pitch = 1.0F;
            if (pitch < 0.5F) pitch = 0.5F;
            if (pitch > 2.0F) pitch = 2.0F;
            m.e.a(x / 8.0D, y / 8.0D, z / 8.0D, name, volume, pitch);
        } catch (Throwable t) { t.printStackTrace(); }
    }

    /** 0x46: 0 = clear, anything else = rain. */
    public static void weather(int state) {
        try { WeatherFx.setRaining(state != 0); } catch (Throwable t) { t.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> RuntimeException sneaky(Throwable t) throws T { throw (T) t; }
}
