/**
 * Periodically Update a Zone
 */

(function() {
    define(["t5/core/dom", "t5/core/events"], function(dom, events) {
        var dataTag = 'periodic-updater-spec';
        
        var init = function(id, period, uri, runOnce)
        {
            dom(id).meta(dataTag, {id: id, period: period, uri: uri,
                runOnce: runOnce,
                numUpdates: 0
            });

            if (runOnce == false) {
                start(id);
            }
            else {
                dom(id).on(events.zone.didUpdate, function() {
                    var self = this.meta(dataTag);
                    if (self.timerId == null) {
                        start(self.id);
                    }
                    else {
                        stop(id);
                        if(self.numUpdates == 0) {
                            start(id);
                        }
                    }
                });
            }
        };

        var start = function(id) {
            var self = dom(id).meta(dataTag);
            self.numUpdates = 0;
            self.timerId = window.setInterval(function() {
                dom(id).trigger(events.zone.refresh, {
                    url: self.uri});
                ++self.numUpdates;
            }, self.period * 1000);
        };

        var stop = function(id) {
            var self = dom(id).meta(dataTag);
            if (self.timerId != null) {
                window.clearInterval(self.timerId);
                self.timerId = null;
            }
        };

        return {
            init: init,
            start: start,
            stop: stop
        };
    });
}).call(this);
