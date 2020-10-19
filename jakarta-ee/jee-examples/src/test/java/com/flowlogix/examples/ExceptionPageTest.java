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

import com.flowlogix.test.UserInterfaceTest;
import java.net.URL;
import java.util.List;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author lprimak
 */
@RunWith(Arquillian.class)
@Category(UserInterfaceTest.class)
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


    @Before
    public void before() {
        webDriver.manage().deleteAllCookies();
        webDriver.get(baseURL + "exception-pages.xhtml");
        waitGui(webDriver);
    }

    @Test
    @OperateOnDeployment("DevMode")
    public void closedByInterrupted() {
        guardAjax(closedByIntrButton).click();
        assertEquals("Exception happened", exceptionHeading.getText());
        assertEquals("Exception type: class java.nio.channels.ClosedByInterruptException", exceptionTypeField.getText());
    }

    @Test
    @OperateOnDeployment("DevMode")
    public void invalidSession() {
        invalidateSession.click();
        webDriver.switchTo().alert().accept();
        noAction.click();
        assertEquals("Logged Out", isExpired.getText());
        noAction.click();
        assertEquals("Logged In", isExpired.getText());
    }

    @Test
    @OperateOnDeployment("DevMode")
    public void lateSqlThrow() {
        guardAjax(lateSqlThrow).click();
        assertEquals("Exception happened", exceptionHeading.getText());
        assertEquals("Exception type: class java.sql.SQLException", exceptionTypeField.getText());
    }

    @Test
    @OperateOnDeployment("DevMode")
    public void versionsOnDev() {
        versions("end of page");
    }

    @Test
    @OperateOnDeployment("ProdMode")
    public void versionsOnProd() {
        versions("end of page - minimized");
    }

    void versions(String expected) {
        assertEquals(expected, endOfPage.getText());
        List<WebElement> scripts = webDriver.findElements(By.tagName("script"));
        int count = 0;
        for(WebElement script : scripts) {
            String href = script.getAttribute("src");
            if (StringUtils.isBlank(href)) {
                continue;
            }
            assertTrue("not versioned", href.contains("v="));
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
            assertTrue("not versioned", href.contains("v="));
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
