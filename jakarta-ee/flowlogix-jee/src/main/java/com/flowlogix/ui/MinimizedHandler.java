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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.util.Faces;

/**
 * Automatically loads minimized CSS / JS files in production mode, if available
 *
 * @author lprimak
 */
@RequiredArgsConstructor
public class MinimizedHandler extends ResourceHandlerWrapper
{
    @Override
    public Resource createResource(String resourceName)
    {
        if(StringUtils.isEmpty(resourceName))
        {
            return null;
        }
        return super.createResource(toMinimized(resourceName));
    }


    @Override
    public Resource createResource(String resourceName, String libraryName)
    {
        if(StringUtils.isEmpty(resourceName))
        {
            return null;
        }
        return super.createResource(libraryName == null? toMinimized(resourceName) : resourceName, libraryName);
    }


    private String toMinimized(String resourceName)
    {
        if(!Faces.isDevelopment() && !resourceName.matches(".*\\.min\\.(js|css)$"))
        {
            if(resourceName.matches(".*\\.(css|js)$"))
            {
                return resourceName.replaceFirst("(.*)(\\.css|\\.js)$", "$1.min$2");
            }
        }
        return resourceName;
    }


    private @Getter final ResourceHandler wrapped;
}
