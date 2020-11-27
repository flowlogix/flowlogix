/*
 * Copyright 2020 lprimak.
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

import java.io.Serializable;
import java.sql.SQLException;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.omnifaces.util.Faces;
import java.nio.channels.ClosedByInterruptException;
import static com.flowlogix.examples.ui.ResponseExceptionSupplier.RUN_BEFORE_RESPONSE;
import lombok.extern.slf4j.Slf4j;
import static org.omnifaces.exceptionhandler.ViewExpiredExceptionHandler.FLASH_ATTRIBUTE_VIEW_EXPIRED;
//import static org.omnifaces.exceptionhandler.ViewExpiredExceptionHandler.wasViewExpired;

/**
 *
 * @author lprimak
 */
@Named
@ViewScoped
@Slf4j
public class ExceptionBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public void throwClosedByInterruptException() {
        Runnable before = () -> ExceptionUtils.rethrow(new ClosedByInterruptException());
        Faces.setRequestAttribute(RUN_BEFORE_RESPONSE, before);
    }

    public void throwSqlBeforeResponse() {
        Runnable before = () -> doThrowSQLException("sql");
        Faces.setRequestAttribute(RUN_BEFORE_RESPONSE, before);
    }

    public void throwExceptionFromMethod() {
        doThrowSQLException("sql-from-method");
    }

    private void doThrowSQLException(String msg) {
        log.info("*-*- Please ignore the next SEVERE message, it is expected");
        ExceptionUtils.rethrow(new SQLException(msg));
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
        // return wasViewExpired() ? "Logged Out" : "Logged In";
        return isSessionExpired() ? "Logged Out" : "Logged In";
    }

    public static boolean isSessionExpired() {
        boolean sessionExpired = false;
        if (!Faces.isAjaxRequest() && !Faces.isSessionNew()) {
            sessionExpired = Faces.getFlashAttribute(FLASH_ATTRIBUTE_VIEW_EXPIRED, () -> false);
        }
        return sessionExpired;
    }
}
