package com.github.iherasymenko.jlink;

import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.net.URI;
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

    public abstract Property<URI> getUrl();

    public abstract Property<String> getChecksum();

    public abstract Property<String> getChecksumAlgorithm();

}
