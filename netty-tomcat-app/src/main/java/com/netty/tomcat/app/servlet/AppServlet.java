package com.netty.tomcat.app.servlet;

import com.netty.tomcat.CustomRequest;
import com.netty.tomcat.CustomResponse;
import com.netty.tomcat.CustomServlet;

public class AppServlet extends CustomServlet {

    @Override
    public void doGet(CustomRequest request, CustomResponse response) throws Exception {
        String uri = request.getUri();
        String path = request.getPath();
        String method = request.getMethod();
        String name = request.getParameter("name");

        String content = "uri = " + uri + "\n" +
                "path = " + path + "\n" +
                "method = " + method + "\n" +
                "param = " + name;
        response.write(content);
    }

    @Override
    public void doPost(CustomRequest request, CustomResponse response) throws Exception {
        doGet(request, response);
    }
}
