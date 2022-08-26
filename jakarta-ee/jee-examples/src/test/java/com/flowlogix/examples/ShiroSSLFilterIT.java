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
package com.flowlogix.examples;

import com.flowlogix.util.ShrinkWrapManipulator;
import static com.flowlogix.util.ShrinkWrapManipulator.getStandardActions;
import static com.flowlogix.util.ShrinkWrapManipulator.toHttpsURL;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

/**
 *
 * @author lprimak
 */
@ExtendWith(ArquillianExtension.class)
@Tag("UserInterface")
public class ShiroSSLFilterIT {
    @Drone
    private WebDriver webDriver;

    @ArquillianResource
    protected URL baseURL;

    @Test
    @OperateOnDeployment("ProdMode")
    void checkNonSSL() {
        assertThrows(WebDriverException.class, () -> webDriver.get(baseURL + "shiro/unprotected/hello"));
    }

    @Test
    @OperateOnDeployment("ProdMode")
    void checkSSL() {
        webDriver.get(toHttpsURL(baseURL) + "shiro/unprotected/hello");
        assertEquals("Hello Unprotected", webDriver.getTitle());
    }

    @Deployment(testable = false, name = "ProdMode")
    public static WebArchive createDeploymentProdMode() {
        WebArchive archive = ShrinkWrap.create(MavenImporter.class, "ShiroSSLFilterTest-prod.war")
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
        var productionList = List.of(new ShrinkWrapManipulator.Action("//web-app/context-param[param-name = 'javax.faces.PROJECT_STAGE']/param-value",
                node -> node.setTextContent("Production")));
        new ShrinkWrapManipulator().webXmlXPath(archive, Stream.concat(productionList.stream(),
                getStandardActions().stream()).collect(Collectors.toList()));
        return archive;
    }
}
