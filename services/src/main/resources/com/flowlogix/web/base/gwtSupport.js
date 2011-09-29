/** 
 * GWT - Tapestry Integration Root Components Dictionary
 * 
 */

var gwtComponents = new Array();

var GWTComponentController = function() {
    return {
        add : function(key, value) {
            array = gwtComponents[key];
            if( array == null ) {
                array = new Array();
                gwtComponents[key] = array;
            }
            array[array.length] = value;
        }
    }
}
();
