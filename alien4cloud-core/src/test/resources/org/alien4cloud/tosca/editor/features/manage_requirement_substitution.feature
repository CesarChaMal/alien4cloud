Feature: Topology editor: requirement substitution

  Background:
    Given I am authenticated with "ADMIN" role
    And I create an empty topology template "TopologyTemplate1"

  Scenario: Add a requirement substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | topologyId  |                                                                                   |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                              |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    And The SPEL expression "substitutionMapping.requirements['network'].nodeTemplateName" should return "Compute"

  Scenario: Add a non existing requirement as requirement substitution should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | topologyId  |                                                                                   |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                              |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network_failed                                                                               |
      | substitutionRequirementId | network                                                                                      |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Add requirement as requirement substitution with an already used substitutionRequirementId should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | topologyId  |                                                                                   |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                              |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                              |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | dependency                                                                                   |
      | substitutionRequirementId | network                                                                                      |
    Then an exception of type "alien4cloud.exception.AlreadyExistException" should be thrown

  Scenario: Remove a requirement substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | topologyId  |                                                                                   |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                              |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    And The SPEL expression "substitutionMapping.requirements['network'].nodeTemplateName" should return "Compute"
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.RemoveRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                                 |
      | substitutionRequirementId | network                                                                                         |
    And The SPEL expression "substitutionMapping.requirements['network']" should return "null"

  Scenario: Remove a non existing requirement substitution should failed
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | topologyId  |                                                                                   |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.RemoveRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                                 |
      | substitutionRequirementId | network                                                                                         |
    Then an exception of type "alien4cloud.exception.NotFoundException" should be thrown

  Scenario: Update a requirement substitution
    When I execute the operation
      | type        | org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation |
      | topologyId  |                                                                                   |
      | elementId   | tosca.nodes.Compute                                                               |
    When I execute the operation
      | type              | org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation |
      | nodeName          | Compute                                                               |
      | indexedNodeTypeId | tosca.nodes.Compute:1.0                                               |
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                              |
      | nodeTemplateName          | Compute                                                                                      |
      | requirementId             | network                                                                                      |
      | substitutionRequirementId | network                                                                                      |
    And The SPEL expression "substitutionMapping.requirements['network'].nodeTemplateName" should return "Compute"
    When I execute the operation
      | type                      | org.alien4cloud.tosca.editor.operations.substitution.UpdateRequirementSubstitutionTypeOperation |
      | topologyId                |                                                                                                 |
      | substitutionRequirementId | network                                                                                         |
      | newRequirementId          | network_bis                                                                                     |
    And The SPEL expression "substitutionMapping.requirements['network_bis'].nodeTemplateName" should return "Compute"
