/*
 * Copyright 2016 lprimak.
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
package com.flowlogix.shiro.ee.cdi;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;

/**
 * Automatically apply Shiro security to all EJBs
 *
 * @author lprimak
 */
public class ShiroSecurityExtension implements Extension {
    public <T> void addSecurity(@Observes @WithAnnotations({
        Stateless.class, Stateful.class, Singleton.class,
        RequiresAuthentication.class, RequiresGuest.class, RequiresPermissions.class,
        RequiresRoles.class, RequiresUser.class}) ProcessAnnotatedType<T> pat) {
        pat.setAnnotatedType(new AnnotatedTypeWrapper<>(pat.getAnnotatedType(), () -> ShiroSecureAnnotation.class));
    }
}
