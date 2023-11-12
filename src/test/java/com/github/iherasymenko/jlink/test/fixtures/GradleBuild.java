package com.github.iherasymenko.jlink.test.fixtures;

import org.gradle.testkit.runner.GradleRunner;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public final class GradleBuild {

    public Path projectDir;

    public String buildFile;

    public String settingsFile;

    public String mainClass;

    public String moduleInfo;

    public GradleRunner runner(String... arguments) throws IOException {
        this.projectDir = Files.createTempDirectory("gradleBuild");
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/demo"));
        Files.writeString(projectDir.resolve("build.gradle"), buildFile);
        Files.writeString(projectDir.resolve("settings.gradle"), settingsFile);
        Files.writeString(projectDir.resolve("src/main/java/com/example/demo/DemoApplication.java"), mainClass);
        Files.writeString(projectDir.resolve("src/main/java/module-info.java"), moduleInfo);

        List<String> args = new ArrayList<>(List.of(arguments));
        String javaHomeOverrideVar = System.getenv("JAVA_HOME_OVERRIDE_VAR");
        if (javaHomeOverrideVar != null) {
            String javaHome = System.getenv("JAVA_HOME");
            String javaHomeOverride = System.getenv(javaHomeOverrideVar);
            if (javaHomeOverride == null) {
                throw new AssertionError("No %s environment variable is set".formatted(javaHomeOverrideVar));
            }
            if (!javaHome.equals(javaHomeOverride)) {
                args.add("-Dorg.gradle.java.home=" + javaHomeOverride);
            }
        }
        return GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir.toFile())
                .withArguments(args);
    }

    public void tearDown() throws IOException {
        if (this.projectDir != null) {
            Files.walkFileTree(this.projectDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

}
