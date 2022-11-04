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
import static com.flowlogix.examples.ExceptionPageIT.DEPLOYMENT_PROD_MODE;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 * @author lprimak
 */
@ArquillianSuiteDeployment
@SuppressWarnings("HideUtilityClassConstructor")
public class Deployments {
    @Deployment(testable = false, name = DEPLOYMENT_DEV_MODE)
    public static WebArchive createDeployment() {
        return ExceptionPageIT.createDeploymentDev("FlowLogixTest.war");
    }

    @Deployment(testable = false, name = DEPLOYMENT_PROD_MODE)
    public static WebArchive createDeploymentProd() {
        return ExceptionPageIT.createDeploymentProd();
    }

    @Deployment(name = LookupIT.DEPLOYMENT_NAME)
    public static WebArchive createDeploymentLookupIT() {
        return LookupIT.createDeployment();
    }
}
