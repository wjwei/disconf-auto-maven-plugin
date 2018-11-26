package com.wjwei.operator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wjwei.dto.DisconfInfo;
import com.wjwei.dto.FileStream;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.util.*;

/**
 * @author 000298
 * @date 2018/4/26
 */
@Slf4j
public class DisconfOperator {

    private OkHttpClient client;

    private HashMap<String, List<Cookie>> cookieStore;

    public DisconfOperator(){

        cookieStore = new HashMap<>();

        client = new OkHttpClient.Builder().cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
                cookieStore.put(httpUrl.host(), list);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                List<Cookie> cookies = cookieStore.get(httpUrl.host());
                return cookies == null ? new ArrayList<>() : cookies;
            }
        }).build();
    }

    /**
     * 获取disconf信息
     * @param disconfStream
     * @return
     * @throws Exception
     */
    public static DisconfInfo getDisconfInfo(InputStream disconfStream) throws Exception {

        Properties disconf = new Properties();
        disconf.load(disconfStream);

        DisconfInfo disconfInfo = new DisconfInfo();
        disconfInfo.setConfServerHost(disconf.getProperty("disconf.conf_server_host"));
        disconfInfo.setDisconfHostUrl("http://" + disconf.getProperty("disconf.conf_server_host"));
        disconfInfo.setVersion(disconf.getProperty("disconf.version"));
        disconfInfo.setApp(disconf.getProperty("disconf.app"));
        disconfInfo.setEnv(disconf.getProperty("disconf.env"));

        String enableAutoUpload = disconf.getProperty("disconf.enable.auto.upload");
        disconfInfo.setEnableAutoUpload(StringUtils.isEmpty(enableAutoUpload) ? null : Boolean.valueOf(enableAutoUpload));

        String enableAutoOverride = disconf.getProperty("disconf.enable.auto.override");
        disconfInfo.setEnableAutoOverride(StringUtils.isEmpty(enableAutoOverride) ? null : Boolean.valueOf(enableAutoOverride));

        return disconfInfo;
    }

    /**
     * 设置Disconf用户名密码信息
     * @param disconfInfo
     * @param hostNamePwdList
     * @return
     */
    public static DisconfInfo setDisconfUserNamePwd(DisconfInfo disconfInfo, List<String> hostNamePwdList){
        if(hostNamePwdList != null && hostNamePwdList.size() > 0){
            Map<String, String> map = new HashMap<>();
            for(String str : hostNamePwdList){
                if(!StringUtils.isEmpty(str)){
                    String[] hostNamePwd = str.split("@");
                    map.put(hostNamePwd[0], hostNamePwd[1]);
                }
            }
            String namePwd = map.get(disconfInfo.getConfServerHost());
            if(!StringUtils.isEmpty(namePwd)){
                String[] array = namePwd.split(":");
                String userName = array[0];
                String pwd = array[1];
                if(!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(pwd)){
                    disconfInfo.setUserName(userName);
                    disconfInfo.setUserPwd(pwd);
                }
            }
        }

        if(StringUtils.isEmpty(disconfInfo.getUserName())
                || StringUtils.isEmpty(disconfInfo.getUserPwd())){
            disconfInfo.setUserName("admin");
            disconfInfo.setUserPwd("admin");
        }

        return disconfInfo;
    }

    /**
     * 检查disconf信息
     * @param disconfInfo
     * @return
     */
    public static void checkDisconfInfo(DisconfInfo disconfInfo){
        if(StringUtils.isEmpty(disconfInfo.getConfServerHost())){
            throw new RuntimeException("disconf host 为空！");
        }

        if(StringUtils.isEmpty(disconfInfo.getVersion())){
            throw new RuntimeException("disconf version 为空！");
        }

        if(StringUtils.isEmpty(disconfInfo.getApp())){
            throw new RuntimeException("disconf app 为空！");
        }

        if(StringUtils.isEmpty(disconfInfo.getEnv())){
            throw new RuntimeException("disconf env 为空！");
        }
    }

    /**
     * 登录disconf
     * @param disconfInfo
     * @return
     * @throws Exception
     */
    public String login(DisconfInfo disconfInfo) throws Exception {
        String sessionId = null;

        RequestBody body = new FormBody.Builder()
                .add("name", disconfInfo.getUserName())
                .add("password", disconfInfo.getUserPwd())
                .add("remember", "0")
                .build();

        Request request = new Request.Builder()
                .url(disconfInfo.getDisconfHostUrl() + "/api/account/signin")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        String res = response.body().string();
        response.close();

        JSONObject jsonObj = null;
        try {
            jsonObj = getJsonObj(res);
        } catch (Exception e) {
            throw new RuntimeException(String.format("登录发生异常，返回结果：%s", res), e);
        }

        if(jsonObj!= null && jsonObj.getBoolean("success")){
            sessionId = jsonObj.getString("sessionId");
        }else{
            throw new RuntimeException(String.format("登录disconf失败，返回结果：%s", res));
        }

        return sessionId;
    }

    /**
     * 检查app是否在disconf已存在
     * @param disconfInfo
     * @return
     * @throws Exception
     */
    public boolean checkAppExist(DisconfInfo disconfInfo) throws Exception {

        Request request = new Request.Builder()
                .url(disconfInfo.getDisconfHostUrl() + "/api/app/list")
                .get()
                .build();

        Response response = client.newCall(request).execute();

        String res = response.body().string();
        response.close();

        JSONObject jsonObj = null;
        try {
            jsonObj = getJsonObj(res);
        } catch (Exception e) {
            throw new RuntimeException(String.format("检查App是否存在发生异常，返回结果：%s", res), e);
        }

        if(jsonObj!= null && jsonObj.getBoolean("success")){
            JSONArray appArray = jsonObj.getJSONObject("page").getJSONArray("result");
            if(appArray.size() > 0){
                for(int i = 0; i < appArray.size(); i++){
                    JSONObject appInfo = appArray.getJSONObject(i);
                    if(disconfInfo.getApp().equals(appInfo.getString("name"))){
                        //记录appId
                        disconfInfo.setAppId(appInfo.getString("id"));
                        return true;
                    }
                }
            }
        }else{
            throw new RuntimeException(String.format("检查App是否存在失败，返回结果：%s", res));
        }

        return false;
    }

    /**
     * 根据检查结果创建App
     * @param disconfInfo
     * @param r
     * @throws Exception
     */
    public void createApp(DisconfInfo disconfInfo, boolean r) throws Exception {
        int count = 1;
        while(!r){
            if(count > 3){
                throw new RuntimeException((count - 1) + "次尝试创建app失败");
            }

            log.info(disconfInfo.getApp() + " 不存在，即将尝试第" + count + "次创建");

            //创建App
            this.createApp(disconfInfo);

            r = this.checkAppExist(disconfInfo);

            count++;
        }

        if(r){
            log.info(disconfInfo.getApp() + " 已存在");
        }
    }

    /**
     * 创建App
     * @param disconfInfo
     * @return
     * @throws Exception
     */
    public void createApp(DisconfInfo disconfInfo) throws Exception{

        RequestBody body = new FormBody.Builder()
                .add("app", disconfInfo.getApp())
                .add("desc", "")
                .add("emails", "")
                .build();

        Request request = new Request.Builder()
                .url(disconfInfo.getDisconfHostUrl() + "/api/app")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        response.close();

        String res = response.body().string();

        JSONObject jsonObj = null;
        try {
            jsonObj = getJsonObj(res);
        } catch (Exception e) {
            throw new RuntimeException(String.format("创建App发生异常，返回结果：%s", res), e);
        }

        if(jsonObj == null || !jsonObj.getBoolean("success")){
            throw new RuntimeException(String.format("创建App失败，返回结果：%s", res));
        }
    }

    /**
     * 检查环境信息是否存在
     * @param disconfInfo
     * @return
     */
    public boolean checkEnvExist(DisconfInfo disconfInfo) throws Exception {

        Request request = new Request.Builder()
                .url(disconfInfo.getDisconfHostUrl() + "/api/env/list")
                .get()
                .build();
        Response response = client.newCall(request).execute();

        String res = response.body().string();
        response.close();

        JSONObject jsonObj = null;
        try {
            jsonObj = getJsonObj(res);
        } catch (Exception e) {
            throw new RuntimeException(String.format("检查环境信息发生异常，返回结果：%s", res), e);
        }

        if(jsonObj!= null && jsonObj.getBoolean("success")){
            JSONArray appArray = jsonObj.getJSONObject("page").getJSONArray("result");
            if(appArray.size() > 0){
                for(int i = 0; i < appArray.size(); i++){
                    JSONObject appInfo = appArray.getJSONObject(i);
                    if(disconfInfo.getEnv().equals(appInfo.getString("name"))){
                        //记录envId
                        disconfInfo.setEnvId(appInfo.getString("id"));
                        return true;
                    }
                }
            }
        }else{
            throw new RuntimeException(String.format("检查环境信息是否存在失败，返回结果：%s", res));
        }

        return false;
    }


    /**
     * 上传watch配置文件
     * @param watchFileStream
     * @throws Exception
     */
    public void uploadDisconfWatchConf(DisconfInfo disconfInfo, InputStream watchFileStream) throws Exception {

        if(watchFileStream == null){
            log.info("无watch配置");
            return;
        }

        Properties disconf = new Properties();
        disconf.load(watchFileStream);

        Set<String> names = disconf.stringPropertyNames();

        if(names != null && names.size() > 0){
            for(String name : names){
                String value = disconf.getProperty(name);
                RequestBody body = new FormBody.Builder()
                        .add("key", name)
                        .add("value", value)
                        .add("appId", disconfInfo.getAppId())
                        .add("version", disconfInfo.getVersion())
                        .add("envId", disconfInfo.getEnvId())
                        .build();

                Request request = new Request.Builder()
                        .url(disconfInfo.getDisconfHostUrl() + "/api/web/config/item")
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();

                String res = response.body().string();
                response.close();

                JSONObject jsonObj = null;
                try {
                    jsonObj = getJsonObj(res);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("上传watch配置项发生异常，key:%s, value:%s，返回结果：%s", name, value, res), e);
                }

                if(jsonObj == null || !jsonObj.getBoolean("success")){
                    throw new RuntimeException(String.format("上传watch配置项失败，key:%s, value:%s，返回结果：%s", name, value, res));
                }

                log.info(String.format("上传watch配置项成功，app:%s env:%s version:%s key:%s, value:%s",
                        disconfInfo.getApp(),
                        disconfInfo.getEnv(),
                        disconfInfo.getVersion(),
                        name,
                        value));
            }
        }
    }

    /**
     * 上传disconf普通配置文件
     * @param disconfInfo
     * @param confFileStreamList
     * @throws Exception
     */
    public void uploadDisconfFileConf(DisconfInfo disconfInfo, List<FileStream> confFileStreamList) throws Exception {

        if(confFileStreamList == null || confFileStreamList.size() <= 0){
            log.info("无普通disconf配置文件");
            return;
        }

        for(FileStream fileStream : confFileStreamList){

            String fileName = fileStream.getFileName();

            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), IOUtils.toByteArray(fileStream.getInputStream()));

            RequestBody multipartBody = new MultipartBody.Builder()
                    .addFormDataPart("appId", disconfInfo.getAppId())
                    .addFormDataPart("version", disconfInfo.getVersion())
                    .addFormDataPart("envId", disconfInfo.getEnvId())
                    .addFormDataPart("myfilerar", fileName,fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(disconfInfo.getDisconfHostUrl() + "/api/web/config/file")
                    .post(multipartBody)
                    .build();
            Response response = client.newCall(request).execute();

            String res = response.body().string();
            response.close();

            JSONObject jsonObj = null;
            try {
                jsonObj = getJsonObj(res);
            } catch (Exception e) {
                throw new RuntimeException(String.format("上传普通配置文件发生异常，fileName:%s，返回结果：%s", fileName, res), e);
            }

            if(jsonObj == null || !jsonObj.getBoolean("success")){
                throw new RuntimeException(String.format("上传普通配置文件失败，fileName:%s，返回结果：%s", fileName, res));
            }

            log.info(String.format("上传普通配置文件成功，app:%s env:%s version:%s fileName:%s",
                    disconfInfo.getApp(),
                    disconfInfo.getEnv(),
                    disconfInfo.getVersion(),
                    fileName));
        }

    }

    /**
     * 获取版本列表
     * @param disconfInfo
     * @return
     * @throws Exception
     */
    public List<String> getVersionList(DisconfInfo disconfInfo) throws Exception {

        String baseUrl = disconfInfo.getDisconfHostUrl() + "/api/web/config/versionlist";

        String url = String.format("%s?appId=%s&envId=%s", baseUrl, disconfInfo.getAppId(), disconfInfo.getEnvId());

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();

        String res = response.body().string();
        response.close();

        JSONObject jsonObj = null;
        try {
            jsonObj = getJsonObj(res);
        } catch (Exception e) {
            throw new RuntimeException(String.format("获取版本列表发生异常，返回结果：%s", res), e);
        }

        List<String> versionList = new ArrayList<>();
        if(jsonObj!= null && jsonObj.getBoolean("success")){
            JSONArray appArray = jsonObj.getJSONObject("page").getJSONArray("result");
            if(appArray.size() > 0){
                for(int i = 0; i < appArray.size(); i++){

                    versionList.add(appArray.getString(i));
                }
            }
        }else{
            throw new RuntimeException(String.format("获取版本列表失败，返回结果：%s", res));
        }

        return versionList;
    }

    /**
     * 删除老版本配置
     * @param disconfInfo
     * @throws Exception
     */
    public void deleteOldVersionConfig(DisconfInfo disconfInfo) throws Exception {

        //获取版本号
        List<String> versionList = getVersionList(disconfInfo);

        List<Integer> list = new ArrayList<>();
        Map<Integer, String> versionMap = new HashMap<>();
        //版本号转数字
        if(versionList.size() > 0){
            for(String version : versionList){

                Integer v = Integer.valueOf(version.replaceAll("_", ""));
                list.add(v);
                versionMap.put(v, version);
            }
        }

        //数字版本号排序
        list.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {

                if(o1.byteValue() == o2.byteValue()){
                    return 0;
                }
                return o1 > o2 ? 1 : -1;
            }
        });

        log.info("version list:" + list);

        if(list.size() > 5){
            int endIndex = list.size() - 5;
            for(int i = 0; i < endIndex; i++){
                Integer v = list.get(i);
                String version = versionMap.get(v);

                DisconfInfo info = new DisconfInfo();
                info.setDisconfHostUrl(disconfInfo.getDisconfHostUrl());
                info.setAppId(disconfInfo.getAppId());
                info.setEnvId(disconfInfo.getEnvId());
                info.setVersion(version);

                //删除版本所有配置
                deleteVersionConfig(info);
            }
        }
    }

    /**
     * 删除版本所有配置
     * @param disconfInfo
     * @throws Exception
     */
    private void deleteVersionConfig(DisconfInfo disconfInfo) throws Exception {

        String res = getAllConfigList(disconfInfo.getDisconfHostUrl(), disconfInfo.getAppId(), disconfInfo.getEnvId(), disconfInfo.getVersion());
        JSONObject jsonObj = null;
        try {
            jsonObj = getJsonObj(res);
        } catch (Exception e) {
            throw new RuntimeException(String.format("获取配置列表发生异常，返回结果：%s", res), e);
        }

        if(jsonObj!= null && jsonObj.getBoolean("success")){
            JSONArray appArray = jsonObj.getJSONObject("page").getJSONArray("result");
            if(appArray.size() > 0){
                for(int j = 0; j < appArray.size(); j++){

                    JSONObject obj = appArray.getJSONObject(j);
                    Integer configId = obj.getInteger("configId");
                    //删除配置
                    this.deleteConfig(disconfInfo, configId);

                    log.info(String.format("成功删除配置！app:%s env:%s version:%s key:%s configId:%s",
                            obj.getString("appName"),
                            obj.getString("envName"),
                            obj.getString("version"),
                            obj.getString("key"),
                            obj.getString("configId")));
                }
            }
        }else{
            throw new RuntimeException(String.format("获取配置列表失败，返回结果：%s", res));
        }
    }

    /**
     * 获取所有配置列表
     * @param disconfHostUrl
     * @param appId
     * @param envId
     * @param version
     * @return
     * @throws Exception
     */
    public String getAllConfigList(String disconfHostUrl, String appId, String envId, String version) throws Exception {

        String baseUrl = disconfHostUrl + "/api/web/config/simple/list";

        String url = String.format("%s?appId=%s&envId=%s&version=%s", baseUrl, appId, envId, version);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();

        String res = response.body().string();
        response.close();

        return res;
    }

    /**
     * 删除配置
     * @param disconfInfo
     * @param configId
     * @throws Exception
     */
    public void deleteConfig(DisconfInfo disconfInfo , Integer configId) throws Exception {

        Request request = new Request.Builder()
                .url(disconfInfo.getDisconfHostUrl() + "/api/web/config/" + configId)
                .delete()
                .build();

        Response response = client.newCall(request).execute();

        String res = response.body().string();
        response.close();

        JSONObject jsonObj = null;
        try {
            jsonObj = getJsonObj(res);
        } catch (Exception e) {
            throw new RuntimeException(String.format("删除配置发生异常，返回结果：%s", res), e);
        }

        if(jsonObj == null || !jsonObj.getBoolean("success")){
            throw new RuntimeException(String.format("删除配置失败，返回结果：%s", res));
        }
    }

    /**
     * 根据覆盖开关判断是否清空配置
     * @param disconfInfo
     */
    public void clearVersionConfig(DisconfInfo disconfInfo) throws Exception {
        if(disconfInfo.getEnableAutoOverride() != null && disconfInfo.getEnableAutoOverride()){
            log.info("自动覆盖配置开启，即将清空配置。");

            this.deleteVersionConfig(disconfInfo);
        }
    }

    /**
     * 检查是否已经存在配置
     * @param disconfInfo
     * @return
     * @throws Exception
     */
    public boolean checkExistConfig(DisconfInfo disconfInfo) throws Exception {
        String res = getAllConfigList(disconfInfo.getDisconfHostUrl(), disconfInfo.getAppId(), disconfInfo.getEnvId(), disconfInfo.getVersion());
        JSONObject jsonObj = null;
        try {
            jsonObj = getJsonObj(res);
        } catch (Exception e) {
            throw new RuntimeException(String.format("获取配置列表发生异常，返回结果：%s", res), e);
        }

        if(jsonObj!= null && jsonObj.getBoolean("success")) {
            JSONArray appArray = jsonObj.getJSONObject("page").getJSONArray("result");
            if (appArray.size() > 0) {
                return true;
            }
        }
        return false;
    }


    /**
     * 转换成JSON
     * @param text
     * @return
     */
    private static JSONObject getJsonObj(String text){

        return JSON.parseObject(text);
    }
}
