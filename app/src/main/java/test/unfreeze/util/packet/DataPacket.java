package test.unfreeze.util.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DataPacket extends Packet {

    private int seq;

    private Data data;

    @Override
    public String packetName() {
        return "data";
    }

    public DataPacket(int seq, Data data) {
        this.seq = seq;
        this.data = data;
    }

    public DataPacket(int seq, byte[] bArr, int i2, int i3) {
        this(seq, new Data(bArr, i2, i3));
    }

    public int getSeq() {
        return this.seq;
    }

    public int getSize() {
        return this.data.size();
    }

    @Override
    public byte[] packetByteBuffer() {
        ByteBuffer order = ByteBuffer.allocate(getSize() + 2).order(ByteOrder.LITTLE_ENDIAN);
        order.putShort((short) this.seq);
        put(order);
        return order.array();
    }

    public void put(ByteBuffer byteBuffer) {
        byteBuffer.put(this.data.bytes, this.data.begin, getSize());
    }

    public String toString() {
        return "DataPacket{seq=" + this.seq + ", size=" + this.data.size() + '}';
    }
}
