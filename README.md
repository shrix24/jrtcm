# jrtcm

Minimal Java RTCM3 frame reader. Reads RTCM3 binary data from a USB serial port, validates CRC-24Q, and stores complete frames as program variables.

Inspired by [pyrtcm](https://github.com/semuconsulting/pyrtcm).

## Features

- Serial port reading via [jSerialComm](https://fazecast.github.io/jSerialComm/)
- RTCM3 frame sync detection (0xD3 preamble)
- Frame length extraction (10-bit, big-endian)
- CRC-24Q validation
- Automatic resync on CRC failure
- Auto-reconnect on serial port disconnect
- Continuous operation until interrupted (Ctrl+C)

## Building

```bash
mvn clean package
```

## Usage

### Command line

```bash
# Default: COM3 @ 115200 baud (edit RTCMBridge.java to change defaults)
java -jar target/jrtcm-0.1.0.jar

# Specify port and baud rate
java -jar target/jrtcm-0.1.0.jar COM5 9600
java -jar target/jrtcm-0.1.0.jar /dev/ttyUSB0 115200
```

### As a library

```java
// Simple continuous reading
RTCMBridge bridge = new RTCMBridge("/dev/ttyUSB0", 115200);
bridge.start(); // blocks until Ctrl+C

// Access stored frames
List<RTCMFrame> frames = bridge.getFrames();
RTCMFrame latest = bridge.getLatestFrame();

// Low-level frame reader
RTCMFrameReader reader = new RTCMFrameReader("COM3", 115200);
reader.open();
RTCMFrame frame = reader.readFrame();
System.out.println(frame.getMessageType());  // e.g. 1077
System.out.println(frame.getPayloadLength());
byte[] raw = frame.getRawFrame();
reader.close();
```

## Project Structure

```
src/main/java/com/github/shrix24/jrtcm/
├── CRC24Q.java          - CRC-24Q lookup table and validation
├── RTCMFrame.java       - Frame data holder (raw bytes, message type, length)
├── RTCMFrameReader.java - Serial port reader, sync detection, CRC validation
└── RTCMBridge.java      - Main loop, frame storage, reconnect handling
```

## RTCM3 Frame Structure

```
┌──────────┬────────────┬─────────────────┬──────────┐
│ Preamble │ Length     │ Payload         │ CRC-24Q  │
│ 0xD3     │ 2 bytes   │ 0-1023 bytes    │ 3 bytes  │
│ (1 byte) │ (10 bits) │                 │          │
└──────────┴────────────┴─────────────────┴──────────┘
```

## Configuration

Edit constants in `RTCMBridge.java`:

```java
private static final String SERIAL_PORT = "COM3";
private static final int BAUD_RATE = 115200;
```

Or pass as command-line arguments.

## Requirements

- Java 11+
- Maven 3.6+
