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

import static com.flowlogix.examples.ExceptionPageIT.DEPLOYMENT_PROD_MODE;
import static com.flowlogix.util.ShrinkWrapManipulator.toHttpsURL;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

/**
 *
 * @author lprimak
 */
@ExtendWith(ArquillianExtension.class)
@Tag("UserInterface")
public class ShiroSSLFilterIT {
    @Drone
    private WebDriver webDriver;

    @SuppressWarnings("DeclarationOrder")
    @ArquillianResource
    protected URL baseURL;

    @Test
    @OperateOnDeployment(DEPLOYMENT_PROD_MODE)
    void checkNonSSL() {
        assertThrows(WebDriverException.class, () -> webDriver.get(baseURL + "shiro/unprotected/manybeans"));
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_PROD_MODE)
    void checkSSL() {
        webDriver.get(toHttpsURL(baseURL) + "shiro/unprotected/manybeans");
        assertEquals("Many Beans Unprotected", webDriver.getTitle());
    }

    @Deployment(testable = false, name = DEPLOYMENT_PROD_MODE)
    public static WebArchive createDeploymentProd() {
        return ExceptionPageIT.createDeploymentProd("ShiroSSLFilterTest-prod.war");
    }
}
