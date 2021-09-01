package test.unfreeze.util;


public enum ChannelState {
    /*
     空闲状态
     */
    IDLE,
    /*
    准备状态
     */
    READY,
    /*
    等待开始接收响应状态
     */
    WAIT_START_ACK,
    /*
    等待状态
     */
    WRITING,
    /*
    同步状态
     */
    SYNC,
    /*
    同步响应状态
     */
    SYNC_ACK,
    /*
    同步等待包
     */
    SYNC_WAIT_PACKET,
    /*
    正在准备状态
     */
    READING
}
