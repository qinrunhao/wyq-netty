package com.wyq.netty.bean;

import com.wyq.netty.annotation.NettyHandler;
import com.wyq.netty.model.Nio;
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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;

import java.util.Comparator;
import java.util.List;
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

    private ChannelFuture channelFuture;

    private List<Object> beansWithNettyHandlerAnnotation;


    TcpServer(@NonNull Nio config, @NonNull List<Object> beansWithNettyHandlerAnnotation) {
        Objects.requireNonNull(config, "TcpServer instantiate fail with null nio config.");
        this.config = config;
        this.beansWithNettyHandlerAnnotation = beansWithNettyHandlerAnnotation;

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

        bootstrap.group(bossEventLoopGroup, workerEventLoopGroup)
                .channel(channelType)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(channelInitializer);
        setChannelOptions();
        start();
        // register shutdown hook to jvm
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, config.getName() + "-shutdownHook"));
    }

    private void start() {
        try {
            log.info("TcpServer[{}] port[:{}] starting ...", config.getName(), config.getPort());
            // wait until the server socket is bind succeed.
            channelFuture = bootstrap.bind(config.getPort()).sync();
            log.info("TcpServer[{}] port[:{}] start success ...", config.getName(), config.getPort());
        } catch (Exception e) {
            log.error("TcpServer[{}] port[:{}] start failed ...", config.getName(), config.getPort(), e);
            throw new RuntimeException(e);
        }
    }

    private void shutdown() {
        log.info("TcpServer[{}] port[{}] shutdown ...", config.getName(), config.getPort());
        //回收线程资源
        if (channelFuture != null) {
            try {
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
            protected void initChannel(SocketChannel ch) {
                final ChannelPipeline channelPipeline = ch.pipeline();
                channelPipeline.addLast("idleStateHandler", new IdleStateHandler(config.getReaderIdleTimeSeconds(), config.getWriterIdleTimeSeconds(),
                        config.getAllIdleTimeSeconds()));
                beansWithNettyHandlerAnnotation.stream()
                        .filter(handlerBean->{
                                    NettyHandler nettyHandler = AnnotationUtils.findAnnotation(handlerBean.getClass(), NettyHandler.class);
                                    return nettyHandler != null && handlerBean instanceof ChannelHandlerAdapter && nettyHandler.name().equals(config.getName());
                                }
                        ).map(handlerBean -> (ChannelHandlerAdapter) handlerBean)
                        .sorted(new Comparator<ChannelHandlerAdapter>() {
                            @Override
                            public int compare(ChannelHandlerAdapter o1, ChannelHandlerAdapter o2) {
                                return AnnotationUtils.findAnnotation(o1.getClass(), NettyHandler.class).order() - AnnotationUtils.findAnnotation(o1.getClass(), NettyHandler.class).order();
                            }
                        })
                        .peek(handler -> {
                            channelPipeline.addLast(handler.getClass().getSimpleName(), handler);
                            beansWithNettyHandlerAnnotation.remove(handler);
                        })
                        .count();
            }
        };
    }

    private void setChannelOptions() {
        bootstrap.option(ChannelOption.SO_BACKLOG, config.getBacklog());
        //使用池化ByteBuf内存分配器
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, config.getKeepAlive());
        bootstrap.childOption(ChannelOption.TCP_NODELAY, config.getTcpNoDelay());
        //使用池化ByteBuf内存分配器
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        if (useNative()) {
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
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
