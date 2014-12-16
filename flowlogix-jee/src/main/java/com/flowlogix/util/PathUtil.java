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
package com.flowlogix.util;

import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.web.util.WebUtils;

/**
 * builds absolute path URI when needed for redirection from Web and REST API
 * for web pages
 * 
 * @author lprimak
 */
public class PathUtil
{
    /**
     * Returns full path URI for a relative path
     * 
     * @param request
     * @param relativePath
     * @return relative URI
     */
    @SneakyThrows(URISyntaxException.class)
    public static URI toAppURI(HttpServletRequest request, String relativePath)
    {
        String absolutePath = String.format("%s%s/%s", getServerPath(request),
                request.getContextPath(), relativePath);

        return new URI(absolutePath);
    }
    
    
    /**
     * @param request
     * @return complete server path i.e. https://www.glowlogix.com:8080
     */
    public static String getServerPath(HttpServletRequest request)
    {
        String port = "";
        switch(request.getScheme())
        {
            case "http":
                if(request.getServerPort() != 80)
                {
                    port = ":" + Integer.toString(request.getServerPort());
                }
                break;
            case "https":
                if(request.getServerPort() != 443)
                {
                    port = ":" + Integer.toString(request.getServerPort());
                }
                break;
        }
        
        return String.format("%s://%s%s", 
                request.getScheme(), request.getServerName(), port);
    }
    
    
    /**
     * @param request
     * @return context path or "/" if there is no context path
     */
    public static String getContextPath(HttpServletRequest request)
    {
        String contextPath = WebUtils.toHttp(request).getContextPath();
        if(StringUtils.isBlank(contextPath))
        {
            contextPath = "/";
        }
        return contextPath;
    }
}
