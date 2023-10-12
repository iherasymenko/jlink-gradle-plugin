package com.github.iherasymenko.jlink;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public abstract class JlinkImageTask extends DefaultTask {

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getJdkFolder();

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
    public abstract Property<Integer> getCompress();

    @Input
    public abstract ListProperty<String> getJvmArgs();

    @Input
    public abstract ListProperty<String> getDisablePlugins();

    @Input
    public abstract MapProperty<String, String> getLaunchers();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    protected DirectoryProperty getJavaHome() {
        File javaHome = getProviderFactory()
                .systemProperty("java.home")
                .map(File::new)
                .get();
        return getObjectFactory().directoryProperty().fileValue(javaHome);
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @TaskAction
    public void execute() {
        var jlink = ToolProvider.findFirst("jlink")
                .orElseThrow(() -> new GradleException("The JDK does not bundle jlink"));
        getFileSystemOperations().delete(spec -> spec.delete(getOutputFolder().get()));

        var jmodsProvider = getJdkFolder()
                .dir("jmods")
                .map(Directory::getAsFile);

        var modulePathEntries = getModulePath()
                .get()
                .getFiles();

        var modulePath = Stream.concat(Stream.ofNullable(jmodsProvider.getOrNull()), modulePathEntries.stream())
                .map(File::getAbsolutePath)
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
        if (getCompress().isPresent()) {
            args.addAll(List.of("--compress", getCompress().map(String::valueOf).get()));
        }
        String jvmArgsLine = String.join(" ", getJvmArgs().get());
        if (!jvmArgsLine.isEmpty()) {
            args.add("--add-options= " + jvmArgsLine);
        }
        for (var entry : getLaunchers().get().entrySet()) {
            args.addAll(List.of("--launcher", entry.getKey() + "=" + entry.getValue()));
        }
        for (var plugin : getDisablePlugins().get()) {
            args.addAll(List.of("--disable-plugin", plugin));
        }
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        int exitCode = jlink.run(new PrintWriter(stdout), new PrintWriter(stderr), args.toArray(String[]::new));
        if (exitCode != 0) {
            throw new GradleException("jlink failed with exit code: " + exitCode + "\n" + stdout.toString() + "\n" + stderr.toString());
        }
    }

}
