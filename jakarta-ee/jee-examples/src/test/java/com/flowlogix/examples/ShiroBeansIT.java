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

import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

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


    @BeforeEach
    void deleteAllCookies() {
        webDriver.manage().deleteAllCookies();
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
    }

    private void login() {
        username.sendKeys("webuser");
        password.sendKeys("webpwd");
        guardHttp(login).click();
    }

    @Deployment(testable = false, name = "DevMode")
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(MavenImporter.class, "shiro-auth-forms.war")
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
    }
}
