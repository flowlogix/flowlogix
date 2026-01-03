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

import static com.flowlogix.util.JakartaTransformerUtils.jakartify;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.omnifaces.util.Lazy;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * modifies web.xml according to xpath and function
 *
 * @author lprimak
 */
@Slf4j
public class ShrinkWrapManipulator {
    public static final String INTEGRATION_TEST_MODE_PROPERTY = "integration.test.mode";
    public static final String CLIENT_STATE_SAVING = "clientStateSaving";
    public static final String SHIRO_NATIVE_SESSIONS = "shiroNativeSessions";
    public static final String SHIRO_EE_DISABLED = "disableShiroEE";

    @RequiredArgsConstructor
    public static class Action {
        private final String path;
        private final Consumer<Node> func;
        private final boolean optional;

        public Action(String path, Consumer<Node> func) {
            this(path, func, false);
        }
    }

    static final String DEFAULT_SSL_PROPERTY = "httpsPort";

    @SuppressWarnings("ConstantName")
    private static final @Getter List<Action> standardActions = initializeStandardActions();

    private final Lazy<DocumentBuilder> builder = new Lazy<>(this::createDocumentBuilder);
    private final Lazy<Transformer> transformer = new Lazy<>(this::createTransformer);

    /**
     * modified web.xml according to xpath and method
     *
     * @param archive to modify
     * @param actions list of actions to perform
     */
    @SneakyThrows
    public void webXmlXPath(WebArchive archive, List<Action> actions) {
        Document webXml;
        try (InputStream strm = archive.get("WEB-INF/web.xml").getAsset().openStream()) {
            webXml = builder.get().parse(strm);
        }
        var xpath = XPathFactory.newInstance().newXPath();
        for (Action action : actions) {
            var expr = xpath.compile(action.path);
            Node node = (Node) expr.evaluate(webXml, XPathConstants.NODE);
            if (node == null && action.optional) {
                log.debug("Optional path {} ignored", action.path);
            } else {
                action.func.accept(node);
            }
        }
        StringWriter writer = new StringWriter();
        transformer.get().transform(new DOMSource(webXml), new StreamResult(writer));
        String newXmlText = writer.getBuffer().toString();
        archive.setWebXML(new StringAsset(newXmlText));
    }

    public static boolean isClientStateSavingIntegrationTest() {
        return CLIENT_STATE_SAVING.equals(System.getProperty(INTEGRATION_TEST_MODE_PROPERTY));
    }

    public static boolean isShiroNativeSessionsIntegrationTest() {
        return SHIRO_NATIVE_SESSIONS.equals(System.getProperty(INTEGRATION_TEST_MODE_PROPERTY));
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
        return toHttpsURL(httpUrl, DEFAULT_SSL_PROPERTY, -1);
    }

    @SneakyThrows
    public static URL toHttpsURL(URL httpUrl, String sslPortPropertyName, int defaultPort) {
        if (httpUrl.getProtocol().endsWith("s")) {
            return httpUrl;
        }
        int sslPort = Integer.getInteger(sslPortPropertyName, defaultPort);
        // try the backup system property
        if (sslPort == defaultPort) {
            sslPort = Integer.getInteger("sslPort", defaultPort);
        }
        return new URI(httpUrl.getProtocol() + "s", null, httpUrl.getHost(), sslPort,
                httpUrl.getPath(), null, null).toURL();
    }

    public static String getContextParamValue(String paramName) {
        return String.format("//web-app/context-param[param-name = '%s']/param-value", paramName);
    }

    private static List<Action> initializeStandardActions() {
        switch (System.getProperty(INTEGRATION_TEST_MODE_PROPERTY, "none")) {
            case CLIENT_STATE_SAVING:
                return List.of(new Action(getContextParamValue(jakartify("javax.faces.STATE_SAVING_METHOD")),
                        node -> node.setTextContent("client")));
            case SHIRO_NATIVE_SESSIONS:
                return List.of(new Action(getContextParamValue("shiroConfigLocations"),
                        node -> node.setTextContent(node.getTextContent()
                                + ",classpath:META-INF/shiro-native-sessions.ini")));
            case SHIRO_EE_DISABLED:
                return List.of(new Action(getContextParamValue("com.flowlogix.shiro.ee.disabled"),
                        node -> node.setTextContent("true"), true),
                        new Action(getContextParamValue("org.apache.shiro.ee.disabled"),
                                node -> node.setTextContent("true"), true));
            default:
                return List.of();
        }
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
