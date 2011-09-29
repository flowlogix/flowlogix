/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.session;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *
 * @author lprimak
 */
@WebListener
public class WebSessionListener implements HttpSessionListener
{
    @Override
    public void sessionCreated(HttpSessionEvent hse)
    {
        // blank
    }

    
    @Override
    public void sessionDestroyed(HttpSessionEvent hse)
    {
        SessionTrackerHolder.get().purge(hse.getSession());
    }
}
