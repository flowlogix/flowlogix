/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

var UpdateEvent = new Class.create();

UpdateEvent.prototype = {
    initialize : function(elementId, eventId, uri) {
        this.eventId = eventId;
        this.elementId = elementId;
        this.uri = uri;
        this.handler = this.triggerEvent.bindAsEventListener(this);
        
        Event.observe($(elementId), Tapestry.ZONE_UPDATED_EVENT, 
            this.handler);
    },
    
    triggerEvent : function() {
        new Ajax.Request(this.uri, {method: 'get'});
        $(this.elementId).stopObserving(Tapestry.ZONE_UPDATED_EVENT, this.handler);
    }
};
