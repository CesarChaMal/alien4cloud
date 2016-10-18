package org.alien4cloud.tosca.editor.processors.nodetemplate.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import alien4cloud.model.topology.Topology;
import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeArtifactAsInputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.utils.InputArtifactUtil;

/**
 * Remove association from an artifact to an input.
 */
@Component
public class UnsetNodeArtifactAsInputProcessor extends AbstractNodeProcessor<UnsetNodeArtifactAsInputOperation> {

    @Override
    protected void processNodeOperation(Topology topology, UnsetNodeArtifactAsInputOperation operation, NodeTemplate nodeTemplate) {
        if (safe(nodeTemplate.getArtifacts()).containsKey(operation.getInputName())) {
            InputArtifactUtil.unsetInputArtifact(nodeTemplate.getArtifacts().get(operation.getInputName()));
        }
    }
}
