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

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class JlinkApplicationPluginExtension {

    public abstract Property<String> getMainModule();

    public abstract Property<String> getMainClass();

    public abstract ListProperty<String> getAddOptions();

    public abstract Property<String> getApplicationName();

    public abstract ListProperty<String> getDisablePlugins();

    public abstract Property<Boolean> getBindServices();

    public abstract Property<String> getCompress();

    public abstract Property<Boolean> getNoHeaderFiles();

    public abstract Property<Boolean> getNoManPages();

    public abstract Property<Boolean> getStripDebug();

    public abstract Property<Boolean> getVerbose();

    public abstract Property<Boolean> getDedupLegalNoticesErrorIfNotSameContent();

    public abstract Property<Boolean> getGenerateCdsArchive();

    public abstract ListProperty<String> getExcludeFiles();

    public abstract ListProperty<String> getExcludeJmodSection();

}
