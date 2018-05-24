### disconf-auto-maven-plugin是一个maven插件，配合disconf使用。

## 用途：
读取jar包或者war包的disconf配置自动上传到disconf。

## 引入方式：
```xml
        <plugin>
            <groupId>com.fcbox</groupId>
            <artifactId>disconf-auto-maven-plugin</artifactId>
            <version>1.0</version>
        </plugin>
```

## 配置：
```properties
#是否开启自动上传（true、false）
enable.auto.upload=true
#是否自动覆盖现有配置（true、false）
enable.auto.override=true
```

在disconf.properties文件中进行配置。

## 配置disconf控制台的用户名密码：
```xml
        <plugin>
            <groupId>com.fcbox</groupId>
            <artifactId>disconf-auto-maven-plugin</artifactId>
            <version>1.0</version>
            <configuration>
                <hostNamePwdList>
                    <hostNamePwd>10.207.33.25@admin:admin</hostNamePwd>
                    <hostNamePwd>10.207.33.55@guest:123456</hostNamePwd>
                </hostNamePwdList>
            </configuration>
        </plugin>
```

如果不同环境有对应不同的disconf控制台，则配置多个，插件会自动识别当前环境对应哪个disconf控制台账号密码。
如果不设置则账号密码都默认为admin

## 运行
运行disconf-auto-maven-plugin插件即可自动上传配置。
