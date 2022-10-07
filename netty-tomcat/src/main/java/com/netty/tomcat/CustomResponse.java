package com.netty.tomcat;

import java.io.File;

public interface CustomResponse {
    // 将响应写入到Channel
    void write(String content) throws Exception;
}
