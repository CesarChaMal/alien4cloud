package alien4cloud.paas.wf;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.declarative.DefaultDeclarativeWorkflows;
import org.alien4cloud.tosca.model.workflow.declarative.NodeDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.OperationDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipDeclarativeWorkflow;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipWeaving;
import org.alien4cloud.tosca.model.workflow.declarative.RelationshipWeavingDeclarativeWorkflow;
import org.apache.commons.lang3.StringUtils;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.paas.wf.util.WorkflowUtils;

public class DefaultWorkflowBuilder extends AbstractWorkflowBuilder {

    private static class Steps {
        private Map<String, WorkflowStep> operationSteps;
        private Map<String, WorkflowStep> stateSteps;
        private WorkflowStep delegateStep;

        private static Map<String, WorkflowStep> getNodeStateSteps(Workflow workflow, String nodeId) {
            // Get all state steps of the given node, then return a map of state name to workflow step
            return workflow.getSteps().values().stream().filter(step -> isNodeStep(step, nodeId) && step.getActivity() instanceof SetStateWorkflowActivity)
                    .collect(Collectors.toMap(step -> ((SetStateWorkflowActivity) step.getActivity()).getStateName(), step -> step));
        }

        private static Map<String, WorkflowStep> getNodeOperationSteps(Workflow workflow, String nodeId) {
            // Get all operation steps of the given node, then return a map of operation name to workflow step
            return workflow.getSteps().values().stream()
                    .filter(step -> isNodeStep(step, nodeId) && (step.getActivity() instanceof CallOperationWorkflowActivity))
                    .collect(Collectors.toMap(step -> ((CallOperationWorkflowActivity) step.getActivity()).getOperationName(), step -> step));
        }

        private static WorkflowStep getDelegateStep(Workflow workflow, String nodeId) {
            return workflow.getSteps().values().stream().filter(step -> isNodeStep(step, nodeId) && step.getActivity() instanceof DelegateWorkflowActivity)
                    .findFirst().orElse(null);
        }

        Steps(Workflow workflow, String nodeId) {
            this.operationSteps = getNodeOperationSteps(workflow, nodeId);
            this.stateSteps = getNodeStateSteps(workflow, nodeId);
            this.delegateStep = getDelegateStep(workflow, nodeId);
        }

        Steps(Map<String, WorkflowStep> operationSteps, Map<String, WorkflowStep> stateSteps, WorkflowStep delegateStep) {
            this.operationSteps = operationSteps;
            this.stateSteps = stateSteps;
            this.delegateStep = delegateStep;
        }

        WorkflowStep getStateStep(String stateName) {
            WorkflowStep stateStep = stateSteps.get(stateName);
            if (stateStep == null) {
                return delegateStep;
            } else {
                return stateStep;
            }
        }

        WorkflowStep getOperationStep(String operationName) {
            WorkflowStep operationStep = operationSteps.get(operationName);
            if (operationStep == null) {
                return delegateStep;
            } else {
                return operationStep;
            }
        }
    }

    private DefaultDeclarativeWorkflows defaultDeclarativeWorkflows;

    DefaultWorkflowBuilder(DefaultDeclarativeWorkflows defaultDeclarativeWorkflows) {
        this.defaultDeclarativeWorkflows = defaultDeclarativeWorkflows;
    }

    private void declareStepDependencies(OperationDeclarativeWorkflow stepDependencies, WorkflowStep currentStep, Steps nodeSteps) {
        if (stepDependencies == null) {
            // The step has no dependencies
            return;
        }
        // Based on the dependencies configuration, link steps
        safe(stepDependencies.getFollowingOperations()).forEach(followingOperation -> {
            // We suppose that the configuration is correct and all reference must exist
            WorkflowUtils.linkSteps(currentStep, nodeSteps.getOperationStep(followingOperation));
        });
        safe(stepDependencies.getPrecedingOperations()).forEach(precedingOperation -> {
            // We suppose that the configuration is correct and all reference must exist
            WorkflowUtils.linkSteps(nodeSteps.getOperationStep(precedingOperation), currentStep);
        });
        String followingState = stepDependencies.getFollowingState();
        if (StringUtils.isNotBlank(followingState)) {
            WorkflowUtils.linkSteps(currentStep, nodeSteps.getStateStep(followingState));
        }
        String precedingState = stepDependencies.getPrecedingState();
        if (StringUtils.isNotBlank(precedingState)) {
            WorkflowUtils.linkSteps(nodeSteps.getStateStep(precedingState), currentStep);
        }
    }

    private void declareWeaving(RelationshipWeaving weaving, Steps fromSteps, Steps toSteps) {
        if (weaving == null) {
            return;
        }
        safe(weaving.getStates())
                .forEach((stateName, stateDependencies) -> declareStepDependencies(stateDependencies, fromSteps.getStateStep(stateName), toSteps));
        safe(weaving.getOperations()).forEach(
                (operationName, operationDependencies) -> declareStepDependencies(operationDependencies, fromSteps.getOperationStep(operationName), toSteps));
    }

    @Override
    public void addNode(Workflow wf, String nodeId, WorkflowsBuilderService.TopologyContext toscaTypeFinder, boolean isCompute) {
        if (WorkflowUtils.isNativeOrSubstitutionNode(nodeId, toscaTypeFinder)) {
            // for a native node, we just add a sub-workflow step
            WorkflowUtils.addDelegateWorkflowStep(wf, nodeId);
        } else {
            NodeDeclarativeWorkflow nodeDeclarativeWorkflow = defaultDeclarativeWorkflows.getNodeWorkflows().get(wf.getName());
            // only trigger this method if it's a default workflow
            if (nodeDeclarativeWorkflow != null) {

                // Create all the states of the workflow at first
                Map<String, WorkflowStep> statesSteps = safe(nodeDeclarativeWorkflow.getStates()).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, stateEntry -> WorkflowUtils.addStateStep(wf, nodeId, stateEntry.getKey())));

                // Create all the operations of the workflow at first
                Map<String, WorkflowStep> operationSteps = safe(nodeDeclarativeWorkflow.getOperations()).entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        operationEntry -> WorkflowUtils.addOperationStep(wf, nodeId, ToscaNodeLifecycleConstants.STANDARD_SHORT, operationEntry.getKey())));
                Steps steps = new Steps(operationSteps, statesSteps, null);
                // Declare dependencies on the states steps
                safe(nodeDeclarativeWorkflow.getStates())
                        .forEach((stateName, stateDependencies) -> declareStepDependencies(stateDependencies, steps.getStateStep(stateName), steps));

                // Declare dependencies on the operation steps
                safe(nodeDeclarativeWorkflow.getOperations()).forEach(
                        (operationName, operationDependencies) -> declareStepDependencies(operationDependencies, steps.getOperationStep(operationName), steps));
            }
        }
    }

    private RelationshipWeavingDeclarativeWorkflow getRelationshipWeavingDeclarativeWorkflow(String relationshipTypeName,
            WorkflowsBuilderService.TopologyContext toscaTypeFinder, String workflowName) {
        RelationshipType indexedRelationshipType = toscaTypeFinder.findElement(RelationshipType.class, relationshipTypeName);
        List<String> typesToCheck = new ArrayList<>();
        typesToCheck.add(indexedRelationshipType.getElementId());
        if (indexedRelationshipType.getDerivedFrom() != null) {
            typesToCheck.addAll(indexedRelationshipType.getDerivedFrom());
        }
        Map<String, Map<String, RelationshipWeavingDeclarativeWorkflow>> weavingConfigsPerRelationshipType = defaultDeclarativeWorkflows
                .getRelationshipsWeaving();
        for (String typeToCheck : typesToCheck) {
            if (weavingConfigsPerRelationshipType.containsKey(typeToCheck)) {
                return weavingConfigsPerRelationshipType.get(typeToCheck).get(workflowName);
            }
        }
        // This will never happen if the declarative configuration has tosca.relationships.Root configured
        throw new IllegalStateException("Default declarative configuration for workflow must have tosca.relationships.Root configured");
    }

    @Override
    public void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate,
            WorkflowsBuilderService.TopologyContext toscaTypeFinder) {
        if (!WorkflowUtils.isNativeOrSubstitutionNode(nodeId, toscaTypeFinder)) {
            // for native types we don't care about relation ships in workflows
            RelationshipDeclarativeWorkflow relationshipDeclarativeWorkflow = defaultDeclarativeWorkflows.getRelationshipWorkflows().get(wf.getName());
            // only trigger this method if it's a default workflow
            if (relationshipDeclarativeWorkflow != null) {
                Map<String, WorkflowStep> relationshipOperationSteps = safe(relationshipDeclarativeWorkflow.getOperations()).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, operationEntry -> WorkflowUtils.addRelationshipOperationStep(wf, nodeId,
                                relationshipTemplate.getName(), ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT, operationEntry.getKey())));
                Steps sourceSteps = new Steps(wf, nodeId);
                Steps targetSteps = new Steps(wf, relationshipTemplate.getTarget());

                safe(relationshipDeclarativeWorkflow.getOperations()).forEach((relationshipOperationName, relationshipOperationDependencies) -> {
                    WorkflowStep currentStep = relationshipOperationSteps.get(relationshipOperationName);
                    declareStepDependencies(relationshipOperationDependencies.getSource(), currentStep, sourceSteps);
                    declareStepDependencies(relationshipOperationDependencies.getTarget(), currentStep, targetSteps);
                    declareStepDependencies(relationshipOperationDependencies, currentStep,
                            new Steps(relationshipOperationSteps, Collections.emptyMap(), null));
                });
                RelationshipWeavingDeclarativeWorkflow relationshipWeavingDeclarativeWorkflow = getRelationshipWeavingDeclarativeWorkflow(
                        relationshipTemplate.getType(), toscaTypeFinder, wf.getName());
                declareWeaving(relationshipWeavingDeclarativeWorkflow.getSource(), sourceSteps, targetSteps);
                declareWeaving(relationshipWeavingDeclarativeWorkflow.getTarget(), targetSteps, sourceSteps);
            }
        }
    }
}
