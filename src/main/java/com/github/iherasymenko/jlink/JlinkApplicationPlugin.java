package com.github.iherasymenko.jlink;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.List;
import java.util.Map;

@NonNullApi
@SuppressWarnings("unused")
public class JlinkApplicationPlugin implements Plugin<Project> {

    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        TaskContainer tasks = project.getTasks();

        JlinkApplicationPluginExtension jlinkApplication = project.getExtensions().create("jlinkApplication", JlinkApplicationPluginExtension.class);

        plugins.withType(ApplicationPlugin.class, applicationPlugin -> {
            JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
            jlinkApplication.getApplicationDefaultJvmArgs().convention(javaApplication.getApplicationDefaultJvmArgs());
            jlinkApplication.getMainModule().convention(javaApplication.getMainModule());
            jlinkApplication.getMainClass().convention(javaApplication.getMainClass());
            jlinkApplication.getApplicationName().convention(javaApplication.getApplicationName());
        });

        plugins.withType(JavaPlugin.class, javaPlugin -> {
            TaskProvider<JlinkImageTask> imageTask = tasks.register("image", JlinkImageTask.class, task -> {
                Provider<Directory> outputFolder = project.getLayout()
                        .getBuildDirectory()
                        .map(it -> it.dir("images"))
                        .flatMap(it -> it.dir(jlinkApplication.getApplicationName()));
                task.setGroup(BasePlugin.BUILD_GROUP);
                task.setDescription("Builds a jlink image using the current JDK");
                task.getOutputFolder().convention(outputFolder);
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
            });

            TaskProvider<JavaExec> imageRunTask = tasks.register("imageRun", JavaExec.class, task -> {
                DirectoryProperty outputFolder = imageTask.get().getOutputFolder();
                task.getInputs().dir(outputFolder);
                task.setGroup(ApplicationPlugin.APPLICATION_GROUP);
                task.setDescription("Runs the project as a JVM application bundled with jlink");
                task.executable(outputFolder.file("bin/java").get());
                task.getMainClass().set(jlinkApplication.getMainClass());
                task.getMainModule().set(jlinkApplication.getMainModule());
            });

            tasks.register("imageModules", Exec.class, task -> {
                DirectoryProperty outputFolder = imageTask.get().getOutputFolder();
                task.getInputs().dir(outputFolder);
                task.setGroup(HelpTasksPlugin.HELP_GROUP);
                task.setDescription("Displays modules of the project JVM application bundled with jlink");
                task.executable(outputFolder.file("bin/java").get());
                task.setArgs(List.of("--list-modules"));
            });

        });
    }

}
