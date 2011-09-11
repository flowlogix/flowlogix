/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

var UpdateEvent = new Class.create();

UpdateEvent.prototype = {
    initialize : function(elementId, eventId, uri) {
        this.eventId = eventId;
        this.uri = uri;
        
        Event.observe($(elementId), Tapestry.ZONE_UPDATED_EVENT, 
            this.triggerEvent.bindAsEventListener(this));
    },
    
    triggerEvent : function() {
        new Ajax.Request(this.uri, { method: 'get' });
    }
};
