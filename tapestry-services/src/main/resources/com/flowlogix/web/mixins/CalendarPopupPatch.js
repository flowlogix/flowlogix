// sets the label showing the year and selected month
DatePicker.prototype._setTopLabel = function ()
{
    var str = DatePicker.months[ this._calendarDate.getMonth() ] + " " + this._calendarDate.getFullYear();
    if (this._topLabel != null)
        this._topLabel.lastChild.data = str;
}


DatePicker.prototype._showLabelPopup = function ()
{
    var dateContext = function (dp, d)
    {
        return function (e)
        {
            dp._hideLabelPopup();
            dp.setCalendarDate(d);
            return false;
        };
    };

    var dp = this;

	// clear all old elements in the popup
    while (this._labelPopup.hasChildNodes())
        this._labelPopup.removeChild(this._labelPopup.firstChild);

    var a, tmp, tmp2;
    for (var i = -3; i < 4; i++)
    {
        tmp = new Date(this._calendarDate);
        tmp2 = new Date(this._calendarDate);	// need another tmp to catch year change when checking leap
        tmp2.setDate(1);
        tmp2.setMonth(tmp2.getMonth() + i);
        tmp.setDate(Math.min(tmp.getDate(), DatePicker.getDaysPerMonth(tmp.getMonth() + i,
                tmp2.getFullYear())));
        tmp.setMonth(tmp.getMonth() + i);

        a = this._document.createElement("a");
        a.href = "javascript:void 0;";
        a.onclick = dateContext(dp, tmp);
        a.appendChild(this._document.createTextNode(DatePicker.months[ tmp.getMonth() ] + " " + tmp.getFullYear()));
        if (i == 0)
            a.className = "selected";
        this._labelPopup.appendChild(a);
    }

    this._topLabel.parentNode.insertBefore(this._labelPopup, this._topLabel.parentNode.firstChild);
};
