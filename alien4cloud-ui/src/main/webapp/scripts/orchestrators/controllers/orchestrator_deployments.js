define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  require('scripts/deployment/services/deployment_services');

  states.state('admin.orchestrators.details.deployments', {
    url: '/deployments',
    templateUrl: 'views/orchestrators/orchestrator_deployments.html',
    controller: 'OrchestratorDeploymentsCtrl',
    menu: {
      id: 'menu.orchestrators.deployments',
      state: 'admin.orchestrators.details.deployments',
      key: 'ORCHESTRATORS.NAV.DEPLOYMENTS',
      icon: 'fa fa-rocket',
      priority: 200
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorDeploymentsCtrl',
    ['$scope', '$uibModal', '$state', 'deploymentServices', 'orchestrator', 'breadcrumbsService', '$translate',
    function($scope, $uibModal, $state, deploymentServices, orchestrator, breadcrumbsService, $translate) {
      $scope.orchestrator = orchestrator;

      breadcrumbsService.putConfig({
        state: 'admin.orchestrators.details.deployments',
        text: function() {
          return $translate.instant('ORCHESTRATORS.NAV.DEPLOYMENTS');
        }
      });

      function processDeployments(deployments){
        if (_.defined(deployments)){
          _.each(deployments, function(deploymentDTO){
            if(_.defined(deploymentDTO.locations)){
              deploymentDTO.locations = _.indexBy(deploymentDTO.locations, 'id');
            }
          });
        }
      }

      //get all deployments for this cloud
      deploymentServices.get({
        orchestratorId: $scope.orchestrator.id,
        includeSourceSummary: true
      }, function(result) {
        processDeployments(result.data);
        $scope.deployments = result.data;
      });

      //Go to runtime view for a deployment
      $scope.goToRuntimeView = function(deployment){
        if(_.defined(deployment.endDate)){
          // do nothing as the deployment is ended already
          return;
        }
        $state.go('applications.detail.runtime', {
          id:deployment.sourceId,
          openOnEnvironment: deployment.environmentId
        });
      };

    }
  ]); // controller
}); // define
