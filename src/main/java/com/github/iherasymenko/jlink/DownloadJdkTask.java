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
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

@CacheableTask
public abstract class DownloadJdkTask extends DefaultTask {

    @Input
    public abstract Property<URI> getUrl();

    @Input
    public abstract Property<String> getChecksum();

    @Input
    public abstract Property<String> getChecksumAlgorithm();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void execute() throws IOException, InterruptedException, NoSuchAlgorithmException {
        URI downloadUrl = getUrl().get();
        Path outputPath = getOutputFile().get().getAsFile().toPath();
        String checksumAlgorithm = getChecksumAlgorithm().get();
        String expectedChecksum = getChecksum().get();

        HttpClient client = HttpClient.newBuilder()
                .executor(Runnable::run)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(downloadUrl)
                .build();
        var response = client.send(request, new DigestBodyHandler<>(HttpResponse.BodyHandlers.ofFile(outputPath), checksumAlgorithm));
        if (response.statusCode() != 200) {
            throw new GradleException("The download of " + downloadUrl + " failed with status code " + response.statusCode() + ".");
        }
        String actualChecksum = response.body().checksum();
        if (!actualChecksum.equalsIgnoreCase(expectedChecksum)) {
            throw new GradleException("The actual checksum " + actualChecksum + " does not match the expected checksum " + expectedChecksum + " for " + downloadUrl + ".");
        }
    }

}
