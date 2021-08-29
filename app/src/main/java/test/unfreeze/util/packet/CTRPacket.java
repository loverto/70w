package test.unfreeze.util.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CTRPacket extends Packet {

    private int frameCount;

    private int packetType;

    @Override
    public String packetName() {
        return "ctr";
    }

    public CTRPacket(int frameCount, int packetType) {
        this.frameCount = frameCount;
        this.packetType = packetType;
    }

    public int getFrameCount() {
        return this.frameCount;
    }

    public int getPacketType() {
        return this.packetType;
    }

    @Override
    public byte[] packetByteBuffer() {
        ByteBuffer order = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
        order.putShort((short) 0);
        order.put((byte) 0);
        order.put((byte) this.packetType);
        order.putShort((short) this.frameCount);
        return order.array();
    }

    public String toString() {
        return "FlowPacket{frameCount=" + this.frameCount + "packageType=" + this.packetType + '}';
    }
}
