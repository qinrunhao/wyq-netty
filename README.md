# wyq-netty
    Netty封装，封装了netty服务的创建逻辑，同时可通过配置灵活增减netty服务
    目前支持TCP连接
# 快速上手
## 扫描包
项目启动时，记得要让Spring扫描到该包，比如在启动类上加上注解@ComponentScan({"com.wyq.netty"})
## application.yml配置
例如下面开通了两个端口5000和11111（配置的具体含义，请查看类Nio）
```yaml
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
    - name: IntraDeviceData
      port: 11111
      boss-count: 1
      worker-count: 8
      reader-idle-time-seconds: 300
      writer-idle-time-seconds: 0
      allIdle-time-seconds: 0
      backlog: 1000000000
  ```
## 编写ChannelHandler
### 需要在类上加上注解
  
    @Component
    @NettyHandler(name = "ExternalDeviceData", order = 3)
### 继承ChannelHandler
  
    可以继承ChannelInboundHandlerAdapte、ChannelOutboundHandlerAdapter等。
    如果是要写处理的业务类，可以继承本包中的AbstractChannelHandlerService，该类已经实现了部分功能，
    开发者只需要实现抽象方法execute()即可。
例如：只实现execute方法
```java
    import xxx.service.AbstractChannelHandlerService;
    import xxx.service.DeviceDataService;
    import io.netty.channel.ChannelHandlerContext;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;
    import com.wyq.netty.annotation.NettyHandler;
    
    @Slf4j
    @Component
    @NettyHandler(name = "ExternalDeviceData", order = 3)
    public class ExternalDeviceDataHandler extends AbstractChannelHandlerService {
        @Autowired
        private DeviceDataService deviceDataService;
    
        @Override
        public void execute(ChannelHandlerContext ctx, String data) {
            deviceDataService.heatMapAndCountDeviceData(ctx, data);
        }
    }
```
例如：重写ChannelHandler的方法
```java

    import xxx.service.AbstractChannelHandlerService;
    import xxx.service.DeviceDataService;
    import io.netty.channel.ChannelHandlerContext;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;
    import com.wyq.netty.annotation.NettyHandler;
    
    import java.util.List;
    
    @Slf4j
    @Component
    @NettyHandler(name = "IntraDeviceData", order = 1)
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
### 编写netty解码器
```java
    import com.hope.common.constant.annotation.NotProguardClassName;
    import com.wyq.netty.annotation.NettyHandler;
    import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
    import org.springframework.stereotype.Component;
    
    import java.nio.ByteOrder;
    
    @Component
    @NettyHandler(name = "ExternalDeviceData", order = 1)
    public class ExternalDeviceDataDecoder extends LengthFieldBasedFrameDecoder {
    
        public ExternalDeviceDataDecoder() {
            this(ByteOrder.LITTLE_ENDIAN, Integer.MAX_VALUE, 0, 4, 0, 4, true);
        }
    
        public ExternalDeviceDataDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
            super(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
        }
    }
```
### 编写netty编码器
```java
    import com.wyq.netty.annotation.NettyHandler;
    import io.netty.channel.ChannelHandler;
    import io.netty.handler.codec.LengthFieldPrepender;
    import org.springframework.stereotype.Component;
    
    import java.nio.ByteOrder;
    
    @Component
    @ChannelHandler.Sharable
    @NettyHandler(name = "ExternalDeviceData", order = 2)
    public class ExternalDeviceDataEncoder extends LengthFieldPrepender {
        public ExternalDeviceDataEncoder() {
            this(ByteOrder.LITTLE_ENDIAN, 4, 0, false);
        }
    
        public ExternalDeviceDataEncoder(ByteOrder byteOrder, int lengthFieldLength, int lengthAdjustment, boolean lengthIncludesLengthFieldLength) {
            super(byteOrder, lengthFieldLength, lengthAdjustment, lengthIncludesLengthFieldLength);
        }
    }
```

## 注意
    注解@NettyHandler中的属性name，代表了一个netty服务。如果编写的多个ChannelHandler是属于一个服务的，那么name就保持一致。
    例如以上代码中的类ExternalDeviceDataHandler、ExternalDeviceDataDecoder和ExternalDeviceDataEncoder，name一致，order不一致。
    同时@NettyHandler中的name值必须与yml配置中的name值一致
    
    注解@NettyHandler中的属性order，当name一样的ChannelHandler有多个时，通过order的值对ChannelHandler进行排序，该顺序代表了被添加到channelPipeline中的顺序。
    order值越小，排的越靠前。
    例如其中ExternalDeviceDataHandler、ExternalDeviceDataDecoder都属于ChannelInboundHandlerAdapter，排序就至关重要了。因为肯定要先解码，所以ExternalDeviceDataDecoder
    的order值就比较小
