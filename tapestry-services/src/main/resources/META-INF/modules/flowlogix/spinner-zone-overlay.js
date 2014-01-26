/**
 * Shows Ajax loading spinner while waiting for results
 */

(function() {
    define(["t5/core/dom", "t5/core/events", "t5/core/zone"], function(dom, events, zone) {
        var createOverlay = function(elt, isZone) {
            var zoneElt = elt;
            if (isZone != true) {
                try {
                    zoneElt = zone.findZone(elt);
                }
                catch(err) { }
            }
            if (zoneElt == null) {
                return;
            }
            
            zoneElt.prepend("<div class='zone-loading-overlay'/>");
            var overlay = zoneElt.$.find("div:first");
            var zoneDims = { width: zoneElt.$.width(), height: zoneElt.$.height() };

            overlay.css({
                width: zoneDims.width + "px",
                height: zoneDims.height + "px"
            });

        };
        
        var removeOverlay = function(elt) {
            elt.$.find("div.zone-loading-overlay").remove();
        };

        return function() {
            dom.onDocument(events.zone.willUpdate, function() {
                createOverlay(this, true);
            });
            dom.onDocument(events.zone.didUpdate, function() {
                removeOverlay(this);
            });
            dom.onDocument(events.form.prepareForSubmit, function() {
                createOverlay(this, false);
            });
        };
    });
}).call(this);
