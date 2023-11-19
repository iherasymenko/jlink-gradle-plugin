package com.github.iherasymenko.jlink;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.*;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;

@DisableCachingByDefault(because = "DownloadJdkTask is a cacheable task and its output is used as an input for this task")
public abstract class ExtractJdkTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getJdkArchive();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Inject
    protected abstract ArchiveOperations getArchiveOperations();

    @TaskAction
    public void execute() {
        File jdkArchive = getJdkArchive().get().getAsFile();
        Directory destPath = getOutputDirectory().get();
        String fileName = jdkArchive.getName();
        FileTree tree;
        if (fileName.endsWith(".zip")) {
            tree = getArchiveOperations().zipTree(jdkArchive);
        } else if (fileName.endsWith(".tar.gz")) {
            tree = getArchiveOperations().tarTree(jdkArchive);
        } else {
            throw new GradleException("Unsupported archive format: " + fileName);
        }
        getFileSystemOperations().delete(spec -> spec.delete(destPath));
        getFileSystemOperations().copy(spec -> {
            spec.from(tree);
            spec.into(destPath);
        });
    }

}
