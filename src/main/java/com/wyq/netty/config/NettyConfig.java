package com.wyq.netty.config;

import com.wyq.netty.model.Nio;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "netty")
@Data
@Component
public class NettyConfig {
    private List<Nio> nioList;
}
