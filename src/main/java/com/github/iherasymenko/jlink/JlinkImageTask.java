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
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.*;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;
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
import java.util.*;
import java.util.spi.ToolProvider;
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

    @Input
    @Optional
    public abstract Property<Boolean> getDedupLegalNoticesErrorIfNotSameContent();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Inject
    public abstract JavaToolchainService getJavaToolchainService();

    @Inject
    public abstract ExecOperations getExecOperations();

    @TaskAction
    public void execute() throws IOException {
        Set<File> modulePathEntries = getModulePath()
                .get()
                .getFiles();

        String modulePath = Stream.concat(resolveCrossTargetJmodsFolder(), modulePathEntries.stream())
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
        if (getDedupLegalNoticesErrorIfNotSameContent().getOrElse(false)) {
            args.addAll(List.of("--dedup-legal-notices", "error-if-not-same-content"));
        }
        String jvmArgsLine = String.join(" ", getJvmArgs().get());
        if (!jvmArgsLine.isEmpty()) {
            args.add("--add-options=" + jvmArgsLine);
        }
        for (Map.Entry<String, String> entry : getLaunchers().get().entrySet()) {
            args.addAll(List.of("--launcher", entry.getKey() + "=" + entry.getValue()));
        }
        for (String plugin : getDisablePlugins().get()) {
            args.addAll(List.of("--disable-plugin", plugin));
        }
        getFileSystemOperations().delete(spec -> spec.delete(getOutputFolder().get()));
        invokeJlink(args);
    }

    private void invokeJlink(List<String> args) {
        JavaInstallationMetadata javaInstallation = getJavaLauncher().get().getMetadata();

        File currentJavaHome = new File(System.getProperty("java.home")).getAbsoluteFile();
        File toolchainJavaHome = javaInstallation.getInstallationPath().getAsFile().getAbsoluteFile();

        //TODO: Replace with javaInstallation.isCurrentJvm() when we drop support of Gradle versions < 8
        if (currentJavaHome.equals(toolchainJavaHome)) {
            ToolProvider jlink = ToolProvider.findFirst("jlink")
                    .orElseThrow(() -> new GradleException("The JDK does not bundle jlink"));
            getLogger().debug("Running jlink via the ToolProvider");
            int errorCode = jlink.run(System.out, System.err, args.toArray(String[]::new));
            if (errorCode != 0) {
                throw new GradleException("jlink finished with non-zero exit value " + errorCode);
            }
        } else {
            getLogger().debug("Running jlink via the binary");
            File jlink = javaInstallation.getInstallationPath()
                    .file("bin/jlink")
                    .getAsFile();
            getExecOperations().exec(spec-> spec.args(args).executable(jlink));
        }
    }

    private Stream<File> resolveCrossTargetJmodsFolder() throws IOException {
        if (!getCrossTargetJdk().isPresent()) {
            return Stream.empty();
        }
        Path directory = getCrossTargetJdk().get().getAsFile().toPath();
        try (Stream<Path> walker = Files.walk(directory)) {
            List<Path> releaseFiles = walker.filter(path -> path.getFileName().toString().equals("release"))
                    .collect(Collectors.toList());
            for (Path releaseFile : releaseFiles) {
                try (InputStream is = Files.newInputStream(releaseFile)) {
                    Properties props = new Properties();
                    props.load(is);
                    String javaVersion = props.getProperty("JAVA_VERSION");
                    String osName = props.getProperty("OS_NAME");
                    String osArch = props.getProperty("OS_ARCH");
                    if (javaVersion != null) {
                        Path jdkRoot = releaseFile.getParent();
                        getLogger().debug("Resolved cross target JDK: {}, {}/{} in {}", javaVersion, osName, osArch, jdkRoot);
                        return Stream.of(jdkRoot.resolve("jmods").toFile());
                    }
                } catch (Exception e) {
                    getLogger().debug("Cannot read 'release' file", e);
                }
            }
        }
        throw new GradleException("Cannot find a valid 'release' file in " + directory + " or any of its subdirectories");
    }

}
