package com.github.iherasymenko.jlink.test;

import com.github.iherasymenko.jlink.test.fixtures.Text;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CustomJvmArgsFunctionalTest extends AbstractTestBase {

    @Test
    @DisabledIf("jdk12Requested")
    void can_add_jvm_args_to_the_image() throws IOException {
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
                	applicationDefaultJvmArgs = ["-Dtest_arg1=hello", "-Dtest_arg2=world"]
                }
                                                
                """;
        build.settingsFile = """
                rootProject.name = 'demo'
                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                    }
                }
                """;
        build.mainClass = """
                package com.example.demo;
                
                public class DemoApplication {
                    public static void main(String[] args) {
                       System.out.println(System.getProperty("test_arg1"));
                       System.out.println(System.getProperty("test_arg2"));
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;
        BuildResult buildResult = build.runner("imageRun")
                .build();
        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageRun", "BUILD SUCCESSFUL");
        assertEquals(2, taskOutput.length);
        assertEquals("hello", taskOutput[0]);
        assertEquals("world", taskOutput[1]);
    }

    boolean jdk12Requested() {
        return System.getenv("JAVA_HOME_12_X64") != null;
    }

}
