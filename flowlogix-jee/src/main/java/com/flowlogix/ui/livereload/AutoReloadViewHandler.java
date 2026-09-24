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
package com.flowlogix.ui.livereload;

import jakarta.faces.application.ViewHandler;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.context.ResponseWriterWrapper;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.component.UIViewRoot;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.util.Faces;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

public class AutoReloadViewHandler extends ViewHandlerWrapper {
    public AutoReloadViewHandler(ViewHandler wrapped) {
        super(wrapped);
    }

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException {
        if (!Faces.isDevelopment() || Faces.isAjaxRequest()) {
            super.renderView(context, viewToRender);
            return;
        }

        super.renderView(new AutoReloadFacesContext(context), viewToRender);
    }

    private static final class AutoReloadFacesContext extends FacesContextWrapper {
        private AutoReloadFacesContext(FacesContext wrapped) {
            super(wrapped);
        }

        @Override
        public void setResponseWriter(ResponseWriter responseWriter) {
            super.setResponseWriter(responseWriter instanceof MyResponseWriter ? responseWriter
                    : new MyResponseWriter(responseWriter, this));
        }
    }

    static class MyResponseWriter extends ResponseWriterWrapper {
        static final String HTTPS_SCHEME = "https";
        static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
        static final Pattern HTTP_TO_HTTPS = Pattern.compile("^\\s*http(.*)");
        private final FacesContext facesContext;

        MyResponseWriter(ResponseWriter wrapped, FacesContext context) {
            super(wrapped);
            this.facesContext = context;
        }

        @Override
        public ResponseWriter cloneWithWriter(Writer writer) {
            return new MyResponseWriter(getWrapped().cloneWithWriter(writer), facesContext);
        }

        @Override
        public void endElement(String name) throws IOException {
            if ("body".equalsIgnoreCase(name)) {
                String script = """
                    <script>
                        function reportLiveReloadError(message) {
                            console.error(message);
                            if (typeof flowlogix_showError === 'function') {
                                flowlogix_showError('livereload-error-message', message);
                            } else if (!window.flowlogixLiveReloadAlertShown) {
                                window.flowlogixLiveReloadAlertShown = true;
                                alert(message + ' FlowLogix error banner helper script is not loaded.');
                            }
                        }

                        function connectWS() {
                            const ws = new WebSocket('%s');
                            ws.onopen = () => ws.send('%s');
                            ws.onmessage = e => {
                                if (e.data === 'reload') {
                                    ws.close();
                                    if (typeof OmniFaces !== 'undefined' && typeof OmniFaces.Unload !== 'undefined') {
                                        window.dispatchEvent(new Event('beforeunload'));
                                        console.log('OmniFaces @ViewScoped Unload triggered');
                                        OmniFaces.Unload.disable();
                                        console.log('Further OmniFaces @ViewScoped Unload is Disabled');
                                        setTimeout(() => { location.replace(location.href); }, 50);
                                    } else {
                                        location.replace(location.href);
                                    }
                                } else if (e.data === 'error') {
                                    reportLiveReloadError(
                                        'Compilation or deployment error occurred, '
                                            + 'please check maven and server logs for details.');
                                } else if (e.data === 'test-failure') {
                                    reportLiveReloadError('Test failure(s) occurred, please check maven for details.');
                                } else if (e.data === 'shutdown') {
                                    ws.close();
                                }
                            };
                            ws.onclose = () => setTimeout(connectWS, 2000);
                            ws.onerror = () => ws.close();
                        }

                        connectWS();
                    </script>
                    """.formatted(toHttpsURL(Faces.getRequestDomainURL())
                                + "/flowlogix-livereload/livereload", getDeploymentKey());
                facesContext.getResponseWriter().write(script);
            }
            getWrapped().endElement(name);
        }

        private static String getContextPathKey() {
            return Faces.getRequestContextPath().startsWith("/")
                    ? Faces.getRequestContextPath().substring(1)
                    : Faces.getRequestContextPath();
        }

        private String getDeploymentKey() {
            try {
                return Optional.ofNullable(Faces.getRealPath("/"))
                        .filter(StringUtils::isNotBlank)
                        .map(path -> Path.of(path).normalize().getFileName())
                        .map(Path::toString)
                        .orElseGet(MyResponseWriter::getContextPathKey);
            } catch (InvalidPathException ignored) {
                return getContextPathKey();
            }
        }

        static String toHttpsURL(String url) {
            return createHttpButNeedHttps()
                    ? HTTP_TO_HTTPS.matcher(url).replaceFirst(HTTPS_SCHEME + "$1")
                    : url;
        }

        private static boolean createHttpButNeedHttps() {
            return !HTTPS_SCHEME.equalsIgnoreCase(Faces.getRequest().getScheme())
                    && HTTPS_SCHEME.equalsIgnoreCase(Faces.getRequest().getHeader(X_FORWARDED_PROTO));
        }
    }
}
