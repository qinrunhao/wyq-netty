package com.wyq.netty.bean;

import com.wyq.netty.config.NettyConfig;
import com.wyq.netty.model.Nio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
public class NettyUtil {

    public static void check(NettyConfig nettyConfig) throws BeansException {
        if (CollectionUtils.isEmpty(nettyConfig.getNioList())) {
            return;
        }
        for (Nio config : nettyConfig.getNioList()) {
            if (config == null) {
                continue;
            }
            validateConfig(config);
        }
    }

    private static void validateConfig(Nio config) {
        if (!StringUtils.hasText(config.getName())) {
            log.error("NettyBean validateConfig error,Nio config [name] value is blank.");
            throw new IllegalArgumentException();
        }
        if (null == config.getPort() || config.getPort() < 1 || config.getPort() > 65535) {
            log.error("NettyBean validateConfig error,Nio config [port] value is not range in (1~65535).");
            throw new IllegalArgumentException();
        }
        if (null == config.getReaderIdleTimeSeconds() || config.getReaderIdleTimeSeconds() < 0) {
            log.error("NettyBean validateConfig error,Nio config [readerIdleTimeSeconds] value is < 0.");
            throw new IllegalArgumentException();
        }
        if (null == config.getWriterIdleTimeSeconds() || config.getWriterIdleTimeSeconds() < 0) {
            log.error("NettyBean validateConfig error,Nio config [writerIdleTimeSeconds] value is < 0.");
            throw new IllegalArgumentException();
        }
        if (null == config.getAllIdleTimeSeconds() || config.getAllIdleTimeSeconds() < 0) {
            log.error("NettyBean validateConfig error,Nio config [allIdleTimeSeconds] value is < 0.");
            throw new IllegalArgumentException();
        }
        if (null == config.getBacklog() || config.getBacklog() < 1) {
            log.error("NettyBean validateConfig error,Nio config [backlog] is < 1.");
            throw new IllegalArgumentException();
        }
    }

    public static void startNettyServer(Nio nio, List<Object> beansWithNettyHandlerAnnotation) {
        new TcpServer(nio, beansWithNettyHandlerAnnotation);
    }
}
