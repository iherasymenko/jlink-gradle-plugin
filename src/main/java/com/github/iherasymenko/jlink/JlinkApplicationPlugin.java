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

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@NonNullApi
@SuppressWarnings("unused")
public class JlinkApplicationPlugin implements Plugin<Project> {

    private static final String IMAGES_OUTPUT_FOLDER = "images";

    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        TaskContainer tasks = project.getTasks();

        JlinkApplicationPluginExtension jlinkApplication = project.getExtensions().create("jlinkApplication", JlinkApplicationPluginExtension.class);
        jlinkApplication.getApplicationName().convention(project.provider(project::getName));

        NamedDomainObjectContainer<JlinkImage> jlinkImages = project.container(JlinkImage.class, name -> project.getObjects().newInstance(JlinkImage.class, name));
        project.getExtensions().add("jlinkImages", jlinkImages);

        plugins.withType(ApplicationPlugin.class, applicationPlugin -> {
            JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
            jlinkApplication.getApplicationDefaultJvmArgs().convention(project.provider(javaApplication::getApplicationDefaultJvmArgs));
            jlinkApplication.getApplicationName().convention(project.provider(javaApplication::getApplicationName));
            jlinkApplication.getMainModule().convention(javaApplication.getMainModule());
            jlinkApplication.getMainClass().convention(javaApplication.getMainClass());
        });

        plugins.withType(JavaPlugin.class, javaPlugin -> {
            Consumer<JlinkImageTask> defaultImageTaskSettings = task -> {
                task.setGroup(BasePlugin.BUILD_GROUP);
                task.getModulePath().convention(project.files(tasks.named(JavaPlugin.JAR_TASK_NAME), project.getConfigurations().named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)));

                task.getMainModule().convention(jlinkApplication.getMainModule());
                task.getJvmArgs().convention(jlinkApplication.getApplicationDefaultJvmArgs());

                task.getBindServices().convention(jlinkApplication.getBindServices());
                task.getCompress().convention(jlinkApplication.getCompress());
                task.getDisablePlugins().convention(jlinkApplication.getDisablePlugins());
                task.getLaunchers().convention(project.provider(() -> Map.of(jlinkApplication.getApplicationName().get(), jlinkApplication.getMainModule().get() + "/" + jlinkApplication.getMainClass().get())));
                task.getNoHeaderFiles().convention(jlinkApplication.getNoHeaderFiles());
                task.getNoManPages().convention(jlinkApplication.getNoManPages());
                task.getStripDebug().convention(jlinkApplication.getStripDebug());
                task.getVerbose().convention(jlinkApplication.getVerbose());
            };

            TaskProvider<JlinkImageTask> imageTask = tasks.register("image", JlinkImageTask.class, task -> {
                Provider<Directory> outputFolder = project.getLayout()
                        .getBuildDirectory()
                        .map(it -> it.dir(IMAGES_OUTPUT_FOLDER))
                        .flatMap(it -> it.dir(jlinkApplication.getApplicationName()));
                task.setGroup(BasePlugin.BUILD_GROUP);
                task.setDescription("Builds a jlink image using the current JDK");
                task.getOutputFolder().convention(outputFolder);
                defaultImageTaskSettings.accept(task);
            });

            TaskProvider<JavaExec> imageRunTask = tasks.register("imageRun", JavaExec.class, task -> {
                DirectoryProperty outputFolder = imageTask.get().getOutputFolder();
                task.getInputs().dir(outputFolder);
                task.setGroup(ApplicationPlugin.APPLICATION_GROUP);
                task.setDescription("Runs the project as a JVM application bundled with jlink");
                task.executable(outputFolder.file("bin/java").get());
                task.getMainClass().convention(jlinkApplication.getMainClass());
                task.getMainModule().convention(jlinkApplication.getMainModule());
            });

            tasks.register("imageModules", Exec.class, task -> {
                DirectoryProperty outputFolder = imageTask.get().getOutputFolder();
                task.getInputs().dir(outputFolder);
                task.setGroup(HelpTasksPlugin.HELP_GROUP);
                task.setDescription("Displays modules of the project JVM application bundled with jlink");
                task.executable(outputFolder.file("bin/java").get());
                task.setArgs(List.of("--list-modules"));
            });

            jlinkImages.all(image -> {
                String name = image.name;
                String capitalizedName = Character.toUpperCase(name.charAt(0)) + name.substring(1);

                TaskProvider<DownloadJdkTask> downloadJdkTask = tasks.register("downloadJdk" + capitalizedName, DownloadJdkTask.class, task -> {
                    Provider<String> fileName = image.getUrl()
                            .map(uri -> new File(uri.getPath()).getName());

                    Provider<RegularFile> outputFile = project.getLayout()
                            .getBuildDirectory()
                            .dir("jdks")
                            .flatMap(it -> it.file(fileName));
                    task.setDescription("Downloads the JDK for " + image.name);
                    task.getUrl().convention(image.getUrl());
                    task.getChecksum().convention(image.getChecksum());
                    task.getChecksumAlgorithm().convention(image.getChecksumAlgorithm());
                    task.getOutputFile().convention(outputFile);
                });

                TaskProvider<ExtractJdkTask> extractJdkTask = tasks.register("extractJdk" + capitalizedName, ExtractJdkTask.class, task -> {
                    RegularFileProperty targetFile = downloadJdkTask.get().getOutputFile();
                    Provider<Directory> outputDirectory = project.getLayout()
                            .getBuildDirectory()
                            .dir("jdks-extracted")
                            .map(it -> it.dir(image.name));
                    task.setDescription("Extracts the JDK for " + image.name);
                    task.getJdkArchive().convention(targetFile);
                    task.getOutputDirectory().convention(outputDirectory);
                });

                TaskProvider<JlinkImageTask> crossTargetImage = tasks.register("image" + capitalizedName, JlinkImageTask.class, task -> {
                    Provider<Directory> outputFolder = project.getLayout()
                            .getBuildDirectory()
                            .map(it -> it.dir(IMAGES_OUTPUT_FOLDER))
                            .map(it -> it.dir(image.name));

                    task.setDescription("Builds a jlink image using the JDK for " + image.name);
                    task.getOutputFolder().convention(outputFolder);
                    task.getCrossTargetJdk().convention(extractJdkTask.get().getOutputDirectory());
                    defaultImageTaskSettings.accept(task);
                });
                tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(crossTargetImage));
            });
        });
    }

}
