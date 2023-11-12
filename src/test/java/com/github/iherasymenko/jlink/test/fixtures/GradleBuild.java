package com.github.iherasymenko.jlink.test.fixtures;

import org.gradle.testkit.runner.GradleRunner;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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
        return GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir.toFile())
                .withArguments(arguments);
    }

    public void tearDown() throws IOException {
        if (this.projectDir != null) {
            Files.walkFileTree(this.projectDir, new SimpleFileVisitor<Path>() {
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
