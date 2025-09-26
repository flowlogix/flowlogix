/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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

import com.flowlogix.util.ShrinkWrapManipulator.Action;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogManager;
import static com.flowlogix.util.ShrinkWrapManipulator.DEFAULT_SSL_PROPERTY;
import static com.flowlogix.util.ShrinkWrapManipulator.runActionOnNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class ShrinkWrapManipulatorTest {
    @Mock(answer = RETURNS_DEEP_STUBS)
    private MavenImporter mavenImporter;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private JavaArchive javaArchive;
    @Mock
    private DocumentBuilderFactory documentBuilderFactory;
    @Mock
    private DocumentBuilder documentBuilder;
    @Mock
    private TransformerFactory transformerFactory;

    private static class ComparableStringAsset extends StringAsset {
        ComparableStringAsset(String content) {
            super(content);
        }

        @Override
        @SuppressWarnings("checkstyle:EqualsHashCode")
        public boolean equals(Object obj) {
            return obj instanceof StringAsset asset && asset.getSource().equals(this.getSource());
        }

        @Override
        public String toString() {
            return "Content[" + this.getSource() + "]";
        }
    }

    @Test
    void httpsUrl() throws MalformedURLException {
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(URI.create("http://localhost:1234").toURL());
        assertThat(httpsUrl).isEqualTo(URI.create(String.format("https://localhost:%s", getPortFromProperty())).toURL());
    }

    @Test
    void alreadyHttpsUrl() throws MalformedURLException {
        var url = URI.create("https://localhost:1234").toURL();
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(url);
        assertThat(httpsUrl).isSameAs(url);
    }

    @Test
    void withoutPort() throws MalformedURLException {
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(URI.create("http://localhost").toURL());
        assertThat(httpsUrl).isEqualTo(URI.create(String.format("https://localhost:%s", getPortFromProperty())).toURL());
    }

    @Test
    void alreadyHttpsWithoutPort() throws MalformedURLException {
        var url = URI.create("https://localhost").toURL();
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(url);
        assertThat(httpsUrl).isSameAs(url);
    }

    @Test
    void createDeployment() {
        try (var shrinkWrap = mockStatic(ShrinkWrap.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            shrinkWrap.when(() -> ShrinkWrap.create(eq(MavenImporter.class),
                    notNull(String.class))).thenReturn(mavenImporter);
            when(mavenImporter.loadPomFromFile(any(File.class)).importBuildOutput()
                    .as(any())).thenReturn(javaArchive);
            ShrinkWrapManipulator.createDeployment(JavaArchive.class);
            shrinkWrap.verify(() -> ShrinkWrap.create(eq(MavenImporter.class), endsWith(".jar")));
        }
    }

    @Test
    void logManager() {
        ShrinkWrapManipulator.removeMavenWarningsFromLogging();
        AtomicBoolean changed = new AtomicBoolean();
        LogManager.getLogManager().addConfigurationListener(() -> changed.set(true));
        ShrinkWrapManipulator.removeMavenWarningsFromLogging();
        assertThat(changed).isFalse();
        ShrinkWrapManipulator.resetMavenWarningsRemovalFlag();
        System.setProperty("com.flowlogix.maven.resolver.warn", Boolean.TRUE.toString());
        ShrinkWrapManipulator.removeMavenWarningsFromLogging();
        assertThat(changed).isFalse();
        System.setProperty("com.flowlogix.maven.resolver.warn", Boolean.FALSE.toString());
    }

    @Test
    void createDeploymentWithMissingTestContainers() {
        try (var shrinkWrap = mockStatic(ShrinkWrap.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            shrinkWrap.when(() -> ShrinkWrap.create(eq(MavenImporter.class),
                    notNull(String.class))).thenReturn(mavenImporter);
            WebArchive webArchive = mock(WebArchive.class);
            when(mavenImporter.loadPomFromFile(any(File.class)).importBuildOutput()
                    .as(any())).thenReturn(webArchive);
            when(webArchive.addClass(startsWith("com.flowlogix.testcontainers"))).thenThrow(NoClassDefFoundError.class);
            ShrinkWrapManipulator.createDeployment(WebArchive.class);
            shrinkWrap.verify(() -> ShrinkWrap.create(eq(MavenImporter.class), endsWith(".war")));
        }
    }

    @Test
    void createDeploymentWithPomPath() {
        try (var shrinkWrap = mockStatic(ShrinkWrap.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            shrinkWrap.when(() -> ShrinkWrap.create(eq(MavenImporter.class),
                    notNull(String.class))).thenReturn(mavenImporter);
            when(mavenImporter.loadPomFromFile(any(File.class)).importBuildOutput()
                    .as(any())).thenReturn(javaArchive);
            ShrinkWrapManipulator.createDeployment(JavaArchive.class, Path.of("abc.xml"));
            verify(mavenImporter).loadPomFromFile(Path.of("abc.xml").toFile());
            shrinkWrap.verify(() -> ShrinkWrap.create(eq(MavenImporter.class), endsWith(".jar")));
        }
    }

    @Test
    void createDeploymentWithArchiveName() {
        try (var shrinkWrap = mockStatic(ShrinkWrap.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            shrinkWrap.when(() -> ShrinkWrap.create(eq(MavenImporter.class),
                    notNull(String.class))).thenReturn(mavenImporter);
            when(mavenImporter.loadPomFromFile(any(File.class)).importBuildOutput()
                    .as(any())).thenReturn(javaArchive);
            ShrinkWrapManipulator.createDeployment(JavaArchive.class, "abc.jar");
            shrinkWrap.verify(() -> ShrinkWrap.create(eq(MavenImporter.class), eq("abc.jar")));
        }
    }

    @Test
    void createDeploymentWithNullArchiveName() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> ShrinkWrapManipulator.createDeployment(JavaArchive.class, (String) null));
    }

    @Test
    void createDeploymentWithNullArchiveName2() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> ShrinkWrapManipulator.createDeployment(JavaArchive.class, (String) null, Path.of("abc.xml")));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void packageTestRequirements() {
        assertThat(ShrinkWrapManipulator.packageTestRequirements((Archive) null)).isNull();
    }

    @Test
    void logArchiveContents() {
        when(javaArchive.toString(true)).thenReturn("archive-output");
        ShrinkWrapManipulator.logArchiveContents(javaArchive, s -> assertThat(s).isEqualTo("archive-output"));
        verifyNoMoreInteractions(javaArchive);
    }

    @Test
    void payaraClassDelegationTrue() {
        var archive = mock(WebArchive.class);
        when(archive.addAsWebInfResource(any(StringAsset.class), any(String.class))).thenReturn(archive);
        ShrinkWrapManipulator.payaraClassDelegation(archive, true);
        verify(archive).addAsWebInfResource(
                new ComparableStringAsset("<payara-web-app><class-loader delegate=\"true\"/></payara-web-app>"),
                "payara-web.xml");
        verifyNoMoreInteractions(archive);
    }

    @Test
    void payaraClassDelegationFalse() {
        var archive = mock(WebArchive.class);
        when(archive.addAsWebInfResource(any(StringAsset.class), any(String.class))).thenReturn(archive);
        ShrinkWrapManipulator.payaraClassDelegation(archive, false);
        checkPayaraClassDelegationFalse(archive);
        verifyNoMoreInteractions(archive);
    }

    private void checkPayaraClassDelegationFalse(WebArchive archive) {
        verify(archive).addAsWebInfResource(
                new ComparableStringAsset("<payara-web-app><class-loader delegate=\"false\"/></payara-web-app>"),
                "payara-web.xml");
    }

    @Test
    void payaraClassDelegationWarningWrongType() {
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class)) {
            Logger log = mock(Logger.class);
            manipulator.when(ShrinkWrapManipulator::getLogger).thenReturn(log);
            manipulator.when(() -> ShrinkWrapManipulator.payaraClassDelegation(any(), anyBoolean())).thenCallRealMethod();
            ShrinkWrapManipulator.payaraClassDelegation(javaArchive, true);
            verify(log).warn("Cannot add payara-web.xml to non-WebArchive");
            verifyNoMoreInteractions(log);
        }
    }

    @Test
    void packageSlf4j() {
        var archive = mock(WebArchive.class);
        when(archive.addAsWebInfResource(any(StringAsset.class), any(String.class))).thenReturn(archive);
        when(archive.addPackages(anyBoolean(), any(Package.class))).thenReturn(archive);
        ShrinkWrapManipulator.packageSlf4j(archive);
        checkPayaraClassDelegationFalse(archive);
        verify(archive).addPackages(true, Logger.class.getPackage());
        verify(archive).addAsWebInfResource("META-INF/services/org.slf4j.spi.SLF4JServiceProvider",
                "classes/META-INF/services/org.slf4j.spi.SLF4JServiceProvider");
        verifyNoMoreInteractions(archive);
    }

    @Test
    void packageSlf4jWarningWrongType() {
        try (var manipulator = mockStatic(ShrinkWrapManipulator.class)) {
            Logger log = mock(Logger.class);
            manipulator.when(ShrinkWrapManipulator::getLogger).thenReturn(log);
            manipulator.when(() -> ShrinkWrapManipulator.packageSlf4j(any())).thenCallRealMethod();
            ShrinkWrapManipulator.packageSlf4j(javaArchive);
            verify(log).warn("Cannot add SLF4J to non-WebArchive");
            verifyNoMoreInteractions(log);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void invalidUrl() throws MalformedURLException {
        var url = URI.create("http://localhost:1234").toURL();
        try (var uriMock = mockConstruction(URI.class, (uri, context) -> when(uri.toURL())
                .thenThrow(MalformedURLException.class))) {
            assertThatExceptionOfType(MalformedURLException.class).isThrownBy(() -> ShrinkWrapManipulator
                    .toHttpsURL(url, "invalid", 1234));
        }
    }

    @Test
    void invalidXmlBuilder() throws ParserConfigurationException {
        try (var docBuilder = mockStatic(DocumentBuilderFactory.class)) {
            docBuilder.when(DocumentBuilderFactory::newInstance).thenReturn(documentBuilderFactory);
            when(documentBuilderFactory.newDocumentBuilder()).thenThrow(ParserConfigurationException.class);
            assertThatExceptionOfType(ParserConfigurationException.class)
                    .isThrownBy(() -> new ShrinkWrapManipulator().builder.get());
        }
    }

    @Test
    void invalidXmlTransformer() throws TransformerConfigurationException {
        try (var transformer = mockStatic(TransformerFactory.class)) {
            transformer.when(TransformerFactory::newInstance).thenReturn(transformerFactory);
            when(transformerFactory.newTransformer()).thenThrow(TransformerConfigurationException.class);
            assertThatExceptionOfType(TransformerConfigurationException.class)
                    .isThrownBy(() -> new ShrinkWrapManipulator().transformer.get());
        }
    }

    @Test
    void transformXmlForceException() throws ParserConfigurationException, IOException, SAXException {
        try (var docBuilder = mockStatic(DocumentBuilderFactory.class)) {
            docBuilder.when(DocumentBuilderFactory::newInstance).thenReturn(documentBuilderFactory);
            when(documentBuilderFactory.newDocumentBuilder()).thenReturn(documentBuilder);
            when(documentBuilder.parse(any(InputStream.class))).thenThrow(IOException.class);
            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> new ShrinkWrapManipulator().manipulateXml(javaArchive, null, "file"));
        }
    }

    @Test
    void persistenceXmlPath() throws ParserConfigurationException, IOException, SAXException {
        try (var docBuilder = mockStatic(DocumentBuilderFactory.class)) {
            docBuilder.when(DocumentBuilderFactory::newInstance).thenReturn(documentBuilderFactory);
            when(documentBuilderFactory.newDocumentBuilder()).thenReturn(documentBuilder);
            new ShrinkWrapManipulator().persistenceXmlXPath(javaArchive, List.of());
            verify(javaArchive).get("WEB-INF/classes/META-INF/persistence.xml");
            verify(javaArchive).addAsResource(
                    argThat((StringAsset asset) -> asset.getSource()
                                    .equals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>")),
                    eq("META-INF/persistence.xml")
            );
            verifyNoMoreInteractions(javaArchive);
        }
    }

    @Test
    void actionOnNodeWhenNull() {
        runActionOnNode(new Action("path", null, true), null);
        verifyNoMoreInteractions(documentBuilder, documentBuilderFactory, transformerFactory);
    }

    @Test
    void logger() {
        assertThat(ShrinkWrapManipulator.getLogger()).isNotNull();
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static int getPortFromProperty() {
        return Integer.parseInt(System.getProperty(DEFAULT_SSL_PROPERTY, String.valueOf(8181)));
    }
}
