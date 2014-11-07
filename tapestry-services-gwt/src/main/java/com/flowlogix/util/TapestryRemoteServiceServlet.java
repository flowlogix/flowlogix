/*
 * Copyright 2013 lprimak.
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

import com.flowlogix.web.services.internal.AssetPathProcessor;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import javax.servlet.http.HttpServletRequest;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.services.SymbolSource;

/**
 * Strips Tapestry asset path from GWT RPC server
 * @author lprimak
 */
public abstract class TapestryRemoteServiceServlet extends RemoteServiceServlet
{
    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL, String strongName)
    {
        final SymbolSource symbolSource = ExternalServiceUtil.getTapestryService(getServletContext(), SymbolSource.class);
        final String prefix = symbolSource.valueForSymbol(SymbolConstants.ASSET_PATH_PREFIX);
        final AssetPathProcessor pathProcessor = new AssetPathProcessor(prefix);
        return super.doGetSerializationPolicy(request, pathProcessor.removeAssetPathPart(moduleBaseURL), strongName);
    }

    private static final long serialVersionUID = 1L;
}
