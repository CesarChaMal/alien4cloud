package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.ReinitializeWorkflowOperation;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.paas.wf.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link ReinitializeWorkflowOperation} operation
 * Reinitialize a workflow
 */
@Slf4j
@Component
public class ReinitializeWorkflowProcessor extends AbstractWorkflowProcessor<ReinitializeWorkflowOperation> {

    @Override
    protected void processWorkflowOperation(ReinitializeWorkflowOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        ensureStandard(workflow, "Non standard workflow <" + workflow.getName() + "> can not be reinitialized");
        log.debug("reinitializing workflow <{}> from topology <{}>", workflow.getName(), topology.getId());
        workflowBuilderService.reinitWorkflow(workflow.getName(), workflowBuilderService.buildTopologyContext(topology));
    }

}
