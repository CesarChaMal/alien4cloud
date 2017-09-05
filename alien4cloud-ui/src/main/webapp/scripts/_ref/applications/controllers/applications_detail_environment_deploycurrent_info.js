define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');
  require('scripts/deployment/directives/display_outputs');

  states.state('applications.detail.environment.deploycurrent.info', {
    url: '/info',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_info.html',
    controller: 'ApplicationEnvDeployCurrentInfoCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.info',
      state: 'applications.detail.environment.deploycurrent.info',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentInfoCtrl',
  ['$scope', 'authService', 'applicationServices', 'application', '$state','breadcrumbsService', '$translate',
  function($scope, authService, applicationServices, applicationResult, $state, breadcrumbsService, $translate) {

    breadcrumbsService.putConfig({
      state : 'applications.detail.environment.deploycurrent.info',
      text: function(){
        return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.INFO');
      },
      onClick: function(){
        $state.go('applications.detail.environment.deploycurrent.info');
      }
    });

    $scope.applicationServices = applicationServices;
    $scope.fromStatusToCssClasses = alienUtils.getStatusIconCss;

    /* Tag name with all letters a-Z and - and _ and no space */
    $scope.tagKeyPattern = /^[\-\w\d_]*$/;
    $scope.application = applicationResult.data;

    $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    $scope.isDeployer = authService.hasResourceRole($scope.application, 'APPLICATION_DEPLOYER');
    $scope.isDevops = authService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    $scope.isUser = authService.hasResourceRole($scope.application, 'APPLICATION_USER');
    $scope.newAppName = $scope.application.name;

    $scope.isAllowedModify = _.defined($scope.application.topologyId) && ($scope.isManager || $scope.isDevops);

    // switch back to 'current deploy' when undeployed completed
    $scope.$watch('environment', function () {
      if ($scope.environment.status === 'UNDEPLOYED') {
        $state.go('applications.detail.environment.deploynext');
      }
    }, true);
  }
]);
});
