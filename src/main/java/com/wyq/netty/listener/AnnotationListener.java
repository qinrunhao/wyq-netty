package com.wyq.netty.listener;

import com.wyq.netty.annotation.NettyHandler;
import com.wyq.netty.bean.NettyUtil;
import com.wyq.netty.config.NettyConfig;
import com.wyq.netty.model.Nio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AnnotationListener implements ApplicationListener<ApplicationStartedEvent> {
    @Autowired
    private NettyConfig nettyConfig;
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if (event.getApplicationContext() instanceof ConfigurableWebServerApplicationContext) {
            NettyUtil.check(nettyConfig);
            Map<String, Object> beans = event.getApplicationContext().getBeansWithAnnotation(NettyHandler.class);
            //必须使用此法，否则无法通过bean获取到注解信息。此法可兼容非代理类和代理类
            ConfigurableListableBeanFactory clbf = event.getApplicationContext().getBeanFactory();
            List<Object> beansWithNettyHandlerAnnotation = new ArrayList<>();
            for (String beanName : beans.keySet()) {
                beansWithNettyHandlerAnnotation.add(clbf.getSingleton(beanName));
            }

            if (!CollectionUtils.isEmpty(beansWithNettyHandlerAnnotation)) {
                try {
                    InetAddress address = InetAddress.getLocalHost();
                    for (Nio nio : nettyConfig.getNioList()) {
                        NettyUtil.startNettyServer(nio, beansWithNettyHandlerAnnotation);
                        log.info("[服务SERVER][{}]-IP:{}:{} 已启动监听]", nio.getName(), address.getHostAddress(), nio.getPort());
                        log.info("nio config[{}]", nio);
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}