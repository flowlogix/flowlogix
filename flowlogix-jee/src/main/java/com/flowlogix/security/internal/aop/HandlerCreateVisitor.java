/*
 * Copyright 2014 lprimak.
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
package com.flowlogix.security.internal.aop;

import lombok.Getter;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.authz.aop.AuthenticatedAnnotationHandler;
import org.apache.shiro.authz.aop.AuthorizingAnnotationHandler;
import org.apache.shiro.authz.aop.GuestAnnotationHandler;
import org.apache.shiro.authz.aop.PermissionAnnotationHandler;
import org.apache.shiro.authz.aop.RoleAnnotationHandler;
import org.apache.shiro.authz.aop.UserAnnotationHandler;

/**
 * Enhanced from Tynamo Security
 */
public class HandlerCreateVisitor implements MethodAnnotationCasterVisitor 
{
    private @Getter AuthorizingAnnotationHandler handler;

    @Override
    public void visitRequiresAuthentication(RequiresAuthentication annotation) {
        handler = new AuthenticatedAnnotationHandler();
    }

    @Override
    public void visitRequiresGuest(RequiresGuest annotation) {
        handler = new GuestAnnotationHandler();
    }

    @Override
    public void visitRequiresPermissions(RequiresPermissions annotation) {
        handler = new PermissionAnnotationHandler();
    }

    @Override
    public void visitRequiresRoles(RequiresRoles annotation) {
        handler = new RoleAnnotationHandler();
    }

    @Override
    public void visitRequiresUser(RequiresUser annotation) {
        handler = new UserAnnotationHandler();
    }

    @Override
    public void visitNotFound() {
        handler = null;
    }
}
