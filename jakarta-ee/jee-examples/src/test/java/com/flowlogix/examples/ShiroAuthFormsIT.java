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
import static com.flowlogix.util.ShrinkWrapManipulator.isClientStateSavingIntegrationTest;
import static com.flowlogix.util.ShrinkWrapManipulator.isShiroNativeSessionsIntegrationTest;
import java.net.URL;
import static org.apache.shiro.web.servlet.ShiroHttpSession.DEFAULT_SESSION_ID_NAME;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.jboss.arquillian.graphene.Graphene.waitForHttp;
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
public class ShiroAuthFormsIT {
    @Drone
    private WebDriver webDriver;

    @ArquillianResource
    protected URL baseURL;

    @FindBy(id = "form:uname")
    private WebElement username;

    @FindBy(id = "form:pwd")
    private WebElement password;

    @FindBy(id = "form:login")
    private WebElement login;

    @FindBy(id = "form:rememberMe")
    private WebElement rememberMe;

    @FindBy(id = "form:logout")
    private WebElement logout;

    @FindBy(id = "firstForm:firstName")
    private WebElement firstName;

    @FindBy(id = "firstForm:lastName")
    private WebElement lastName;

    @FindBy(id = "firstForm:submitFirst")
    private WebElement submitFirst;

    @FindBy(id = "secondForm:address")
    private WebElement address;

    @FindBy(id = "secondForm:city")
    private WebElement city;

    @FindBy(id = "secondForm:submitSecond")
    private WebElement submitSecond;

    @FindBy(id = "secondForm:messages")
    private WebElement secondFormMessages;

    @FindBy(id = "invalidate")
    private WebElement invalidateSession;

    @FindBy(id = "messages")
    private WebElement messages;

    @FindBy(id = "sessionExpiredMessage")
    private WebElement sessionExpiredMessage;

    @BeforeEach
    void deleteAllCookies() {
        webDriver.manage().deleteAllCookies();
    }

    @Test
    void protectedPageWithLogin() {
        webDriver.get(baseURL + "shiro/protected");
        assertTrue(webDriver.getCurrentUrl().contains("shiro/auth"), "redirect to login");
        login();
        assertEquals("Protected Page", webDriver.getTitle());
    }

    @Test
    void checkLogout() {
        webDriver.get(baseURL + "shiro/protected");
        login();
        guardHttp(logout).click();
        assertTrue(webDriver.getCurrentUrl().contains("shiro/auth"), "redirect to login");
        login();
        assertEquals("Protected Page", webDriver.getTitle());
    }

    @Test
    void rememberMe() {
        webDriver.get(baseURL + "shiro/protected");
        if (!rememberMe.isSelected()) {
            rememberMe.click();
        }
        login();
        webDriver.manage().deleteCookieNamed(isShiroNativeSessionsIntegrationTest() ?
                "native_session_cookie" : DEFAULT_SESSION_ID_NAME);
        webDriver.navigate().refresh();
        assertEquals("Protected Page", webDriver.getTitle());
        guardHttp(logout).click();
        assertTrue(webDriver.getCurrentUrl().contains("shiro/auth"), "redirect to login");
    }

    @Test
    void unauthorized() {
        webDriver.get(baseURL + "shiro/adminpage");
        login();
        assertEquals("Unauthorized", webDriver.getTitle());
        guardHttp(logout).click();
        assertEquals(baseURL + "index", webDriver.getCurrentUrl());
        webDriver.get(baseURL + "shiro/adminpage");
        username.sendKeys("admin");
        password.sendKeys("adminpwd");
        guardHttp(login).click();
        assertEquals("Admin Page", webDriver.getTitle());
    }

    @Test
    void incorrectLoginOnce() {
        webDriver.get(baseURL + "shiro/protected");
        username.sendKeys("webuser");
        password.sendKeys("wrongpwd");
        guardHttp(login).click();
        assertEquals("Incorrect Login", messages.getText());
        login();
        assertEquals("Protected Page", webDriver.getTitle());
    }

    @Test
    void nonAjaxSessionExpired() {
        webDriver.get(baseURL + "shiro/form");
        login();
        invalidateSession.click();
        waitGui(webDriver).until(ExpectedConditions.alertIsPresent());
        webDriver.switchTo().alert().accept();
        firstName.sendKeys("Jack");
        lastName.sendKeys("Frost");
        guardHttp(submitFirst).click();
        assertEquals("Your Session Has Expired", sessionExpiredMessage.getText());
    }

    @Test
    void nonAjaxResubmit() {
        nonAjaxSessionExpired();
        login();
        assertEquals("Form Submitted - firstName: Jack, lastName: Frost", messages.getText());
    }

    @Test
    void nonAjaxResubmitAfterFailedLogin() {
        nonAjaxSessionExpired();
        username.sendKeys("xxx");
        password.sendKeys("yyy");
        guardHttp(login).click();
        login();
        assertEquals("Form Submitted - firstName: Jack, lastName: Frost", messages.getText());
    }

    @Test
    void nonAjaxRememberedResubmit() {
        webDriver.get(baseURL + "shiro/form");
        rememberMe.click();
        login();
        invalidateSession.click();
        waitGui(webDriver).until(ExpectedConditions.alertIsPresent());
        webDriver.switchTo().alert().accept();
        firstName.sendKeys("Jack");
        lastName.sendKeys("Frost");
        guardHttp(submitFirst).click();
        assertEquals("Form Submitted - firstName: Jack, lastName: Frost", messages.getText());
    }

    @Test
    void ajaxSessionExpired() {
        webDriver.get(baseURL + "shiro/form");
        login();
        invalidateSession.click();
        waitGui(webDriver).until(ExpectedConditions.alertIsPresent());
        webDriver.switchTo().alert().accept();
        address.sendKeys("1 Houston Street");
        city.sendKeys("New York");
        waitForHttp(submitSecond).click();
        assertEquals("Your Session Has Expired", sessionExpiredMessage.getText());
    }

    @Test
    void ajaxResubmit() {
        ajaxSessionExpired();
        login();
        assertEquals("2nd Form Submitted - Address: 1 Houston Street, City: New York",
                secondFormMessages.getText());
        address.sendKeys("Workshop");
        city.sendKeys("North Pole");
        guardAjax(submitSecond).click();
        assertEquals("2nd Form Submitted - Address: Workshop, City: North Pole",
                secondFormMessages.getText());
    }

    @Test
    void ajaxRememberedResubmit() {
        webDriver.get(baseURL + "shiro/form");
        rememberMe.click();
        login();
        address.sendKeys("1 Houston Street");
        city.sendKeys("New York");
        invalidateSession.click();
        waitGui(webDriver).until(ExpectedConditions.alertIsPresent());
        webDriver.switchTo().alert().accept();
        if (isClientStateSavingIntegrationTest()) {
            guardAjax(submitSecond).click();
            address.clear();
            city.clear();
        } else {
            waitForHttp(submitSecond).click();
        }
        assertEquals("2nd Form Submitted - Address: 1 Houston Street, City: New York",
                secondFormMessages.getText());
        address.sendKeys("Workshop");
        city.sendKeys("North Pole");
        invalidateSession.click();
        waitGui(webDriver).until(ExpectedConditions.alertIsPresent());
        webDriver.switchTo().alert().accept();
        if (isClientStateSavingIntegrationTest()) {
            guardAjax(submitSecond).click();
            address.clear();
            city.clear();
        } else {
            waitForHttp(submitSecond).click();
        }
        assertEquals("2nd Form Submitted - Address: Workshop, City: North Pole",
                secondFormMessages.getText());
        address.sendKeys("LAX Airport");
        city.sendKeys("Los Angeles");
        guardAjax(submitSecond).click();
        assertEquals("2nd Form Submitted - Address: LAX Airport, City: Los Angeles",
                secondFormMessages.getText());
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
