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
package com.flowlogix.examples;

import static com.flowlogix.util.JakartaTransformerUtils.jakartify;
import com.flowlogix.test.PayaraServerLifecycle;
import com.flowlogix.util.ShrinkWrapManipulator;
import com.flowlogix.util.ShrinkWrapManipulator.Action;
import static com.flowlogix.util.ShrinkWrapManipulator.getContextParamValue;
import java.net.URL;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Condition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static com.flowlogix.util.ShrinkWrapManipulator.logArchiveContents;
import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.waitForHttp;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 *
 * @author lprimak
 */
@Slf4j
@Tag("UserInterface")
@RunAsClient
@PayaraServerLifecycle
class ExceptionPageIT {
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

    @FindBy(id = "form:printWarning")
    private WebElement printWarning;

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
        assertThat(exceptionHeading.getText()).isEqualTo("Exception happened");
        assertThat(exceptionTypeField.getText())
                .isEqualTo("Exception type: class java.nio.channels.ClosedByInterruptException");
        assertThat(getLastException(true)).isEmpty();
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void invalidSession() {
        invalidateSession.click();
        waitGui(webDriver).until(ExpectedConditions.alertIsPresent());
        webDriver.switchTo().alert().accept();
        if (!Boolean.parseBoolean(stateSaving.getText())) {
            waitForHttp(noAction).click();
            assertThat(isExpired.getText()).isEqualTo("Logged Out");
        }
        guardAjax(noAction).click();
        assertThat(isExpired.getText()).isEqualTo("Logged In");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void lateSqlThrow() {
        guardAjax(lateSqlThrow).click();
        assertThat(exceptionHeading.getText()).isEqualTo("Exception happened");
        assertThat(exceptionTypeField.getText()).isEqualTo("Exception type: class java.sql.SQLException");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void sqlThrowFromFacesMethod() {
        guardAjax(methodSqlThrow).click();
        assertThat(exceptionHeading.getText()).isEqualTo("Exception happened");
        assertThat(exceptionTypeField.getText()).isEqualTo("Exception type: class java.sql.SQLException");
        String exceptionString = getLastException();
        while (exceptionString.startsWith("WARNING: java.io.IOException: Connection is closed")) {
            exceptionString = exceptionString.lines().skip(1).findFirst().orElseGet(this::getLastException);
        }
        assertThat(exceptionString).
                as(String.format("exceptionBean.throwExceptionFromMethod() - exception string <%s> doesn't match",
                        exceptionString)).is(anyOf(new Condition<>(String::isEmpty, "empty exception"),
                        new Condition<>(log -> log.matches(jakartify("""
                                ^WARNING: javax.faces.FacesException: #\\{exceptionBean.throwExceptionFromMethod\\(\\)\\}: .*""")
                                + "java.sql.SQLException: sql-from-method$".replaceAll("\\.", "\\.")),
                                "exception log warning")));
        if (!exceptionString.isEmpty()) {
            fetchExceptionPage();
            guardAjax(printWarning).click();
        }
    }

    private String getLastException() {
        return getLastException(false);
    }

    private String getLastException(boolean checkForInterrupt) {
        webDriver.get(baseURL + "lastException");
        String exceptionText =  webDriver.findElement(By.tagName("body")).getText();
        if (checkForInterrupt && exceptionText.startsWith("WARNING: java.io.IOException: Connection is closed")) {
            exceptionText = "";
        }
        return exceptionText;
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
        assertThat(endOfPage.getText()).isEqualTo(expected);
        List<WebElement> scripts = webDriver.findElements(By.tagName("script"));
        int count = 0;
        for (WebElement script : scripts) {
            String href = script.getDomAttribute("src");
            if (StringUtils.isBlank(href)) {
                continue;
            }
            assertThat(href).as("not versioned").contains("v=");
            ++count;
        }
        assertThat(count).isEqualTo(3);

        count = 0;
        List<WebElement> csses = webDriver.findElements(By.tagName("link"));
        for (WebElement css : csses) {
            String href = css.getDomAttribute("href");
            if (StringUtils.isBlank(href)) {
                continue;
            }
            assertThat(href).as("not versioned").contains("v=");
            ++count;
        }
        assertThat(count).isEqualTo(2);
    }

    @Deployment(name = DEPLOYMENT_DEV_MODE)
    static WebArchive createDeploymentDev() {
        return createDeployment("");
    }

    static WebArchive createDeployment(String suffix) {
        WebArchive archive = ShrinkWrapManipulator.createDeployment(WebArchive.class,
                        name -> StringUtils.isBlank(suffix) ? name : String.format("%s-%s", name, suffix))
                .addClass(DaoHelperIT.class)
                .addClass(DataModelBackendIT.class);
        return logArchiveContents(archive, log::debug);
    }

    @Deployment(name = DEPLOYMENT_PROD_MODE)
    static WebArchive createDeploymentProd() {
        var productionList = List.of(new Action(getContextParamValue("jakarta.faces.PROJECT_STAGE"),
                node -> node.setTextContent("Production")));
        return new ShrinkWrapManipulator().webXmlXPath(createDeployment("prod"), productionList);
    }
}
