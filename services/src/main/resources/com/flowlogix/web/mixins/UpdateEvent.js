/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

var UpdateEvent = new Class.create();

UpdateEvent.prototype = {
    initialize : function(elementId, uri) {
        this.elementId = elementId;
        this.uri = uri;
        this.handler = this.triggerEvent.bindAsEventListener(this);
        
        Event.observe($(elementId), Tapestry.ZONE_UPDATED_EVENT, 
            this.handler);
    },
    
    triggerEvent : function() {
        new Ajax.Request(this.uri, {
            method: 'get', 
            evalJSON:true,
            onSuccess: this.checkSession.bind(this),
            onFailure: this.reloadHandler.bind(this)
            });
        $(this.elementId).stopObserving(Tapestry.ZONE_UPDATED_EVENT, this.handler);
    },
    
    checkSession: function(transport) {
        this.reloadPageOnly = false;
        if(transport.responseJSON != null) {
            reloadPage = transport.responseJSON.reloadPage;  
            if (!isNaN(reloadPage)) this.reloadHandler();
        }            
    },

    reloadHandler : function() {
        window.location.reload();
    }
};
