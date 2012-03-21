/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.web.services.SecurityModule.Symbols;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.LocalizationSetter;
import org.tynamo.security.SecuritySymbols;
import org.tynamo.security.services.impl.PageServiceImpl;

/**
 * Override Tynamo Login, Success and Unauthorized pages
 * 
 * @author lprimak
 */
public class PageServiceOverride extends PageServiceImpl
{
    public PageServiceOverride(@Symbol(Symbols.SUCCESS_URL) String successUrl,
            @Symbol(Symbols.LOGIN_URL) String loginUrl,
            @Symbol(Symbols.UNAUTHORIZED_URL) String unauthorizedUrl,
            @Symbol(SecuritySymbols.UNAUTHORIZED_URL) String tynamoUnauthorizedUrl,
            HttpServletRequest request,
            HttpServletResponse response,
            LocalizationSetter lc)
    {
        super(successUrl, loginUrl, unauthorizedUrl.isEmpty() ? tynamoUnauthorizedUrl : unauthorizedUrl,
                request, response, lc);
    }
}
