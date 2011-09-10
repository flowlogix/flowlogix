/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


var SessionMonitor = Class.create();

function notImplemented(notImplemented) {
    alert("Not yet implemented " + notImplemented);
}

SessionMonitor.prototype = {
    initialize: function(baseURI, defaultURIparameters, keepAlive, endOnClose, idleCheckSeconds, warnBeforeSeconds, 
        warnBeforeHandler, endedHandler) {
        this.baseURI = baseURI;
        this.defaultURIparameters = defaultURIparameters;
        this.keepAlive = keepAlive;
        this.endOnClose = endOnClose;
        this.idleCheckSeconds = idleCheckSeconds;
        this.warnBeforeSeconds = warnBeforeSeconds;
        this.warnBeforeHandler = warnBeforeHandler;
        this.endedHandler = endedHandler;
        this.idleCheckId = null;
		
        if (idleCheckSeconds != null && idleCheckSeconds > 0) this.checkIdleNext(idleCheckSeconds);
    },

    checkIdle: function() {
        new Ajax.Request(this.baseURI + "checkidle" + this.defaultURIparameters + this.keepAlive + '&warn=' + this.warnBeforeSeconds
            +'&timestamp='+(new Date()).getTime(), {
                method: 'get',
                evalJSON:true,
                onSuccess: this.handleIdleCheckResult.bind(this)
            });
    },

    end: function() {
        if (!this.endOnClose) return;
        new Ajax.Request(this.baseURI + "end" + this.defaultURIparameters + false, {
            method: 'get'
        });
    },
	
    refresh: function() {
        new Ajax.Request(this.baseURI + "refresh" + this.defaultURIparameters + 'true', {
            method: 'get'
        });
    },

    checkIdleNext: function(nextCheck) {
        if (typeof(nextCheck) == 'undefined' || nextCheck <= 0) return;
        if (this.idleCheckId != null) clearTimeout(this.idleCheckId);
        this.idleCheckId = setTimeout(this.checkIdle.bind(this), nextCheck * 1000);
    },
	
    callHandler : function(handlerName, arg) {
        // handlerName should be a string identifier of form "obj.property.function"
        var pos = handlerName.lastIndexOf('.');
        var context = null;
        if (pos > 0 ) context = eval(handlerName.substring(0,pos));
        if (handlerName.substr(handlerName.length-2,2) == '()' ) handlerName = handlerName.substring(0,handlerName.length - 2);
        var operation = eval(handlerName);
        // FIXME should log something if operation doesn't exist
        if (typeof(operation) == 'function') {
            if (context == null) operation(arg);
            else operation.bind(context)(arg);
        }
    },
	
    warnOfEnd : function(inSeconds) {
        if (this.warnBeforeHandler != null) {
            this.callHandler(this.warnBeforeHandler, inSeconds);
        }
        else alert('The page will become idle soon...');
    },
	
    handleIdleCheckResult: function(transport) {
        var nextCheck = transport.responseJSON.nextCheck;
        if (isNaN(nextCheck)) nextCheck = -1; 
        if (nextCheck <= 0 ) {
            if (this.endedHandler != null) this.callHandler(this.endedHandler);
            return;
        }
        var warnFor = transport.responseJSON.warn;
        if (!isNaN(warnFor)) if (warnFor > 0) this.warnOfEnd(warnFor);

        this.checkIdleNext(nextCheck);
    }
}
