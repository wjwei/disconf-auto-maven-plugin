package com.wjwei.plugin;

import com.wjwei.operator.DisconfAutoOperator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

/**
 * @goal upload
 * @execute phase = "package"
 * @author 000298
 */
@Mojo(name = "upload", defaultPhase = LifecyclePhase.PACKAGE)
public class DisconfAutoPlugin extends AbstractMojo {

    /**
     * disconf host userName Pwd List
     * @parameter
     */
    @Parameter(property = "hostNamePwdList")
    private List<String> hostNamePwdList;

    /**
     * @parameter expression = "${project.build.directory}"
     * @readonly
     * @required
     */
    @Parameter(property = "project.build.directory")
    private String buildDir;

    /**
     * @parameter expression = "${project.build.finalName}"
     * @readonly
     * @required
     */
    @Parameter(property = "project.build.finalName")
    private String finalName;

    /**
     * @parameter expression = "${project.packaging}"
     * @readonly
     * @required
     */
    @Parameter(property = "project.packaging")
    private String packing;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String fileName = String.format("%s.%s", finalName, packing);

        String jarPath = String.format("%s/%s.%s", buildDir, finalName, packing);

        getLog().info("jarPath : " + jarPath);

        try {
            //上传应用的disconf配置
            DisconfAutoOperator.uploadAppDisconf(buildDir, fileName, hostNamePwdList);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }
}
