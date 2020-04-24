package com.wyq.netty.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Wangyuqing
 * 所有Netty服务的业务处理逻辑，包括编码解码等channelHandler都使用该注解注释
 * 同时，
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NettyHandler {
    /**
     * netty服务的名称
     * @return
     */
    String name();

    /**
     * 被注解的channelHandler在加入到channelPipeline中的顺序
     * 值越小，越靠前
     * @return
     */
    int order();
}
