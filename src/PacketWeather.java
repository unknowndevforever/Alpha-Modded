import java.io.*;

/** 0x46 server -> client: weather. Format: byte state (0 = clear, 1 = rain). */
public class PacketWeather extends fn {
    public int state;

    public PacketWeather() { }
    public PacketWeather(int state) { this.state = state; }

    public void a(DataInputStream in) { try { state = in.readByte(); } catch (IOException e) { throw NetFx.<RuntimeException>sneaky(e); } }
    public void a(DataOutputStream out) { try { out.writeByte(state); } catch (IOException e) { throw NetFx.<RuntimeException>sneaky(e); } }
    public void a(lb handler) { NetFx.weather(state); }
    public int a() { return 1; }
}
