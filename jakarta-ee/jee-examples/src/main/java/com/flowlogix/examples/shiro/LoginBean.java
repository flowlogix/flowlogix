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

import javax.enterprise.inject.Model;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.web.util.WebUtils;
import org.omnifaces.util.Faces;

/**
 *
 * @author lprimak
 */
@Model
@Getter @Setter
@Slf4j
public class LoginBean {
    @NotBlank
    private String uname;
    @NotBlank
    private String pwd;
    private boolean rememberMe;

    public void login() {
        SecurityUtils.getSubject().login(new UsernamePasswordToken(uname, pwd, rememberMe));
        redirectToSaved();
    }

    public void logout() {
        SecurityUtils.getSubject().logout();
        redirectToView();
    }

    void redirectToSaved() {
        String savedRequest = Faces.getRequestCookie(WebUtils.SAVED_REQUEST_KEY);
        if (savedRequest != null) {
            Faces.redirect(savedRequest);
        } else {
            redirectToView();
        }
    }

    void redirectToView() {
        if (Faces.getViewId().contains("shiro/auth/")) {
            Faces.redirect(Faces.getRequestContextPath());
        } else {
            Faces.redirect(Faces.getRequestURLWithQueryString());
        }
    }
}
