import java.io.*;
import java.net.*;

/**
 * A Socket that tunnels the normal game protocol through HTTP POSTs, for servers that can only speak HTTP
 * (the Next.js server on wasmer.app). Every poll sends the bytes the game wrote since the last one and
 * gets back the bytes the server queued for us. The game's network code (ii) never notices the difference.
 */
public class HttpSocket extends Socket {
    private final String base;
    private final String sid;
    private volatile boolean closed = false;
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    private final Object outLock = new Object();
    private final In in = new In();
    private final OutputStream out = new OutputStream() {
        public void write(int b) throws IOException { check(); synchronized (outLock) { pending.write(b); } }
        public void write(byte[] b, int off, int len) throws IOException { check(); synchronized (outLock) { pending.write(b, off, len); } }
    };

    /**
     * "host" as typed in the Multiplayer screen -> base URL, or null for a normal TCP server.
     * name.wasmer.app (any port) and host:443 -> https, host:80 / host:3000 / host:8080 -> http.
     */
    public static String baseFor(String host, int port) {
        String h = host.trim().toLowerCase();
        boolean https = h.endsWith(".wasmer.app") || h.endsWith(".wasmer.dev") || h.endsWith(".hf.space") || h.endsWith(".trycloudflare.com") || port == 443;
        if (https) return "https://" + h + (port == 443 || port == 25565 ? "" : ":" + port);
        if (port == 80) return "http://" + h;
        if (port == 3000 || port == 8080) return "http://" + h + ":" + port;
        return null;
    }

    public HttpSocket(String base) throws IOException {
        this.base = base;
        HttpURLConnection c = open("/api/mc?op=open");
        c.getOutputStream().close();
        int code = c.getResponseCode();
        if (code != 200) throw new ConnectException("Server answered HTTP " + code + " (is this a recreatecraft server?)");
        String id = readAll(c.getInputStream()).trim();
        if (id.length() == 0) throw new ConnectException("Server gave no session id");
        this.sid = id;
        Thread t = new Thread("HTTP tunnel " + base) { public void run() { loop(); } };
        t.setDaemon(true);
        t.start();
    }

    private HttpURLConnection open(String path) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(base + path).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(10000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Content-Type", "application/octet-stream");
        return c;
    }

    private void loop() {
        int fails = 0;
        boolean more = false;
        while (!closed) {
            try {
                byte[] send;
                synchronized (outLock) {
                    // one request per tick carries all its packets; poll straight away while the server is still sending
                    outLock.wait(more ? 10 : pending.size() == 0 ? 50 : 20);
                    send = pending.toByteArray();
                    pending.reset();
                }
                HttpURLConnection c = open("/api/mc?sid=" + URLEncoder.encode(sid, "UTF-8"));
                c.setFixedLengthStreamingMode(send.length);
                OutputStream o = c.getOutputStream();
                o.write(send); o.close();
                int code = c.getResponseCode();
                if (code == 410 || code == 404) { in.push(null); closed = true; return; }  // session gone: end of stream
                if (code != 200) throw new IOException("HTTP " + code);
                InputStream is = c.getInputStream();
                ByteArrayOutputStream got = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                for (int n; (n = is.read(buf)) > 0; ) got.write(buf, 0, n);
                is.close();
                more = got.size() > 0;
                if (more) in.push(got.toByteArray());
                fails = 0;
            } catch (Exception e) {
                if (closed) return;
                if (++fails >= 8) { e.printStackTrace(); in.push(null); closed = true; return; }
                try { Thread.sleep(250L * fails); } catch (InterruptedException ie) { }
            }
        }
    }

    private void check() throws IOException { if (closed) throw new SocketException("Socket closed"); }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (int n; (n = is.read(buf)) > 0; ) b.write(buf, 0, n);
        is.close();
        return new String(b.toByteArray(), "UTF-8");
    }

    /** Blocking input stream fed by the poll thread. null chunk = end of stream. */
    private class In extends InputStream {
        private final java.util.LinkedList<byte[]> q = new java.util.LinkedList<byte[]>();
        private byte[] cur; private int pos; private boolean eof;
        synchronized void push(byte[] b) { if (b == null) eof = true; else q.add(b); notifyAll(); }
        private synchronized boolean fill() throws IOException {
            while (cur == null || pos >= cur.length) {
                if (!q.isEmpty()) { cur = q.removeFirst(); pos = 0; continue; }
                if (eof || closed) return false;
                try { wait(); } catch (InterruptedException e) { throw new InterruptedIOException(); }
            }
            return true;
        }
        public synchronized int read() throws IOException { return fill() ? cur[pos++] & 0xFF : -1; }
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            if (!fill()) return -1;
            int n = Math.min(len, cur.length - pos);
            System.arraycopy(cur, pos, b, off, n); pos += n;
            return n;
        }
        public synchronized int available() { return cur == null ? 0 : cur.length - pos; }
    }

    public InputStream getInputStream() { return in; }
    public OutputStream getOutputStream() { return out; }
    public void setTrafficClass(int tc) { }
    public boolean isConnected() { return true; }
    public boolean isClosed() { return closed; }

    public synchronized void close() {
        if (closed) return;
        closed = true;
        synchronized (in) { in.notifyAll(); }
        synchronized (outLock) { outLock.notifyAll(); }
        new Thread() { public void run() {
            try { HttpURLConnection c = open("/api/mc?op=close&sid=" + URLEncoder.encode(sid, "UTF-8")); c.getOutputStream().close(); c.getResponseCode(); } catch (Exception e) { }
        } }.start();
    }

    /** Upload the local skin so other players see it. */
    public static void uploadSkin(String base, String name, File png) {
        try {
            byte[] data = new byte[(int) png.length()];
            DataInputStream d = new DataInputStream(new FileInputStream(png)); d.readFully(data); d.close();
            HttpURLConnection c = (HttpURLConnection) new URL(base + "/api/skin?name=" + URLEncoder.encode(name, "UTF-8")).openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "image/png");
            OutputStream o = c.getOutputStream(); o.write(data); o.close();
            c.getResponseCode();
        } catch (Exception e) { System.out.println("Skin upload failed: " + e); }
    }
}
