/*
 * Copyright 2011 lprimak.
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
package com.flowlogix.web.base;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;

/**
 * Determines the User's browser and Operating system
 * 
 * <a href="http://code.google.com/p/flowlogix/w/edit/TLUserEnvironment"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
public class UserEnvironment
{
    public void setupRender()
    {
        final String userAgent = request.getHeader("User-Agent");
        windows = userAgent.toUpperCase().contains("WINDOWS");
        Matcher matcher = ieVersionPattern.matcher(userAgent);
        msIE = matcher.matches();
        if(msIE)
        {
            msIEVersion = Double.valueOf(matcher.group(1));
        }
    }
    
    
    private @Getter boolean msIE;
    private @Getter double msIEVersion;
    private @Getter boolean windows;
    private @Inject Request request;
    private final Pattern ieVersionPattern = Pattern.compile(".*MSIE.([0-9]*\\.[0-9]*).*", Pattern.CASE_INSENSITIVE);
}
