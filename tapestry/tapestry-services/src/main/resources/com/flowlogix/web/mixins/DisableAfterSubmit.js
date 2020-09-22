/**
 * Disable Submit button after AJAX Form Submission
 */

var DisableAfterSubmit = Class.create();
DisableAfterSubmit.prototype = {
    initialize: function(elementId, formId) {
        var element = $(elementId);

        if(element.disablerAttached != undefined) {
            return;
        }
        
        this.formId = formId;
        this.elementId = elementId;
        this.handler = this.doEnable.bindAsEventListener(this);
        Event.observe(element, 'click',
            this.doDisable.bindAsEventListener(this));	
        element.disablerAttached = true;
    },

    doDisable: function(domevent) {
        var element = $(this.elementId);
        var form = $(this.formId);
        element.disable();
        var tapStorage = form.getStorage();
        var isZone = tapStorage.zoneId != null;

        if(isZone) {
            this.zoneElement = Tapestry.findZoneManager(this.formId).element;
            Event.observe(this.zoneElement, Tapestry.ZONE_UPDATED_EVENT, 
                this.handler); 
        }

        form.setSubmittingElement(element);
        form.onsubmit(domevent);
        if(tapStorage.validationError) {
            if(isZone) {
                this.doEnable();
            }
            else {
                element.enable();                       
            }
        }
        else {
            if(isZone == false) {
                form.submit();
            }
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
