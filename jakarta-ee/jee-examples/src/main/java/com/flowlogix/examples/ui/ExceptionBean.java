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

import static com.flowlogix.examples.ui.ResponseExceptionSupplier.CALL_RESPONSE_COMPLETE;
import java.io.Serializable;
import java.sql.SQLException;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.omnifaces.util.Faces;
import java.nio.channels.ClosedByInterruptException;
import static com.flowlogix.examples.ui.ResponseExceptionSupplier.RUN_BEFORE_RESPONSE;
import lombok.extern.slf4j.Slf4j;
import static org.omnifaces.exceptionhandler.ViewExpiredExceptionHandler.wasViewExpired;

/**
 *
 * @author lprimak
 */
@Named
@ViewScoped
@Slf4j
public class ExceptionBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public void throwClosedByInterruptException(boolean responseComplete) {
        Runnable before = () -> ExceptionUtils.rethrow(new ClosedByInterruptException());
        Faces.setRequestAttribute(RUN_BEFORE_RESPONSE, before);
        Faces.setRequestAttribute(CALL_RESPONSE_COMPLETE, responseComplete);
    }

    public void throwSqlBeforeResponse() {
        Runnable before = () -> ExceptionUtils.rethrow(new SQLException("sql"));
        Faces.setRequestAttribute(RUN_BEFORE_RESPONSE, before);
    }

    public void throwExceptionFromMethod() {
        log.info("*=*=*=*= The next WARNING is legit, it's expected");
        ExceptionUtils.rethrow(new SQLException("sql-from-method"));
    }

    public void success() {

    }

    public void invalidateSession() {
        Faces.invalidateSession();
    }

    public String getSessionValue() {
        return Faces.getSessionId();
    }

    public String expired() {
        return wasViewExpired() ? "Logged Out" : "Logged In";
    }
}
