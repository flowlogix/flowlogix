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

    @ArquillianResource
    protected URL baseURL;

    @FindBy(id = "model1")
    private WebElement first_table;

    @FindBy(id = "model1:fullNameFilter:filter")
    private WebElement fullNameFilterInput;

    @FindBy(id = "model1:0:fullName")
    private WebElement firstRowFullName;

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void checkPage() {
        webDriver.get(baseURL + "view-users");
        assertEquals("View Users", webDriver.getTitle());
        ((JavascriptExecutor) webDriver)
                .executeScript("arguments[0].scroll(0, 100);", first_table);
        fullNameFilterInput.sendKeys("ly l");
        guardAjax(fullNameFilterInput).sendKeys(Keys.RETURN);
        assertEquals("Lovely Lady", firstRowFullName.getText());
    }

    @Deployment(testable = false, name = DEPLOYMENT_DEV_MODE)
    public static WebArchive createDeployment() {
        return ExceptionPageIT.createDeploymentDev("DataModel.war");
    }
}
