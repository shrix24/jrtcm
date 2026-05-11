package com.github.shrix24.jrtcm;

public final class CRC24Q {

    private static final int CRC24Q_POLY = 0x1864CFB;
    private static final int[] TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i << 16;
            for (int j = 0; j < 8; j++) {
                crc <<= 1;
                if ((crc & 0x1000000) != 0) {
                    crc ^= CRC24Q_POLY;
                }
            }
            TABLE[i] = crc & 0xFFFFFF;
        }
    }

    private CRC24Q() {}

    public static int compute(byte[] data, int offset, int length) {
        int crc = 0;
        for (int i = offset; i < offset + length; i++) {
            crc = ((crc << 8) & 0xFFFFFF) ^ TABLE[((crc >> 16) ^ (data[i] & 0xFF)) & 0xFF];
        }
        return crc;
    }

    public static boolean validate(byte[] frame, int frameLength) {
        int computed = compute(frame, 0, frameLength - 3);
        int embedded = ((frame[frameLength - 3] & 0xFF) << 16)
                     | ((frame[frameLength - 2] & 0xFF) << 8)
                     | (frame[frameLength - 1] & 0xFF);
        return computed == embedded;
    }
}
