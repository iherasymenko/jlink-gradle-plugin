package com.github.iherasymenko.jlink.test;

import com.github.iherasymenko.jlink.test.fixtures.Text;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageModulesFunctionalTest extends AbstractTestBase {

    @Test
    void can_list_modules_in_a_simple_image() throws IOException {
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
                       System.out.println("Hello, world!");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;
        BuildResult buildResult = build.runner("imageModules")
                .build();
        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageModules", "BUILD SUCCESSFUL");
        assertEquals(2, taskOutput.length);
        assertEquals("demo.main", taskOutput[0]);
        assertTrue(taskOutput[1].startsWith("java.base@"));
    }

}
