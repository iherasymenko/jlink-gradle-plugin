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

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class VendorPropsPluginFunctionalTest extends AbstractTestBase {

    @Test
    void can_add_vendor_information() throws IOException {
        build.buildFile = """
                plugins {
                	id 'java'
                	id 'com.github.iherasymenko.jlink'
                }
                                
                group = 'com.example'
                version = '0.0.1-SNAPSHOT'

                java {
                	toolchain {
                		languageVersion = JavaLanguageVersion.of(System.getenv().getOrDefault('TESTING_AGAINST_JDK', '21'))
                		vendor = JvmVendorSpec.AZUL
                	}
                }
                
                dependencies {
                    implementation 'org.ow2.asm:asm-util:9.6'
                }
              
                jlinkApplication {
                	mainClass = 'com.example.demo.DemoApplication'
                	mainModule = 'demo.main'
                	vendorVersion = 'My JRE version 99.9'
                    vendorBugUrl = 'https://github.com/iherasymenko/jlink-gradle-plugin/issues?q=label:bug'
                    vendorVmBugUrl = 'https://github.com/iherasymenko/jlink-gradle-plugin/issues?q=label:vmbug'
                }
              
                """;
        build.settingsFile = """
                rootProject.name = 'demo'
                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                    }
                }
                """;
        build.mainClass = """
                package com.example.demo;
                
                import org.objectweb.asm.util.ASMifier;
                import org.objectweb.asm.ClassReader;
                import org.objectweb.asm.tree.ClassNode;
                import org.objectweb.asm.util.TraceClassVisitor;
                
                import java.io.PrintWriter;
                
                public class DemoApplication {
                
                    public static void main(String[] args) throws Exception {
                        System.out.println("Vendor Version: " + System.getProperty("java.vendor.version"));
                        System.out.println("Vendor Url Bug: " + System.getProperty("java.vendor.url.bug"));
                        // No easy way to read the VENDOR_URL_VM_BUG field unless crashing VM (which is also not an easy task)
                        var classBytes = Class.forName("java.lang.VersionProps").getResourceAsStream("VersionProps.class").readAllBytes();
                        var cr = new ClassReader(classBytes);
                        var tcv = new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out));
                        cr.accept(tcv, 0);
                    }
                
                }
                """;
        build.moduleInfo = """
                module demo.main {
                    requires org.objectweb.asm.tree;
                    requires org.objectweb.asm.util;
                }
                """;

        BuildResult buildResult = build.runner("imageRun").build();
        assertThat(buildResult.getOutput())
                .contains("Vendor Version: My JRE version 99.9")
                .contains("Vendor Url Bug: https://github.com/iherasymenko/jlink-gradle-plugin/issues?q=label:bug")
        .containsIgnoringNewLines(
                """
                methodVisitor.visitLdcInsn("https://github.com/iherasymenko/jlink-gradle-plugin/issues?q=label:vmbug");
                methodVisitor.visitFieldInsn(PUTSTATIC, "java/lang/VersionProps", "VENDOR_URL_VM_BUG", "Ljava/lang/String;");
                """
        );
    }

}
