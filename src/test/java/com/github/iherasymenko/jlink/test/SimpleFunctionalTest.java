package com.github.iherasymenko.jlink.test;

import com.github.iherasymenko.jlink.test.fixtures.GradleBuild;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class SimpleFunctionalTest {

    final GradleBuild build = new GradleBuild();

    @AfterEach
    void tearDown() throws IOException {
        build.tearDown();
    }

    @Test
    void javaBaseOnly() throws IOException {
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
                	applicationDefaultJvmArgs = ["-Xms128m", "-Xmx128m"]
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
    }

}
