/* 
 * Copyright 2013 lprimak.
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


/*
 * sends periodic heartbeats frmo the browser to the server
 * to either detect session timeout or to keep session alive
 */

(function() {
    define(["t5/core/ajax"], function(ajax) {
        var self = {};
        
        var checkIdle = function() {
            ajax(self.baseURI + "checkidle" + self.defaultURIparameters + self.keepAlive
                    + '&timestamp=' + (new Date()).getTime(), {
                success: handleIdleCheckResult,
                failure: endHandler,
                exception: endHandler
            });
        };

        var checkIdleNext = function(nextCheck) {
            if (typeof(nextCheck) === 'undefined' || nextCheck <= 0)
                return;
            if (self.idleCheckId !== null)
                clearTimeout(self.idleCheckId);
            self.idleCheckId = window.setTimeout(checkIdle, nextCheck * 1000);
        };

        var end = function(result) {
            if (self.endOnClose === false)
                return;
            window.location.reload();
        };

        var endHandler = function(result) {
            if (self.endedHandler !== null) {
                eval(self.endedHandler)(result);
            }
            else {
                end();
            }
        };
        
        var refresh = function() {
            ajax(self.baseURI + "refresh" + self.defaultURIparameters + 'true', {});
        };

        var handleIdleCheckResult = function(response) {
            var nextCheck = -1;
            self.reloadPageOnly = false;
            if (response.json !== null) {
                nextCheck = response.json.nextCheck;
                self.reloadPageOnly = response.json.reloadPageOnly;
                if (isNaN(self.reloadPageOnly))
                    self.reloadPageOnly = false;
            }
            if (isNaN(nextCheck))
                nextCheck = -1;
            if (nextCheck <= 0) {
                endHandler();
                return;
            }
            checkIdleNext(nextCheck);
        };
        
        return function(spec) {
            self.contextPath = spec.contextPath;
            self.baseURI = spec.baseURI;
            self.defaultURIparameters = spec.defaultURIparameters;
            self.keepAlive = spec.keepAlive;
            self.endOnClose = spec.endOnClose;
            self.idleCheckSeconds = spec.idleCheckSeconds;
            self.endedHandler = spec.endedHandler;
            self.idleCheckId = null;
            self.reloadPageOnly = false;

            if (spec.idleCheckSeconds !== null && spec.idleCheckSeconds > 0) {
                checkIdleNext(spec.idleCheckSeconds);
            }
        };
    });
}).call(this);
