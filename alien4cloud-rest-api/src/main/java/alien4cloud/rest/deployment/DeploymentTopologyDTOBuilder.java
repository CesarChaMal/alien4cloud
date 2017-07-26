package alien4cloud.rest.deployment;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutor;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.OrchestratorDeploymentProperties;
import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.topology.TopologyDTOBuilder;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.deployment.model.DeploymentSubstitutionConfiguration;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationResourceTypes;
import alien4cloud.rest.deployment.DeploymentTopologyController.IDeploymentConfigAction;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.utils.ReflectionUtil;

/**
 * Construct a deployment topology dto for rest api and ui consumption.
 */
@Service
public class DeploymentTopologyDTOBuilder implements IDeploymentTopologyBuilder {
    @Inject
    private TopologyDTOBuilder topologyDTOBuilder;
    @Inject
    private FlowExecutor flowExecutor;
    @Inject
    private ILocationResourceService locationResourceService;
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;

    @Override
    @ToscaContextual
    public DeploymentTopologyDTO prepareDeployment(Topology topology, Application application, ApplicationEnvironment environment) {
        FlowExecutionContext executionContext = flowExecutor.executeDeploymentFlow(topology, application, environment);
        return build(executionContext);
    }

    @Override
    @ToscaContextual
    public DeploymentTopologyDTO prepareDeployment(Topology topology, Application application, ApplicationEnvironment environment,
            ApplicationTopologyVersion topologyVersion, IDeploymentConfigAction deploymentConfigAction) {
        // Execute the update
        deploymentConfigAction.execute(application, environment, topologyVersion, topology);

        FlowExecutionContext executionContext = flowExecutor.executeDeploymentFlow(topology, application, environment);
        return build(executionContext);
    }

    @Override
    @ToscaContextual
    public DeploymentTopologyDTO prepareDeployment(Topology topology, Supplier<FlowExecutionContext> contextSupplier) {
        return build(contextSupplier.get());
    }

    /**
     * Create a deployment topology dto from the context of the execution of a deployment flow.
     *
     * @param executionContext The deployment flow execution context.
     * @return The deployment topology.
     */
    private DeploymentTopologyDTO build(FlowExecutionContext executionContext) {
        // re-create the deployment topology object for api compatibility purpose
        DeploymentTopology deploymentTopology = new DeploymentTopology();
        ReflectionUtil.mergeObject(executionContext.getTopology(), deploymentTopology);
        deploymentTopology.setInitialTopologyId(executionContext.getTopology().getId());
        deploymentTopology.setEnvironmentId(executionContext.getEnvironmentContext().get().getEnvironment().getId());
        deploymentTopology.setVersionId(executionContext.getEnvironmentContext().get().getEnvironment().getTopologyVersion());

        DeploymentTopologyDTO deploymentTopologyDTO = new DeploymentTopologyDTO();
        topologyDTOBuilder.initTopologyDTO(deploymentTopology, deploymentTopologyDTO);

        // Convert log result to validation result.
        TopologyValidationResult validationResult = new TopologyValidationResult();
        for (AbstractTask task : executionContext.getLog().getWarnings()) {
            validationResult.addWarning(task);
        }
        for (AbstractTask task : executionContext.getLog().getErrors()) {
            validationResult.addTask(task);
        }
        validationResult.setValid(validationResult.getTaskList() == null || validationResult.getTaskList().isEmpty());
        deploymentTopologyDTO.setValidation(validationResult);

        Optional<DeploymentInputs> inputsOptional = executionContext.getConfiguration(DeploymentInputs.class,
                DeploymentTopologyDTOBuilder.class.getSimpleName());
        if (!inputsOptional.isPresent()) {
            deploymentTopology.setInputProperties(Maps.newHashMap());
            deploymentTopology.setUploadedInputArtifacts(Maps.newHashMap());
        } else {
            deploymentTopology.setInputProperties(inputsOptional.get().getInputs());
            deploymentTopology.setUploadedInputArtifacts(inputsOptional.get().getInputArtifacts());
        }

        Optional<DeploymentMatchingConfiguration> matchingConfigurationOptional = executionContext.getConfiguration(DeploymentMatchingConfiguration.class,
                DeploymentTopologyDTOBuilder.class.getSimpleName());
        if (!matchingConfigurationOptional.isPresent()) {
            return deploymentTopologyDTO;
        }
        DeploymentMatchingConfiguration matchingConfiguration = matchingConfigurationOptional.get();

        deploymentTopology.setOrchestratorId(matchingConfiguration.getOrchestratorId());
        deploymentTopology.setLocationGroups(matchingConfiguration.getLocationGroups());
        deploymentTopologyDTO.setLocationPolicies(matchingConfiguration.getLocationIds());
        // Good enough approximation as it doesn't contains just the location dependencies.
        deploymentTopology.setLocationDependencies(executionContext.getTopology().getDependencies());
        // used by ui to know if a property is editable. This should however be done differently with a better v2 api.
        deploymentTopology.setOriginalNodes((Map<String, NodeTemplate>) executionContext.getExecutionCache().get(FlowExecutionContext.MATCHING_ORIGINAL_NODES));
        deploymentTopology.setSubstitutedNodes(matchingConfiguration.getMatchedLocationResources());

        // Restrict the map of LocationResourceTemplate to the ones that are actually substituted after matching.
        Map<String, LocationResourceTemplate> allLocationResourcesTemplates = (Map<String, LocationResourceTemplate>) executionContext.getExecutionCache()
                .get(FlowExecutionContext.MATCHED_LOCATION_RESOURCE_TEMPLATES);
        Map<String, LocationResourceTemplate> substitutedLocationResourceTemplate = Maps.newHashMap(); //
        matchingConfiguration.getMatchedLocationResources().forEach((nodeKey, locationResourceId) -> substitutedLocationResourceTemplate.put(locationResourceId,
                allLocationResourcesTemplates.get(locationResourceId)));
        deploymentTopologyDTO.setLocationResourceTemplates(substitutedLocationResourceTemplate);

        DeploymentSubstitutionConfiguration substitutionConfiguration = new DeploymentSubstitutionConfiguration();

        substitutionConfiguration.setAvailableSubstitutions(
                (Map<String, Set<String>>) executionContext.getExecutionCache().get(FlowExecutionContext.MATCHED_LOCATION_RESOURCE_TEMPLATE_IDS_PER_NODE));
        substitutionConfiguration.setSubstitutionsTemplates(allLocationResourcesTemplates);
        // Fetch all required types associated with the location substitution templates.
        substitutionConfiguration
                .setSubstitutionTypes(locationResourceService.getLocationResourceTypes(safe(substitutionConfiguration.getSubstitutionsTemplates()).values()));
        enrichSubstitutionTypesWithServicesDependencies(safe(substitutionConfiguration.getSubstitutionsTemplates()).values(),
                substitutionConfiguration.getSubstitutionTypes());

        deploymentTopologyDTO.setAvailableSubstitutions(substitutionConfiguration);

        ApplicationEnvironment environment = executionContext.getEnvironmentContext().get().getEnvironment();
        OrchestratorDeploymentProperties orchestratorDeploymentProperties = executionContext
                .getConfiguration(OrchestratorDeploymentProperties.class, this.getClass().getSimpleName())
                .orElse(new OrchestratorDeploymentProperties(environment.getTopologyVersion(), environment.getId()));
        deploymentTopology.setProviderDeploymentProperties(orchestratorDeploymentProperties.getProviderDeploymentProperties());

        return deploymentTopologyDTO;
    }

    /**
     * Enrich {@link LocationResourceTypes} adding types coming from on demand service resources.
     */
    private void enrichSubstitutionTypesWithServicesDependencies(Collection<LocationResourceTemplate> resourceTemplates,
            LocationResourceTypes locationResourceTypes) {
        Set<String> serviceTypes = Sets.newHashSet();
        Set<CSARDependency> dependencies = Sets.newHashSet();
        for (LocationResourceTemplate resourceTemplate : resourceTemplates) {
            if (resourceTemplate.isService()) {
                String serviceId = resourceTemplate.getId();
                ServiceResource serviceResource = serviceResourceService.getOrFail(serviceId);

                NodeType nodeType = ToscaContext.get(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType());
                if (nodeType == null || !nodeType.getArchiveVersion().equals(serviceResource.getNodeInstance().getTypeVersion())) {
                    NodeType serviceType = toscaTypeSearchService.findOrFail(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType(),
                            serviceResource.getNodeInstance().getTypeVersion());
                    serviceTypes.add(serviceResource.getNodeInstance().getNodeTemplate().getType());
                    Csar csar = toscaTypeSearchService.getArchive(serviceType.getArchiveName(), serviceType.getArchiveVersion());
                    if (csar.getDependencies() != null) {
                        dependencies.addAll(csar.getDependencies());
                    }
                    dependencies.add(new CSARDependency(csar.getName(), csar.getVersion()));
                }
            }
        }
        locationResourceService.fillLocationResourceTypes(serviceTypes, locationResourceTypes, dependencies);
    }
}