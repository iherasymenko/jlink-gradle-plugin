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
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;

public abstract class JlinkImage {
    final String name;

    @Inject
    public JlinkImage(String name) {
        this.name = Objects.requireNonNull(name, "name");
        if (this.name.isBlank()) {
            throw new IllegalArgumentException("'name' must not be blank");
        }
    }

    public abstract Property<String> getJdkArchive();

    public abstract Property<String> getGroup();

    Provider<Map<String, String>> getDependencyClassifier() {
        return getJdkArchive().zip(getGroup(), ((jdkArchiveName, group) -> {
            String ext;
            String fileName;
            if (jdkArchiveName.endsWith(".zip")) {
                ext = "zip";
                fileName = jdkArchiveName.substring(0, jdkArchiveName.length() - ".zip".length());
            } else if (jdkArchiveName.endsWith(".tar.gz")) {
                ext = "tar.gz";
                fileName = jdkArchiveName.substring(0, jdkArchiveName.length() - ".tar.gz".length());
            } else {
                throw new GradleException("Unsupported archive format: " + jdkArchiveName);
            }
            return Map.of("group", group, "name", fileName, "ext", ext);
        }));
    }

    String getCapitalizedName() {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

}
