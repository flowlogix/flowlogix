/*
 * Copyright 2022 lprimak.
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
package com.flowlogix.examples.shiro;

import com.flowlogix.logcapture.LogCapture;
import com.flowlogix.shiro.ee.filters.Forms;
import javax.ejb.EJBException;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.UnauthenticatedException;
import static org.omnifaces.util.Exceptions.unwrap;
import org.omnifaces.util.Messages;

/**
 *
 * @author lprimak
 */
@Model
@Slf4j
public class UnprotectedFacade {
    @Inject
    ProtectedFacesViewScopedBean viewScoped;

    @Inject
    ProtectedOmniViewScopedBean omniViewScoped;

    @Inject
    ProtectedSessionScopedBean sessionScoped;

    @Inject
    ProtectedStatelessBean stateless;

    public void callFacesViewScoped() {
        try {
            Messages.addGlobalInfo(viewScoped.hello());
        } catch (UnauthenticatedException e) {
            Messages.addGlobalInfo("view scope unauth: {0}", e.getMessage());
        }
    }

    public void callOmniViewScoped() {
        try {
            Messages.addGlobalInfo(omniViewScoped.hello());
        } catch (UnauthenticatedException e) {
            Messages.addGlobalInfo("omni view scope unauth: {0}", e.getMessage());
        }
    }

    public void callSessionScoped() {
        try {
            Messages.addGlobalInfo(sessionScoped.hello());
        } catch (UnauthenticatedException e) {
            Messages.addGlobalInfo("session scoped unauth: {0}", e.getMessage());
        }
    }

    public void callStatelessBean() {
        try {
            if (!Forms.isLoggedIn()) {
                log.info("*=*=*=*= The next WARNING is legit, it's expected");
            }
            Messages.addGlobalInfo(stateless.hello());
        } catch (EJBException e) {
            var real = unwrap(e, EJBException.class);
            if (real instanceof UnauthenticatedException) {
                Messages.addGlobalInfo("stateless bean unauth: {0}", e.getMessage());
                LogCapture.get().poll();
            } else {
                Messages.addGlobalError("Stateless - Unexpected Exception: {0}", e.getMessage());
            }
        }
    }
}
