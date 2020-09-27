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
package com.flowlogix.security;

import com.flowlogix.jndi.JNDIObjectLocator;
import java.io.IOException;
import javax.faces.application.ProjectStage;
import static javax.faces.application.ProjectStage.PROJECT_STAGE_JNDI_NAME;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Enables Shiro' SslFilter depending
 * whether in production mode or not
 * @author lprimak
 */
public class SslFilter extends org.apache.shiro.web.filter.authz.SslFilter
{
    @Override
    protected boolean isEnabled(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        boolean enabled = true;
        try
        {
            String stageOverride = locator.getObject(PROJECT_STAGE_JNDI_NAME);
            if(stageOverride == null || ProjectStage.Development.name().equalsIgnoreCase(stageOverride))
            {
                enabled = false;
            }
        } catch (NamingException ex) {}
        return enabled && super.isEnabled(request, response);
    }


    private final JNDIObjectLocator locator = JNDIObjectLocator.builder().build();
}
