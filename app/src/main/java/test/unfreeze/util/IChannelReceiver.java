package test.unfreeze.util;


public interface IChannelReceiver {
    void receiver(byte[] bytes, int packetType);
}
