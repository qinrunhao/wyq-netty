# wyq-netty
Netty封装，可通过配置灵活增减netty端口
# 快速上手
## application.yml配置
例如下面开通了两个个端口
```
netty:
  nio-list:
    - name: ExternalDeviceData
      port: 5000
      boss-count: 1
      worker-count: 8
      reader-idle-time-seconds: 20
      writer-idle-time-seconds: 20
      allIdle-time-seconds: 40
      backlog: 100
      channel-handler-class-name: com.hope.**.ExternalDeviceDataHandler
      decoder-adapter-class-name: com.hope.**.ExternalDeviceDataDecoder
      encoder-adapter-class-name: com.hope.**.ExternalDeviceDataEncoder
    - name: IntraDeviceData
      port: 11111
      boss-count: 1
      worker-count: 8
      reader-idle-time-seconds: 300
      writer-idle-time-seconds: 0
      allIdle-time-seconds: 0
      backlog: 1000000000
      channel-handler-class-name: com.hope.**.IntraDeviceDataHandler
  ```
  ## 继承抽象类AbstractChannelHandlerService
  例1：
  ```java
    import com.hope.common.netty.service.AbstractChannelHandlerService;
    import com.hope.magic.box.service.DeviceDataService;
    import io.netty.channel.ChannelHandlerContext;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;
    
    @Slf4j
    @Component
    public class ExternalDeviceDataHandler extends AbstractChannelHandlerService {
        @Autowired
        private DeviceDataService deviceDataService;
    
        @Override
        public void execute(ChannelHandlerContext ctx, String data) {
            deviceDataService.heatMapAndCountDeviceData(ctx, data);
        }
    }
```
例2：
```java

    import com.hope.common.netty.service.AbstractChannelHandlerService;
    import com.hope.magic.box.service.DeviceDataService;
    import io.netty.channel.ChannelHandlerContext;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;
    
    import java.util.List;
    
    @Slf4j
    @Component
    public class IntraDeviceDataHandler extends AbstractChannelHandlerService {
    
        @Autowired
        private List<ChannelHandlerContext> channelHandlerContextList;
    
        @Autowired
        private DeviceDataService deviceDataService;
    
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            channelHandlerContextList.remove(ctx);
            ctx.close();
        }
    
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            log.info("IntraDeviceChannelHandlerAdapter channelActive 连上了:" + ctx.toString());
            channelHandlerContextList.add(ctx);
            log.info("TCP连接数：" + channelHandlerContextList.size());
        }
    
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            log.info("IntraDeviceChannelHandlerAdapter channelInactive 断开了:" + ctx.toString());
            channelHandlerContextList.remove(ctx);
            ctx.close();
        }
    
        @Override
        public void execute(ChannelHandlerContext ctx, String data) {
            if (log.isDebugEnabled()) {
                log.info("DeviceDataServiceImpl intraDevice 正在处理数据...");
                log.info("DeviceDataServiceImpl intraDevice data:{}", data);
            }
            deviceDataService.intraDevice(ctx, data);
        }
    }
```
将业务逻辑重写在抽象类的execute()方法中。
