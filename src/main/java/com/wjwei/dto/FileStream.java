package com.fcbox.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

/**
 * @author 000298
 * @date 2018/4/27
 */
@Getter
@Setter
public class FileStream {

    private String fileName;
    private InputStream inputStream;

}
