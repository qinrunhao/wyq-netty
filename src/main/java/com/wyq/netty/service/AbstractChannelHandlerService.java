package com.wyq.netty.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractChannelHandlerService extends ChannelInboundHandlerAdapter  {

    public abstract void execute(ChannelHandlerContext ctx, String data);

    /** 单例无状态，可以被多个 channel 共享*/
    @Override
    public final boolean isSharable() {
        return true;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ByteBuf in = (ByteBuf) msg;
            String data = in.toString(CharsetUtil.US_ASCII);
            data = data.trim();
            execute(ctx, data);
        } finally {
            //ByteBuf是一个引用计数对象，这个对象必须显示地调用release()方法来释放。
            //请记住处理器的职责是释放所有传递到处理器的引用计数对象。
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public final void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                log.info("userEventTriggered {} -> [释放不活跃通道] {}", this.getClass().getName(), ctx.channel().id());
                ctx.channel().close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
