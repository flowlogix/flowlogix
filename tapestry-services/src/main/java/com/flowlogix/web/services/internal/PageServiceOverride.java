/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.web.services.SecurityModule.Symbols;
import java.io.IOException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.web.util.WebUtils;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.LocalizationSetter;
import org.tynamo.security.SecuritySymbols;
import org.tynamo.security.services.PageService;
import org.tynamo.security.services.impl.PageServiceImpl;

/**
 * Override Tynamo Login, Success and Unauthorized pages
 * 
 * @author lprimak
 */
public class PageServiceOverride implements PageService
{       
    public PageServiceOverride(ObjectLocator locator)
    {
        this.locator = locator;
    }
    
    
    private void init()
    {
        if(impl != null)
        {
            return;
        }
        String unauth = unauthorizedUrl.isEmpty()? tynamoUnauthorizedUrl : unauthorizedUrl;
        request = locator.getService(HttpServletRequest.class);
        response = locator.getService(HttpServletResponse.class);
        impl = new PageServiceImpl(successUrl, loginUrl, unauth, request, response, 
        locator.getService(LocalizationSetter.class));
    }
    

    @Override
    public String getLoginPage()
    {
        init();
        return impl.getLoginPage();
    }
    

    @Override
    public String getSuccessPage()
    {
        init();
        return impl.getSuccessPage();
    }

    
    @Override
    public String getUnauthorizedPage()
    {
        init();
        return impl.getUnauthorizedPage();
    }

    
    @Override
    public String getLocalelessPathWithinApplication()
    {
        init();
        return impl.getLocalelessPathWithinApplication();
    }

    
    @Override
    public String getLocaleFromPath(String path)
    {
        init();
        return impl.getLocaleFromPath(path);
    }
    

    @Override
    public void saveRequest()
    {
        init();
        impl.saveRequest();
    }


    /**
     * +++ See http://jira.codehaus.org/browse/TYNAMO-120
     * Once that issue is fixed, this method can be
     * made the a simple proxy again
     */
    @Override
    public void redirectToSavedRequest(String fallbackUrl) throws IOException
    {
        init();
        for (Cookie cookie : request.getCookies())
        {
            if (WebUtils.SAVED_REQUEST_KEY.equals(cookie.getName()))
            {
                impl.redirectToSavedRequest(fallbackUrl);
                return;
            }
        }
        WebUtils.issueRedirect(request, response, fallbackUrl);
    }
    
    
    private PageServiceImpl impl;
    private @Inject @Symbol(Symbols.SUCCESS_URL) String successUrl;
    private @Inject @Symbol(Symbols.LOGIN_URL) String loginUrl;
    private @Inject @Symbol(Symbols.UNAUTHORIZED_URL) String unauthorizedUrl;
    private @Inject @Symbol(SecuritySymbols.UNAUTHORIZED_URL) String tynamoUnauthorizedUrl;
    private final ObjectLocator locator;
    private HttpServletRequest request;
    private HttpServletResponse response;
}
