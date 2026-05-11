package com.github.shrix24.jrtcm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RTCMBridge {

    // ---- CONFIGURATION ---- change these values to match your setup ----
    private static final String SERIAL_PORT = "COM3";   // e.g. "COM3" on Windows, "/dev/ttyUSB0" on Linux
    private static final int BAUD_RATE = 115200;
    // --------------------------------------------------------------------

    private final RTCMFrameReader reader;
    private final List<RTCMFrame> frames;
    private volatile boolean running;

    public RTCMBridge(String port, int baudRate) {
        this.reader = new RTCMFrameReader(port, baudRate);
        this.frames = Collections.synchronizedList(new ArrayList<>());
        this.running = false;
    }

    public void start() throws IOException {
        reader.open();
        running = true;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        System.out.println("JRTCM Bridge started on " + SERIAL_PORT + " @ " + BAUD_RATE + " baud");
        System.out.println("Press Ctrl+C to stop\n");

        while (running) {
            try {
                RTCMFrame frame = reader.readFrame();
                frames.add(frame);
                System.out.printf("[%d] %s%n", frames.size(), frame);
            } catch (IOException e) {
                if (!running) break;
                System.err.println("Serial error: " + e.getMessage());
                System.err.println("Attempting reconnect in 2s...");
                try {
                    reader.close();
                    Thread.sleep(2000);
                    reader.open();
                    System.err.println("Reconnected.");
                } catch (Exception re) {
                    System.err.println("Reconnect failed: " + re.getMessage());
                    break;
                }
            }
        }

        printStats();
    }

    public void stop() {
        running = false;
        reader.close();
    }

    public List<RTCMFrame> getFrames() {
        return Collections.unmodifiableList(new ArrayList<>(frames));
    }

    public RTCMFrame getLatestFrame() {
        if (frames.isEmpty()) return null;
        return frames.get(frames.size() - 1);
    }

    public int getFrameCount() {
        return frames.size();
    }

    private void printStats() {
        System.out.println("\n--- JRTCM Stats ---");
        System.out.println("Frames read:  " + reader.getFramesRead());
        System.out.println("CRC failures: " + reader.getCrcFailures());
        System.out.println("Resyncs:      " + reader.getResyncs());
        System.out.println("Stored:       " + frames.size());
    }

    public static void main(String[] args) throws IOException {
        String port = args.length > 0 ? args[0] : SERIAL_PORT;
        int baud = args.length > 1 ? Integer.parseInt(args[1]) : BAUD_RATE;

        RTCMBridge bridge = new RTCMBridge(port, baud);
        bridge.start();
    }
}
