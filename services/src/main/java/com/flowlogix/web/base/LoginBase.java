/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.base;

import com.flowlogix.session.SessionTrackerSSO;
import java.io.IOException;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.BeginRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;

/**
 *
 * @author lprimak
 */
public class LoginBase
{        
    /**
     * when a user is redirected to the Logon page through a AJAX
     * request after their session has expired,
     * return an json response that redirects them to the logon page
     * @return
     * @throws IOException
     */
    @BeginRender
    private Object checkForAjax() throws IOException
    {
        SessionTrackerSSO.redirectToSelf(null, linkSource, isSecure);
        return null;
    }
   
    private @Inject RequestGlobals rg;
    private @Inject PageRenderLinkSource linkSource;
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;  
}
