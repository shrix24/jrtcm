package com.github.shrix24.jrtcm;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.io.InputStream;

public class RTCMFrameReader implements AutoCloseable {

    private static final int SYNC_BYTE = 0xD3;
    private static final int HEADER_LENGTH = 3;
    private static final int CRC_LENGTH = 3;
    private static final int MAX_PAYLOAD_LENGTH = 1023;

    private final SerialPort serialPort;
    private InputStream inputStream;
    private long crcFailures = 0;
    private long framesRead = 0;
    private long resyncs = 0;

    public RTCMFrameReader(String portName, int baudRate) {
        this.serialPort = SerialPort.getCommPort(portName);
        this.serialPort.setBaudRate(baudRate);
        this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
    }

    public void open() throws IOException {
        if (!serialPort.openPort()) {
            throw new IOException("Failed to open serial port: " + serialPort.getSystemPortName());
        }
        this.inputStream = serialPort.getInputStream();
    }

    public RTCMFrame readFrame() throws IOException {
        while (true) {
            waitForSync();

            byte[] header = new byte[HEADER_LENGTH];
            header[0] = (byte) SYNC_BYTE;
            if (readExact(header, 1, 2) < 2) {
                throw new IOException("Serial port read failed during header");
            }

            int payloadLength = ((header[1] & 0x03) << 8) | (header[2] & 0xFF);
            if (payloadLength > MAX_PAYLOAD_LENGTH) {
                resyncs++;
                continue;
            }

            int frameLength = HEADER_LENGTH + payloadLength + CRC_LENGTH;
            byte[] frame = new byte[frameLength];
            System.arraycopy(header, 0, frame, 0, HEADER_LENGTH);

            int remaining = payloadLength + CRC_LENGTH;
            if (readExact(frame, HEADER_LENGTH, remaining) < remaining) {
                throw new IOException("Serial port read failed during payload");
            }

            if (CRC24Q.validate(frame, frameLength)) {
                framesRead++;
                return new RTCMFrame(frame, payloadLength);
            }

            crcFailures++;
            resyncs++;
        }
    }

    private void waitForSync() throws IOException {
        int b;
        while (true) {
            b = readByte();
            if (b == SYNC_BYTE) {
                return;
            }
        }
    }

    private int readByte() throws IOException {
        if (inputStream == null) {
            throw new IOException("Serial port not open");
        }
        int b = inputStream.read();
        if (b < 0) {
            throw new IOException("Serial port disconnected");
        }
        return b;
    }

    private int readExact(byte[] buf, int offset, int length) throws IOException {
        int totalRead = 0;
        while (totalRead < length) {
            int n = inputStream.read(buf, offset + totalRead, length - totalRead);
            if (n < 0) {
                throw new IOException("Serial port disconnected");
            }
            totalRead += n;
        }
        return totalRead;
    }

    public boolean isOpen() {
        return serialPort.isOpen();
    }

    public long getCrcFailures() {
        return crcFailures;
    }

    public long getFramesRead() {
        return framesRead;
    }

    public long getResyncs() {
        return resyncs;
    }

    @Override
    public void close() {
        if (serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}
