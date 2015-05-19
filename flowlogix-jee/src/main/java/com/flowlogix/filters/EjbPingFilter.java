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
package com.flowlogix.filters;

import com.flowlogix.ejb.StatefulUtil;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.web.session.HttpServletSession;
import org.omnifaces.filter.HttpFilter;
import org.omnifaces.util.Servlets;

/**
 *
 * @author lprimak
 */
@WebFilter(filterName = "EjbPingFilter", urlPatterns =
{
    "/*"
}, servletNames = "Faces Servlet")
@Slf4j
public class EjbPingFilter extends HttpFilter
{
    /**
     * Ping all Stateful EJBs, and remove those who are stale
     * 
     * @param request
     * @param response
     * @param session
     * @param chain
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException, IOException
    {
        if (session != null)
        {
            if(StatefulUtil.pingStateful(new HttpServletSession(session, null)) == false)
            {
                log.info("Failed EJB ping(s) for request: {}", Servlets.getRequestURLWithQueryString(request));
            }
        }
        
        chain.doFilter(request, response);
    }
}
