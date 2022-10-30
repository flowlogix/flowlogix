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

import static com.flowlogix.examples.ExceptionPageIT.DEPLOYMENT_DEV_MODE;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author lprimak
 */
@ExtendWith(ArquillianExtension.class)
@Tag("UserInterface")
public class DataModelIT {
    @Drone
    private WebDriver webDriver;

    @SuppressWarnings("DeclarationOrder")
    @ArquillianResource
    protected URL baseURL;

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
        assertEquals("View Users", webDriver.getTitle());
        // sort
        guardAjax(userIdHeader.findElement(By.className("ui-column-title"))).click();
        assertEquals("anya", firstRowUserId.getText());
        WebElement scrollable = firstTable.findElement(By.className("ui-datatable-scrollable-body"));
        ((JavascriptExecutor) webDriver)
                .executeScript("arguments[0].scroll(0, 500);", scrollable);
        ((JavascriptExecutor) webDriver)
                .executeScript("arguments[0].scroll(0, 0);", scrollable);
        fullNameFilterInput.sendKeys("ly l");
        guardAjax(fullNameFilterInput).sendKeys(Keys.RETURN);
        assertEquals("Lovely Lady", firstRowFullName.getText());
    }

    @Deployment(testable = false, name = DEPLOYMENT_DEV_MODE)
    public static WebArchive createDeployment() {
        return ExceptionPageIT.createDeploymentDev("DataModel.war");
    }
}
