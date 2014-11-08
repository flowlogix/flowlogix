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

import java.lang.annotation.Annotation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;

/**
 * Enhanced from Tynamo Security
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public class MethodAnnotationCaster 
{
    private @Getter static final MethodAnnotationCaster instance = new MethodAnnotationCaster();

    public void accept(MethodAnnotationCasterVisitor visitor, Annotation annotation) {
        if (annotation instanceof RequiresPermissions) {

            visitor.visitRequiresPermissions((RequiresPermissions) annotation);

        } else if (annotation instanceof RequiresRoles) {

            visitor.visitRequiresRoles((RequiresRoles) annotation);

        } else if (annotation instanceof RequiresUser) {

            visitor.visitRequiresUser((RequiresUser) annotation);

        } else if (annotation instanceof RequiresGuest) {

            visitor.visitRequiresGuest((RequiresGuest) annotation);

        } else if (annotation instanceof RequiresAuthentication) {

            visitor.visitRequiresAuthentication((RequiresAuthentication) annotation);

        } else {

            visitor.visitNotFound();

        }
    }
}
