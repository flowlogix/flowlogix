// when manual input is erroneous, do not pop up validatio errors here
Tapestry.DateField.prototype.triggerClicked = function() {
    if (this.field.disabled)
        return;

    if (this.popup == null) {
        this.createPopup();

    } else {
        if (this.popup.visible()) {
            this.hidePopup();
            return;
        }
    }

    var value = $F(this.field).escapeHTML();

    if (value == "") {
        this.datePicker._selectedDate = null;
        this.datePicker.setDate(null);
        this.positionPopup();
        this.revealPopup();
        return;
    }

    var resultHandler = function(result) {
        var date = new Date();
        date.setTime(result);
        this.datePicker.setDate(date);
        this.positionPopup();
        this.revealPopup();
    };

    var errorHandler = function(message) {
        this.datePicker._selectedDate = null;
        this.datePicker.setDate(null);
        this.positionPopup();
        this.revealPopup();
    };

    this.sendServerRequest(this.parseURL, value, resultHandler,
        errorHandler);
}
