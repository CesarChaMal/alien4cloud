package org.alien4cloud.tosca.editor.processors.relationshiptemplate;

import alien4cloud.model.topology.Topology;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AbstractRelationshipOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Abstract operation to get a required node template.
 */
public abstract class AbstractRelationshipProcessor<T extends AbstractRelationshipOperation> extends AbstractNodeProcessor<T> {

    @Override
    protected void processNodeOperation(Topology topology, T operation, NodeTemplate nodeTemplate) {
        RelationshipTemplate relationshipTemplate = safe(nodeTemplate.getRelationships()).get(operation.getRelationshipName());
        if (relationshipTemplate == null) {
            throw new NotFoundException("The relationship with name [" + operation.getRelationshipName() + "] do not exist for the node ["
                    + operation.getNodeName() + "] of the topology [" + EditionContextManager.getTopology().getId() + "]");
        }
        processRelationshipOperation(operation, nodeTemplate, relationshipTemplate);
    }

    protected abstract void processRelationshipOperation(T operation, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate);
}
