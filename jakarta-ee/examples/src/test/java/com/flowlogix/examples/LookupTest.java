/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.examples;

import com.flowlogix.examples.jndi.ejbs.AnotherEJB;
import com.flowlogix.test.ArquillianTest;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 *
 * @author lprimak
 */
@RunWith(Arquillian.class)
@Category(ArquillianTest.class)
public class LookupTest {
    private JndiExample example;


    @Before
    public void before() {
        example = new JndiExample();
    }

    @After
    public void after() {
        example = null;
    }

    @Test
    public void happyPath() {
        assertEquals(5, example.getNumber());
        assertNotNull("should not be null", example.getLocator().getObject(AnotherEJB.class));
    }

    @Test
    public void unhappyPath() throws NamingException {
        assertThrows(NamingException.class, () -> example.getLocator().getObject("hello"));
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, "com.flowlogix");
    }
}
