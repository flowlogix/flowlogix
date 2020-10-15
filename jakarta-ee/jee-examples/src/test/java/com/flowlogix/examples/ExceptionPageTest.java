/*
 * Copyright 2020 lprimak.
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

import com.flowlogix.test.ArquillianTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author lprimak
 */
@RunWith(Arquillian.class)
@Category(ArquillianTest.class)
public class ExceptionPageTest {
    @Drone
    private WebDriver webDriver;

    @FindBy(id = "exception")
    private WebElement exceptionHeading;

    @FindBy(id = "exceptionType")
    private WebElement exceptionTypeField;

    @FindBy(id = "form:closeByIntr")
    private WebElement closedByIntrButton;


    @Test
    public void closedByInterrupted() {
        webDriver.get("http://localhost:8080/exctest" + "/exception-pages.xhtml");
        waitGui(webDriver);
        guardAjax(closedByIntrButton).click();
        assertEquals("Exception happened", exceptionHeading.getText());
        assertEquals("Exception type: class java.nio.channels.ClosedByInterruptException", exceptionTypeField.getText());
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(MavenImporter.class, "exctest.war")
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
    }
}
