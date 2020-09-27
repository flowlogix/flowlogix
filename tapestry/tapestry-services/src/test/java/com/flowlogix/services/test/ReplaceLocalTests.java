/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.services.test;

import com.flowlogix.jndi.JNDIObjectLocator;
import com.flowlogix.web.services.internal.EJBAnnotationWorker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;


/**
 *
 * @author lprimak
 */
public class ReplaceLocalTests
{
    @Test
    public void stripLocal()
    {
        EJBAnnotationWorker worker = new EJBAnnotationWorker();
        assertEquals("java:module/WebusersFacade", JNDIObjectLocator.StripInterfaceSuffixPattern.matcher("java:module/WebusersFacadeLocal").replaceFirst(""));
        assertEquals("java:module/WebusersFacade", JNDIObjectLocator.StripInterfaceSuffixPattern.matcher("java:module/WebusersFacadeRemote").replaceFirst(""));
    }
}
