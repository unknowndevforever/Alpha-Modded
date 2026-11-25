import java.io.*;

/** 0x08 server -> client: hurt the local player. Format: short damage (1..20). */
public class PacketHurt extends fn {
    public int damage;

    public PacketHurt() { }
    public PacketHurt(int damage) { this.damage = damage; }

    public void a(DataInputStream in) { try { damage = in.readShort(); } catch (IOException e) { throw NetFx.<RuntimeException>sneaky(e); } }
    public void a(DataOutputStream out) { try { out.writeShort(damage); } catch (IOException e) { throw NetFx.<RuntimeException>sneaky(e); } }
    public void a(lb handler) { NetFx.hurt(damage); }
    public int a() { return 2; }
}
