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

import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.html.HtmlOutputText;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import org.omnifaces.util.Faces;

public class AjaxErrorHandlerListener implements SystemEventListener {
    private static final String CDN_URL_PARAM = "com.flowlogix.ui.cdn-url";
    private static final String CDN_URL_TARGET_PARAM = "com.flowlogix.ui.cdn-url.target";
    private static final String DEFAULT_CDN_URL = "https://flowlogix.com/js/apps/ajax-error-handler.js";

    @Override
    public void processEvent(SystemEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        UIViewRoot view = context.getViewRoot();
        String url = Faces.getInitParameterOrDefault(CDN_URL_PARAM, DEFAULT_CDN_URL);
        String marker = url + ".flowlogix-url-added";

        if (!Boolean.TRUE.equals(view.getAttributes().get(marker))) {
            HtmlOutputText script = (HtmlOutputText) context.getApplication()
                    .createComponent(HtmlOutputText.COMPONENT_TYPE);
            script.setEscape(false);
            script.setValue("<script src=\"" + url + "\"></script>");
            script.getAttributes().put("name", url);
            view.addComponentResource(context, script,
                    Faces.getInitParameterOrDefault(CDN_URL_TARGET_PARAM, "body"));
            view.getAttributes().put(marker, Boolean.TRUE);
        }
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return source instanceof UIViewRoot;
    }
}
