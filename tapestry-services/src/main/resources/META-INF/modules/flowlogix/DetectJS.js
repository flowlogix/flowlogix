/* 
 * trigger ajax request by URI
 * Detect JavaScript presence
 */

(function() {
  define(["t5/core/ajax"], function(ajax) {
      return function(uri) {
          ajax(uri, {});
      };
  });
}).call(this);
