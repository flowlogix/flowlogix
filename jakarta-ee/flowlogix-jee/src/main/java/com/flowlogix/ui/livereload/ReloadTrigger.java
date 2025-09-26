/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.ui.livereload;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class ReloadTrigger {
    @POST
    @Path("/reload")
    public Response reload() throws IOException {
        if (ReloadEndpoint.broadcastReload()) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Live Reloading Disabled").build();
        }
    }
}
