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
package com.flowlogix.ui.livereload;

import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.context.ResponseWriterWrapper;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;
import lombok.SneakyThrows;
import org.omnifaces.util.Faces;
import java.io.IOException;
import java.io.Writer;

public class AutoReloadPhaseListener implements PhaseListener {
    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void beforePhase(PhaseEvent event) {
        if (!Faces.isDevelopment()) {
            return;
        }

        FacesContext context = event.getFacesContext();
        String encoding = context.getExternalContext().getRequestCharacterEncoding();
        context.getExternalContext().setResponseCharacterEncoding(encoding);

        ResponseWriter originalWriter = context.getRenderKit().createResponseWriter(
                context.getExternalContext().getResponseOutputWriter(),
                null,
                context.getExternalContext().getRequestCharacterEncoding()
        );

        event.getFacesContext().setResponseWriter(
                new MyResponseWriter(originalWriter, event.getFacesContext()));
    }

    static class MyResponseWriter extends ResponseWriterWrapper {
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
                        function connectWS() {
                        const ws = new WebSocket('%s');
                        ws.onopen = () => ws.send('%s');
                        ws.onmessage = e => {
                            if (e.data === 'reload') location.reload();
                            if (e.data === 'shutdown') ws.close();
                        };
                        ws.onclose = () => setTimeout(connectWS, 2000);
                        ws.onerror = () => ws.close();
                        }
                        connectWS();
                    </script>
                    """.formatted(Faces.getRequestDomainURL() + "/flowlogix-livereload/livereload",
                        Faces.getRequestContextPath().startsWith("/")
                                ? Faces.getRequestContextPath().substring(1)
                                : Faces.getRequestContextPath());
                facesContext.getResponseWriter().write(script);
            }
            getWrapped().endElement(name);
        }
    }
}
