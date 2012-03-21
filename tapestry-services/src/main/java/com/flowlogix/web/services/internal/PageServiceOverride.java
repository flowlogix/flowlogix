/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

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
    public PageServiceOverride(@Symbol(SecuritySymbols.SUCCESS_URL) String successUrl,
            @Symbol(SecuritySymbols.LOGIN_URL) String loginUrl,
            @Symbol(SecuritySymbols.UNAUTHORIZED_URL) String unauthorizedUrl,
            HttpServletRequest request, HttpServletResponse response, LocalizationSetter lc)
    {
        super(successUrl, loginUrl, unauthorizedUrl,
                request, response, lc);
    }
}
