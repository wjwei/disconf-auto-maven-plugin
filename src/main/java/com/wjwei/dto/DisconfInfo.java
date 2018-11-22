package com.wjwei.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author 000298
 * @date 2018/4/26
 */
@Getter
@Setter
@ToString
public class DisconfInfo {

    private String confServerHost;
    private String disconfHostUrl;
    private String version;
    private String app;
    private String env;
    private String userName;
    private String userPwd;

    private Boolean enableAutoUpload;
    private Boolean enableAutoOverride;

    private String appId;
    private String envId;

}
