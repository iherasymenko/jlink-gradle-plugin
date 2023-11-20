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

import org.gradle.api.GradleException;
import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;

@NonNullApi
@DisableCachingByDefault(because = "Not worth caching")
public abstract class ExtractJdkTransform implements TransformAction<TransformParameters.None> {

    @InputArtifact
    protected abstract Provider<FileSystemLocation> getInputArtifact();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Inject
    protected abstract ArchiveOperations getArchiveOperations();

    @Override
    public void transform(TransformOutputs outputs) {
        File jdkArchive = getInputArtifact().get().getAsFile();
        String fileName = jdkArchive.getName();
        File destPath = outputs.dir(fileName);
        FileTree tree;
        if (fileName.endsWith(".zip")) {
            tree = getArchiveOperations().zipTree(jdkArchive);
        } else if (fileName.endsWith(".tar.gz")) {
            tree = getArchiveOperations().tarTree(jdkArchive);
        } else {
            throw new GradleException("Unsupported archive format: " + fileName);
        }
        getFileSystemOperations().sync(spec -> spec.from(tree).into(destPath));
    }

}
