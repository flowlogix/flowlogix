/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


var SessionMonitor = Class.create();


SessionMonitor.prototype = {
    initialize: function(contextPath, baseURI, defaultURIparameters, keepAlive, endOnClose, idleCheckSeconds, warnBeforeSeconds, 
        warnBeforeHandler, endedHandler) {
        this.contextPath = contextPath;
        this.baseURI = baseURI;
        this.defaultURIparameters = defaultURIparameters;
        this.keepAlive = keepAlive;
        this.endOnClose = endOnClose;
        this.idleCheckSeconds = idleCheckSeconds;
        this.endedHandler = endedHandler;
        this.idleCheckId = null;
        this.reloadPageOnly = false;
		
        if (idleCheckSeconds != null && idleCheckSeconds > 0) this.checkIdleNext(idleCheckSeconds);
    },

    checkIdle: function() {
        new Ajax.Request(this.baseURI + "checkidle" + this.defaultURIparameters + this.keepAlive
            +'&timestamp='+(new Date()).getTime(), {
                method: 'get',
                evalJSON:true,
                onSuccess: this.handleIdleCheckResult.bind(this),
                onFailure: this.endHandler.bind(this)
            });
    },

    end: function() {
        if (!this.endOnClose) return;
        new Ajax.Request(this.baseURI + "end" + this.defaultURIparameters + false, {
            method: 'get'
        });
    },
    
    endHandler : function() {
        if (this.endedHandler != null) {
                this.callHandler(this.endedHandler);
            }
            else {
                if(this.reloadPageOnly == false) {
                    alert('Your Session Has Expired');
                }
                window.location.reload();
            }
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
        handlerName = "SessionMonitor." + handlerName;
        var operation = eval(handlerName);
        operation(arg);
    },
		
    handleIdleCheckResult: function(transport) {
        var nextCheck = -1;
        this.reloadPageOnly = false;
        if(transport.responseJSON != null) {
            nextCheck = transport.responseJSON.nextCheck;  
            this.reloadPageOnly = transport.responseJSON.reloadPageOnly;
            if (isNaN(this.reloadPageOnly)) this.reloadPageOnly = false; 
        }            
        if (isNaN(nextCheck)) nextCheck = -1; 
        if (nextCheck <= 0 ) {
            this.endHandler();
            return;
        }
        this.checkIdleNext(nextCheck);
    }
}
