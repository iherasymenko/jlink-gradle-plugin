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
package com.github.iherasymenko.jlink.test;

import com.github.iherasymenko.jlink.test.fixtures.GradleBuild;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;

abstract class AbstractTestBase {
    final GradleBuild build = new GradleBuild();

    @BeforeEach
    final void buildSetUp(TestInfo ti) throws IOException {
        String methodName = ti.getTestMethod()
                .orElseThrow(() -> new AssertionError("Test method should always be present"))
                .getName();
        build.setUp(methodName);
    }

    @AfterEach
    final void buildTearDown() throws IOException {
        build.tearDown();
    }

}
