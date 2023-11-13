package com.github.iherasymenko.jlink.test;

import com.github.iherasymenko.jlink.test.fixtures.Text;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

final class ImageFunctionalTest extends AbstractTestBase {

    boolean jdk12Requested() {
        return System.getenv("JAVA_HOME_12_X64") != null;
    }

    @Test
    @DisabledIf("jdk12Requested")
    void can_add_jvm_args_to_the_image_via_application_extension() throws IOException {
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
    @DisabledIf("jdk12Requested")
    void can_add_jvm_args_to_the_image_via_jlink_application_extension() throws IOException {
        build.buildFile = """
                plugins {
                	id 'java'
                	id 'io.github.iherasymenko.jlink-application'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
                jlinkApplication {
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
    void jlink_configuration_can_be_specified_via_jlink_application_extension() throws IOException {
        build.buildFile = """
                plugins {
                	id 'java'
                	id 'io.github.iherasymenko.jlink-application'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
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
                	id 'io.github.iherasymenko.jlink-application'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
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
                	id 'io.github.iherasymenko.jlink-application'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
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
                	id 'io.github.iherasymenko.jlink-application'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
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
    void can_disable_a_jlink_plugin() throws IOException {
        build.buildFile = """
                plugins {
                	id 'java'
                	id 'io.github.iherasymenko.jlink-application'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'
                                
                jlinkApplication {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                	applicationDefaultJvmArgs = ["-Dtest_arg1=hello", "-Dtest_arg2=world"]
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
        BuildResult buildResult = build.runner("imageRun")
                .build();
        String[] taskOutput = Text.linesBetweenTags(buildResult.getOutput(), "> Task :imageRun", "BUILD SUCCESSFUL");
        assertThat(taskOutput).hasSize(2);
        assertThat(taskOutput[0]).isEqualTo("Arg1: null");
        assertThat(taskOutput[1]).isEqualTo("Arg2: null");
    }

}
