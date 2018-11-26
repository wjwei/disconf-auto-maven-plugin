package com.wjwei.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.util.List;

/**
 * @author 000298
 * @date 2018/4/26
 */
@Getter
@Setter
public class DisconfFileStream {

    /**
     * application.properties文件流
     */
    private InputStream disconfStream;

    /**
     * watchConfFile.properties文件流
     */
    private InputStream watchFileStream;

    /**
     * 普通disconf配置文件流
     */
    private List<FileStream> confFileStreamList;
}
