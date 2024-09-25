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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;

class LegalNoticeFilePluginFunctionalTest extends AbstractTestBase {

    @BeforeEach
    void setUp() throws IOException {
        ToolProvider javac = ToolProvider.findFirst("javac").orElseThrow(() -> new IllegalStateException("javac not found"));
        ToolProvider jmod = ToolProvider.findFirst("jmod").orElseThrow(() -> new IllegalStateException("jmod not found"));

        Path src = Files.createDirectories(build.projectDir.resolve("foo-module/src"));
        Path classes = Files.createDirectories(build.projectDir.resolve("foo-module/classes"));
        Path legal = Files.createDirectories(build.projectDir.resolve("foo-module/legal"));
        Path libs = Files.createDirectories(build.projectDir.resolve("libs"));

        Path moduleInfo = src.resolve("module-info.java");
        Files.writeString(moduleInfo, "module foo {}");
        Files.writeString(legal.resolve("LICENSE"), "DUMMY LICENSE TEXT");

        Path fooJmod = libs.resolve("foo.jmod");

        javac.run(System.out, System.err, "-d", classes.toString(), "--release", "11", moduleInfo.toString());
        jmod.run(System.out, System.err, "create", "--class-path", classes.toString(), "--legal-notices", legal.toString(), fooJmod.toString());

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
                    requires foo;
                }
                """;
    }

    @Test
    void dedup_legal_notices_error_if_not_same_content() throws IOException {
        build.buildFile = """
                plugins {
                	id 'application'
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
                
                application {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                }
                
                dependencies {
                    implementation files("libs/foo.jmod")
                }
                
                jlinkApplication {
                    dedupLegalNoticesErrorIfNotSameContent = true
                }
                
                // Gradle does not recognize *.jmod files as modules
                tasks.withType(JavaCompile).configureEach {
                    doFirst { task ->
                        task.options.compilerArgs += ["--module-path", task.classpath.asPath]
                    }
                }
                """;
        BuildResult buildResult = build.runner("image").buildAndFail();
        assertThat(buildResult)
                .extracting(BuildResult::getOutput, InstanceOfAssertFactories.STRING)
                .contains("Error: /java.base/legal/java.base/LICENSE /foo/legal/foo/LICENSE contain different content");
        assertThat(build.projectDir.resolve("build/images/demo/bin")).doesNotExist();
        assertThat(build.projectDir.resolve("build/images/demo/release")).doesNotExist();
    }

    @Test
    void dedup_legal_notices_error_if_not_same_content_is_disabled_by_default() throws IOException {
        build.buildFile = """
                plugins {
                	id 'application'
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
                
                application {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                }
                
                dependencies {
                    implementation files("libs/foo.jmod")
                }
                
                // Gradle does not recognize *.jmod files as modules
                tasks.withType(JavaCompile).configureEach {
                    doFirst { task ->
                        task.options.compilerArgs += ["--module-path", task.classpath.asPath]
                    }
                }
                """;
        build.runner("image").build();
        assertThat(build.projectDir.resolve("build/images/demo/legal/foo/LICENSE"))
                .content()
                .isEqualTo("DUMMY LICENSE TEXT");
        assertThat(build.projectDir.resolve("build/images/demo/legal/java.base/LICENSE"))
                .content()
                .startsWith("The GNU General Public License (GPL)");
    }

}
