/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.resourcehandler.DefaultResourceHandler;
import org.omnifaces.util.Faces;

/**
 * Automatically resolves CSS / JS, etc. resource URLs to there minimized versions in non-development mode.
 * In its default form, it will resolve my.js into my.min.js or my.css into my.min.css
 * This works for JSF resource references only, as this is a JSF handler
 * <p>
 * Works together with Maven minimize plugin, or other build tools what will generate
 * minimized versions of resources. Requires that all resources have their minimized versions
 * generated in production mode, otherwise links will point to files that do not exist.
 * <p>
 * Example:
 * <pre>
 * faces-config.xml:
 * {@code
 *   <application>
 *       <resource-handler>com.flowlogix.ui.MinimizedHandler</resource-handler>
 *   </application>
 * }
 *
 * web.xml:
 * {@code
 *   <!-- Optional, default is "min" -->
 *   <context-param>
 *       <param-name>com.flowlogix.MINIMIZED_PREFIX</param-name>
 *       <param-value>minimized</param-value>
 *   </context-param>

 *   <!-- Optional, default is "css,js" -->
 *   <context-param>
 *       <param-name>com.flowlogix.MINIMIZED_FILE_TYPES</param-name>
 *       <param-value>css, js, tsx, scss, sass, less</param-value>
 *   </context-param>
 * }
 * </pre>
 * @author lprimak
 */
public class MinimizedHandler extends DefaultResourceHandler {
    private final Pattern alreadyMinimizedPattern;
    private final Pattern extensionsPattern;
    private final String replacementMatchPattern;
    private final String replacementString;


    public MinimizedHandler(ResourceHandler wrapped) {
        this(wrapped, Optional.ofNullable(Faces.getExternalContext()
                .getInitParameter("com.flowlogix.MINIMIZED_PREFIX")).orElse("min"),
                parseExtensions(Optional.ofNullable(Faces.getExternalContext()
                        .getInitParameter("com.flowlogix.MINIMIZED_FILE_TYPES")).orElse("css, js")));
    }

    /**
     * for testing purposes only
     *
     * @param minimizedPrefix
     * @param minimizedExtensions
     */
    MinimizedHandler(ResourceHandler wrapped, String minimizedPrefix, Set<String> minimizedExtensions) {
        super(wrapped);
        alreadyMinimizedPattern = Pattern.compile(String.format(".*\\.%s\\.(%s)$", minimizedPrefix,
                String.join("|", minimizedExtensions)));
        extensionsPattern = Pattern.compile(String.format(".*\\.(%s)$", String.join("|", minimizedExtensions)));
        replacementMatchPattern = String.format("(.*)(%s)$", minimizedExtensions.stream()
                .map(str -> "\\.".concat(str)).collect(Collectors.joining("|")));
        replacementString = String.format("$1.%s$2", minimizedPrefix);
    }

    @Override
    public Resource decorateResource(Resource resource) {
        if (resource != null && !Faces.isDevelopment() && resource.getLibraryName() == null) {
            resource.setResourceName(toMinimized(resource.getResourceName()));
        }
        return resource;
    }

    String toMinimized(String resourceName) {
        if (!alreadyMinimizedPattern.matcher(resourceName).matches() && extensionsPattern.matcher(resourceName).matches()) {
            return resourceName.replaceFirst(replacementMatchPattern, replacementString);
        }
        return resourceName;
    }

    static Set<String> parseExtensions(String extensions) {
        return Stream.of(StringUtils.split(extensions, ", ")).collect(Collectors.toSet());
    }
}
