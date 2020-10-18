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
package com.flowlogix.ui;

import com.flowlogix.util.Lazy;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceWrapper;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.resourcehandler.DefaultResourceHandler;
import org.omnifaces.util.Faces;

/**
 * adds forever caching functionality to the JSF resources
 *
 * +++ TODO documentation
 *
 * @author lprimak
 */
public class CacheResourcesForever extends DefaultResourceHandler {
    private static final String VERSION_SUFFIX = "v=";
    private final Lazy<String> versionString;


    public CacheResourcesForever(ResourceHandler wrapped) {
        super(wrapped);
        versionString = new Lazy<>(() -> Faces.evaluateExpressionGet(
                Faces.getExternalContext().getInitParameter("com.flowlogix.VERSION_STRING")));
    }

    @Override
    public Resource decorateResource(Resource resource) {
        if (resource == null || StringUtils.isBlank(versionString.get())) {
            return resource;
        }
        String requestPath = resource.getRequestPath();
        if (requestPath.contains('&' + VERSION_SUFFIX) || requestPath.contains('?' + VERSION_SUFFIX)) {
            // ignore already-versioned resources
            return resource;
        } else {
            return new CachingWrapper(resource);
        }
    }

    private class CachingWrapper extends ResourceWrapper {
        public CachingWrapper(Resource wrapped) {
            super(wrapped);
        }

        @Override
        public String getRequestPath() {
            String requestPath = getWrapped().getRequestPath();
            if (!requestPath.contains(ResourceHandler.RESOURCE_IDENTIFIER)) {
                // do not touch CDN resources
                return requestPath;
            }

            if (requestPath.contains("?")) {
                requestPath = requestPath + '&' + VERSION_SUFFIX + versionString.get();
            } else {
                requestPath = requestPath + '?' + VERSION_SUFFIX + versionString.get();
            }

            return requestPath;
        }
    }
}
