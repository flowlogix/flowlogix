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

import io.github.artsok.RepeatedIfExceptionsTest;
import java.net.URL;
import java.util.List;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author lprimak
 */
@ExtendWith(ArquillianExtension.class)
@Tag("UserInterface")
public class ExceptionPageTest {
    @Drone
    private WebDriver webDriver;

    @ArquillianResource
    protected URL baseURL;

    @FindBy(id = "exception")
    private WebElement exceptionHeading;

    @FindBy(id = "exceptionType")
    private WebElement exceptionTypeField;

    @FindBy(id = "form:closeByIntr")
    private WebElement closedByIntrButton;

    @FindBy(id = "invalidate")
    private WebElement invalidateSession;

    @FindBy(id = "form:noAction")
    private WebElement noAction;

    @FindBy(id = "isExpired")
    private WebElement isExpired;

    @FindBy(id = "form:lateSqlThrow")
    private WebElement lateSqlThrow;

    @FindBy(id = "end-of-page")
    private WebElement endOfPage;

    @FindBy(id = "mode")
    private WebElement modeField;


    @BeforeEach
    void fetchExceptionPage() {
        webDriver.get(baseURL + "exception-pages.xhtml");
        waitGui(webDriver);
    }

    @Test
    @OperateOnDeployment("DevMode")
    void closedByInterrupted() {
        guardAjax(closedByIntrButton).click();
        assertEquals("Exception happened", exceptionHeading.getText());
        assertEquals("Exception type: class java.nio.channels.ClosedByInterruptException", exceptionTypeField.getText());
        webDriver.get(baseURL + "lastException");
        waitGui(webDriver);
        assertEquals("", webDriver.findElement(By.tagName("body")).getText());
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    @OperateOnDeployment("DevMode")
    void invalidSession() {
        invalidateSession.click();
        waitGui(webDriver);
        webDriver.switchTo().alert().accept();
        guardAjax(noAction).click();
        assertEquals("Logged Out", isExpired.getText());
        guardAjax(noAction).click();
        assertEquals("Logged In", isExpired.getText());
    }

    @Test
    @OperateOnDeployment("DevMode")
    void lateSqlThrow() {
        guardAjax(lateSqlThrow).click();
        assertEquals("Exception happened", exceptionHeading.getText());
        assertEquals("Exception type: class java.sql.SQLException", exceptionTypeField.getText());
    }

    @Test
    @OperateOnDeployment("DevMode")
    void versionsOnDev() {
        versions("end of page");
    }

    @Test
    @OperateOnDeployment("ProdMode")
    void versionsOnProd() {
        versions("end of page - minimized");
    }

    private void versions(String expected) {
        assertEquals(expected, endOfPage.getText());
        List<WebElement> scripts = webDriver.findElements(By.tagName("script"));
        int count = 0;
        for(WebElement script : scripts) {
            String href = script.getAttribute("src");
            if (StringUtils.isBlank(href)) {
                continue;
            }
            assertTrue(href.contains("v="), "not versioned");
            ++count;
        }
        assertEquals(3, count);

        count = 0;
        List<WebElement> csses = webDriver.findElements(By.tagName("link"));
        for(WebElement css : csses) {
            String href = css.getAttribute("href");
            if (StringUtils.isBlank(href)) {
                continue;
            }
            assertTrue(href.contains("v="), "not versioned");
            ++count;
        }
        assertEquals(1, count);
    }

    @Deployment(testable = false, name = "DevMode")
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(MavenImporter.class, "ExceptionPageTest.war")
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
    }

    @Deployment(testable = false, name = "ProdMode")
    public static WebArchive createDeploymentProdMode() {
        WebArchive archive = ShrinkWrap.create(MavenImporter.class, "ExceptionPageTest-prod.war")
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
        archive.setWebXML(archive.get("WEB-INF/web-production.xml").getAsset());
        return archive;
    }
}
