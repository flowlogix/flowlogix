/**
 * Periodically Update a Zone
 */

Tapestry.PeriodicUpdater = Class.create({
    initialize: function(element, url, period) {      
        this.period = period; 
        this.element = element;
        this.url = url; 
        this.once = false;  
        
        this.numUpdates = 0;
    },
 
    start: function(once) {

        if(once == true)
            this.once = once;
        
        this.onUpdate = this.updateComplete.bind(this);
        this.timer = this.onTimerEvent.bind(this).delay(this.period);
    },
 
    stop: function() {
        this.onUpdate = undefined;
        clearTimeout(this.timer);
    },
 
    updateComplete: function() {
        if(this.once == false)
        {
            this.timer = this.onTimerEvent.bind(this).delay(this.period);                
        }
        else
        {
            this.timer = undefined;
            this.onUpdate = undefined;
        }
    },
 
    onTimerEvent: function() {
        var zoneObject = Tapestry.findZoneManagerForZone(this.element);
 
        if (!zoneObject) return;
 
        ++this.numUpdates;
        zoneObject.updateFromURL(this.url);
 
        (this.onUpdate || Prototype.emptyFunction).apply(this, arguments);
    }
});
 
Tapestry.Initializer.PeriodicUpdater = function(spec)
{
    var elementId = spec.elementId;
    var uri = spec.uri;
    var period = spec.period;
    
    $(elementId).PeriodicUpdater = new Tapestry.PeriodicUpdater(elementId, uri, period);
};

