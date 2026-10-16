package com.yesclient.utils;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;

 






public final class DiscordRPCClient {

    private static final String CLIENT_ID = "1549134058481524766";
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int OP_PING = 3;
    private static final int OP_PONG = 4;

    private final Object ioLock = new Object();
    private volatile boolean running;
    private volatile RandomAccessFile pipe;
    private Thread worker;
    private volatile String details = "Playing YesClient";
    private volatile String state = "YesClient";
    private volatile long startedAt = System.currentTimeMillis() / 1000L;

    public synchronized void start() {
        if (running) return;
        running = true;
        startedAt = System.currentTimeMillis() / 1000L;
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                runLoop();
            }
        }, "YesClient-DiscordRPC");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        running = false;
        closePipe();
        Thread current = worker;
        worker = null;
        if (current != null && current != Thread.currentThread()) {
            current.interrupt();
            try {
                current.join(1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void restart() {
        stop();
        start();
    }

    public void setActivity(String details, String state) {
        if (details != null && !details.isEmpty()) this.details = details;
        if (state != null && !state.isEmpty()) this.state = state;
    }

    private void runLoop() {
        while (running) {
            try {
                if (!connect()) {
                    sleepQuietly(5000L);
                    continue;
                }

                sendFrame(OP_HANDSHAKE, "{\"v\":1,\"client_id\":\"" + CLIENT_ID + "\"}");
                readResponse();

                while (running && pipe != null) {
                    sendActivity();
                    sleepQuietly(15000L);
                }
            } catch (IOException ignored) {
                 
                 
            } finally {
                closePipe();
            }
        }
    }

    private boolean connect() {
        for (int i = 0; i < 10 && running; i++) {
            try {
                pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                return true;
            } catch (IOException ignored) {
                 
            }
        }
        return false;
    }

    private void sendActivity() throws IOException {
        long pid = processId();
        String nonce = UUID.randomUUID().toString();
        String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + pid
                + ",\"activity\":{\"type\":0,\"details\":\"" + escape(details)
                + "\",\"state\":\"" + escape(state) + "\",\"timestamps\":{\"start\":"
                + startedAt + "}}},\"nonce\":\"" + nonce + "\"}";
        sendFrame(OP_FRAME, payload);
        readResponse();
    }

    private void sendFrame(int opcode, String payload) throws IOException {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream(body.length + 8);
        writeLittleEndian(frame, opcode);
        writeLittleEndian(frame, body.length);
        frame.write(body);

        synchronized (ioLock) {
            if (pipe == null) throw new IOException("Discord IPC is not connected");
            pipe.write(frame.toByteArray());
        }
    }

    private void readResponse() throws IOException {
        while (running && pipe != null) {
            Frame frame = readFrame();
            if (frame.opcode == OP_PING) {
                sendRawFrame(OP_PONG, frame.payload);
                continue;
            }
            if (frame.opcode == OP_CLOSE) throw new IOException("Discord IPC closed");
            return;
        }
        throw new IOException("Discord IPC stopped");
    }

    private Frame readFrame() throws IOException {
        synchronized (ioLock) {
            if (pipe == null) throw new IOException("Discord IPC is not connected");
            int opcode = readLittleEndian(pipe);
            int length = readLittleEndian(pipe);
            if (length < 0 || length > 1024 * 1024) throw new IOException("Invalid Discord IPC frame");
            byte[] payload = new byte[length];
            pipe.readFully(payload);
            return new Frame(opcode, payload);
        }
    }

    private void sendRawFrame(int opcode, byte[] body) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream(body.length + 8);
        writeLittleEndian(frame, opcode);
        writeLittleEndian(frame, body.length);
        frame.write(body);
        synchronized (ioLock) {
            if (pipe == null) throw new IOException("Discord IPC is not connected");
            pipe.write(frame.toByteArray());
        }
    }

    private void closePipe() {
        RandomAccessFile current = pipe;
        pipe = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void writeLittleEndian(ByteArrayOutputStream output, int value) {
        output.write(value & 255);
        output.write((value >>> 8) & 255);
        output.write((value >>> 16) & 255);
        output.write((value >>> 24) & 255);
    }

    private static int readLittleEndian(RandomAccessFile input) throws IOException {
        int a = input.read();
        int b = input.read();
        int c = input.read();
        int d = input.read();
        if ((a | b | c | d) < 0) throw new IOException("Discord IPC ended");
        return a | b << 8 | c << 16 | d << 24;
    }

    private static long processId() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int at = name.indexOf('@');
            return Long.parseLong(at > 0 ? name.substring(0, at) : name);
        } catch (Exception ignored) {
            return 1L;
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class Frame {
        private final int opcode;
        private final byte[] payload;

        private Frame(int opcode, byte[] payload) {
            this.opcode = opcode;
            this.payload = payload;
        }
    }
}
