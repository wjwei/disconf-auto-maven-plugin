## disconf-auto-maven-plugin是一个maven插件，配合disconf使用。

## 用途
读取jar包或者war包的disconf配置自动上传到disconf。

## 使用
将插件安装到本地仓库或者发布到maven私服。

## 引入方式
在maven工程中做如下插件引入：
```xml
        <plugin>
            <groupId>com.wjwei</groupId>
            <artifactId>disconf-auto-maven-plugin</artifactId>
            <version>1.0</version>
        </plugin>
```

## 配置
```properties
#是否开启自动上传（true、false）
enable.auto.upload=true
#是否自动覆盖现有配置（true、false）
enable.auto.override=true
```

在disconf.properties文件中进行配置。

## 配置disconf控制台的用户名密码
```xml
        <plugin>
            <groupId>com.wjwei</groupId>
            <artifactId>disconf-auto-maven-plugin</artifactId>
            <version>1.0</version>
            <configuration>
                <hostNamePwdList>
                    <hostNamePwd>80.207.56.25@admin:admin</hostNamePwd>
                    <hostNamePwd>80.207.56.55@guest:123456</hostNamePwd>
                </hostNamePwdList>
            </configuration>
        </plugin>
```

如果不同环境有对应不同的disconf控制台，则配置多个，插件会自动识别当前环境对应哪个disconf控制台账号密码。
如果不设置则账号密码都默认为admin

hostNamePwd的组成是：disconf控制台访问IP或域名 @ 用户名：密码

## disconf文件存放要求
disconf文件、或者配置项必须以如下结构存放

```java
-src
  -main
    -resources
      -config
        -disconf
          -rd
            watchConfFile.properties
            sysConfig.properties
            redisConfig.properties
          -qa
            watchConfFile.properties
            sysConfig.properties
            redisConfig.properties
          -online
            watchConfFile.properties
            sysConfig.properties
            redisConfig.properties
```

其中   
```
 -disconf
    -rd
    -qa
    -online
```
这个目录结构是不能改变的因为插件是按照这个目录结构加载配置的，disconf文件夹名称不能变，
下面的环境（rd、qa、online）文件夹可根据你的环境名称而变，watch配置项的watchConfFile.properties文件名称是不能改变的，
插件是到这个名称的文件中加载disconf的watche配置项的。 

## 运行
运行disconf-auto-maven-plugin插件即可自动上传配置。

```manven
mvn disconf-auto:upload
```

