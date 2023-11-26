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

class StripJavaDebugAttributesPluginFunctionalTest extends AbstractTestBase {

    @Test
    @DisabledIfEnvironmentVariable(
            named = "TESTING_AGAINST_JDK",
            matches = "11",
            disabledReason = "Strip Java Debug Attributes plugin was first introduced in JDK 12. Prior to that, the functionality was provided by the jlink --strip-debug option."
    )
    void can_strip_java_debug_attributes_via_stripJavaDebugAttributes() throws IOException {
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
                    stripJavaDebugAttributes = true
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
                        Runtime.getRuntime().loadLibrary("nonexistentlib.dll");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                    
                }
                """;

        BuildResult buildResult = build.runner("imageRun").buildAndFail();
        String stackTraceWithNoDebugInformation = """
                	at java.base/java.lang.ClassLoader.loadLibrary(Unknown Source)
                	at java.base/java.lang.Runtime.loadLibrary0(Unknown Source)
                	at java.base/java.lang.Runtime.loadLibrary(Unknown Source)
                	at demo.main/com.example.demo.DemoApplication.main(Unknown Source)
                """;
        assertThat(buildResult).extracting(BuildResult::getOutput, InstanceOfAssertFactories.STRING).contains(stackTraceWithNoDebugInformation);
    }

    @Test
    void can_strip_java_debug_attributes_via_stripDebug() throws IOException {
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
                    stripDebug = true
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
                        Runtime.getRuntime().loadLibrary("nonexistentlib.dll");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                    
                }
                """;

        BuildResult buildResult = build.runner("imageRun").buildAndFail();
        String stackTraceWithNoDebugInformation = """
                	at java.base/java.lang.ClassLoader.loadLibrary(Unknown Source)
                	at java.base/java.lang.Runtime.loadLibrary0(Unknown Source)
                	at java.base/java.lang.Runtime.loadLibrary(Unknown Source)
                	at demo.main/com.example.demo.DemoApplication.main(Unknown Source)
                """;
        assertThat(buildResult).extracting(BuildResult::getOutput, InstanceOfAssertFactories.STRING).contains(stackTraceWithNoDebugInformation);

        // Verification that native binary does not contain debug information is skipped
    }

    @Test
    void should_not_strip_java_debug_attributes_by_default() throws IOException {
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
                        Runtime.getRuntime().loadLibrary("nonexistentlib.dll");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                    
                }
                """;

        BuildResult buildResult = build.runner("imageRun").buildAndFail();
        assertThat(buildResult).extracting(BuildResult::getOutput, InstanceOfAssertFactories.STRING).contains("at demo.main/com.example.demo.DemoApplication.main(DemoApplication.java:5)");
    }

}
