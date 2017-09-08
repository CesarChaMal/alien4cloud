define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/_ref/applications/controllers/applications_detail_info');

  require('scripts/_ref/common/directives/breadcrumbs');
  require('scripts/_ref/common/services/breadcrumbs_service');

  require('scripts/common/services/user_context_services');


  states.state('applications.detail', {
    url: '/detail/:id',
    templateUrl: 'views/_ref/applications/applications_detail.html',
    controller: 'ApplicationDetailCtrl',
    resolve: {
      application: ['applicationServices', '$stateParams',
        function(applicationServices, $stateParams) {
          return _.catch(function() {
            return applicationServices.get({
              applicationId: $stateParams.id
            }).$promise;
          });
        }
      ],
      applicationEnvironmentsManager: ['application', 'applicationEnvironmentsManagerFactory',
        function(applicationResponse, applicationEnvironmentsManagerFactory) {
          return _.catch(function() {
            return applicationEnvironmentsManagerFactory(applicationResponse.data);
          });
        }
      ],
      archiveVersions: ['$http', 'application', 'applicationVersionServices',
        function($http, application, applicationVersionServices) {
          return _.catch(function() {
            var searchAppVersionRequestObject = {
              'from': 0,
              'size': 400
            };
            return applicationVersionServices.searchVersion({
              delegateId: application.data.id
            }, angular.toJson(searchAppVersionRequestObject)).$promise.then(function(result) {
              _.each(result.data.data, function(version) {
                _.each(version.topologyVersion, function(variant, variantKey) {
                  variant.version = variantKey;
                });
              });
              return result.data;
            });
          });
        }
      ]
    },
    params: {
      // optional id of the environment to automatically select when triggering this state
      openOnEnvironment: null
    }
  });

  modules.get('a4c-applications').controller('ApplicationDetailCtrl',['$scope','application', 'breadcrumbsService','$state',
    function($scope, application, breadcrumbsService, $state) {
      breadcrumbsService.putConfig({
        state : 'applications.detail',
        text: function(){
          return application.data.name;
        },
        onClick: function(){
          $state.go('applications.detail', { id: application.data.id });
        }
      });
  }]);
});
