package test.unfreeze.util;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import java.util.UUID;

/**
 * 半双工通道
 */
public class HalfDuplexChannel extends Channel {

    private final UUID uuidService;

    private final UUID uuidNotify;

    private final IChannelReceiver ichannelReceiver;

    private final BleDevice bleDevice;

    @Override
    public boolean isCrcVerifyData() {
        return true;
    }

    public HalfDuplexChannel(UUID uuidService, UUID uuidNotify, IChannelReceiver iChannelReceiver, BleDevice bleDevice) {
        this.uuidService = uuidService;
        this.uuidNotify = uuidNotify;
        this.ichannelReceiver = iChannelReceiver;
        this.bleDevice = bleDevice;
    }

    @Override
    public void channelReceiver(byte[] bytes, ChannelCallback channelCallback, boolean flag) {
        BleManager.getInstance().write(this.bleDevice, this.uuidService.toString(), this.uuidNotify.toString(), bytes, new BleWriteCallback() {

            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {
            }

            @Override
            public void onWriteFailure(BleException aVar) {
            }
        });
    }

    @Override
    public void receiver(byte[] bytes, int packetType) {
        if (this.ichannelReceiver != null) {
            this.ichannelReceiver.receiver(bytes, packetType);
        }
    }
}
