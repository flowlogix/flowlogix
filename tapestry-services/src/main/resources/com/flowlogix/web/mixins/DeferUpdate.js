/** 
 * Triggers a zone update after a timer expires
 */

var DeferUpdate = Class.create();
DeferUpdate.prototype = {
    initialize: function(zoneId) {
        this.zoneId = zoneId;
        this.handler = this.doDefer.bindAsEventListener(this);
        Event.observe($(zoneId), Tapestry.ZONE_UPDATED_EVENT,
            this.handler);
    },
		
    doDefer: function() {
        if($(this.zoneId).PeriodicUpdater.numUpdates == 0)
        {
            $(this.zoneId).PeriodicUpdater.stop();
            $(this.zoneId).PeriodicUpdater.start(true);                            
        }
        else
        {
            $(this.zoneId).PeriodicUpdater.numUpdates = 0;
        }
    }
};


// Extend the Tapestry.Initializer with a static method that instantiates us
Tapestry.Initializer.deferUpdate = function(zoneId) {
    new DeferUpdate(zoneId);
};
