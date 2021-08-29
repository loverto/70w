package test.unfreeze.util.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class ACKPacket extends Packet {

    private int status;

    private int seq;

    @Override
    public String packetName() {
        return "ack";
    }

    public ACKPacket(int status) {
        this(status, 0);
    }

    public ACKPacket(int status, int seq) {
        this.status = status;
        this.seq = seq;
    }

    public int getStatus() {
        return this.status;
    }

    public int getSeq() {
        return this.seq;
    }

    @Override
    public byte[] packetByteBuffer() {
        ByteBuffer order = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
        order.putShort((short) 0);
        order.put((byte) 1);
        order.put((byte) this.status);
        order.putShort((short) this.seq);
        return order.array();
    }

    public String toString() {
        return "ACKPacket{status=" + this.status + ", seq=" + this.seq + '}';
    }
}
