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

class IncludeLocalesPluginFunctionalTest extends AbstractTestBase {

    @Test
    void can_include_locales() throws IOException {
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
                    includeLocales = ['en-CA']
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
                               
                import java.util.Locale;
                
                public class DemoApplication {
                    public static void main(String[] args) {
                        for (Locale locale : Locale.getAvailableLocales()) {
                            System.out.println(locale);
                        }
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                    requires jdk.localedata;
                }
                """;

        BuildResult buildResult = build.runner("imageRun").build();

        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageRun", "BUILD SUCCESSFUL");
        assertThat(taskOutput)
                .hasSizeLessThan(10)
                .contains("en", "en_CA", "en_US", "en_US_POSIX");
    }

    @Test
    void many_locales_are_available_by_default() throws IOException {
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
                               
                import java.util.Arrays;
                import java.util.Locale;
                import java.util.Comparator;
                
                public class DemoApplication {
                    public static void main(String[] args) {
                        Locale[] availableLocales = Locale.getAvailableLocales();
                        Arrays.sort(availableLocales, Comparator.comparing(Locale::toString));
                        for (Locale locale : availableLocales) {
                            System.out.println(locale);
                        }
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                    requires jdk.localedata;
                }
                """;

        BuildResult buildResult = build.runner("imageRun").build();

        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageRun", "BUILD SUCCESSFUL");
        assertThat(taskOutput)
                .hasSizeGreaterThan(100)
                .contains("uk_UA", "en_CA", "fr_CA");
    }

}
