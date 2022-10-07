package com.netty.tomcat.app;

import com.netty.tomcat.CustomServer;

public class AppBoostrap {
    public static void main(String[] args) throws Exception {
        CustomServer server = new CustomServer("com.netty.tomcat.app");
        server.start();
    }
}
