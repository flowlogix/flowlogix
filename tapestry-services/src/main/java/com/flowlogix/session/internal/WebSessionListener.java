/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.session.internal;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Internal support class for {@see com.flowlogix.web.services.annotations.AJAX} annotation
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
