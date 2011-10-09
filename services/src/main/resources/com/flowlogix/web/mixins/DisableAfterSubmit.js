/**
 * Disable Submit button after AJAX Form Submission
 */

var DisableAfterSubmit = Class.create();
DisableAfterSubmit.prototype = {
    initialize: function(elementId, formId) {
        this.formId = formId;
        this.elementId = elementId;

        Event.observe($(elementId), 'click',
            this.doDisable.bindAsEventListener(this));			
    },

    doDisable: function() {
        $(this.elementId).disable();
        if(this.zoneId == null) {	
            this.zoneId = Tapestry.findZoneManager(this.formId).element;
            Event.observe($(this.zoneId), Tapestry.ZONE_UPDATED_EVENT,
                this.doEnable.bindAsEventListener(this));
        }

        $(this.formId).onsubmit();
    },
		
    doEnable: function() {
        $(this.elementId).enable();
    }
};
