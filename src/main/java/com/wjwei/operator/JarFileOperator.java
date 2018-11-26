package com.wjwei.operator;

import com.wjwei.dto.DisconfFileStream;
import com.wjwei.dto.DisconfInfo;
import com.wjwei.dto.FileStream;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author 000298
 * @date 2018/4/26
 */
public class JarFileOperator {

    /**
     * 读取jar文件的disconf配置流信息
     * @param jarUrl
     * @return
     * @throws Exception
     */
    public static DisconfFileStream getDisconfFileStream(String jarUrl) throws Exception {

        DisconfFileStream streamDto = new DisconfFileStream();

        JarEntry disconfEntry = null;
        JarEntry disconfWatchFileEntry = null;
        List<JarEntry> disconfFiles = new ArrayList<>();

        JarFile jar = new JarFile(jarUrl);
        Enumeration<JarEntry> entries = jar.entries();
        while(entries.hasMoreElements()){
            JarEntry element = entries.nextElement();
            String elementName = element.getName();
            if(elementName.endsWith("application.properties")){
                disconfEntry = element;
            }
            if(elementName.contains("/disconf/") && elementName.endsWith("watchConfFile.properties")){
                disconfWatchFileEntry = element;
            }
        }

        if(disconfEntry == null){
            throw new RuntimeException("未找到application.properties文件！");
        }
        InputStream disconfStream = jar.getInputStream(disconfEntry);

        DisconfInfo disconfInfo = DisconfOperator.getDisconfInfo(disconfStream);

        String env = disconfInfo.getEnv();

        entries = jar.entries();
        while(entries.hasMoreElements()){
            JarEntry element = entries.nextElement();
            String elementName = element.getName();
            if(elementName.contains("/disconf/" + env + "/") && elementName.endsWith(".properties")){
                disconfFiles.add(element);
            }
        }

        disconfStream = jar.getInputStream(disconfEntry);

        InputStream watchFileStream = null;
        if(disconfWatchFileEntry != null){
            watchFileStream = jar.getInputStream(disconfWatchFileEntry);
        }

        List<FileStream> confFileStreamList = new ArrayList<>();
        if(disconfFiles.size() > 0){
            for(JarEntry disconfFile : disconfFiles){

                String fileName = disconfFile.getName();

                FileStream fileStream = new FileStream();
                fileStream.setFileName(fileName.substring(fileName.lastIndexOf("/") + 1));
                fileStream.setInputStream(jar.getInputStream(disconfFile));

                confFileStreamList.add(fileStream);
            }
        }

        streamDto.setDisconfStream(disconfStream);
        streamDto.setWatchFileStream(watchFileStream);
        streamDto.setConfFileStreamList(confFileStreamList);

        return streamDto;
    }
}
