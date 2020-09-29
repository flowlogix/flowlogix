/*
 * Copyright 2014 lprimak.
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
package com.flowlogix.ui;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.application.ResourceWrapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * adds foreer caching functionality to the JSF resources
 *
 * @author lprimak
 */
@RequiredArgsConstructor
public abstract class AbstractForeverCachingHandler extends ResourceHandlerWrapper
{
    /**
     * Override this function in your child class to get version string
     * @return versino string
     */
    protected abstract String getVersionString();


    @Override
    public Resource createResource(String resourceName)
    {
        return createResource(resourceName, null);
    }


    @Override
    public Resource createResource(String resourceName, String libraryName)
    {
        if(StringUtils.isEmpty(resourceName))
        {
            return null;
        }
        Resource resource;
        if(StringUtils.isEmpty(libraryName))
        {
            resource = super.createResource(resourceName);
        }
        else
        {
            resource = super.createResource(resourceName, libraryName);
        }
        if(resource == null)
        {
            return null;
        }

        String requestPath = resource.getRequestPath();
        if(!StringUtils.isEmpty(libraryName) && requestPath != null &&
            (requestPath.contains("&v=") || requestPath.contains("?v=")))
        {
            // ignore already-versioned resources
            return resource;
        }
        return new CachingWrapper(resource, getVersionString());
    }


    private @Getter final ResourceHandler wrapped;


    @RequiredArgsConstructor
    private static class CachingWrapper extends ResourceWrapper
    {
        @Override
        public String getRequestPath()
        {
            String requestPath = wrapped.getRequestPath();
            if(!requestPath.contains(ResourceHandler.RESOURCE_IDENTIFIER))
            {
                // do not touch CDN resources
                return requestPath;
            }

            if(requestPath.contains("?"))
            {
                requestPath = requestPath + "&rv=" + versionString;
            }
            else
            {
                requestPath = requestPath + "?rv=" + versionString;
            }

            return requestPath;
        }


        @Override
        public String getContentType()
        {
            return getWrapped().getContentType();
        }


        private @Getter final Resource wrapped;
        private final String versionString;
    }
}
