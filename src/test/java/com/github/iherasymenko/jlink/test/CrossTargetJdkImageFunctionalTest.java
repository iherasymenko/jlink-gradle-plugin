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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CrossTargetJdkImageFunctionalTest extends AbstractTestBase {

    @Test
    public void can_create_image_with_a_cross_target_jdk() throws IOException {
        build.settingsFile = """
                rootProject.name = 'demo'
                dependencyResolutionManagement {
                    repositories {
                        ivy {
                            url = uri('https://cdn.azul.com/zulu/bin/')
                            patternLayout {
                                artifact '[artifact].[ext]'
                            }
                            metadataSources {
                                artifact()
                            }
                            content {
                                includeGroup 'com.azul.cdn'
                            }
                        }
                        mavenCentral()
                    }
                }
                """;
        String javaVersion = System.getenv().getOrDefault("TESTING_AGAINST_JDK", "25");
        String jdkDistro = switch (javaVersion) {
            case "17" -> "zulu17.66.19-ca-jdk17.0.19";
            case "21" -> "zulu21.50.19-ca-jdk21.0.11";
            case "25" -> "zulu25.34.17-ca-jdk25.0.3";
            case "26" -> "zulu26.30.11-ca-jdk26.0.1";
            default -> throw new AssertionError("Unknown java version: " + javaVersion);
        };
        build.buildFile = """
                plugins {
                	id 'application'
                	id 'com.github.iherasymenko.jlink'
                }
                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                
                java {
                	toolchain {
                		languageVersion = JavaLanguageVersion.of($1%s)
                		vendor = JvmVendorSpec.AZUL
                	}
                }
                
                application {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                }
                
                jlinkImages {
                	linuxX64 {
                		group = 'com.azul.cdn'
                		jdkArchive = '%2$s-linux_x64.zip'
                	}
                	windowsX64 {
                		group = 'com.azul.cdn'
                		jdkArchive = '%2$s-win_x64.zip'
                	}
                	macOsX64 {
                		group = 'com.azul.cdn'
                		jdkArchive = '%2$s-macosx_x64.tar.gz'
                	}
                }
                """.formatted(javaVersion, jdkDistro);
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

    @Test
    public void can_not_create_image_with_a_cross_target_jdk_when_no_jmods_found() throws IOException {
        build.settingsFile = """
                rootProject.name = 'demo'
                dependencyResolutionManagement {
                    repositories {
                        ivy {
                            url = uri('https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.1%2B9/')
                            patternLayout {
                                artifact '[artifact].[ext]'
                            }
                            metadataSources {
                                artifact()
                            }
                            content {
                                includeGroup 'net.adoptium.temurin'
                            }
                        }
                        mavenCentral()
                    }
                }
                """;
        build.buildFile = """
                plugins {
                	id 'application'
                	id 'com.github.iherasymenko.jlink'
                }
                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                
                java {
                	toolchain {
                		languageVersion = JavaLanguageVersion.of(25)
                		vendor = JvmVendorSpec.AZUL
                	}
                }
                
                application {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                }
                
                jlinkImages {
                	linuxX64 {
                		group = 'net.adoptium.temurin'
                		jdkArchive = 'OpenJDK24U-jdk_x64_linux_hotspot_24.0.1_9.tar.gz'
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
                .buildAndFail();

        assertThat(buildResult.task(":imageLinuxX64"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.FAILED);

        assertThat(buildResult.getOutput())
                .contains("jmods directory is not found. Cross-linking is not available with the given distribution. See https://openjdk.org/jeps/493 for details.");
    }

}