/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.session.internal;

/**
 * Internal support class for {@see com.flowlogix.web.services.annotations.AJAX} annotation
 * 
 * @author lprimak
 */
public interface SessionTrackerBase
{
    public boolean isValidSession();
}
