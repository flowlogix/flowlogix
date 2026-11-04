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

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenWorkingSessionContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomaticDeploymentTest {
    @ParameterizedTest
    @CsvSource({"war, WebArchive", "jar, JavaArchive", "ejb, JavaArchive",
            "ear, EnterpriseArchive", "rar, ResourceAdapterArchive"})
    void detectArchiveType(String packaging, String expectedType) {
        var pom = Path.of("custom/pom.xml");
        var container = mock(MavenWorkingSessionContainer.class, RETURNS_DEEP_STUBS);
        when(container.getMavenWorkingSession().getParsedPomFile().getPackagingType().getId()).thenReturn(packaging);
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class)) {
            manipulator.when(() -> ShrinkWrapManipulator.getMavenSessionContainer(pom)).thenReturn(container);
            manipulator.when(() -> ShrinkWrapManipulator.detectArchiveType(pom)).thenCallRealMethod();
            assertThat(ShrinkWrapManipulator.detectArchiveType(pom).getSimpleName()).isEqualTo(expectedType);
        }
    }

    @Test
    void unsupportedPackaging() {
        var pom = Path.of("custom/pom.xml");
        var container = mock(MavenWorkingSessionContainer.class, RETURNS_DEEP_STUBS);
        when(container.getMavenWorkingSession().getParsedPomFile().getPackagingType().getId()).thenReturn("pom");
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class)) {
            manipulator.when(() -> ShrinkWrapManipulator.getMavenSessionContainer(pom)).thenReturn(container);
            manipulator.when(() -> ShrinkWrapManipulator.detectArchiveType(pom)).thenCallRealMethod();
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> ShrinkWrapManipulator.detectArchiveType(pom))
                    .withMessageContaining("unsupported packaging");
        }
    }

    @Test
    void copiesFilteredAssetsFromConfiguredOutputToResourceContainer() {
        var pom = Path.of("custom/pom.xml");
        var output = new File("custom/test-output");
        var container = mock(MavenWorkingSessionContainer.class, RETURNS_DEEP_STUBS);
        when(container.getMavenWorkingSession().getParsedPomFile().getTestOutputDirectory()).thenReturn(output);
        var importer = mock(ExplodedImporter.class);
        var classes = mock(JavaArchive.class);
        when(importer.importDirectory(output)).thenReturn(importer);
        when(importer.as(JavaArchive.class)).thenReturn(classes);
        var path = mock(ArchivePath.class);
        var node = mock(Node.class);
        var asset = new StringAsset("class bytes");
        when(node.getAsset()).thenReturn(asset);
        when(classes.getContent(any())).thenReturn(Map.of(path, node));
        var archive = mock(WebArchive.class);
        Filter<ArchivePath> classesOnly = entry -> entry.get().endsWith(".class");
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class);
                var shrinkWrap = mockStatic(ShrinkWrap.class);
                var filters = mockStatic(Filters.class)) {
            manipulator.when(() -> ShrinkWrapManipulator.getMavenSessionContainer(pom)).thenReturn(container);
            manipulator.when(() -> ShrinkWrapManipulator.addTestClasses(archive, pom)).thenCallRealMethod();
            shrinkWrap.when(() -> ShrinkWrap.create(ExplodedImporter.class)).thenReturn(importer);
            filters.when(() -> Filters.include(".*\\.class")).thenReturn(classesOnly);

            assertThat(ShrinkWrapManipulator.addTestClasses(archive, pom)).isSameAs(archive);
            verify(archive).addAsResource(asset, path);
            verify(classes).getContent(classesOnly);
            filters.verify(() -> Filters.include(".*\\.class"));
        }
    }

    @Test
    void defaultTestClassesOverloadUsesDefaultPom() {
        var archive = mock(WebArchive.class);
        var pom = ShrinkWrapManipulator.defaultPomFilePath.get();
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class)) {
            manipulator.when(() -> ShrinkWrapManipulator.addTestClasses(archive)).thenCallRealMethod();
            manipulator.when(() -> ShrinkWrapManipulator.addTestClasses(archive, pom)).thenReturn(archive);
            assertThat(ShrinkWrapManipulator.addTestClasses(archive)).isSameAs(archive);
            manipulator.verify(() -> ShrinkWrapManipulator.addTestClasses(archive, pom));
        }
    }

    @Test
    void generatedSuiteDeploymentAddsTestClasses() {
        var archive = mock(WebArchive.class);
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class)) {
            manipulator.when(() -> ShrinkWrapManipulator.createDeployment()).thenReturn(archive);
            manipulator.when(() -> ShrinkWrapManipulator.addTestClasses(archive)).thenReturn(archive);
            var deployments = new MavenDeploymentScenarioGenerator().generate(new TestClass(EmptySuite.class));
            assertThat(deployments).singleElement().satisfies(deployment -> {
                assertThat(deployment.getName()).isEqualTo("_DEFAULT_");
                assertThat(deployment.getArchive()).isSameAs(archive);
            });
            manipulator.verify(() -> ShrinkWrapManipulator.addTestClasses(archive));
        }
    }

    @Test
    void explicitDeploymentIsNotModified() {
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class)) {
            assertThat(new MavenDeploymentScenarioGenerator().generate(new TestClass(ExplicitDeployment.class)))
                    .isEmpty();
            manipulator.verifyNoInteractions();
        }
    }

    @Test
    void onlyMissingNamedDeploymentsAreGenerated() {
        var archive = mock(JavaArchive.class);
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class)) {
            manipulator.when(() -> ShrinkWrapManipulator.createDeployment()).thenReturn(archive);
            manipulator.when(() -> ShrinkWrapManipulator.addTestClasses(archive)).thenReturn(archive);
            var deployments = new MavenDeploymentScenarioGenerator().generate(new TestClass(MixedDeployments.class));
            assertThat(deployments).singleElement().satisfies(deployment -> {
                assertThat(deployment.getName()).isEqualTo("missing");
                assertThat(deployment.getArchive()).isSameAs(archive);
            });
            manipulator.verify(() -> ShrinkWrapManipulator.addTestClasses(archive));
        }
    }

    static class EmptySuite {
    }

    static class ExplicitDeployment {
        @Deployment
        public static JavaArchive deployment() {
            throw new UnsupportedOperationException("Deployment method must not be invoked");
        }
    }

    static class MixedDeployments {
        @Deployment(name = "existing")
        public static JavaArchive deployment() {
            throw new UnsupportedOperationException("Deployment method must not be invoked");
        }

        @OperateOnDeployment("existing")
        void existing() {
        }

        @OperateOnDeployment("missing")
        void missing() {
        }
    }
}
