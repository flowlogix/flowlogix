/**
 * Generate Events after Zone Update
 */

(function() {
    define(["t5/core/dom", "t5/core/events", "t5/core/ajax"], function(dom, events, ajax) {
        var updateEvent = function(id, uri) {
            dom(id).on(events.zone.didUpdate, function() {
                ajax(uri, {
                success: checkSession,
                failure: reloadHandler,
                exception: reloadHandler
                });
            });
        };
        
        var checkSession = function(response) {
            if (response.json != null) {
                reloadPage = response.json.reloadPage;
                if (!isNaN(reloadPage))
                    reloadHandler();
            }
        };
        
        var reloadHandler = function() {
            window.location.reload();
        };
        
        return {
            updateEvent: updateEvent
        };

    });
}).call(this);
