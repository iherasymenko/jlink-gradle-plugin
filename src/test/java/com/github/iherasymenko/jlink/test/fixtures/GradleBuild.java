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
package com.github.iherasymenko.jlink.test.fixtures;

import org.gradle.testkit.runner.GradleRunner;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.ArrayList;
import java.util.List;

public final class GradleBuild {

    public Path projectDir;

    public String buildFile;

    public String settingsFile;

    public String mainClass;

    public String moduleInfo;

    public GradleRunner runner(String... arguments) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java/com/example/demo"));
        Files.writeString(projectDir.resolve("build.gradle"), buildFile);
        Files.writeString(projectDir.resolve("settings.gradle"), settingsFile);
        Files.writeString(projectDir.resolve("src/main/java/com/example/demo/DemoApplication.java"), mainClass);
        Files.writeString(projectDir.resolve("src/main/java/module-info.java"), moduleInfo);

        List<String> args = new ArrayList<>(List.of(arguments));
        args.add("--configuration-cache");
        return GradleRunner.create()
                .withDebug(isDebug())
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir.toFile())
                .withArguments(args);
    }

    public void setUp(String methodName) throws IOException {
        this.projectDir = Files.createDirectories(Path.of("build/functional-tests/" + methodName + System.currentTimeMillis()));
    }

    public void tearDown() throws IOException {
        if (this.projectDir != null && !isDebug()) {
            Files.walkFileTree(this.projectDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        DosFileAttributeView dos = Files.getFileAttributeView(file, DosFileAttributeView.class);
                        if (dos != null) {
                            dos.setReadOnly(false);
                        }
                    } catch (IOException ignored) { }
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

    boolean isDebug() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
    }

}
