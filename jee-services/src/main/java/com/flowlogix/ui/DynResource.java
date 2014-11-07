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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.application.Resource;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.omnifaces.util.Faces;

/**
 * DreamWeaver viewable resource support for JSF
 * 
 * @author lprimak
 */
@FacesComponent("flowlogix:resource")
public class DynResource extends UINamingContainer
{
    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
        String outerTag = (String)getAttributes().get(FLOWLOGIX_OUTERTAG);
        getStateHelper().put(FLOWLOGIX_OUTERTAG, outerTag);
        String sourceKey = (String)getAttributes().get("flowlogix.sourceKey");
        ResponseWriter responseWriter = context.getResponseWriter();
        responseWriter.startElement(outerTag, this);
        for(Object _key : getAttributes().keySet())
        {
            if(!(_key instanceof String))
            {
                continue;
            }
            String key = (String)_key;
            if(key.startsWith("com.sun.faces") || key.startsWith("javax.faces") || key.startsWith("flowlogix."))
            {
                continue;
            }
            Object _value = getAttributes().get(key);
            if(_value instanceof String)
            {
                String value = (String)_value;
                if(sourceKey.equalsIgnoreCase(key))
                {
                    boolean staticResource = Boolean.valueOf(context
                            .getExternalContext().getInitParameter(getClass().getPackage().getName() + ".useLibrary"));
                    value = value.replaceFirst("^.*/resources/", "/");
                    if(!Faces.isDevelopment() && (value.endsWith(".css") || value.endsWith(".js")))
                    {
                        value = value.replaceFirst("(.*)(\\.css|\\.js)$", "$1.min$2");
                    }
                    if(!Faces.isDevelopment() && value.endsWith(".less"))
                    {
                        value = value.replaceFirst("\\.less$", ".css");
                    }
                    if(Faces.isDevelopment() && value.endsWith(".css"))
                    {
                        String relStr = (String)getAttributes().get("rel");
                        if(relStr != null && relStr.endsWith("/less"))
                        {
                            staticResource = false;
                            value = value.replaceFirst("\\.css$", ".less");
                        }
                    }
                    Resource resource;
                    Matcher matcher = firstPathPattern.matcher(value);
                    if(staticResource == true && matcher.matches())
                    {
                        value = value.replaceFirst(String.format("^/%s/", matcher.group(1)), "");
                        resource = context.getApplication().getResourceHandler().createResource(value, matcher.group(1));
                    }
                    else
                    {
                        resource = context.getApplication().getResourceHandler().createResource(value);
                    }
                    if(resource == null)
                    {
                        throw new IOException(String.format("Unable to Find Resource: %s", value));
                    }
                    value = resource.getRequestPath();
                }
                else if(!Faces.isDevelopment() && "rel".equalsIgnoreCase(key))
                {
                    value = value.replaceFirst("\\/less$", "");
                }
                responseWriter.writeAttribute(key, value, key);
            }
        }
    }   
    
    
    @Override
    public void encodeEnd(FacesContext context) throws IOException
    {
        ResponseWriter responseWriter = context.getResponseWriter();
        responseWriter.endElement((String)getStateHelper().get(FLOWLOGIX_OUTERTAG));
    }

    
    private static final String FLOWLOGIX_OUTERTAG = "flowlogix.outerTag";
    private static final Pattern firstPathPattern = Pattern.compile("^/([^/]*)/.*");
}

