/*
 * Copyright (C) 2011-2026 Flow Logix, Inc. All Rights Reserved.
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

import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.ViewHandler;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.util.JNDIObjectLocator;
import java.util.Set;

@Slf4j
public class FacesBufferSizeInitializer implements ServletContainerInitializer {
    /**
     * Sets the {@link ViewHandler#FACELETS_BUFFER_SIZE_PARAM_NAME} to supplied value in development mode only.
     * The value is kilobytes, default is 1k.
     * This is needed to error page is rendered when the page is too large for the default buffer of 1k.
     */
    static final String COM_FLOWLOGIX_SET_FACELET_BUFFER_SIZE = "com.flowlogix.set-facelet-buffer-size";
    private static final JNDIObjectLocator LOCATOR = JNDIObjectLocator.builder().build();

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) {
        String bufferSizeStr = ctx.getInitParameter(COM_FLOWLOGIX_SET_FACELET_BUFFER_SIZE);
        if (StringUtils.isBlank(bufferSizeStr)) {
            return;
        }

        String stage = LOCATOR.getObject(ProjectStage.PROJECT_STAGE_JNDI_NAME);
        if (stage == null) {
            stage = ctx.getInitParameter(ProjectStage.PROJECT_STAGE_PARAM_NAME);
        }
        if (ProjectStage.Development.name().equals(stage)) {
            // convert to bytes from kilobytes
            @SuppressWarnings("checkstyle:MagicNumber")
            var bufferSize = Integer.parseInt(bufferSizeStr.strip()) * 1_024;
            log.debug("Setting {} to {} in development mode", ViewHandler.FACELETS_BUFFER_SIZE_PARAM_NAME, bufferSize);
            ctx.setInitParameter(ViewHandler.FACELETS_BUFFER_SIZE_PARAM_NAME, Integer.toString(bufferSize));
        }
    }
}
