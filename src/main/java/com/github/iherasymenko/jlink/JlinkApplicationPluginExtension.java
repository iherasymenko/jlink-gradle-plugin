package com.github.iherasymenko.jlink;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class JlinkApplicationPluginExtension {

    public abstract Property<String> getMainModule();

    public abstract Property<String> getMainClass();

    public abstract ListProperty<String> getApplicationDefaultJvmArgs();

    public abstract Property<String> getApplicationName();

    public abstract ListProperty<String> getDisablePlugins();

    public abstract Property<Boolean> getBindServices();

    public abstract Property<String> getCompress();

    public abstract Property<Boolean> getNoHeaderFiles();

    public abstract Property<Boolean> getNoManPages();

    public abstract Property<Boolean> getStripDebug();

    public abstract Property<Boolean> getVerbose();

}
