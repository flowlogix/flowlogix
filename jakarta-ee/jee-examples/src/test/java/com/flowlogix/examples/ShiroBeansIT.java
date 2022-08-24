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
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
public class ShiroBeansIT {
    @Drone
    private WebDriver webDriver;

    @ArquillianResource
    protected URL baseURL;

    @FindBy(id = "form:facesViewScoped")
    private WebElement facesViewScoped;

    @FindBy(id = "form:omniViewScoped")
    private WebElement omniViewScoped;

    @FindBy(id = "form:sessionScoped")
    private WebElement sessionScoped;

    @FindBy(id = "form:stateless")
    private WebElement stateless;

    @FindBy(id = "form:unprotected")
    private WebElement unprotectedMethod;

    @FindBy(id = "form:protected")
    private WebElement protectedMethod;

    @FindBy(id = "invalidate")
    private WebElement invalidateSession;

    @FindBy(id = "form:messages")
    private WebElement messages;

    @FindBy(id = "form:uname")
    private WebElement username;

    @FindBy(id = "form:pwd")
    private WebElement password;

    @FindBy(id = "form:login")
    private WebElement login;

    @FindBy(id = "usingWebSessions")
    private WebElement usingWebSessions;

    @BeforeEach
    void deleteAllCookies() {
        webDriver.manage().deleteAllCookies();
        webDriver.get(baseURL + "api/statistics/clear");
    }

    @Test
    void checkDontCallWhenNotAuth() {
        webDriver.get(baseURL + "shiro/unprotected/hello");
        guardAjax(facesViewScoped).click();
        assertTrue(messages.getText().startsWith("view scope unauth: Attempting to perform a user-only operation"),
                "anonymous user should get an exception");
        guardAjax(omniViewScoped).click();
        assertTrue(messages.getText().startsWith("omni view scope unauth: Attempting to perform a user-only operation"),
                "anonymous user should get an exception");
        guardAjax(sessionScoped).click();
        assertTrue(messages.getText().startsWith("session scoped unauth: Attempting to perform a user-only operation"),
                "anonymous user should get an exception");
        guardAjax(stateless).click();
        assertTrue(messages.getText().startsWith("stateless bean unauth: Attempting to perform a user-only operation"),
                "anonymous user should get an exception");
        guardAjax(unprotectedMethod).click();
        assertEquals("unprotected method: hello from unprotected", messages.getText());
        guardAjax(protectedMethod).click();
        assertTrue(messages.getText().startsWith("protected unauth: Attempting to perform a user-only operation"),
                "anonymous user should get an exception");
    }

    @Test
    void checkCallWhenAuth() {
        webDriver.get(baseURL + "shiro/auth/loginform");
        login();

        webDriver.get(baseURL + "shiro/unprotected/hello");
        guardAjax(facesViewScoped).click();
        assertTrue(messages.getText().startsWith("Hello from FacesViewScoped"));
        guardAjax(omniViewScoped).click();
        assertTrue(messages.getText().startsWith("Hello from OmniViewScoped"));
        guardAjax(sessionScoped).click();
        assertTrue(messages.getText().startsWith("Hello from SessionScoped"));
        guardAjax(stateless).click();
        assertTrue(messages.getText().startsWith("Hello from ProtectedStatelessBean"));
        guardAjax(protectedMethod).click();
        assertEquals("protected method: hello from protected", messages.getText());
    }

    @Test
    void beanDestroyCalled() {
        exersizeViewAndSessionScoped(facesViewScoped, "api/statistics/pc_fv", "api/statistics/pd_fv", true);
        webDriver.get(baseURL + "api/statistics/clear");
        exersizeViewAndSessionScoped(omniViewScoped, "api/statistics/pc_ofv", "api/statistics/pd_ofv", false);
    }

    private void exersizeViewAndSessionScoped(WebElement elem, String createStatistic, String destroyStatistic,
            boolean isBrokenDestructor) {
        webDriver.get(baseURL + "shiro/auth/loginform");
        login();

        webDriver.get(baseURL + "shiro/unprotected/hello");
        boolean webSessions = Boolean.parseBoolean(usingWebSessions.getText());
        guardAjax(elem).click();
        webDriver.navigate().refresh();
        guardAjax(elem).click();
        guardAjax(sessionScoped).click();

        invalidateSession.click();
        waitGui(webDriver).until(ExpectedConditions.alertIsPresent());
        webDriver.switchTo().alert().accept();
        webDriver.get(baseURL + createStatistic);
        assertEquals("2", webDriver.findElement(By.tagName("body")).getText());
        webDriver.get(baseURL + destroyStatistic);
        assertEquals(isBrokenDestructor && webSessions ? "1" : "2", webDriver.findElement(By.tagName("body")).getText());
        webDriver.get(baseURL + "api/statistics/pc_ss");
        assertEquals("1", webDriver.findElement(By.tagName("body")).getText());
        webDriver.get(baseURL + "api/statistics/pd_ss");
        assertEquals("1", webDriver.findElement(By.tagName("body")).getText());
    }

    private void login() {
        username.sendKeys("webuser");
        password.sendKeys("webpwd");
        guardHttp(login).click();
    }

    @Deployment(testable = false, name = "DevMode")
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(MavenImporter.class, "shiro-auth-forms.war")
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
        new ShrinkWrapManipulator().webXmlXPath(archive, getStandardActions());
        return archive;
    }
}
