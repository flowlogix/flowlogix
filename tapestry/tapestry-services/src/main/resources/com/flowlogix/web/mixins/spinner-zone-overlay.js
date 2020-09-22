// A script that detects when any Form is being submitted or any component issues a request involving a zone. It reacts by overlaying the 
// zone with a div of class "zone-loading-overlay". The idea is that you should define that class, in css, to display an animated GIF.
//
// Based on a solution by Howard Lewis Ship at http://tapestryjava.blogspot.co.uk/2011/12/adding-ajax-throbbers-to-zone-updates.html .
// Written in Protoype style because Tapestry includes the Protoype library (http://www.prototypejs.org/).

Tapestry.onDOMLoaded(function() {

	function addZoneOverlay(event, element) {
        var mgr = Tapestry.findZoneManager(element);
        var zone = mgr && mgr.element;

        if (!zone) {
            return;
        }

        zone.insert({top:"<div class='zone-loading-overlay'/>"});
        var zoneDims = zone.getDimensions()
        var overlay = zone.down("div");

        overlay.setStyle({
            width: zoneDims.width + "px",
            height: zoneDims.height + "px" 
        });
    }

	// Tell document body to call addAjaxOverlay whenever a Form is submitted or a zone-related form or link is clicked.
	
    $(document.body).on(Tapestry.FORM_PROCESS_SUBMIT_EVENT, addZoneOverlay);
    $(document.body).on(Tapestry.TRIGGER_ZONE_UPDATE_EVENT, addZoneOverlay);
}); 
