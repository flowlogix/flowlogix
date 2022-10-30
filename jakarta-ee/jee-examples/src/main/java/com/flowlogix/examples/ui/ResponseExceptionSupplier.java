/*
 * Copyright (C) 2011-2022 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.examples.ui;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import org.omnifaces.util.Faces;

/**
 *
 * @author lprimak
 */
public class ResponseExceptionSupplier implements PhaseListener {
    static final String RUN_BEFORE_RESPONSE = "com.flowlogix.response.runBefore";
    static final String CALL_RESPONSE_COMPLETE = "com.flowlogix.response.responseComplete";

    private static final long serialVersionUID = 1L;

    @Override
    public void afterPhase(PhaseEvent event) {
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        Runnable before = Faces.getRequestAttribute(RUN_BEFORE_RESPONSE);
        if (before != null) {
            // The next line forces (Omni | Prime) exception handler to
            // actually see / handle the exception, and not just show it in the server log.
            // To see the exception in the server log, responseComlete() shouldn't be called,
            // otherwise it will never show up in the server log
            if (Faces.getRequestAttribute(CALL_RESPONSE_COMPLETE, () -> true)) {
                event.getFacesContext().responseComplete();
            }
            before.run();
        }
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }
}
