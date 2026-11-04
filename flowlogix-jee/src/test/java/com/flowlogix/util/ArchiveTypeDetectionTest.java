/*
 * Copyright (C) 2011-2026 Flow Logix, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowlogix.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class ArchiveTypeDetectionTest {
    @ParameterizedTest
    @CsvSource({"war, WebArchive", "jar, JavaArchive", "ejb, JavaArchive",
            "ear, EnterpriseArchive", "rar, ResourceAdapterArchive"})
    void detectArchiveType(String packaging, String expectedType, @TempDir Path tempDir) throws IOException {
        var pom = writePom(tempDir, packaging);
        assertThat(ShrinkWrapManipulator.detectArchiveType(pom).getSimpleName()).isEqualTo(expectedType);
    }

    @Test
    void defaultsToJar(@TempDir Path tempDir) throws IOException {
        assertThat(ShrinkWrapManipulator.detectArchiveType(writePom(tempDir, null))).isEqualTo(JavaArchive.class);
    }

    @Test
    void unsupportedPackaging(@TempDir Path tempDir) throws IOException {
        var pom = writePom(tempDir, "pom");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> ShrinkWrapManipulator.detectArchiveType(pom))
                .withMessageContaining("unsupported packaging 'pom'");
    }

    private static Path writePom(Path dir, String packaging) throws IOException {
        var packagingElement = packaging == null ? "" : "<packaging>%s</packaging>".formatted(packaging);
        return Files.writeString(dir.resolve("pom.xml"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1</version>
                    %s
                </project>
                """.formatted(packagingElement));
    }
}
