package com.github.iherasymenko.jlink.test;

import com.github.iherasymenko.jlink.test.fixtures.GradleBuild;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;

abstract class AbstractTestBase {
    final GradleBuild build = new GradleBuild();

    @AfterEach
    final void buildTearDown() throws IOException {
        build.tearDown();
    }

}
