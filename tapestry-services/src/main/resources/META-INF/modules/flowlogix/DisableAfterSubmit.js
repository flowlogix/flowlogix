/* 
 * Copyright 2013 lprimak.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* 
 * Disable Buttons after Submit
 */

(function() {
  define(["t5/core/dom", "t5/core/events", "underscore", "t5/core/zone"], 
  function(dom, events, _, zone) {
      var metaButtonIdStr = 'disabled-after-submit-id';
      var metaButtonExcluded = 'disable-after-submit-excluded';
      
      var enableButtons = function(self) {
            var btnElt = self.meta(metaButtonIdStr);
            if (btnElt != null) {
                btnElt.attribute('disabled', null);
                self.meta(metaButtonIdStr, null);
            }
      };
      
      var findZone = function(elt) {
          var formElt = elt.findParent('form');
          var zoneElt = null;
          try {
            zoneElt = (formElt !== null)? zone.findZone(formElt) : null; 
          }
          catch(err) { }
          return zoneElt;
      };
      
      return function() {
          dom.onDocument('click', 'input[type=submit], input[type=button]', function() { 
              if(this.meta(metaButtonExcluded) == true) { return; }
              var zoneElt = findZone(this);
              if(zoneElt !== null) {
                  enableButtons(zoneElt);
                  zoneElt.meta(metaButtonIdStr, this);
              }
                _.defer(function(self) {
                    if (zoneElt.meta(metaButtonIdStr) !== null) {
                        self.attribute('disabled', 'disabled');
                    }
                }, this);
          });
          
          dom.onDocument(events.zone.didUpdate, function() {
              enableButtons(this);
          });
          
          dom.onDocument(events.field.showValidationError, function() {
              var zoneElt = findZone(this);
              if(zoneElt != null) {
                enableButtons(zoneElt);
              }
          });
      };
  });
}).call(this);
