package com.github.iherasymenko.jlink.test;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CrossTargetJdkImageFunctionalTest extends AbstractTestBase {

    @Test
    @EnabledIf("jdk21")
    public void can_create_image_with_a_cross_target_jdk() throws IOException {
        build.settingsFile = """
                rootProject.name = 'demo'
                dependencyResolutionManagement {
                    repositories {
                        ivy {
                            url = uri("https://cdn.azul.com/zulu/bin/")
                            patternLayout {
                                artifact "[artifact].[ext]"
                            }
                            metadataSources {
                                artifact()
                            }
                            content {
                                includeGroup("com.azul.cdn")
                            }
                        }
                        mavenCentral()
                    }
                }
                """;
        build.buildFile = """
                plugins {
                	id 'application'
                	id 'io.github.iherasymenko.jlink-application'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
                application {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                }
                
                jlinkImages {
                	linuxX64 {
                		group = 'com.azul.cdn'
                		jdkArchive = 'zulu21.30.15-ca-jdk21.0.1-linux_x64.zip'
                	}
                	windowsX64 {
                		group = 'com.azul.cdn'
                		jdkArchive = 'zulu21.30.15-ca-jdk21.0.1-win_x64.zip'
                	}
                	macOsX64 {
                		group = 'com.azul.cdn'
                		jdkArchive = 'zulu21.30.15-ca-jdk21.0.1-macosx_x64.tar.gz'
                	}
                }
                """;
        build.mainClass = """
                package com.example.demo;
                
                public class DemoApplication {
                    public static void main(String[] args) {
                       System.out.println("Hello, world!");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;

        BuildResult buildResult = build.runner("assemble")
                .build();

        assertThat(buildResult.task(":imageLinuxX64"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.SUCCESS);
        assertThat(buildResult.task(":imageWindowsX64"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.SUCCESS);
        assertThat(buildResult.task(":imageMacOsX64"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.SUCCESS);

        assertThat(build.projectDir.resolve("build/images/linuxX64/lib/libjava.so")).exists();
        assertThat(build.projectDir.resolve("build/images/windowsX64/bin/java.dll")).exists();
        assertThat(build.projectDir.resolve("build/images/macOsX64/lib/libjava.dylib")).exists();
    }

    boolean jdk21() {
        return "JAVA_HOME_21_X64".equals(System.getenv("JAVA_HOME_OVERRIDE_VAR")) // see .github/workflows/test.yml
               || System.getenv("GITHUB_ACTIONS") == null && Runtime.version().feature() == 21; // Running locally
    }

}