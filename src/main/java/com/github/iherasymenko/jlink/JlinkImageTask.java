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
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Optional;
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
import java.util.*;
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
    public abstract DirectoryProperty getOutput();

    @Input
    public abstract ListProperty<String> getAddModules();

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
    public abstract ListProperty<String> getAddOptions();

    @Input
    public abstract ListProperty<String> getDisablePlugin();

    @Input
    public abstract MapProperty<String, String> getLauncher();

    @Input
    @Optional
    public abstract Property<Boolean> getDedupLegalNoticesErrorIfNotSameContent();

    @Input
    @Optional
    public abstract Property<Boolean> getGenerateCdsArchive();

    @Input
    public abstract ListProperty<String> getExcludeFiles();

    @Input
    public abstract ListProperty<String> getExcludeResources();

    @Input
    public abstract ListProperty<String> getIncludeLocales();

    @Input
    @Optional
    public abstract Property<Boolean> getStripJavaDebugAttributes();

    @Input
    @Optional
    public abstract Property<Boolean> getStripNativeCommands();

    @Input
    public abstract ListProperty<String> getLimitModules();

    @Input
    @Optional
    public abstract Property<String> getVm();

    @Input
    @Optional
    public abstract Property<String> getEndian();

    @Input
    @Optional
    public abstract Property<String> getVendorBugUrl();

    @Input
    @Optional
    public abstract Property<String> getVendorVersion();

    @Input
    @Optional
    public abstract Property<String> getVendorVmBugUrl();

    @Input
    @Optional
    public abstract Property<Boolean> getIgnoreSigningInformation();

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
        args.addAll(List.of("--output", getOutput().get().toString()));
        String addModules = String.join(",", getAddModules().get());
        if (!addModules.isEmpty()) {
            args.addAll(List.of("--add-modules", addModules));
        }
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
        if (getGenerateCdsArchive().getOrElse(false)) {
            args.add("--generate-cds-archive");
        }
        String addOptions = String.join(" ", getAddOptions().get());
        if (!addOptions.isEmpty()) {
            args.add("--add-options=" + addOptions);
        }
        for (Map.Entry<String, String> entry : getLauncher().get().entrySet()) {
            args.addAll(List.of("--launcher", entry.getKey() + "=" + entry.getValue()));
        }
        for (String plugin : getDisablePlugin().get()) {
            args.addAll(List.of("--disable-plugin", plugin));
        }
        String excludeFilesPatterns = String.join(",", getExcludeFiles().get());
        if (!excludeFilesPatterns.isEmpty()) {
            args.addAll(List.of("--exclude-files", excludeFilesPatterns));
        }
        String excludeResources = String.join(",", getExcludeResources().get());
        if (!excludeResources.isEmpty()) {
            args.addAll(List.of("--exclude-resources", excludeResources));
        }
        String includeLocales = String.join(",", getIncludeLocales().get());
        if (!includeLocales.isEmpty()) {
            args.addAll(List.of("--include-locales", includeLocales));
        }
        if (getStripJavaDebugAttributes().getOrElse(false)) {
            args.add("--strip-java-debug-attributes");
        }
        if (getStripNativeCommands().getOrElse(false)) {
            args.add("--strip-native-commands");
        }
        String limitModules = String.join(",", getLimitModules().get());
        if (!limitModules.isEmpty()) {
            args.addAll(List.of("--limit-modules", limitModules));
        }
        if (getVm().isPresent()) {
            args.addAll(List.of("--vm", getVm().get()));
        }
        if (getEndian().isPresent()) {
            args.addAll(List.of("--endian", getEndian().get()));
        }
        if (getVendorBugUrl().isPresent()) {
            args.addAll(List.of("--vendor-bug-url", getVendorBugUrl().get()));
        }
        if (getVendorVersion().isPresent()) {
            args.addAll(List.of("--vendor-version", getVendorVersion().get()));
        }
        if (getVendorVmBugUrl().isPresent()) {
            args.addAll(List.of("--vendor-vm-bug-url", getVendorVmBugUrl().get()));
        }
        if (getIgnoreSigningInformation().getOrElse(false)) {
            args.add("--ignore-signing-information");
        }
        getFileSystemOperations().delete(spec -> spec.delete(getOutput().get()));
        RegularFile jlink = getJavaLauncher()
                .get()
                .getMetadata()
                .getInstallationPath()
                .dir("bin")
                .file(Os.jlinkBinaryName());
        getExecOperations().exec(spec -> spec.args(args).executable(jlink));
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
                        getLogger().info("Resolved cross target JDK: {}, {}/{} in {}", javaVersion, osName, osArch, jdkRoot);
                        Path jmodsFolder = jdkRoot.resolve("jmods");
                        if (!Files.exists(jmodsFolder)) {
                            throw new GradleException("jmods directory is not found. Cross-linking is not available with the given distribution. See https://openjdk.org/jeps/493 for details.");
                        }
                        return Stream.of(jmodsFolder.toFile());
                    }
                } catch (IOException e) {
                    getLogger().info("Cannot read 'release' file", e);
                }
            }
        }
        throw new GradleException("Cannot find a valid 'release' file in " + directory + " or any of its subdirectories");
    }

}
