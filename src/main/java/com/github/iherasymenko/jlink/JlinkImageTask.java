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
package com.github.iherasymenko.jlink;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@CacheableTask
public abstract class JlinkImageTask extends DefaultTask {

    public JlinkImageTask() {
        JavaToolchainSpec toolchain = getProject()
                .getExtensions()
                .getByType(JavaPluginExtension.class)
                .getToolchain();
        Provider<JavaLauncher> defaultLauncher = getJavaToolchainService().launcherFor(toolchain);
        getJavaLauncher().convention(defaultLauncher);
    }

    @Nested
    public abstract Property<JavaLauncher> getJavaLauncher();

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getCrossTargetJdk();

    @Classpath
    public abstract Property<FileCollection> getModulePath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputFolder();

    @Input
    public abstract Property<String> getMainModule();

    @Input
    @Optional
    public abstract Property<Boolean> getNoManPages();

    @Input
    @Optional
    public abstract Property<Boolean> getNoHeaderFiles();

    @Input
    @Optional
    public abstract Property<Boolean> getBindServices();

    @Input
    @Optional
    public abstract Property<String> getCompress();

    @Input
    @Optional
    public abstract Property<Boolean> getVerbose();

    @Input
    @Optional
    public abstract Property<Boolean> getStripDebug();

    @Input
    public abstract ListProperty<String> getJvmArgs();

    @Input
    public abstract ListProperty<String> getDisablePlugins();

    @Input
    public abstract MapProperty<String, String> getLaunchers();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Inject
    public abstract JavaToolchainService getJavaToolchainService();

    @Inject
    public abstract ExecOperations getExecOperations();

    @TaskAction
    public void execute() throws IOException {
        var modulePathEntries = getModulePath()
                .get()
                .getFiles();

        var modulePath = Stream.concat(Stream.ofNullable(resolveCrossTargetJmodsFolder()), modulePathEntries.stream())
                .map(File::getAbsolutePath)
                .sorted()
                .collect(joining(File.pathSeparator));

        List<String> args = new ArrayList<>();
        args.addAll(List.of("--module-path", modulePath));
        args.addAll(List.of("--output", getOutputFolder().get().toString()));
        args.addAll(List.of("--add-modules", getMainModule().get()));
        if (getNoHeaderFiles().getOrElse(false)) {
            args.add("--no-header-files");
        }
        if (getNoManPages().getOrElse(false)) {
            args.add("--no-man-pages");
        }
        if (getBindServices().getOrElse(false)) {
            args.add("--bind-services");
        }
        if (getCompress().isPresent()) {
            args.addAll(List.of("--compress", getCompress().get()));
        }
        if (getVerbose().getOrElse(false)) {
            args.add("--verbose");
        }
        if (getStripDebug().getOrElse(false)) {
            args.add("--strip-debug");
        }
        String jvmArgsLine = String.join(" ", getJvmArgs().get());
        if (!jvmArgsLine.isEmpty()) {
            args.add("--add-options=" + jvmArgsLine);
        }
        for (var entry : getLaunchers().get().entrySet()) {
            args.addAll(List.of("--launcher", entry.getKey() + "=" + entry.getValue()));
        }
        for (var plugin : getDisablePlugins().get()) {
            args.addAll(List.of("--disable-plugin", plugin));
        }
        File jlink = getJavaLauncher().get()
                .getMetadata()
                .getInstallationPath()
                .file("bin/jlink")
                .getAsFile();
        getLogger().debug("jlink executable: {}, args: {}", jlink, args);
        getFileSystemOperations().delete(spec -> spec.delete(getOutputFolder().get()));
        getExecOperations().exec(spec-> spec.args(args).executable(jlink));
    }

    private File resolveCrossTargetJmodsFolder() throws IOException {
        if (!getCrossTargetJdk().isPresent()) {
            return null;
        }
        Path directory = getCrossTargetJdk().get().getAsFile().toPath();
        try (var walker = Files.walk(directory)) {
            List<Path> releaseFiles = walker.filter(path -> path.getFileName().toString().equals("release"))
                    .collect(Collectors.toList());
            for (Path releaseFile : releaseFiles) {
                try (InputStream is = Files.newInputStream(releaseFile)) {
                    var props = new Properties();
                    props.load(is);
                    var javaVersion = props.getProperty("JAVA_VERSION");
                    var osName = props.getProperty("OS_NAME");
                    var osArch = props.getProperty("OS_ARCH");
                    if (javaVersion != null) {
                        Path jdkRoot = releaseFile.getParent();
                        getLogger().debug("Resolved cross target JDK: {}, {}/{} in {}", javaVersion, osName, osArch, jdkRoot);
                        return jdkRoot.resolve("jmods").toFile();
                    }
                } catch (Exception e) {
                    getLogger().debug("Cannot read 'release' file", e);
                }
            }
        }
        throw new GradleException("Cannot find a valid 'release' file in " + directory + " or any of its subdirectories");
    }

}
