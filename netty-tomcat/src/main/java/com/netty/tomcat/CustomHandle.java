package com.netty.tomcat;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;

public class CustomHandle extends ChannelInboundHandlerAdapter {

    private Map<String, CustomServlet> nameToServletMap;//线程安全  servlet--> 对象
    private Map<String, String> nameToClassNameMap;//线程不安全  servlet--> 全限定名称

    public CustomHandle(Map<String, CustomServlet> nameToServletMap, Map<String, String> nameToClassNameMap) {
        this.nameToServletMap = nameToServletMap;
        this.nameToClassNameMap = nameToClassNameMap;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            String uri = request.uri();

            // 从请求中解析出要访问的Servlet名称
            //aaa/bbb/twoservlet?name=aa
            String servletName = "";
            if (uri.contains("?") && uri.contains("/")){
                servletName= uri.substring(uri.lastIndexOf("/") + 1, uri.indexOf("?"));
            } else {
                servletName = uri.substring(uri.indexOf("/") + 1);
            }
            CustomServlet servlet = new DefaultCustomServlet();
            boolean staticFlag = false;
            if (servletName.contains(".")) {
                if (servletName.contains(".do") || servletName.contains(".action")) {
                    servletName = servletName.substring(0, servletName.lastIndexOf("."));
                } else {
                    staticFlag = true;
                }
            }

            if (staticFlag) {
                String filePath = this.getClass().getResource("/").getPath() + servletName;
                // 访问静态资源
                File file = new File(filePath);
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
                String contentType = mimetypesFileTypeMap.getContentType(file);
                HttpHeaders headers = new DefaultHttpHeaders();
                headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, headers);
                ctx.write(response);
                ctx.write(new DefaultFileRegion(raf.getChannel(), 0, raf.length()));
                ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                future.addListener(ChannelFutureListener.CLOSE);
                return;
            }

            //第一次访问，Servlet是不会被加载的
            //初始化加载的只是类全限定名称，懒加载
            //如果访问Servlet才会去初始化它对象
            if (nameToServletMap.containsKey(servletName)) {
                servlet = nameToServletMap.get(servletName);
            } else if (nameToClassNameMap.containsKey(servletName.toLowerCase())) {
                // double-check，双重检测锁：为什么要在锁前判断一次，还要在锁后继续判断一次？
                if (nameToServletMap.get(servletName) == null) {
                    synchronized (this) {
                        if (nameToServletMap.get(servletName) == null) {
                            // 获取当前Servlet的全限定性类名
                            String className = nameToClassNameMap.get(servletName);
                            // 使用反射机制创建Servlet实例
                            servlet = (CustomServlet) Class.forName(className).newInstance();
                            // 将Servlet实例写入到nameToServletMap
                            nameToServletMap.put(servletName, servlet);
                        }
                    }
                }
            } //  end-else if

            // 代码走到这里，servlet肯定不空
            CustomRequest req = new HttpCustomRequest(request);
            CustomResponse res = new HttpCustomResponse(request, ctx);
            // 根据不同的请求类型，调用servlet实例的不同方法
            if (request.method().name().equalsIgnoreCase("GET")) {
                servlet.doGet(req, res);
            } else if(request.method().name().equalsIgnoreCase("POST")) {
                servlet.doPost(req, res);
            }
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
