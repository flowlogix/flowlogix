function toggleDisabled(el) {
    try {
        el.disabled = el.disabled ? false : true;
                    
        //  if this is a link
        if (el.tagName && el.tagName.toLowerCase() == "a") {
            var link = LinkCache.getLink(el);
                      
            //  disable or enable it
            if (el.disabled) {
                link.disable();
            }
            else {
                link.enable();
            }
        }
    }
    catch(E){ }
                
    if (el.childNodes && el.childNodes.length > 0) {
        for (var x = 0; x < el.childNodes.length; x++) {
            toggleDisabled(el.childNodes[x]);
        }
    }
}
            
/**
             * represents a link element
             */
function Link(el) {
    function generateId(el) {
        self.Link_ids = self.Link_ids || 0;
        var id = "_link_" + self.Link_ids++ + "_" + new Date().getTime();
        el.id = id;
        return id;
    }
              
    this.disable = function() {
        this.element.onclick = function(){
            return false;
        };
    }
              
    this.enable = function() {
        this.element.onclick = this.onclick;
    }
              
    this.element = el;
    this.id = el.id || generateId(el);
    this.href = el.href;
    this.onclick = el.onclick;
                            
    return this;
}
            
/**
             * stores Links
             */
function LinkCache() {
    function getCache() {
        this._cache = this._cache || [];
        return this._cache;
    }
              
    /** add a Link to the cache */
    function add(l) {
        getCache().push(l);
    }
              
    /** get a Link from cache */
    function getLink(el) {
        var _cache = getCache();
        var link = null;
        var id = el.id;
                
            //  see if el exists in cache
            FIND:
            for (var x = 0; x < _cache.length; x++) {
                if (_cache[x].id == id) {
                    link = _cache[x];
                    break FIND;
                }
            }
                
        //  if still null, create Link
        if (link == null) LinkCache.add( link = new Link(el) );
        return link;
    }
}
