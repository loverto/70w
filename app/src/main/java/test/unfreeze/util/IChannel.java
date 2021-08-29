package test.unfreeze.util;


public interface IChannel extends IChannelReceiver {
    void channelReceiver(byte[] bytes, ChannelCallback channelCallback, boolean flag);
}
