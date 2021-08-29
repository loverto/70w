package test.unfreeze.util;

public class ChannelStateBlock {

    public ChannelState channelState;

    public ChannelEvent channelEvent;

    public IChannelStateHandler iChannelStateHandler;

    public ChannelStateBlock(ChannelState channelState, ChannelEvent channelEvent, IChannelStateHandler iChannelStateHandler) {
        this.channelState = channelState;
        this.channelEvent = channelEvent;
        this.iChannelStateHandler = iChannelStateHandler;
    }
}
