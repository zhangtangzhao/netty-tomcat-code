package com.netty.tomcat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.HttpServerCodec;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class CustomServer {

    private String backPackage;

    private HashMap<String, String> nameToClassMap = new HashMap<>();
    private Map<String, CustomServlet> nameToServletMap = new ConcurrentHashMap();

    public CustomServer(String backPackage) {
        this.backPackage = backPackage;
    }

    public void start() throws Exception {
        // 加载类
        loadClass(backPackage);
        // 启动netty服务
        runTomcat();
    }

    private void loadClass(String backPackage) {
        if (backPackage == null || "".equals(backPackage.trim())) {
            return;
        }
        URL resource = this.getClass().getClassLoader().getResource(backPackage.replaceAll("\\.", "/"));
        if (resource == null) {
            return;
        }
        File dir = new File(resource.getFile());
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                loadClass(backPackage + "." + f.getName());
            } else if (f.getName().endsWith(".class")) {
                String simpleClassName = f.getName().replace(".class", "").trim();
                nameToClassMap.put(simpleClassName.toLowerCase(), backPackage + "." + simpleClassName);
            }
        }
    }

    private void runTomcat() throws Exception{
        EventLoopGroup parent = new NioEventLoopGroup();
        EventLoopGroup child = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(parent, child)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new CustomHandle(nameToServletMap, nameToClassMap));
                        }
                    });
            int port = initPort();
            ChannelFuture future = bootstrap.bind(port);
            System.out.println("服务启动成功：监听端口为："+ port);
            future.channel().closeFuture().sync();
        }finally {
            child.shutdownGracefully();
            parent.shutdownGracefully();
        }
    }

    //初始化端口
    private int initPort() throws DocumentException {
        //初始化端口
        //读取配置文件Server.xml中的端口号
        InputStream in = CustomServer.class.getClassLoader().getResourceAsStream("server.xml");
        File file = new File(this.getClass().getClassLoader().getResource("").getPath()+"server.xml");
        String port;
        if (file.exists()) {
            //获取配置文件输入流
            SAXReader saxReader = new SAXReader();
            Document doc = saxReader.read(in);
            //使用SAXReader + XPath读取端口配置
            Element portEle = (Element) doc.selectSingleNode("//port");
            port = portEle.getText();
        } else {
            port = "8080";
        }
        return Integer.valueOf(port);
    }
}
