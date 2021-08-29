package test.unfreeze.util.packet;

public class InvalidPacket extends Packet {
    @Override
    public String packetName() {
        return "invalid";
    }

    public String toString() {
        return "InvalidPacket{}";
    }

    @Override
    public byte[] packetByteBuffer() {
        return new byte[0];
    }
}
