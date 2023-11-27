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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

final class ImageFunctionalTest extends AbstractTestBase {

    @Test
    void can_add_jvm_args_to_the_image_via_application_extension() throws IOException {
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
                	applicationDefaultJvmArgs = ["-Dtest_arg1=hello", "-Dtest_arg2=world"]
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
                       System.out.println(System.getProperty("test_arg1"));
                       System.out.println(System.getProperty("test_arg2"));
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;
        BuildResult buildResult = build.runner("imageRun")
                .build();
        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageRun", "BUILD SUCCESSFUL");
        assertThat(taskOutput).hasSize(2);
        assertThat(taskOutput[0]).isEqualTo("hello");
        assertThat(taskOutput[1]).isEqualTo("world");
    }

    @Test
    void can_add_jvm_args_to_the_image_via_jlink_application_extension() throws IOException {
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
                       System.out.println(System.getProperty("test_arg1"));
                       System.out.println(System.getProperty("test_arg2"));
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;
        BuildResult buildResult = build.runner("imageRun")
                .build();
        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageRun", "BUILD SUCCESSFUL");
        assertThat(taskOutput).hasSize(2);
        assertThat(taskOutput[0]).isEqualTo("hello");
        assertThat(taskOutput[1]).isEqualTo("world");
    }

    @Test
    void jlink_configuration_can_be_specified_via_jlink_application_extension() throws IOException {
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
                	applicationName = 'cool-app'
                }
                                                
                """;
        build.settingsFile = """
                rootProject.name = 'my-project'
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
                       System.out.println("Specified via `jlinkApplication` extension");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;
        BuildResult buildResult = build.runner("imageRun")
                .build();
        assertThat(buildResult.getOutput()).contains("Specified via `jlinkApplication` extension");
        assertThat(build.projectDir).satisfiesAnyOf(
                path -> assertThat(path.resolve("build/images/cool-app/bin/cool-app")).exists(),
                path -> assertThat(path.resolve("build/images/cool-app/bin/cool-app.bat")).exists()
        );
    }

    @Test
    void launcher_and_output_folder_configuration_can_be_inferred_from_project_name() throws IOException {
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
                rootProject.name = 'really-cool-application'
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
                       System.out.println("Specified via `jlinkApplication` extension");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;
        BuildResult buildResult = build.runner("imageRun")
                .build();
        assertThat(buildResult.getOutput()).contains("Specified via `jlinkApplication` extension");
        assertThat(build.projectDir).satisfiesAnyOf(
                path -> assertThat(path.resolve("build/images/really-cool-application/bin/really-cool-application")).exists(),
                path -> assertThat(path.resolve("build/images/really-cool-application/bin/really-cool-application.bat")).exists()
        );
    }

    @Test
    void jlink_configuration_can_be_inferred_from_application_extension() throws IOException {
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
                	applicationName = 'my-app'
                }
                                                
                """;
        build.settingsFile = """
                rootProject.name = 'my-project'
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
                       System.out.println("Inherited from `application` extension");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;
        BuildResult buildResult = build.runner("imageRun")
                .build();
        assertThat(buildResult.getOutput()).contains("Inherited from `application` extension");
        assertThat(build.projectDir).satisfiesAnyOf(
                path -> assertThat(path.resolve("build/images/my-app/bin/my-app")).exists(),
                path -> assertThat(path.resolve("build/images/my-app/bin/my-app.bat")).exists()
        );
    }

    @Test
    void jlink_configuration_defined_via_jlink_application_extension_takes_precedence_over_configuration_defined_via_application_extension() throws IOException {
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
                	mainClass = 'com.example.demo.DemoApplication$CoolApplication'
                	mainModule = 'coolapplication.main'
                	applicationName = 'cool-app'
                }
                
                jlinkApplication {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                	applicationName = 'my-app'
                }
                                                
                """;
        build.settingsFile = """
                rootProject.name = 'my-project'
                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                    }
                }
                """;
        build.mainClass = """
                package com.example.demo;
                
                public class DemoApplication {
                
                    public static class CoolApplication {
                        public static void main(String[] args) {
                           System.out.println("This is the cool app");
                        }
                    }
                    
                    public static void main(String[] args) {
                       System.out.println("Overriden by `jlinkApplication` extension");
                    }
                }
                """;
        build.moduleInfo = """
                module demo.main {
                                                                                            
                }
                """;
        BuildResult buildResult = build.runner("imageRun")
                .withDebug(true)
                .build();
        assertThat(buildResult.getOutput()).contains("Overriden by `jlinkApplication` extension");
        assertThat(build.projectDir).satisfiesAnyOf(
                path -> assertThat(path.resolve("build/images/my-app/bin/my-app")).exists(),
                path -> assertThat(path.resolve("build/images/my-app/bin/my-app.bat")).exists()
        );
    }

    @Test
    void can_add_an_additional_launcher() throws IOException {
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
                	launcher = [
                	    'jlink-in-disguise': 'jdk.jlink/jdk.tools.jimage.Main',
                	    'jar-in-disguise': 'jdk.jartool/sun.tools.jar.Main'
                	]
                }
                                                
                """;
        build.settingsFile = """
                rootProject.name = 'really-cool-application'
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
                    requires jdk.jartool;
                    requires jdk.jlink;
                }
                """;
        build.runner("image").build();

        assertThat(build.projectDir).satisfiesAnyOf(
                path -> assertThat(path.resolve("build/images/really-cool-application/bin/really-cool-application")).exists(),
                path -> assertThat(path.resolve("build/images/really-cool-application/bin/really-cool-application.bat")).exists()
        );
        assertThat(build.projectDir).satisfiesAnyOf(
                path -> assertThat(path.resolve("build/images/really-cool-application/bin/jlink-in-disguise")).exists(),
                path -> assertThat(path.resolve("build/images/really-cool-application/bin/jlink-in-disguise.bat")).exists()
        );
        assertThat(build.projectDir).satisfiesAnyOf(
                path -> assertThat(path.resolve("build/images/really-cool-application/bin/jar-in-disguise")).exists(),
                path -> assertThat(path.resolve("build/images/really-cool-application/bin/jar-in-disguise.bat")).exists()
        );
    }

}
