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
        
        if(document.getElementById(elementId) != null) {
            Event.observe($(elementId), Tapestry.ZONE_UPDATED_EVENT, 
                this.handler);    
        }
    },
    
    triggerEvent : function() {
        new Ajax.Request(this.uri, {
            method: 'post', 
            evalJSON:true,
            onSuccess: this.checkSession.bind(this),
            onFailure: this.reloadHandler.bind(this)
            });
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

// Extend the Tapestry.Initializer with a static method that instantiates us
Tapestry.Initializer.updateEvent = function(spec) {
    new UpdateEvent(spec.elementId, spec.uri);
};
