package org.alien4cloud.tosca.editor.processors.relationshiptemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.CapabilityBoundException;
import org.alien4cloud.tosca.editor.exception.RequirementBoundException;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.validation.TopologyCapabilityBoundsValidationServices;
import alien4cloud.topology.validation.TopologyRequirementBoundsValidationServices;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Component
public class AddRelationshipProcessor extends AbstractNodeProcessor<AddRelationshipOperation> {
    @Inject
    private IToscaTypeSearchService searchService;
    @Resource
    private TopologyService topologyService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Resource
    private TopologyRequirementBoundsValidationServices topologyRequirementBoundsValidationServices;
    @Resource
    private TopologyCapabilityBoundsValidationServices topologyCapabilityBoundsValidationServices;
    @Inject
    private ICsarService csarService;

    @Override
    protected void processNodeOperation(Topology topology, AddRelationshipOperation operation, NodeTemplate sourceNode) {
        if (operation.getRelationshipName() == null || operation.getRelationshipName().isEmpty()) {
            throw new InvalidNameException("relationshipName", operation.getRelationshipName(), "Not null or empty");
        }

        if (sourceNode.getRequirements() == null || sourceNode.getRequirements().get(operation.getRequirementName()) == null) {
            throw new NotFoundException(
                    "Unable to find requirement with name <" + operation.getRequirementName() + "> on the source node" + operation.getNodeName());
        }

        Topology topology = EditionContextManager.getTopology();
        Set<CSARDependency> oldDependencies = topology.getDependencies();
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        // ensure that the target node exists
        TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getTarget(), nodeTemplates);

        // We don't use the tosca context as the relationship type may not be in dependencies yet (that's why we use the load type below).
        RelationshipType indexedRelationshipType = searchService.find(RelationshipType.class, operation.getRelationshipType(),
                operation.getRelationshipVersion());
        if (indexedRelationshipType == null) {
            throw new NotFoundException(RelationshipType.class.getName(), operation.getRelationshipType() + ":" + operation.getRelationshipVersion(),
                    "Unable to find relationship type to create template in topology.");
        }

        boolean upperBoundReachedSource = topologyRequirementBoundsValidationServices.isRequirementUpperBoundReachedForSource(sourceNode,
                operation.getRequirementName(), topology.getDependencies());
        if (upperBoundReachedSource) {
            // throw exception here
            throw new RequirementBoundException(operation.getNodeName(), operation.getRequirementName());
        }

        boolean upperBoundReachedTarget = topologyCapabilityBoundsValidationServices.isCapabilityUpperBoundReachedForTarget(operation.getTarget(),
                nodeTemplates, operation.getTargetedCapabilityName(), topology.getDependencies());
        // return with a rest response error
        if (upperBoundReachedTarget) {
            throw new CapabilityBoundException(operation.getTarget(), operation.getTargetedCapabilityName());
        }

        // FIXME impact ToscaContext
        topologyService.loadType(topology, indexedRelationshipType);

        Map<String, RelationshipTemplate> relationships = sourceNode.getRelationships();
        if (relationships == null) {
            relationships = Maps.newHashMap();
            sourceNode.setRelationships(relationships);
        }
        if (relationships.containsKey(operation.getRelationshipName())) {
            throw new AlreadyExistException("Relationship " + operation.getRelationshipName() + " already exist on node " + operation.getNodeName());
        }

        RelationshipTemplate relationshipTemplate = new RelationshipTemplate();
        relationshipTemplate.setName(operation.getRelationshipName());
        relationshipTemplate.setTarget(operation.getTarget());
        relationshipTemplate.setTargetedCapabilityName(operation.getTargetedCapabilityName());
        relationshipTemplate.setRequirementName(operation.getRequirementName());
        relationshipTemplate.setRequirementType(sourceNode.getRequirements().get(operation.getRequirementName()).getType());
        relationshipTemplate.setType(indexedRelationshipType.getElementId());
        relationshipTemplate.setArtifacts(newLinkedHashMap(indexedRelationshipType.getArtifacts()));
        relationshipTemplate.setAttributes(newLinkedHashMap(indexedRelationshipType.getAttributes()));
        Map<String, AbstractPropertyValue> properties = new LinkedHashMap<String, AbstractPropertyValue>();
        NodeTemplateBuilder.fillProperties(properties, indexedRelationshipType.getProperties(), null);
        relationshipTemplate.setProperties(properties);

        relationships.put(operation.getRelationshipName(), relationshipTemplate);
        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);
        workflowBuilderService.addRelationship(topologyContext, operation.getNodeName(), operation.getRelationshipName());
        log.debug("Added relationship to the topology [" + topology.getId() + "], node name [" + operation.getNodeName() + "], relationship name ["
                + operation.getRelationshipName() + "]");
        // If dependencies changed then must update also CSAR dependencies
        if (!Objects.equals(topology.getDependencies(), oldDependencies)) {
            csarService.setDependencies(topology.getId(), topology.getDependencies());
        }
    }

    private <T, V> Map<T, V> newLinkedHashMap(Map<T, V> from) {
        if (from == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(from);
    }
}
