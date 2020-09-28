/*
 * Copyright 2020 Flow Logix, Inc.
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
package com.flowlogix.jndi;

import javax.naming.NamingException;
//import org.jboss.arquillian.container.test.api.Deployment;
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.asset.EmptyAsset;
//import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Example program for {@link com.flowlogix.jndi.JNDIObjectLocator}
 *
 * @author lprimak
 */
@Tag("Arquillian")
public class JndiLocatorExample {
    @Test
    void basicLookup() throws NamingException {
        JNDIObjectLocator.builder().build().getObject("hello");
    }

//    @Deployment
//    public static JavaArchive createDeployment() {
//        return ShrinkWrap.create(JavaArchive.class)
//                .addClass(JndiLocatorExample.class)
//                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
//    }
}
