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
import org.assertj.core.api.InstanceOfAssertFactories;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class PluginsFunctionalTest extends AbstractTestBase {

    @Test
    void default_configuration() throws IOException {
        build.buildFile = """
                plugins {
                	id 'java'
                	id 'com.github.iherasymenko.jlink'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'

                java {
                	toolchain {
                		languageVersion = JavaLanguageVersion.of(System.getenv().getOrDefault('TESTING_AGAINST_JDK', '21'))
                		vendor = JvmVendorSpec.AZUL
                	}
                }
              
                jlinkApplication {
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

                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {

                }
                """;
        BuildResult buildResult = build.runner("image").build();
        // --generate-cds-archive is disabled by default
        assertThat(buildResult)
                .extracting(BuildResult::getOutput, InstanceOfAssertFactories.STRING)
                .doesNotContain("Created CDS archive successfully");
        assertThat(build.projectDir.resolve("build/images/demo/lib/server/classes.jsa")).doesNotExist();
        assertThat(build.projectDir.resolve("build/images/demo/lib/server/classes_nocoops.jsa")).doesNotExist();
        // --exclude-files is disabled by default
        assertThat(build.projectDir.resolve("build/images/demo/legal")).isNotEmptyDirectory();
    }

    @Test
    void can_disable_a_jlink_plugin() throws IOException {
        build.buildFile = """
                plugins {
                	id 'java'
                	id 'com.github.iherasymenko.jlink'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'

                java {
                	toolchain {
                		languageVersion = JavaLanguageVersion.of(System.getenv().getOrDefault('TESTING_AGAINST_JDK', '21'))
                		vendor = JvmVendorSpec.AZUL
                	}
                }
              
                jlinkApplication {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                	addOptions = ["-Dtest_arg1=hello", "-Dtest_arg2=world"]
                	disablePlugins = ["add-options"]
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
                       System.out.println("Arg1: " + System.getProperty("test_arg1"));
                       System.out.println("Arg2: " + System.getProperty("test_arg2"));
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {

                }
                """;

        BuildResult buildResult = build.runner("imageRun").build();

        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageRun", "BUILD SUCCESSFUL");
        assertThat(taskOutput).hasSize(2);
        assertThat(taskOutput[0]).isEqualTo("Arg1: null");
        assertThat(taskOutput[1]).isEqualTo("Arg2: null");
    }

}
