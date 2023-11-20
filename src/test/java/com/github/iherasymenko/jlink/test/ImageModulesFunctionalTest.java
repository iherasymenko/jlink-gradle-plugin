/*
 * Copyright 2023 Ihor Herasymenko.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.iherasymenko.jlink.test;

import com.github.iherasymenko.jlink.test.fixtures.Text;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageModulesFunctionalTest extends AbstractTestBase {

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void can_list_modules_in_a_simple_image() throws IOException {
        build.buildFile = """
                plugins {
                	id 'application'
                	id 'com.github.iherasymenko.jlink'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
                application {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
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

    @Test
    void can_list_modules_in_an_image_with_third_party_modules() throws IOException {
        build.buildFile = """
                plugins {
                	id 'application'
                	id 'com.github.iherasymenko.jlink'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
                application {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                }
                
                dependencies {
                    implementation platform('org.slf4j:slf4j-bom:2.0.9')
                    implementation 'com.zaxxer:HikariCP:5.1.0'
                }
                """;
        build.moduleInfo = """
                module demo.main {
                    requires com.zaxxer.hikari;
                }
                """;
        BuildResult buildResult = build.runner("imageModules")
                .build();
        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageModules", "BUILD SUCCESSFUL");
        assertThat(taskOutput).hasSize(11);
        assertThat(taskOutput[0]).isEqualTo("com.zaxxer.hikari@5.1.0");
        assertThat(taskOutput[1]).isEqualTo("demo.main");
        assertThat(taskOutput[2]).startsWith("java.base@");
        assertThat(taskOutput[3]).startsWith("java.logging@");
        assertThat(taskOutput[4]).startsWith("java.management@");
        assertThat(taskOutput[5]).startsWith("java.naming@");
        assertThat(taskOutput[6]).startsWith("java.security.sasl@");
        assertThat(taskOutput[7]).startsWith("java.sql@");
        assertThat(taskOutput[8]).startsWith("java.transaction.xa@");
        assertThat(taskOutput[9]).startsWith("java.xml@");
        assertThat(taskOutput[10]).isEqualTo("org.slf4j@2.0.9");
    }

}
