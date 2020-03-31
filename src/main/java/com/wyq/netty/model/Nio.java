package com.wyq.netty.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

@Data
@ToString(callSuper = true)
@ConfigurationProperties(prefix = "netty.nio-list")
public class Nio implements Serializable {

    private static final long serialVersionUID = 8270682356133147367L;
    /**
     * 名称/标记
     */
    private String name;
    /**
     * 端口
     */
    private Integer port;
    /**
     * accept io 线程数
     */
    private Integer bossCount = 0;
    /**
     * read/write io 线程数
     */
    private Integer workerCount = 0;
    /**
     * 读操作空闲单位秒
     */
    private Integer readerIdleTimeSeconds = 0;
    /***
     * 写操作空闲单位秒
     */
    private Integer writerIdleTimeSeconds = 0;
    /***
     * 读写全部空闲单位秒
     */
    private Integer allIdleTimeSeconds = 0;

    /**
     * 处理业务逻辑的java全路径
     */
    private String channelHandlerClassName;

    /**
     * Channeloption.SO_KEEPALIVE参数对应于套接字选项中的SO_KEEPALIVE，
     * 该参数用于设置TCP连接，当设置该选项以后，连接会测试链接的状态，
     * 这个选项用于可能长时间没有数据交流的连接。当设置该选项以后，
     * 如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文。
     */
    private Boolean keepAlive = Boolean.TRUE;
    /**
     * ChannelOption.SO_BACKLOG对应的是tcp/ip协议listen函数中的backlog参数，
     * 函数listen(int socketfd,int backlog)用来初始化服务端可连接队列，
     * 服务端处理客户端连接请求是顺序处理的，
     * 所以同一时间只能处理一个客户端连接，
     * 多个客户端来的时候，服务端将不能处理的客户端连接请求放在队列中等待处理，
     * backlog参数指定了队列的大小
     */
    private Integer backlog = 10000;
    /**
     * 禁用 Nagle 算法，不使用写缓存，直接发送数据
     */
    private Boolean tcpNoDelay = Boolean.TRUE;

    /**
     * 消息解码器类名
     */
    private String decoderAdapterClassName;
    /**
     * 消息编码器类名
     */
    private String encoderAdapterClassName;

    /**
     * 是否使用 Linux native transport,默认使用
     */
    private Boolean useEpoll = Boolean.TRUE;
}