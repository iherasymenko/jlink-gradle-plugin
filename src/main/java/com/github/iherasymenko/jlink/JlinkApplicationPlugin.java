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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
            jlinkApplication.getAddOptions().convention(project.provider(javaApplication::getApplicationDefaultJvmArgs));
            jlinkApplication.getApplicationName().convention(project.provider(javaApplication::getApplicationName));
            jlinkApplication.getMainModule().convention(javaApplication.getMainModule());
            jlinkApplication.getMainClass().convention(javaApplication.getMainClass());
        });

        plugins.withType(JavaPlugin.class, javaPlugin -> {
            Consumer<JlinkImageTask> defaultImageTaskSettings = task -> {
                task.setGroup(BasePlugin.BUILD_GROUP);
                task.getModulePath().convention(project.files(tasks.named(JavaPlugin.JAR_TASK_NAME), project.getConfigurations().named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)));
                task.getAddModules().convention(jlinkApplication.getAddModules().zip(jlinkApplication.getMainModule(), (addModules, mainModule) -> {
                    List<String> out = new ArrayList<>();
                    out.add(mainModule);
                    out.addAll(addModules);
                    return out;
                }));
                Provider<Map<String, String>> mainLauncherProvider = project.provider(() -> Map.of(jlinkApplication.getApplicationName().get(), jlinkApplication.getMainModule().get() + "/" + jlinkApplication.getMainClass().get()));
                task.getLauncher().convention(jlinkApplication.getLauncher().zip(mainLauncherProvider, (launcher, mainLauncher) -> {
                    Map<String, String> out = new LinkedHashMap<>();
                    out.putAll(mainLauncher);
                    out.putAll(launcher);
                    return out;
                }));
                task.getAddOptions().convention(jlinkApplication.getAddOptions());

                task.getBindServices().convention(jlinkApplication.getBindServices());
                task.getCompress().convention(jlinkApplication.getCompress());
                task.getDisablePlugin().convention(jlinkApplication.getDisablePlugin());
                task.getNoHeaderFiles().convention(jlinkApplication.getNoHeaderFiles());
                task.getNoManPages().convention(jlinkApplication.getNoManPages());
                task.getStripDebug().convention(jlinkApplication.getStripDebug());
                task.getVerbose().convention(jlinkApplication.getVerbose());
                task.getDedupLegalNoticesErrorIfNotSameContent().convention(jlinkApplication.getDedupLegalNoticesErrorIfNotSameContent());
                task.getGenerateCdsArchive().convention(jlinkApplication.getGenerateCdsArchive());
                task.getExcludeFiles().convention(jlinkApplication.getExcludeFiles());
                task.getExcludeResources().convention(jlinkApplication.getExcludeResources());
                task.getIncludeLocales().convention(jlinkApplication.getIncludeLocales());
                task.getStripJavaDebugAttributes().convention(jlinkApplication.getStripJavaDebugAttributes());
                task.getStripNativeCommands().convention(jlinkApplication.getStripNativeCommands());
                task.getLimitModules().convention(jlinkApplication.getLimitModules());
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

            DependencyHandler dependencies = project.getDependencies();
            Attribute<Boolean> extractedArchive = registerExtractTransform(dependencies);

            jlinkImages.all(image -> {
                String capitalizedName = image.getCapitalizedName();
                Configuration conf = project.getConfigurations().create("jdkArchive" + capitalizedName, it -> it.getAttributes().attribute(extractedArchive, true));
                dependencies.addProvider(conf.getName(), image.getDependencyClassifier());

                TaskProvider<JlinkImageTask> crossTargetImage = tasks.register("image" + capitalizedName, JlinkImageTask.class, task -> {
                    Provider<Directory> outputFolder = project.getLayout()
                            .getBuildDirectory()
                            .map(it -> it.dir(IMAGES_OUTPUT_FOLDER))
                            .map(it -> it.dir(image.name));

                    task.setDescription("Builds a jlink image using the JDK for " + image.name);
                    task.getOutputFolder().convention(outputFolder);
                    task.getCrossTargetJdk().convention(project.getLayout().dir(project.provider(() -> project.files(conf).getSingleFile())));
                    defaultImageTaskSettings.accept(task);
                });
                tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(crossTargetImage));
            });
        });
    }

    private static Attribute<Boolean> registerExtractTransform(DependencyHandler dependencies) {
        Attribute<Boolean> extractedArchive = Attribute.of("extracted", Boolean.class);
        Attribute<String> artifactType = Attribute.of("artifactType", String.class);

        dependencies.getArtifactTypes().maybeCreate("zip").getAttributes().attribute(extractedArchive, false);
        dependencies.getArtifactTypes().maybeCreate("tar.gz").getAttributes().attribute(extractedArchive, false);

        dependencies.registerTransform(ExtractJdkTransform.class, transform -> {
            transform.getFrom().attribute(artifactType, "zip").attribute(extractedArchive, false);
            transform.getTo().attribute(artifactType, "zip").attribute(extractedArchive, true);
        });

        dependencies.registerTransform(ExtractJdkTransform.class, transform -> {
            transform.getFrom().attribute(artifactType, "tar.gz").attribute(extractedArchive, false);
            transform.getTo().attribute(artifactType, "tar.gz").attribute(extractedArchive, true);
        });

        return extractedArchive;
    }

}
