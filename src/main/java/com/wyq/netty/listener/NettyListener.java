package com.wyq.netty.listener;

import com.wyq.netty.bean.TcpServer;
import com.wyq.netty.model.Nio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;

@Slf4j
@Component
public class NettyListener implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private List<TcpServer> tcpServers;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if (event.getApplicationContext() instanceof AnnotationConfigApplicationContext) {
            try {
                InetAddress address = InetAddress.getLocalHost();
                tcpServers.forEach(server -> {
                    log.info("-------------------------------------");
                    Nio config = server.getConfig();
                    log.info("[服务SERVER][{}]-IP:{}:{} 已启动监听]", config.getName(), address.getHostAddress()
                            , config.getPort());
                    log.info("serviceClass[{}]", server.getChannelHandler().getClass().getCanonicalName());
                    log.info("decoderClass[{}]", server.getDecoder() != null ? server.getDecoder().getClass().getCanonicalName() : null);
                    log.info("encoderClass[{}]", server.getEncoder() != null ? server.getEncoder().getClass().getCanonicalName() : null);
                    log.info("nio config[{}]", server.getConfig());
                });
            } catch (Exception e) {
                log.error("ApplicationReadyListener start error:", e);
            }
        }
    }
}
