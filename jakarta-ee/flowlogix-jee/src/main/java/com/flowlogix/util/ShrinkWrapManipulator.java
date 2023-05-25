/*
 * Copyright (C) 2011-2023 Flow Logix, Inc. All Rights Reserved.
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

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.omnifaces.util.Lazy;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * modifies xml files inside the archive according to xpath and function
 * <p>
 * <em>Examples:</em>
 * {@snippet class="com.flowlogix.demo.util.ShrinkWrapDemo" region="productionMode"}
 * {@snippet class="com.flowlogix.demo.util.ShrinkWrapDemo" region="persistence"}
 *
 * @author lprimak
 */
@Slf4j
public class ShrinkWrapManipulator {
    @RequiredArgsConstructor
    public static class Action {
        private final String path;
        private final Consumer<Node> func;
        private final boolean optional;

        public Action(String path, Consumer<Node> func) {
            this(path, func, false);
        }
    }

    private final Lazy<DocumentBuilder> builder = new Lazy<>(this::createDocumentBuilder);
    private final Lazy<Transformer> transformer = new Lazy<>(this::createTransformer);

    /**
     * modifies web.xml according to xpath and method
     *
     * @param archive to modify
     * @param actions list of actions to perform
     */
    public void webXmlXPath(WebArchive archive, List<Action> actions) {
        var asset = "WEB-INF/web.xml";
        archive.setWebXML(new StringAsset(manipulateXml(archive, actions, asset)));
    }

    /**
     * modifies persistence.xml according to xpath and method
     *
     * @param archive to modify
     * @param actions list of actions to perform
     */
    public void persistenceXmlXPath(WebArchive archive, List<Action> actions) {
        var asset = "META-INF/persistence.xml";
        archive.addAsResource(new StringAsset(manipulateXml(archive, actions, "WEB-INF/classes/" + asset)), asset);
    }

    /**
     * Transform http to https URL using {@code sslPort} system property,
     * and default port 8181 if system property is not defined
     *
     * @param httpUrl http URL
     * @return https URL
     */
    @SuppressWarnings("MagicNumber")
    public static URL toHttpsURL(URL httpUrl) {
        return toHttpsURL(httpUrl, "sslPort", 8181);
    }

    /**
     * Transform http to https URL using the specified system property and default port,
     * if the system property is not defined
     *
     * @param httpUrl http URL
     * @param sslPortPropertyName
     * @param defaultPort
     * @return https URL
     */
    @SneakyThrows
    public static URL toHttpsURL(URL httpUrl, String sslPortPropertyName, int defaultPort) {
        if (httpUrl.getProtocol().endsWith("//")) {
            return httpUrl;
        }
        int sslPort = Integer.getInteger(sslPortPropertyName, defaultPort);
        return new URL(httpUrl.getProtocol() + "s", httpUrl.getHost(), sslPort, httpUrl.getFile());
    }

    /**
     * Constructs XPath for web.xml context param
     *
     * @param paramName
     * @return XPath for web.xml context param
     */
    public static String getContextParamValue(String paramName) {
        return String.format("//web-app/context-param[param-name = '%s']/param-value", paramName);
    }

    /**
     * Parse XML file from the archive, perform actions to modify the file,
     * and return a string representing the modified XML file

     * @param archive to retrieve the xml file from
     * @param actions to perform on the xml file
     * @param xmlFileName xml file name to retrive from the archive
     * @return string representation of the modified xml file
     */
    @SneakyThrows
    public String manipulateXml(WebArchive archive, List<Action> actions, String xmlFileName) {
        Document xmlDocument;
        try (InputStream strm = archive.get(xmlFileName).getAsset().openStream()) {
            xmlDocument = builder.get().parse(strm);
        }
        var xpath = XPathFactory.newInstance().newXPath();
        for (Action action : actions) {
            var expr = xpath.compile(action.path);
            Node node = (Node) expr.evaluate(xmlDocument, XPathConstants.NODE);
            if (node == null && action.optional) {
                log.debug("Optional path {} ignored", action.path);
            } else {
                action.func.accept(node);
            }
        }
        StringWriter writer = new StringWriter();
        transformer.get().transform(new DOMSource(xmlDocument), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    @SneakyThrows
    private DocumentBuilder createDocumentBuilder() {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    @SneakyThrows
    private Transformer createTransformer() {
        return TransformerFactory.newInstance().newTransformer();
    }
}
