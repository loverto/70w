package test.unfreeze.util.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public abstract class Packet {
    public abstract String packetName();

    public abstract byte[] packetByteBuffer();


    private static class PacketMetaInfo {

        int orderShort;

        int packetType;

        int status;

        int seq;

        byte[] byteArray;

        private PacketMetaInfo() {
        }
    }


    static class Data {

        byte[] bytes;
        // TODO 含义待定
        int begin;
        // TODO 含义待定
        int end;

        Data(byte[] bytes, int begin) {
            this(bytes, begin, bytes.length);
        }

        Data(byte[] bytes, int begin, int end) {
            this.bytes = bytes;
            this.begin = begin;
            this.end = end;
        }
        int size() {
            return this.end - this.begin;
        }
    }

    private static PacketMetaInfo buildPacketMetaInfo(byte[] bytes) {
        PacketMetaInfo packetMetaInfo = new PacketMetaInfo();
        ByteBuffer order = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        packetMetaInfo.orderShort = order.getShort();
        packetMetaInfo.byteArray = bytes;
        if (packetMetaInfo.orderShort == 0) {
            packetMetaInfo.packetType = order.get();
            packetMetaInfo.status = order.get();
            try {
                packetMetaInfo.seq = order.getShort();
            } catch (BufferUnderflowException unused) {
            }
        }
        return packetMetaInfo;
    }

    public static Packet buildPacket(byte[] bytes) {
        PacketMetaInfo packetMetaInfo;
        try {
            packetMetaInfo = buildPacketMetaInfo(bytes);
        } catch (BufferUnderflowException e) {
            e.printStackTrace();
            packetMetaInfo = null;
        }
        if (packetMetaInfo == null) {
            return new InvalidPacket();
        }
        if (packetMetaInfo.orderShort == 0) {
            return buildPacket(packetMetaInfo);
        }
        return buildCRCDataPacket(packetMetaInfo);
    }

    private static Packet buildPacket(PacketMetaInfo packetMetaInfo) {
        int seq = packetMetaInfo.seq;
        switch (packetMetaInfo.packetType) {
            case 0:
                return new CTRPacket(seq, packetMetaInfo.status);
            case 1:
                return new ACKPacket(packetMetaInfo.status, seq);
            default:
                return new InvalidPacket();
        }
    }

    private static Packet buildCRCDataPacket(PacketMetaInfo packetMetaInfo) {
        return new DataPacket(packetMetaInfo.orderShort, new Data(packetMetaInfo.byteArray, 2));
    }
}
