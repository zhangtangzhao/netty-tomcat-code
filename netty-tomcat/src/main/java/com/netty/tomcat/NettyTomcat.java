package com.netty.tomcat;

public class NettyTomcat {
    public static void main(String[] args) throws Exception {
        CustomServer server = new CustomServer("com.netty.tomcat");
        server.start();
    }
}
