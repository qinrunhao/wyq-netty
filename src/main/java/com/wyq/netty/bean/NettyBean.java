package com.wyq.netty.bean;

import com.wyq.netty.config.NettyConfig;
import com.wyq.netty.model.Nio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Objects;

@Slf4j
@Component
public class NettyBean {

    @Autowired
    private NettyConfig nettyConfig;

    @Autowired
    private ConfigurableBeanFactory beanFactory;

    @PostConstruct
    public void init() throws BeansException {
        if (CollectionUtils.isEmpty(nettyConfig.getNioList())) {
            return;
        }
        DefaultListableBeanFactory bf = (DefaultListableBeanFactory) beanFactory;
        for (Nio config : nettyConfig.getNioList()) {
            if (config == null) {
                continue;
            }
            validateConfig(config);
            registerTcpServerBean(config, bf);
        }
    }

    private void validateConfig(Nio config) {
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
        if (!StringUtils.hasText(config.getChannelHandlerClassName())) {
            log.error("NettyBean validateConfig error,Nio config [serviceCalssName] is blank.");
            throw new IllegalArgumentException();
        }
        if (null == config.getBacklog() || config.getBacklog() < 1) {
            log.error("NettyBean validateConfig error,Nio config [backlog] is < 1.");
            throw new IllegalArgumentException();
        }
    }

    private void registerTcpServerBean(Nio config, DefaultListableBeanFactory beanFactory) throws BeansException {
        BeanDefinitionBuilder bdBuilder = BeanDefinitionBuilder
                .genericBeanDefinition(TcpServer.class)
                .addConstructorArgValue(config)
                .addConstructorArgReference(Objects.requireNonNull(getBeanName(config.getChannelHandlerClassName())))
                .setInitMethodName("start")
                .setDestroyMethodName("shutdown")
                .setScope(BeanDefinition.SCOPE_SINGLETON);
        String encoderBeanName = getBeanName(config.getEncoderAdapterClassName());
        String decodeBeanName = getBeanName(config.getDecoderAdapterClassName());
        if (null != encoderBeanName) {
            bdBuilder.addConstructorArgReference(encoderBeanName);
        } else {
            bdBuilder.addConstructorArgValue(null);
        }
        if (null != decodeBeanName) {
            bdBuilder.addConstructorArgReference(decodeBeanName);
        } else {
            bdBuilder.addConstructorArgValue(null);
        }
        beanFactory.registerBeanDefinition(config.getName() + "TcpServer", bdBuilder.getRawBeanDefinition());
    }
    private String getBeanName(String className){
        if (StringUtils.hasText(className)) {
            String beanName = className;
            beanName = beanName.substring(beanName.lastIndexOf(".") + 1);
            beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
            return beanName;
        }
        return null;
    }
}
