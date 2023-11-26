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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfEnvironmentVariable(named = "TESTING_AGAINST_JDK", matches = "11|17", disabledReason = "Generate CDS Archive plugin was introduced in JDK 18")
class GenerateCdsArchivePluginFunctionalTest extends AbstractTestBase {

    @Test
    void can_generate_cds_archive() throws IOException {
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
                    generateCdsArchive = true
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
                    requires java.sql;
                }
                """;

        BuildResult buildResult = build.runner("image").build();

        assertThat(build.projectDir.resolve("build/images/demo/lib/server/classes.jsa")).exists();
        assertThat(build.projectDir.resolve("build/images/demo/lib/server/classes_nocoops.jsa")).exists();
        assertThat(buildResult)
                .extracting(BuildResult::getOutput, InstanceOfAssertFactories.STRING)
                .contains("Created CDS archive successfully");
    }

}
