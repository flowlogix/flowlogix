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

import static com.flowlogix.examples.shiro.StatisticsResource.increment;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.omnifaces.cdi.ViewScoped;

/**
 *
 * @author lprimak
 */
@ViewScoped
@Named
@RequiresUser
@Slf4j
public class ProtectedOmniViewScopedBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final AtomicInteger instanceCount = new AtomicInteger();
    private final int count = instanceCount.incrementAndGet();

    @PostConstruct
    void postConstruct() {
        log.info("OmniViewScoped: postConstruct: {}", count);
        increment("pc_ofv");
    }

    @PreDestroy
    void preDestroy() {
        log.info("OmniViewScoped: preDestroy: {}", count);
        increment("pd_ofv");
    }

    public String hello() {
        return String.format("Hello from OmniViewScoped %s - %s", count,
        FacesContext.class.getPackage().getImplementationVersion());
    }
}
