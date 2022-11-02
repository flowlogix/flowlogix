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

import static com.flowlogix.examples.LookupIT.DEPLOYMENT_NAME;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 *
 * @author lprimak
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class LazyIT {
    @Test
    public void lazy() {
        LazyExample example = new LazyExample();
        assertEquals(1, example.numInitialized.get());
        example.usingLazy();
        assertEquals(2, example.numInitialized.get());
    }

    @Deployment(name = DEPLOYMENT_NAME)
    public static WebArchive createDeployment() {
        return LookupIT.createDeployment();
    }
}
