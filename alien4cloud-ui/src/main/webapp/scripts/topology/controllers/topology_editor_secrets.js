/**
*  Service that provides functionalities to edit nodes in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/common/controllers/confirm_modal');

  modules.get('a4c-topology-editor').factory('topoEditSecrets', ['toscaService', '$filter', '$uibModal', '$translate',
    function(toscaService, $filter, $uibModal, $translate) {

      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        toggleGetSecret: function(property) {
          // tranform the property as get secret function
          //  ==> ui shlould display the getSecret label, the input field for the path and a button to add a plugin configuration
          var scope = this.scope;
          if (scope.properties.isGetSecretProperty(property.value)) {
            // reset the secret to originalValue
            property.value.is_secret = false;
            property.value = property.originalValue;
            property.originalValue = undefined;
          } else {
            property.originalValue = property.value;
            property.value = {function:"get_secret", path:"", value:"get_secret", is_secret:true};  // short version
          }

        },
        updateSecretPath: function(property) {
          // sent the get function property with the path secret to Alien backend
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.secrets = instance;
      };
    }
  ]); // modules
}); // define
