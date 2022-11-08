/*
 * Copyright (C) 2011-2022 Flow Logix, Inc. All Rights Reserved.
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

import static com.flowlogix.util.JakartaTransformerUtils.jakartify;
import com.flowlogix.util.ShrinkWrapManipulator;
import com.flowlogix.util.ShrinkWrapManipulator.Action;
import static com.flowlogix.util.ShrinkWrapManipulator.getStandardActions;
import static com.flowlogix.util.ShrinkWrapManipulator.isClientStateSavingIntegrationTest;
import static com.flowlogix.util.ShrinkWrapManipulator.isShiroNativeSessionsIntegrationTest;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.waitForHttp;
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
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 *
 * @author lprimak
 */
@ExtendWith(ArquillianExtension.class)
@Tag("UserInterface")
public class ExceptionPageIT {
    static final String DEPLOYMENT_DEV_MODE = "DevMode";
    static final String DEPLOYMENT_PROD_MODE = "ProdMode";

    @Drone
    private WebDriver webDriver;

    @SuppressWarnings("DeclarationOrder")
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

    @FindBy(id = "form:methodSqlThrow")
    private WebElement methodSqlThrow;

    @FindBy(id = "end-of-page")
    private WebElement endOfPage;

    @FindBy(id = "mode")
    private WebElement modeField;

    @FindBy(id = "stateSaving")
    private WebElement stateSaving;

    @BeforeEach
    void fetchExceptionPage() {
        webDriver.get(baseURL + "exception-pages.xhtml");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void closedByInterrupted() {
        guardAjax(closedByIntrButton).click();
        assertEquals("Exception happened", exceptionHeading.getText());
        assertEquals("Exception type: class java.nio.channels.ClosedByInterruptException", exceptionTypeField.getText());
        webDriver.get(baseURL + "lastException");
        assertEquals("", webDriver.findElement(By.tagName("body")).getText());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void invalidSession() {
        invalidateSession.click();
        waitGui(webDriver).until(ExpectedConditions.alertIsPresent());
        webDriver.switchTo().alert().accept();
        if (!Boolean.parseBoolean(stateSaving.getText())) {
            waitForHttp(noAction).click();
            assertEquals("Logged Out", isExpired.getText());
        }
        guardAjax(noAction).click();
        assertEquals("Logged In", isExpired.getText());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void checkStateSavingDev() {
        assertEquals(Boolean.parseBoolean(stateSaving.getText()), isClientStateSavingIntegrationTest());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_PROD_MODE)
    void checkStateSavingProd() {
        assertEquals(Boolean.parseBoolean(stateSaving.getText()), isClientStateSavingIntegrationTest());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void lateSqlThrow() {
        guardAjax(lateSqlThrow).click();
        assertEquals("Exception happened", exceptionHeading.getText());
        assertEquals("Exception type: class java.sql.SQLException", exceptionTypeField.getText());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void sqlThrowFromFacesMethod() {
        guardAjax(methodSqlThrow).click();
        assertEquals("Exception happened", exceptionHeading.getText());
        assertEquals("Exception type: class java.sql.SQLException", exceptionTypeField.getText());
        webDriver.get(baseURL + "lastException");
        assertEquals(jakartify("WARNING: javax.faces.FacesException: #{exceptionBean.throwExceptionFromMethod()}: ")
                + "java.sql.SQLException: sql-from-method", webDriver.findElement(By.tagName("body")).getText());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void versionsOnDev() {
        versions("end of page");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_PROD_MODE)
    void versionsOnProd() {
        versions("end of page - minimized");
    }

    @SuppressWarnings("MagicNumber")
    private void versions(String expected) {
        assertEquals(expected, endOfPage.getText());
        List<WebElement> scripts = webDriver.findElements(By.tagName("script"));
        int count = 0;
        for (WebElement script : scripts) {
            String href = script.getAttribute("src");
            if (StringUtils.isBlank(href)) {
                continue;
            }
            assertTrue(href.contains("v="), "not versioned");
            ++count;
        }
        assertEquals(isShiroNativeSessionsIntegrationTest() ? 4 : 3, count);

        count = 0;
        List<WebElement> csses = webDriver.findElements(By.tagName("link"));
        for (WebElement css : csses) {
            String href = css.getAttribute("href");
            if (StringUtils.isBlank(href)) {
                continue;
            }
            assertTrue(href.contains("v="), "not versioned");
            ++count;
        }
        assertEquals(2, count);
    }

    @Deployment(testable = false, name = DEPLOYMENT_DEV_MODE)
    public static WebArchive createDeploymentDev() {
        return createDeploymentDev("ExceptionPageTest.war");
    }

    static WebArchive createDeploymentDev(String archiveName) {
        WebArchive archive = ShrinkWrap.create(MavenImporter.class, archiveName)
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
        new ShrinkWrapManipulator().webXmlXPath(archive, getStandardActions());
        return archive;
    }

    @Deployment(testable = false, name = DEPLOYMENT_PROD_MODE)
    public static WebArchive createDeploymentProd() {
        return createDeploymentProd("ExceptionPageTest-prod.war");
    }

    static WebArchive createDeploymentProd(String archiveName) {
        WebArchive archive = ShrinkWrap.create(MavenImporter.class, archiveName)
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
        var productionList = List.of(new Action(
                jakartify("//web-app/context-param[param-name = 'javax.faces.PROJECT_STAGE']/param-value"),
                node -> node.setTextContent("Production")));
        new ShrinkWrapManipulator().webXmlXPath(archive, Stream.concat(productionList.stream(),
                getStandardActions().stream()).collect(Collectors.toList()));
        return archive;
    }
}
