/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.services.test;

import com.flowlogix.web.services.internal.EJBAnnotationWorker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.omnifaces.util.JNDI;


/**
 *
 * @author lprimak
 */
public class ReplaceLocalTest
{
    @Test
    public void stripLocal()
    {
        EJBAnnotationWorker worker = new EJBAnnotationWorker();
        assertEquals("java:module/WebusersFacade", JNDI.PATTERN_EJB_INTERFACE_SUFFIX.matcher("java:module/WebusersFacadeLocal").replaceFirst(""));
        assertEquals("java:module/WebusersFacade", JNDI.PATTERN_EJB_INTERFACE_SUFFIX.matcher("java:module/WebusersFacadeRemote").replaceFirst(""));
    }
}
