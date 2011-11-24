// when manual input is erroneous, do not pop up validatio errors here
Tapestry.DateField.prototype.sendServerRequest = function(url, input, resultHandler, errorHandler) {
    var successHandler = function(response) {
        var json = response.responseJSON;

        var result = json.result;

        if (result == null)
        {
            result = new Date();
            this.datePicker.setDate(null, false);
        }
        resultHandler.call(this, result);
    }.bind(this);

    Tapestry.ajaxRequest(url, {
        method : 'get',
        parameters : {
            input : input
        },
        onSuccess : successHandler
    });
}
