package com.wyq.netty.bean;

import com.wyq.netty.model.Nio;
import com.wyq.netty.service.AbstractChannelHandlerService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

@Slf4j
@Getter
@ToString(callSuper = true, of = "config")
public class TcpServer {

    private final Nio config;

    private ServerBootstrap bootstrap;

    private ThreadFactory bossThreadFactory;

    private ThreadFactory workerThreadFactory;

    private EventLoopGroup bossEventLoopGroup;

    private EventLoopGroup workerEventLoopGroup;

    private Class<? extends ServerChannel> channelType;

    private ChannelInitializer channelInitializer;
    /**
     * boss channel options
     */
    private Map<ChannelOption<?>, Object> channelOptions = new HashMap<>();
    /**
     * worker channel options
     */
    private Map<ChannelOption<?>, Object> childChannelOptions = new HashMap<>();

    private ChannelHandlerAdapter channelHandler;

    private final ChannelOutboundHandlerAdapter encoder;

    private final ChannelInboundHandlerAdapter decoder;


    public TcpServer(Nio config, AbstractChannelHandlerService channelHandler, @Nullable ChannelOutboundHandlerAdapter encoder, @Nullable ChannelInboundHandlerAdapter decoder) {
        Objects.requireNonNull(config, "TcpServer instantiate fail with null nio config.");
        Objects.requireNonNull(channelHandler, String.format("TcpServer[%s] port[:%d] instantiate fail with null deviceDataService.", config.getName(), config.getPort()));
        this.config = config;
        this.channelHandler = channelHandler;
        this.encoder = encoder;
        this.decoder = decoder;
        try {
            log.info("TcpServer[{}] port[:{}] initializing ...", config.getName(), config.getPort());
            init();
            log.info("TcpServer[{}] port[:{}] initialize success ...", config.getName(), config.getPort());
        } catch (Exception e) {
            log.error("TcpServer[{}] port[:{}] initialize failed ...", config.getName(), config.getPort(), e);
            throw e;
        }
    }

    private void init() {
        bootstrap = new ServerBootstrap();
        initEventLoopGroup();
        initChannelType();
        initChannelInitializer();
        initChannelOptions();

        bootstrap.group(bossEventLoopGroup, workerEventLoopGroup)
                .channel(channelType)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(channelInitializer);

        for (ChannelOption opt : channelOptions.keySet()) {
            bootstrap.option(opt, channelOptions.get(opt));
        }

        for (ChannelOption childOpt : childChannelOptions.keySet()) {
            bootstrap.childOption(childOpt, childChannelOptions.get(childOpt));
        }

        // register shutdown hook to jvm
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, config.getName() + "-shutdownHook"));
    }

    public void start() {
        try {
            log.info("TcpServer[{}] port[:{}] starting ...", config.getName(), config.getPort());
            // wait until the server socket is bind succeed.
            bootstrap.bind(config.getPort()).sync();
            log.info("TcpServer[{}] port[:{}] start success ...", config.getName(), config.getPort());
        } catch (Exception e) {
            log.error("TcpServer[{}] port[:{}] start failed ...", config.getName(), config.getPort(), e);
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        log.info("TcpServer[{}] port[{}] shutdown ...", config.getName(), config.getPort());
        //回收线程资源
        if (bossEventLoopGroup != null) {
            bossEventLoopGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (workerEventLoopGroup != null) {
            workerEventLoopGroup.shutdownGracefully().syncUninterruptibly();
        }
    }


    private void initEventLoopGroup() {
        bossThreadFactory = new DefaultThreadFactory(config.getName() + "-boss", Thread.MAX_PRIORITY);
        workerThreadFactory = new DefaultThreadFactory(config.getName() + "-worker", Thread.MAX_PRIORITY);
        bossEventLoopGroup = buildBossEventLoopGroup();
        workerEventLoopGroup = buildWorkerEventLoopGroup();
    }

    private void initChannelType() {
        channelType = useNative() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    private void initChannelInitializer() {
        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                final ChannelPipeline channelPipeline = ch.pipeline();
                channelPipeline.addLast("idleStateHandler", new IdleStateHandler(config.getReaderIdleTimeSeconds(), config.getWriterIdleTimeSeconds(),
                        config.getAllIdleTimeSeconds()));
                if (decoder != null) {
                    channelPipeline.addLast("frameDecoder", decoder.getClass().newInstance());
                }
                if (encoder != null) {
                    channelPipeline.addLast("frameEncoder", encoder);
                }
                channelPipeline.addLast(channelHandler);
            }
        };
    }

    private void initChannelOptions() {
        channelOptions.put(ChannelOption.SO_BACKLOG, config.getBacklog());
        childChannelOptions.put(ChannelOption.SO_KEEPALIVE, config.getKeepAlive());
        childChannelOptions.put(ChannelOption.TCP_NODELAY, config.getTcpNoDelay());
        //使用池化ByteBuf内存分配器
        channelOptions.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        childChannelOptions.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        if (useNative()) {
            channelOptions.put(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            childChannelOptions.put(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
        }
    }

    private EventLoopGroup buildBossEventLoopGroup() {
        return useNative() ? new EpollEventLoopGroup(config.getBossCount(), bossThreadFactory)
                : new NioEventLoopGroup(config.getBossCount(), bossThreadFactory);
    }

    private EventLoopGroup buildWorkerEventLoopGroup() {
        return useNative() ? new EpollEventLoopGroup(config.getWorkerCount(), workerThreadFactory)
                : new NioEventLoopGroup(config.getWorkerCount(), workerThreadFactory);
    }

    private boolean useNative() {
        return config.getUseEpoll() && Epoll.isAvailable();
    }
}
