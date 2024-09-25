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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ImageIgnoreSignedJarsFunctionalTest extends AbstractTestBase {

    private static final String SIGNED_JAR_BLOB = """
            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
            AAoACgCHAgAAzwwAAAAA
            """;

    @Test
    void cannot_build_an_image_with_a_signed_jar() throws IOException {
        Files.write(build.projectDir.resolve("demo-signed.jar"), Base64.getMimeDecoder().decode(SIGNED_JAR_BLOB));
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
                
                dependencies {
                   implementation files('demo-signed.jar')
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
                    requires demo.main.signed;
                }
                """;

        BuildResult buildResult = build.runner("image").buildAndFail();

        assertThat(buildResult)
                .extracting(BuildResult::getOutput, InstanceOfAssertFactories.STRING)
                .contains("Error: signed modular JAR");
    }

    @Test
    void can_ignore_signing_information() throws IOException {
        Files.write(build.projectDir.resolve("demo-signed.jar"), Base64.getMimeDecoder().decode(SIGNED_JAR_BLOB));
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
                	ignoreSigningInformation = true
                }
                
                dependencies {
                   implementation files('demo-signed.jar')
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
                    requires demo.main.signed;
                }
                """;

        BuildResult buildResult = build.runner("image").build();

        assertThat(buildResult)
                .extracting(BuildResult::getOutput, InstanceOfAssertFactories.STRING)
                .contains("WARNING: signed modular JAR");
    }

}
