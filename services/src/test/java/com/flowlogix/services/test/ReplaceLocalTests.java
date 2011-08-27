/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.services.test;

import com.flowlogix.web.services.internal.EJBAnnotationWorker;
import org.junit.Test;
import static org.junit.Assert.*;


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
        assertEquals("java:module/WebusersFacade", worker.getStripLocalPattern().matcher("java:module/WebusersFacadeLocal").replaceFirst(""));
        assertEquals("java:module/WebusersFacade", worker.getStripLocalPattern().matcher("java:module/WebusersFacadeRemote").replaceFirst(""));
    }
}
