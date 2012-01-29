/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.web.services.SecurityModule.Symbols;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    public PageServiceOverride(
            @Inject @Symbol(Symbols.SUCCESS_URL) String successUrl,
            @Inject @Symbol(Symbols.LOGIN_URL) String loginUrl,
            @Inject @Symbol(Symbols.UNAUTHORIZED_URL) String unauthorizedUrl,
            @Inject @Symbol(SecuritySymbols.UNAUTHORIZED_URL) String tynamoUnauthorizedUrl,
            HttpServletRequest request, HttpServletResponse response,
            LocalizationSetter localizationSetter)
    {
        if(unauthorizedUrl.isEmpty())
        {
            unauthorizedUrl = tynamoUnauthorizedUrl;
        }
        impl = new PageServiceImpl(successUrl, loginUrl, unauthorizedUrl,
                request, response, localizationSetter);
    }
    

    @Override
    public String getLoginPage()
    {
        return impl.getLoginPage();
    }
    

    @Override
    public String getSuccessPage()
    {
        return impl.getSuccessPage();
    }

    
    @Override
    public String getUnauthorizedPage()
    {
        return impl.getUnauthorizedPage();
    }

    
    @Override
    public String getLocalelessPathWithinApplication()
    {
        return impl.getLocalelessPathWithinApplication();
    }

    
    @Override
    public String getLocaleFromPath(String path)
    {
        return impl.getLocaleFromPath(path);
    }
    

    @Override
    public void saveRequest()
    {
        impl.saveRequest();
    }

    
    @Override
    public void redirectToSavedRequest(String fallbackUrl) throws IOException
    {
        redirectToSavedRequest(fallbackUrl);
    }
    
    
    private final PageServiceImpl impl;
}
