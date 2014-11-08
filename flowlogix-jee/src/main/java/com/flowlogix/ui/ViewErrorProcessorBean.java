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
package com.flowlogix.ui;

import static com.flowlogix.ui.AttributeKeys.SESSION_EXPIRED_KEY;
import java.io.IOException;
import javax.enterprise.inject.Model;
import javax.faces.application.ViewExpiredException;
import javax.servlet.RequestDispatcher;
import org.omnifaces.util.Faces;

/**
 * redirect to previous page if view expired
 * 
 * @author lprimak
 */
@Model
public class ViewErrorProcessorBean
{
    public void redirectToPrevious() throws IOException
    {
        Class<?> cls = Faces.getRequestAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        if(ViewExpiredException.class.equals(cls))
        {
            String uri = Faces.getRequestAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            Faces.setSessionAttribute(SESSION_EXPIRED_KEY, Boolean.TRUE);
            Faces.redirect(uri);
        }
    }
}
