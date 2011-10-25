/**
 * Disable Submit button after AJAX Form Submission
 */

var DisableAfterSubmit = Class.create();
DisableAfterSubmit.prototype = {
    initialize: function(elementId, formId) {
        this.formId = formId;
        this.elementId = elementId;
        this.handler = this.doEnable.bindAsEventListener(this);

        Event.observe($(elementId), 'click',
            this.doDisable.bindAsEventListener(this));			
    },

    doDisable: function() {
        $(this.elementId).disable();
        
        this.zoneId = Tapestry.findZoneManager(this.formId).element;
        Event.observe($(this.zoneId), Tapestry.ZONE_UPDATED_EVENT, 
            this.handler);    

        $(this.formId).onsubmit();
    },
		
    doEnable: function() {
        $(this.zoneId).stopObserving(Tapestry.ZONE_UPDATED_EVENT, this.handler);
        var element = $(this.elementId);
        if(element != null) {            
            $(this.elementId).enable();
        }
    }
};


// Extend the Tapestry.Initializer with a static method that instantiates us
Tapestry.Initializer.disableAfterSubmit = function(spec) {
    new DisableAfterSubmit(spec.elementId, spec.formId);
};
