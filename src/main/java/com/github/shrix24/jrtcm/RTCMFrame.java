package com.github.shrix24.jrtcm;

import java.util.Arrays;

public class RTCMFrame {

    private final byte[] rawFrame;
    private final int messageType;
    private final int payloadLength;
    private final long timestamp;

    public RTCMFrame(byte[] rawFrame, int payloadLength) {
        this.rawFrame = Arrays.copyOf(rawFrame, 3 + payloadLength + 3);
        this.payloadLength = payloadLength;
        this.timestamp = System.currentTimeMillis();
        // Message type is first 12 bits of payload
        if (payloadLength >= 2) {
            this.messageType = ((rawFrame[3] & 0xFF) << 4) | ((rawFrame[4] & 0xF0) >>> 4);
        } else {
            this.messageType = 0;
        }
    }

    public byte[] getRawFrame() {
        return Arrays.copyOf(rawFrame, rawFrame.length);
    }

    public byte[] getPayload() {
        return Arrays.copyOfRange(rawFrame, 3, 3 + payloadLength);
    }

    public int getMessageType() {
        return messageType;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public int getTotalLength() {
        return rawFrame.length;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("RTCMFrame[type=%d, payloadLen=%d, totalLen=%d]",
                messageType, payloadLength, rawFrame.length);
    }
}
