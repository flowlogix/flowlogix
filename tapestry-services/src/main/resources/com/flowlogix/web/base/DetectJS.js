/**
 * Detect JavaScript presence
 */

var DetectJS = Class.create();
DetectJS.prototype = {
    initialize: function(uri) {
        new Ajax.Request(uri, { method: 'post' });
    }
};

// Extend the Tapestry.Initializer with a static method that instantiates us
Tapestry.Initializer.detectJS = function(uri) {
    new DetectJS(uri);
};
