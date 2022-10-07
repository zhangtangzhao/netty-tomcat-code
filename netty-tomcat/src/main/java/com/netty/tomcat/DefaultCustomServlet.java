package com.netty.tomcat;

public class DefaultCustomServlet extends CustomServlet {

    @Override
    public void doGet(CustomRequest request, CustomResponse response) throws Exception {
        // http://localhost:8080/aaa/bbb/userservlet?name=xiong
        // path：/aaa/bbb/userservlet?name=xiong
        String uri = request.getUri();
        response.write("404 - no this servlet : " + (uri.contains("?")?uri.substring(0,uri.lastIndexOf("?")):uri));
    }


    @Override
    public void doPost(CustomRequest request, CustomResponse response) throws Exception {
        doGet(request, response);
    }
}
