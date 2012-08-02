Effect.Highlight.prototype.initialize = function(element) {
    this.element = $(element);
    if (!this.element) throw(Effect._elementDoesNotExistError);
    var options = Object.extend({ startcolor: '%s' }, arguments[1] || { });
    this.start(options);
  };
