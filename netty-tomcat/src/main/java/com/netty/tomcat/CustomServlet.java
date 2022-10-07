package com.netty.tomcat;

public abstract class CustomServlet {

    public abstract void doGet(CustomRequest request, CustomResponse response) throws Exception;
    public abstract void doPost(CustomRequest request, CustomResponse response) throws Exception;

}
