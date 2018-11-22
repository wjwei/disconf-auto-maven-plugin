package com.wjwei.operator;

import com.wjwei.dto.DisconfAutoConfig;
import com.wjwei.dto.DisconfFileStream;
import com.wjwei.dto.DisconfInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author 000298
 * @date 2018/4/26
 */
@Slf4j
public class DisconfAutoOperator {

    /**
     * 加载DisconfAuto配置信息
     * @return
     * @throws Exception
     */
    public static DisconfAutoConfig loadDisconfAutoConfig() throws Exception {

        /*String disconfAutoConf = System.getProperty("config.path", "/app/deploy_apps/update-disconf/disconfauto.properties");

        InputStream fileStream = new FileInputStream(disconfAutoConf);

        Properties prop = new Properties();
        prop.load(fileStream);

        String deployListPath = prop.getProperty("deploy.list.path", "/app/deploy_apps/deploy_list.txt");
        String jarDir = prop.getProperty("jar.dir", "/app/deploy_apps/soft");
        String hostUserNamePwd = prop.getProperty("disconf.host.user.name.pwd");*/

        String deployListPath = System.getProperty("deploy.list.path", "/app/deploy_apps/deploy_list.txt");
        String jarDir = System.getProperty("jar.dir", "/app/deploy_apps/soft");
        String hostUserNamePwd = System.getProperty("disconf.host.user.name.pwd");

        DisconfAutoConfig config = new DisconfAutoConfig();
        config.setDeployListPath(deployListPath);
        config.setJarDir(jarDir);
        config.setHostUserNamePwd(hostUserNamePwd);

        return config;
    }

    /**
     * 上传应用的disconf配置
     * @param jarDir
     * @param jarName
     * @throws Exception
     */
    public static void uploadAppDisconf(String jarDir, String jarName, List<String> hostNamePwdList) throws Exception {
        String jarPath = jarDir + "/" + jarName;

        log.info("==============开始上传" + jarName + "应用disconf配置==============");

        //读取jar文件的disconf配置流信息
        DisconfFileStream stream = JarFileOperator.getDisconfFileStream(jarPath);

        //获取Disconf信息
        DisconfInfo disconfInfo = DisconfOperator.getDisconfInfo(stream.getDisconfStream());

        //设置Disconf用户名密码信息
        DisconfOperator.setDisconfUserNamePwd(disconfInfo, hostNamePwdList);

        if(disconfInfo.getEnableAutoUpload() == null || !disconfInfo.getEnableAutoUpload()){
            log.info("==============" + jarName + "应用disconf自动上传未开启，上传终止==============");
            return;
        }

        //检查Disconf配置信息
        DisconfOperator.checkDisconfInfo(disconfInfo);

        //创建Disconf操作对象
        DisconfOperator disconfOperator = new DisconfOperator();

        //登录disconf
        disconfOperator.login(disconfInfo);

        //检查要创建的App是否已经存在
        boolean r = disconfOperator.checkAppExist(disconfInfo);

        //根据检查结果创建App
        disconfOperator.createApp(disconfInfo, r);

        //检查环境信息
        r = disconfOperator.checkEnvExist(disconfInfo);
        if(!r){
            throw new RuntimeException(disconfInfo.getEnv() + " 环境不存在");
        }

        //判断是否清空现有版本的配置
        disconfOperator.clearVersionConfig(disconfInfo);

        //检查是否已经存在配置
        r = disconfOperator.checkExistConfig(disconfInfo);
        if(r){
            log.warn(String.format("%s %s %s版本已经存在部分配置", disconfInfo.getApp(), disconfInfo.getEnv(), disconfInfo.getVersion()));
        }

        //上传watch配置项
        disconfOperator.uploadDisconfWatchConf(disconfInfo, stream.getWatchFileStream());

        //上传普通配置文件
        disconfOperator.uploadDisconfFileConf(disconfInfo, stream.getConfFileStreamList());

        //删除老版本的配置
        disconfOperator.deleteOldVersionConfig(disconfInfo);

        log.info("==============" + jarName + "应用disconf自动配置成功==============");
    }

}
