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
import java.util.List;
import java.util.concurrent.Callable;
import javax.ejb.EJBException;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.UnauthenticatedException;
import static org.omnifaces.util.Exceptions.unwrap;

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
    ProtectedStatelessBean stateless;

    public String hello() throws Exception {
        StringBuilder sb = new StringBuilder();
        if (!Forms.isLoggedIn()) {
            log.info("*=*=*=*= The next WARNING is legit, it's expected");
        }
        for (var method : List.<Callable<String>>of(viewScoped::hello, omniViewScoped::hello,
                stateless::hello)) {
            try {
                sb.append(method.call());
            } catch (UnauthenticatedException e) {
                sb.append(e.getMessage());
            } catch (EJBException e) {
                var real = unwrap(e, EJBException.class);
                if (real instanceof UnauthenticatedException) {
                    sb.append(real.getMessage());
                    LogCapture.get().poll();
                } else {
                    throw e;
                }
            }
            sb.append("<br>");
        }
        return sb.toString();
    }
}
