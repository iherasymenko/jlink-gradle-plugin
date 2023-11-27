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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;

import static com.github.iherasymenko.jlink.Os.javaBinaryName;

public abstract class JlinkRunImageTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getImageDirectory();

    @Input
    public abstract Property<String> getMainModule();

    @Input
    public abstract Property<String> getMainClass();

    @Inject
    public abstract ExecOperations getExecOperations();

    @TaskAction
    public void execute() {
        getExecOperations().javaexec(spec -> {
            spec.getMainClass().set(getMainClass());
            spec.getMainModule().set(getMainModule());
            spec.setExecutable(getImageDirectory().get().dir("bin").file(javaBinaryName()));
        });
    }

}
