/*
 * Copyright (C) 2011-2024 Flow Logix, Inc. All Rights Reserved.
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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import static com.flowlogix.util.ShrinkWrapManipulator.DEFAULT_SSL_PORT;
import static com.flowlogix.util.ShrinkWrapManipulator.DEFAULT_SSL_PROPERTY;
import static com.flowlogix.util.ShrinkWrapManipulator.runActionOnNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
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
            when(mavenImporter.loadPomFromFile(any(String.class)).importBuildOutput()
                    .as(any())).thenReturn(javaArchive);
            ShrinkWrapManipulator.createDeployment(JavaArchive.class);
            shrinkWrap.verify(() -> ShrinkWrap.create(eq(MavenImporter.class), endsWith(".jar")));
        }
    }

    @Test
    void createDeploymentWithNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> ShrinkWrapManipulator.createDeployment(JavaArchive.class, (String) null));
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
    void actionOnNodeWhenNull() {
        runActionOnNode(new Action("path", null, true), null);
        verifyNoMoreInteractions(documentBuilder, documentBuilderFactory, transformerFactory);
    }

    private static int getPortFromProperty() {
        return Integer.parseInt(System.getProperty(DEFAULT_SSL_PROPERTY, String.valueOf(DEFAULT_SSL_PORT)));
    }
}
