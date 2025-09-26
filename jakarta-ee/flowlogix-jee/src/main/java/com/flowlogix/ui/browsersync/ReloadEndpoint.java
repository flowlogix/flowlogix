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
package com.flowlogix.ui.browsersync;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ServerEndpoint("/flowlogix/browsersync")
public class ReloadEndpoint {
    private static final Set<Session> SESSIONS = new CopyOnWriteArraySet<>();
    private static final AtomicBoolean NEEDS_ANOTHER_RELOAD = new AtomicBoolean();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        SESSIONS.add(session);
        if (NEEDS_ANOTHER_RELOAD.getAndSet(false)) {
            session.getBasicRemote().sendText("reload");
            log.debug("Reloading Web BrowserSync session {} on connect", session.getId());
        }
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session);
    }

    public static void broadcastReload() throws IOException {
        log.debug("broadcasting reload endpoint");
        if (SESSIONS.isEmpty()) {
            NEEDS_ANOTHER_RELOAD.set(true);
            log.debug("Setting Another Reload");
        }
        for (Session session : SESSIONS) {
            log.debug("Reloading Web BrowserSync session {}", session.getId());
            session.getBasicRemote().sendText("reload");
        }
    }
}
