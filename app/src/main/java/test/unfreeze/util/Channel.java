package test.unfreeze.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import test.unfreeze.util.packet.ACKPacket;
import test.unfreeze.util.packet.CTRPacket;
import test.unfreeze.util.packet.DataPacket;
import test.unfreeze.util.packet.Packet;

public abstract class Channel implements IChannel {

    private int begin = 32;

    private int totalBytes;

    private int frameCount;

    private int packageType;

    private ChannelState channelState = ChannelState.IDLE;

    private ChannelCallback channelCallback;

    private byte[] bytes;

    private int seq;

    private Timer timer;

    private SparseArray<Packet> packetSparseArray = new SparseArray<>();

    private int dataPacketSeq;

    private Handler mainHandler;

    private Handler channelHandler;

    private final IChannelStateHandler handlerSyncAckChannelStateHandler = new IChannelStateHandler() {

        @Override
        public void stateHandler(Object... objArr) {
            Channel.this.switchLooper(false);
            DataPacket dataPacket = (DataPacket) objArr[0];
            if (dataPacket.getSeq() != Channel.this.dataPacketSeq) {
                Log.e("Channel", "sync packet not matched!!");
            } else if (!Channel.this.addPacketSparseArray( dataPacket)) {
                Log.w("Channel", "sync packet repeated!!");
            } else {
                Channel.this.seq = Channel.this.dataPacketSeq;
                Channel.this.dataPacketSeq = 0;
                Channel.this.start();
            }
        }
    };

    private final IChannelStateHandler handlerReadingChannelStateHandler = new IChannelStateHandler() {

        @Override
        public void stateHandler(Object... objArr) {
            Channel.this.switchLooper( false);
            DataPacket cVar = (DataPacket) objArr[0];
            if (!Channel.this.addPacketSparseArray( cVar)) {
                Log.w("Channel", "dataPacket repeated!!");
            } else if (cVar.getSeq() == Channel.this.frameCount) {
                Channel.this.start();
            } else {
                Channel.this.start(6000,new Timer.AbstractRunnable("WaitData") {
                    @Override
                    public void begin() {
                        Channel.this.start();
                    }

                    @Override
                    public void end() {
                        Channel.this.timer.destroy();
                    }
                });
            }
        }
    };

    private final IChannelStateHandler handlerIdleChannelStateHandler = new IChannelStateHandler() {

        @Override
        public void stateHandler(Object... objArr) {
            Channel.this.switchLooper( false);
            CTRPacket ctrPacket = (CTRPacket) objArr[0];
            Channel.this.frameCount = ctrPacket.getFrameCount();
            Channel.this.packageType = ctrPacket.getPacketType();
            ACKPacket ackPacket = new ACKPacket(1);
            Channel.this.switchChannelState(ChannelState.READING);
            Channel.this.handlerPacket( ackPacket,new ChannelCallback() {
                @Override
                public void channelCallback(int requestCode) {
                    Channel.this.switchLooper( false);
                    if (requestCode == 0) {
                        Channel.this.startFor6000L();
                    } else {
                        Channel.this.clear();
                    }
                }
            });
            Channel.this.switchChannelState(ChannelState.READY);
        }
    };

    private final IChannelStateHandler handlerReadyChannelStateHandler = new IChannelStateHandler() {
        @Override
        public void stateHandler(Object... objArr) {
            Channel.this.switchLooper( false);
            Channel.this.switchChannelState( ChannelState.READY);
            Channel.this.startFor6000L();
        }
    };

    private final Timer.AbstractRunnable abstractRunnable = new Timer.AbstractRunnable(getClass().getSimpleName()) {

        @Override
        public void begin() {
            Channel.this.switchLooper(false);
            Channel.this.handlerMainHandlerChannelCallback( -2);
            Channel.this.clear();
        }

        @Override
        public void end() {
            Channel.this.timer.destroy();
        }
    };

    private final IChannelStateHandler handlerWaitStartAckAndSyncChannelStateHandler = new IChannelStateHandler() {

        @Override
        public void stateHandler(Object... objArr) {
            Channel.this.switchLooper( false);
            ACKPacket ackPacket = (ACKPacket) objArr[0];
            int status = ackPacket.getStatus();
            if (status != 5) {
                switch (status) {
                    case 0:
                        Channel.this.handlerMainHandlerChannelCallback( 0);
                        Channel.this.clear();
                        return;
                    case 1:
                        Channel.this.destroy();
                        Channel.this.switchChannelState( ChannelState.IDLE);
                        Channel.this.handlerPacket( 0, true);
                        return;
                    default:
                        Channel.this.handlerMainHandlerChannelCallback( -1);
                        Channel.this.clear();
                        return;
                }
            } else {
                int c = ackPacket.getSeq();
                if (c >= 1 && c <= Channel.this.frameCount) {
                    Channel.this.handlerPacket( (c - 1), false);
                    Channel.this.startFor6000L();
                }
            }
        }
    };
    private final ChannelStateBlock[] channelStateBlocks = {
            new ChannelStateBlock(ChannelState.READY, ChannelEvent.SEND_CTR, this.handlerReadyChannelStateHandler),
            new ChannelStateBlock(ChannelState.WAIT_START_ACK, ChannelEvent.RECV_ACK, this.handlerWaitStartAckAndSyncChannelStateHandler),
            new ChannelStateBlock(ChannelState.SYNC, ChannelEvent.RECV_ACK, this.handlerWaitStartAckAndSyncChannelStateHandler),
            new ChannelStateBlock(ChannelState.IDLE, ChannelEvent.RECV_CTR, this.handlerIdleChannelStateHandler),
            new ChannelStateBlock(ChannelState.READING, ChannelEvent.RECV_DATA, this.handlerReadingChannelStateHandler),
            new ChannelStateBlock(ChannelState.SYNC_ACK, ChannelEvent.RECV_DATA, this.handlerSyncAckChannelStateHandler)};

    public abstract boolean isCrcVerifyData();

    public Channel() {
        Looper looper = ChannelLooper.getInstance();
        this.timer = new Timer(looper);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.channelHandler = new Handler(looper);
    }

    public final void writeData(final byte[] bytesData, final int packageType, final ChannelCallback channelCallback) {
        this.channelHandler.post(new Runnable() {
            public void run() {
                Channel.this.writeDataHandler(bytesData, packageType, channelCallback);
            }
        });
    }

    public final void dataChange(final byte[] bArr) {
        this.channelHandler.post(new Runnable() {
            public void run() {
                Channel.this.handlerDataChange(bArr);
            }
        });
    }


    public void handlerPacket(Packet packet, ChannelCallback channelCallback) {
        handlerPacket(packet, channelCallback, false);
    }

    private void handlerPacket(Packet packet, final ChannelCallback channelCallback, final boolean flag) {
        switchLooper(false);
        if (channelCallback != null) {
            if (!timerExists()) {
                timeout();
            }
            final byte[] packetByteBuffer = packet.packetByteBuffer();
            this.mainHandler.post(new Runnable() {
                public void run() {
                    Channel.this.channelReceiver(packetByteBuffer, new ChannelCallBackImpl(channelCallback), flag);
                }
            });
            if (flag) {
                channelCallback.channelCallback(0);
                return;
            }
            return;
        }
        throw new NullPointerException("callback can't be null");
    }

    private class ChannelCallBackImpl implements ChannelCallback {

        ChannelCallback callback;

        ChannelCallBackImpl(ChannelCallback channelCallback) {
            this.callback = channelCallback;
        }

        @Override
        public void channelCallback(final int requestCode) {
            if (Channel.this.isExceptionRunnable()) {
                Channel.this.destroy();
            }
            Channel.this.channelHandler.post(new Runnable() {
                public void run() {
                    ChannelCallBackImpl.this.callback.channelCallback(requestCode);
                }
            });
        }
    }

    private void handlerPacket(int packetType) {
        switchLooper(false);
        handlerPacket(new CTRPacket(this.frameCount, packetType), new ChannelCallback() {
            @Override
            public void channelCallback(int requestCode) {
                Channel.this.switchLooper(false);
                if (requestCode != 0) {
                    Channel.this.handlerMainHandlerChannelCallback( -1);
                    Channel.this.clear();
                }
            }
        });
        handlerChannelStateBlock(ChannelEvent.SEND_CTR, new Object[0]);
    }

    public void handlerMainHandlerChannelCallback(final int requestCode) {
        switchLooper(false);
        if (this.channelCallback != null) {
            final ChannelCallback channelCallback = this.channelCallback;
            this.mainHandler.post(new Runnable() {
                public void run() {
                    channelCallback.channelCallback(requestCode);
                }
            });
        }
    }


    public boolean addPacketSparseArray(DataPacket dataPacket) {
        switchLooper(false);
        if (this.packetSparseArray.get(dataPacket.getSeq()) != null) {
            return false;
        }
        this.packetSparseArray.put(dataPacket.getSeq(), dataPacket);
        this.totalBytes += dataPacket.getSize();
        destroy();
        return true;
    }


    public void start() {
        switchLooper(false);
        startFor6000L();
        switchChannelState(ChannelState.SYNC);
        if (!handlerSeq()) {
            final byte[] channelData = getChannelData();
            if (!ByteUtil.isNull(channelData)) {
                handlerPacket(new ACKPacket(0), new ChannelCallback() {
                    @Override
                    public void channelCallback(int requestCode) {
                        Channel.this.switchLooper(false);
                        Channel.this.clear();
                        if (requestCode == 0) {
                            Channel.this.handlerChannelData(channelData);
                        }
                    }
                });
            } else {
                clear();
            }
        }
    }


    public void handlerChannelData(byte[] channelData) {
        this.mainHandler.post(new MainHandlerRunnable(channelData, this.packageType));
    }


    private class MainHandlerRunnable implements Runnable {

        private byte[] channelData;

        private int packageType;

        MainHandlerRunnable(byte[] channelData, int packageType) {
            this.channelData = channelData;
            this.packageType = packageType;
        }

        public void run() {
            Channel.this.receiver(this.channelData, this.packageType);
        }
    }

    private byte[] getChannelData() {
        switchLooper(false);
        if (this.packetSparseArray.size() == this.frameCount) {
            ByteBuffer allocate = ByteBuffer.allocate(this.totalBytes);
            for (int i = 1; i <= this.frameCount; i++) {
                ((DataPacket) this.packetSparseArray.get(i)).put(allocate);
            }
            if (!isCrcVerifyData()) {
                return allocate.array();
            }
            byte[] bytes = {allocate.get(this.totalBytes - 4), allocate.get(this.totalBytes - 3), allocate.get(this.totalBytes - 2), allocate.get(this.totalBytes - 1)};
            byte[] arraycopy = new byte[(this.totalBytes - 4)];
            System.arraycopy(allocate.array(), 0, arraycopy, 0, this.totalBytes - 4);
            if (bytesCompare(arraycopy, bytes)) {
                return arraycopy;
            }
            Log.e("Channel", "check crc failed!!");
            return ByteUtil.EMPTY_BYTES;
        }
        throw new IllegalStateException();
    }

    private boolean handlerSeq() {
        switchLooper(false);
        int seq = this.seq + 1;
        while (seq <= this.frameCount && this.packetSparseArray.get(seq) != null) {
            seq++;
        }
        if (seq > this.frameCount) {
            return false;
        }
        this.dataPacketSeq = seq;
        handlerPacket(new ACKPacket(5, seq), new ChannelCallback() {
            @Override
            public void channelCallback(int requestCode) {
                Channel.this.switchLooper( false);
                if (requestCode == 0) {
                    Channel.this.startFor6000L();
                } else {
                    Channel.this.clear();
                }
            }
        });
        switchChannelState(ChannelState.SYNC_ACK);
        return true;
    }


    public void clear() {
        switchLooper(false);
        destroy();
        switchChannelState(ChannelState.IDLE);
        this.bytes = null;
        this.frameCount = 0;
        this.channelCallback = null;
        this.packetSparseArray.clear();
        this.dataPacketSeq = 0;
        this.seq = 0;
        this.totalBytes = 0;
    }


    public void handlerPacket(final int frameCount, final boolean flag) {
        switchLooper(false);
        if (frameCount >= this.frameCount) {
            Log.d("Channel", " all packets sent!!");
            switchChannelState(ChannelState.SYNC);
            start(30000L);
            return;
        }
        int begin = getBegin();
        int seq = frameCount + 1;
        handlerPacket(new DataPacket(seq, this.bytes, frameCount * begin, Math.min(this.bytes.length, begin * seq)), new ChannelCallback() {
            @Override
            public void channelCallback(int requestCode) {
                Channel.this.switchLooper( false);
                if (requestCode != 0) {
                    Log.w("Channel", String.format("packet %d write failed", Integer.valueOf(requestCode)));
                }
                if (flag) {
                    Channel.this.handlerPacket( (requestCode + 1), flag);
                }
            }
        }, true);
    }


    public void switchChannelState(ChannelState channelState) {
        switchLooper(false);
        Log.d("Channel", String.format("state = %s", channelState));
        this.channelState = channelState;
    }

    private void handlerChannelStateBlock(ChannelEvent event, Object... objArr) {
        boolean flag = false;
        switchLooper(false);
        Log.d("Channel", String.format("state = %s, event = %s", this.channelState, event));
        ChannelStateBlock[] channelStateBlocks = this.channelStateBlocks;
        int length = channelStateBlocks.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            ChannelStateBlock channelStateBlock = channelStateBlocks[i];
            if (channelStateBlock.channelState == this.channelState && channelStateBlock.channelEvent == event) {
                channelStateBlock.iChannelStateHandler.stateHandler(objArr);
                flag = true;
                break;
            }
            i++;
        }
        if (!flag) {
            Log.e("Channel", "STATE_MACHINE not handled!");
        }
    }

    public void switchLooper(boolean switchMainLooperFlag) {
        if (Looper.myLooper() != (switchMainLooperFlag ? Looper.getMainLooper() : this.channelHandler.getLooper())) {
            throw new RuntimeException();
        }
    }


    public void handlerDataChange(byte[] bytes) {
        char packetType;
        switchLooper(false);
        Packet packet = Packet.buildPacket(bytes);
        String packetName = packet.packetName();
        int hashCode = packetName.hashCode();
        if (hashCode != 96393) {
            if (hashCode != 98849) {
                if (hashCode == 3076010 && packetName.equals("data")) {
                    packetType = 1;
                    switch (packetType) {
                        case 0:
                            handlerChannelStateBlock(ChannelEvent.RECV_ACK, packet);
                            return;
                        case 1:
                            handlerChannelStateBlock(ChannelEvent.RECV_DATA, packet);
                            return;
                        case 2:
                            handlerChannelStateBlock(ChannelEvent.RECV_CTR, packet);
                            return;
                        default:
                            return;
                    }
                }
            } else if (packetName.equals("ctr")) {
                packetType = 2;
                switch (packetType) {
                }
            }
        } else if (packetName.equals("ack")) {
            packetType = 0;
            switch (packetType) {
            }
        }
        packetType = 65535;
        switch (packetType) {
        }
    }

    private int getBegin() {
        return (this.begin - 3) - 2;
    }

    public void writeDataHandler(byte[] bytesData, int packageType, final ChannelCallback channelCallback) {
        switchLooper(false);
        // 非空闲状态直接主线程返回异常
        if (this.channelState != ChannelState.IDLE) {
            this.mainHandler.post(new Runnable() {
                public void run() {
                    channelCallback.channelCallback(-3);
                }
            });
            return;
        }
        this.packageType = packageType;
        this.channelState = ChannelState.READY;
        this.channelCallback = channelCallback;
        this.totalBytes = bytesData.length;
        this.frameCount = getFrameCount(this.totalBytes);
        Log.d("Channel", String.format("totalBytes = %d, frameCount = %d", Integer.valueOf(this.totalBytes), Integer.valueOf(this.frameCount)));
        if (isCrcVerifyData()) {
            this.bytes = Arrays.copyOf(bytesData, bytesData.length + 4);
            System.arraycopy(CRCUtil.getCRCData(bytesData), 0, this.bytes, bytesData.length, 4);
        } else {
            this.bytes = Arrays.copyOf(bytesData, bytesData.length);
        }
        handlerPacket(packageType);
    }

    private int getFrameCount(int totalBytes) {
        if (isCrcVerifyData()) {
            totalBytes += 4;
        }
        return ((totalBytes - 1) / getBegin()) + 1;
    }

    public void startFor6000L() {
        start(6000L);
    }

    private void timeout() {
        start(6000, new Timer.AbstractRunnable("exception") {
            @Override
            public void begin() throws TimeoutException {
                throw new TimeoutException();
            }
            @Override
            public void end() {
                Channel.this.timer.destroy();
            }
        });
    }

    private void start(long delayMillis) {
        start(delayMillis, this.abstractRunnable);
    }


    public void start(long delayMillis, Timer.AbstractRunnable aVar) {
        this.timer.start(aVar, delayMillis);
    }


    public void destroy() {
        this.timer.disable();
    }

    private boolean timerExists() {
        return this.timer.isNotNull();
    }

    public boolean isExceptionRunnable() {
        return "exception".equals(this.timer.getRunnableName());
    }

    private boolean bytesCompare(byte[] bArr, byte[] bArr2) {
        return ByteUtil.isEquals(bArr2, CRCUtil.getCRCData(bArr));
    }
}
