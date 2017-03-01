define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');
  var GROUP_ALL = '_A4C_ALL';

  require('scripts/deployment/directives/display_outputs');
  require('scripts/common/filters/inputs');
  require('scripts/applications/services/locations_matching_services');
  require('scripts/applications/services/deployment_topology_services');
  require('scripts/applications/services/deployment_topology_processor.js');
  require('scripts/applications/services/tasks_processor.js');

  states.state('applications.detail.deployment.locations', {
    url: '/locations',
    templateUrl: 'views/applications/application_deployment_locations.html',
    controller: 'ApplicationDeploymentLocationCtrl',
    menu: {
      id: 'am.applications.detail.deployment.locations',
      state: 'applications.detail.deployment.locations',
      key: 'ORCHESTRATORS.NAV.LOCATIONS',
      icon: 'fa fa-cloud-upload',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 200,
      step: {
        // task code in validation DTO bound to this step
        taskCodes: ['LOCATION_POLICY', 'LOCATION_DISABLED', 'LOCATION_UNAUTHORIZED']
      }
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentLocationCtrl',
    ['$scope', 'locationsMatchingServices', '$state', 'menu', 'deploymentTopologyServices',
      function($scope, locationsMatchingServices, $state, menu, deploymentTopologyServices) {
        var thisMenu = _.find(menu, {id:'am.applications.detail.deployment.locations' });
        if (_.has($scope, 'deploymentContext.deploymentTopologyDTO.topology.orchestratorId') && _.has($scope, 'deploymentContext.deploymentTopologyDTO.locationPolicies.' + GROUP_ALL)) {
          $scope.oldSelectedOrchestratorId = $scope.deploymentContext.deploymentTopologyDTO.topology.orchestratorId;
          $scope.oldSelectedLocationId = $scope.deploymentContext.deploymentTopologyDTO.locationPolicies[GROUP_ALL];
        }

        function formatLocationMatches(locationMatches) {
          $scope.deploymentContext.locationMatches = {};
          _.each(locationMatches, function(locationMatch) {
            $scope.deploymentContext.locationMatches[locationMatch.location.id] = locationMatch;
          });
        }

        //check and fill selected location from deploymentTopologyDTO
        function initSelectedLocation() {
          delete $scope.deploymentContext.selectedLocation;
          if (_.has($scope, 'deploymentContext.deploymentTopologyDTO.locationPolicies.' + GROUP_ALL)) {
            var selectedLocationId = $scope.deploymentContext.deploymentTopologyDTO.locationPolicies[GROUP_ALL];
            if ($scope.deploymentContext.locationMatches && $scope.deploymentContext.locationMatches[selectedLocationId]) {
              $scope.deploymentContext.selectedLocation = $scope.deploymentContext.locationMatches[selectedLocationId].location;
            }
          }
        }

        var refreshLocationMatching = function() {
          locationsMatchingServices.getLocationsMatches({topologyId: $scope.topologyId, environmentId: $scope.deploymentContext.selectedEnvironment.id}, function(result) {
            formatLocationMatches(result.data);
            initSelectedLocation();
          });
        };
        // Watch over deployment topology to initialize selected location
        $scope.$watch('deploymentContext.deploymentTopologyDTO', function() {
          refreshLocationMatching();
        });

        $scope.selectLocation = function(locationMatch) {
          var groupsToLocations = {};
          groupsToLocations[GROUP_ALL] = locationMatch.location.id;

          var configRequest = {
            orchestratorId: locationMatch.location.orchestratorId,
            groupsToLocations: groupsToLocations
          };

          deploymentTopologyServices.setLocationPolicies({
            appId: $scope.application.id,
            envId: $scope.deploymentContext.selectedEnvironment.id
          }, angular.toJson(configRequest), function(response) {
            $scope.updateScopeDeploymentTopologyDTO(response.data);
            $scope.deploymentContext.selectedLocation = locationMatch.location;
            $state.go(thisMenu.nextStep.state);
          });
        };

        // checks if a location is the selected one for this deployment
        $scope.isLocationSelected = function(location) {
          return _.get($scope, 'deploymentContext.selectedLocation.id') === location.id;
        };

      }
    ]); //controller
}); //Define
