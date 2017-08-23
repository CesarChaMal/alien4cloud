define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_version');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_topology');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_inputs');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_locations');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_matching');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_deploy');

  require('scripts/applications/services/deployment_topology_services.js');
  require('scripts/applications/services/deployment_topology_processor.js');
  require('scripts/applications/services/tasks_processor.js');
  require('scripts/applications/services/locations_matching_services.js');

  states.state('applications.detail.environment.deploynext', {
    url: '/deploy_next',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext.html',
    controller: 'ApplicationEnvDeployNextCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext',
      state: 'applications.detail.environment.deploynext',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT',
      icon: '',
      priority: 100
    },
    resolve: {
      deploymentTopologyDTO: ['$stateParams', 'deploymentTopologyServices', 'deploymentTopologyProcessor', 'tasksProcessor',
        function($stateParams, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor) {
          return _.catch(function() {
            return deploymentTopologyServices.get({
              appId: $stateParams.id,
              envId: $stateParams.environmentId
            }).$promise.then(function(response) {
              var deploymentTopologyDTO = response.data;
              deploymentTopologyProcessor.process(deploymentTopologyDTO);
              tasksProcessor.processAll(deploymentTopologyDTO.validation);
              return deploymentTopologyDTO;
            });
          });
      }]
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployNextCtrl',
    ['$scope', '$state', 'menu', 'deploymentTopologyDTO', 'deploymentTopologyProcessor', 'tasksProcessor', 'locationsMatchingServices',
    function ($scope, $state, menu, deploymentTopologyDTO, deploymentTopologyProcessor, tasksProcessor, locationsMatchingServices) {
      $scope.deploymentTopologyDTO = deploymentTopologyDTO;

      // Initialize menu by setting next step property.
      for(var i=0; i<menu.length-1; i++) {
        menu[i].nextStep = menu[i+1];
      }
      $scope.menu = menu;

      $scope.onItemClick = function($event, menuItem) {
        if (menuItem.disabled) {
          $event.preventDefault();
          $event.stopPropagation();
        }
      };

      var isLocationStepEnabled = false;
      // Fetch location matches information that are not in the deploymentTopologyDTO
      function initLocationMatches() {
        // If the location step is enabled fetch location matches
        if(isLocationStepEnabled) {
          locationsMatchingServices.getLocationsMatches({topologyId: deploymentTopologyDTO.topology.id, environmentId: $scope.environment.id}, function(result) {
            locationsMatchingServices.processLocationMatches($scope, result.data);
          });
        }
      }

      function updateStepsStatuses(menu, validationDTO) {
        // set the status of each menu, based on the defined taskCodes and their presence in the validationDTO
        isLocationStepEnabled = false;
        var nextDisabled = false;
        _.each(menu, function(menuItem) {
          if(_.definedPath(menuItem, 'step.taskCodes')) {
            delete menuItem.step.status;
            if(nextDisabled) {
              menuItem.disabled = true;
              return;
            }
            menuItem.step.status = 'SUCCESS';
            _.each(menuItem.step.taskCodes, function(taskCode) {
              if(_.definedPath(validationDTO, 'taskList['+taskCode+']')) {
                menuItem.step.status = 'ERROR';
                nextDisabled = true;
                return false; // stop the _.each
              }
            });
          }
          if(menuItem.state === 'applications.detail.environment.deploynext.locations') {
            isLocationStepEnabled = !menuItem.disabled;
          }
        });
        initLocationMatches();
      }

      updateStepsStatuses(menu, deploymentTopologyDTO.validation);

      $scope.updateScopeDeploymentTopologyDTO = function(deploymentTopologyDTO) {
        if(_.undefined(deploymentTopologyDTO)){
          return;
        }
        deploymentTopologyProcessor.process(deploymentTopologyDTO);
        tasksProcessor.processAll(deploymentTopologyDTO.validation);

        $scope.deploymentTopologyDTO = deploymentTopologyDTO;
        updateStepsStatuses(menu, deploymentTopologyDTO.validation);
      };

      // INITIALIZE the selected menu to the first invalid or latest step
      var goToNextInvalidStep = function() {
        //menus are sorted by priority. first step is the top one
        var stepToGo = $scope.menu[0];

        //look for the first invalid step, or the last one if all are valid
        while(stepToGo.nextStep){
          if(_.get(stepToGo, 'step.status', 'SUCCESS') !== 'SUCCESS'){
            break;
          }
          stepToGo = stepToGo.nextStep;
        }

        //go to the found step
        $state.go(stepToGo.state);
      };

      goToNextInvalidStep();
    }
  ]);
});
