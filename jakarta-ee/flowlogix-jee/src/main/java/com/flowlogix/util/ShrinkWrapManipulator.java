/*
 * Copyright 2022 lprimak.
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
public class ShrinkWrapManipulator {
    private final Lazy<DocumentBuilder> builder = new Lazy<>(this::createDocumentBuilder);
    private final Lazy<Transformer> transformer = new Lazy<>(this::createTransformer);

    private static final @Getter List<Action> standardActions = initializeStandardActions();

    @RequiredArgsConstructor
    public static class Action {
        private final String path;
        private final Consumer<Node> func;
    }

    /**
     * modified web.xml according to xpath and method
     *
     * @param archive to modify
     * @param actions list of actions to perform
     */
    @SneakyThrows
    public void webXmlXPath(WebArchive archive, List<Action> actions) {
        Document webXml;
        try ( InputStream strm = archive.get("WEB-INF/web.xml").getAsset().openStream()) {
            webXml = builder.get().parse(strm);
        }
        var xpath = XPathFactory.newInstance().newXPath();
        for (Action action : actions) {
            var expr = xpath.compile(action.path);
            Node node = (Node) expr.evaluate(webXml, XPathConstants.NODE);
            action.func.accept(node);
        }
        StringWriter writer = new StringWriter();
        transformer.get().transform(new DOMSource(webXml), new StreamResult(writer));
        String newXmlText = writer.getBuffer().toString();
        archive.setWebXML(new StringAsset(newXmlText));
    }

    private static List<Action> initializeStandardActions() {
        switch (System.getProperty("integration.test.mode")) {
            case "clientStateSaving":
                return List.of(new Action("//web-app/context-param[param-name = 'javax.faces.STATE_SAVING_METHOD']/param-value",
                node -> node.setTextContent("client")));
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
