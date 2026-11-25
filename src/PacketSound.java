import java.io.*;

/**
 * 0x3e server -> client: play a named sound at a position.
 * Format: utf name, int x, int y, int z (world coordinates * 8), float volume, float pitch.
 */
public class PacketSound extends fn {
    public String name = "";
    public int x, y, z;
    public float volume, pitch;

    public PacketSound() { }
    public PacketSound(String name, int x, int y, int z, float volume, float pitch) {
        this.name = name; this.x = x; this.y = y; this.z = z; this.volume = volume; this.pitch = pitch;
    }

    public void a(DataInputStream in) {
        try {
            name = in.readUTF();
            x = in.readInt(); y = in.readInt(); z = in.readInt();
            volume = in.readFloat(); pitch = in.readFloat();
        } catch (IOException e) { throw NetFx.<RuntimeException>sneaky(e); }
    }
    public void a(DataOutputStream out) {
        try {
            out.writeUTF(name == null ? "" : name);
            out.writeInt(x); out.writeInt(y); out.writeInt(z);
            out.writeFloat(volume); out.writeFloat(pitch);
        } catch (IOException e) { throw NetFx.<RuntimeException>sneaky(e); }
    }
    public void a(lb handler) { NetFx.sound(name, x, y, z, volume, pitch); }
    public int a() { return 2 + (name == null ? 0 : name.length()) + 12 + 8; }
}
