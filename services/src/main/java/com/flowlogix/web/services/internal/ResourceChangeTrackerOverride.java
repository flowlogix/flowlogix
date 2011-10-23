/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import org.apache.tapestry5.internal.services.assets.ResourceChangeTracker;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.ioc.internal.util.URLChangeTracker;
import org.apache.tapestry5.services.InvalidationListener;

/**
 *
 * @author lprimak
 */
public class ResourceChangeTrackerOverride implements ResourceChangeTracker
{
    public ResourceChangeTrackerOverride()
    {
        this.tracker = new URLChangeTracker(URLChangeTracker.DEFAULT_CONVERTER, true, false);
    }
    
    
    @Override
    public void addInvalidationListener(InvalidationListener listener)
    {
        // blank
    }

    @Override
    public void addDependency(Resource dependency)
    {
        // blank
    }

    
    @Override
    public long trackResource(Resource resource)
    {
        return tracker.add(resource.toURL());
    }
    
    
    private final URLChangeTracker tracker;
}
