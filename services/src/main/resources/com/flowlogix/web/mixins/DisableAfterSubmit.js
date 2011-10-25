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

        if($(this.formId).getStorage().zoneId != null) {
            this.zoneElement = Tapestry.findZoneManager(this.formId).element;
            Event.observe(this.zoneElement, Tapestry.ZONE_UPDATED_EVENT, 
                this.handler);    

            $(this.formId).setSubmittingElement($(this.elementId));
            $(this.formId).onsubmit();
        }
        else {
            $(this.formId).submit();
        }
    },
		
    doEnable: function() {
        this.zoneElement.stopObserving(Tapestry.ZONE_UPDATED_EVENT, this.handler);
        var element = $(this.elementId);
        if(element != null) {            
            element.enable();
        }
    }
};


// Extend the Tapestry.Initializer with a static method that instantiates us
Tapestry.Initializer.disableAfterSubmit = function(spec) {
    new DisableAfterSubmit(spec.elementId, spec.formId);
};
