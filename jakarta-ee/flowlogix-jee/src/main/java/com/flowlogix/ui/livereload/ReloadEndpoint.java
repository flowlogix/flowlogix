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

import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ServerEndpoint(value = "/flowlogix/livereload", configurator = ReloadEndpointConfigurator.class)
public class ReloadEndpoint {
    private static final AtomicBoolean NEEDS_ANOTHER_RELOAD = new AtomicBoolean();
    private static final Set<Session> SESSIONS = new CopyOnWriteArraySet<>();
    private static final AtomicInteger MAX_SESSIONS = new AtomicInteger(
            Integer.getInteger("com.flowlogix.faces.MAX_LIVE_RELOAD_SESSIONS", 20));

    @OnOpen
    public void onOpen(Session session) throws IOException {
        SESSIONS.add(session);
        if (NEEDS_ANOTHER_RELOAD.getAndSet(false)) {
            session.getBasicRemote().sendText("reload");
            log.debug("Reloading Web LiveReload session {} on connect", session.getId());
        }
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session);
    }

    public static boolean broadcastReload() throws IOException {
        if (MAX_SESSIONS.get() == 0) {
            log.debug("Max sessions is 0, not broadcasting reload");
            return false;
        }
        log.debug("broadcasting reload endpoint");
        if (SESSIONS.isEmpty()) {
            NEEDS_ANOTHER_RELOAD.set(true);
            log.debug("Setting Another Reload");
        }
        for (Session session : SESSIONS) {
            log.debug("Reloading Web LiveReload session {}", session.getId());
            session.getBasicRemote().sendText("reload");
        }
        return true;
    }

    static int getMaxSessions() {
        return MAX_SESSIONS.get();
    }

    static void setMaxSessions(int max) {
        MAX_SESSIONS.set(max);
    }

    static int getSessionCount() {
        return SESSIONS.size();
    }
}
