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

import static com.flowlogix.shiro.ee.filters.Forms.redirectToSaved;
import static com.flowlogix.shiro.ee.filters.Forms.redirectToView;
import javax.enterprise.inject.Model;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;

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
        try {
            SecurityUtils.getSubject().login(new UsernamePasswordToken(uname, pwd, rememberMe));
            // redirect to index page as a fallback
            redirectToSaved(() -> Faces.getViewId().contains("shiro/auth/"), Faces.getRequestContextPath());
        } catch (AuthenticationException e) {
            Messages.addFlashGlobalError("Incorrect Login");
            redirectToView();
        }
    }

    public void logout() {
        SecurityUtils.getSubject().logout();
        redirectToView();
    }
}
