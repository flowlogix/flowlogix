/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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

import static com.flowlogix.examples.ExceptionPageIT.DEPLOYMENT_DEV_MODE;
import static org.assertj.core.api.Assertions.assertThat;
import java.net.URL;
import com.flowlogix.test.PayaraServerLifecycle;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.waitAjax;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 *
 * @author lprimak
 */
@Tag("UserInterface")
@RunAsClient
@PayaraServerLifecycle
class DataModelIT {
    @Drone
    WebDriver webDriver;

    @ArquillianResource
    URL baseURL;
    @ArquillianResource
    JavascriptExecutor jsExecutor;

    @FindBy(id = "model1")
    private WebElement firstTable;

    @FindBy(id = "model1:fullNameHeader:filter")
    private WebElement fullNameFilterInput;

    @FindBy(id = "model1:0:fullNameRow")
    private WebElement firstRowFullName;

    @FindBy(id = "model1:0:userIdRow")
    private WebElement firstRowUserId;

    @FindBy(id = "model1:userIdHeader")
    private WebElement userIdHeader;

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void checkPage() {
        webDriver.get(baseURL + "view-users");
        waitGui(webDriver).until(ExpectedConditions.titleIs("View Users"));
        assertThat(webDriver.getTitle()).isEqualTo("View Users");
        // sort
        guardAjax(userIdHeader.findElement(By.className("ui-column-title"))).click();
        assertThat(firstRowUserId.getText()).isEqualTo("anya");
        WebElement scrollable = firstTable.findElement(By.className("ui-datatable-scrollable-body"));
        jsExecutor.executeScript("arguments[0].scroll(0, 500);", scrollable);
        waitAjax(webDriver).until().element(firstRowFullName).text().equalTo("Friendly Pal");
        assertThat(firstRowFullName.getText()).isEqualTo("Friendly Pal");
        jsExecutor.executeScript("arguments[0].scroll(0, 0);", scrollable);
        waitAjax(webDriver).until().element(firstRowFullName).text().equalTo("Lovely Daughter");
        assertThat(firstRowFullName.getText()).isEqualTo("Lovely Daughter");
        fullNameFilterInput.sendKeys("ly l");
        guardAjax(fullNameFilterInput).sendKeys(Keys.RETURN);
        waitAjax(webDriver).until().element(firstRowFullName).text().equalTo("Lovely Lady");
        assertThat(firstRowFullName.getText()).isEqualTo("Lovely Lady");
    }

    @Deployment(name = DEPLOYMENT_DEV_MODE)
    static WebArchive createDeployment() {
        return ExceptionPageIT.createDeploymentDev();
    }
}
