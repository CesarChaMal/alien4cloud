package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.FilterUtil.singleKeyFilter;
import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import alien4cloud.common.AlienConstants;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.VersionUtil;

/**
 * Service responsible for indexing and updating topologies.
 */
@Service
public class TopologyCatalogService extends AbstractToscaIndexSearchService<Topology> {
    @Inject
    private ArchiveIndexer archiveIndexer;

    /**
     * Creates a topology and register it as a template in the catalog
     *
     * @param name The name of the topology template
     * @param description The description of the topology template
     * @param version The version of the topology
     * @param fromTopologyId The id of an existing topology to use to create the new topology.
     * @return The @{@link Topology} newly created
     */
    public Topology createTopologyAsTemplate(String name, String description, String version, String fromTopologyId) {
        // Every version of a topology template has a Cloud Service Archive
        Csar csar = new Csar(name, StringUtils.isNotBlank(version) ? version : VersionUtil.DEFAULT_VERSION_NAME);
        csar.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        csar.setDelegateType(ArchiveDelegateType.CATALOG.toString());
        csar.setDescription("Enclosing archive for topology: " + description);

        Topology topology;
        if (fromTopologyId != null) { // "cloning" the topology
            topology = alienDAO.findById(Topology.class, fromTopologyId);
        } else {
            topology = new Topology();
        }
        topology.setDescription(description);
        topology.setArchiveName(csar.getName());
        topology.setArchiveVersion(csar.getVersion());
        topology.setWorkspace(csar.getWorkspace());

        archiveIndexer.importNewArchive(csar, topology);

        return topology;
    }

    @Override
    protected Topology[] getArray(int size) {
        return new Topology[size];
    }

    @Override
    protected String getAggregationField() {
        return "archiveName";
    }

    /**
     * Get all topologies matching the given set of filters.
     *
     * @param filters The filters to query the topologies.
     * @param archiveName The name of the related archive
     * @return Return the matching
     */
    public Topology[] getAll(Map<String, String[]> filters, String archiveName) {
        return alienDAO.buildQuery(Topology.class)
                .setFilters(fromKeyValueCouples(filters, "workspace", AlienConstants.GLOBAL_WORKSPACE_ID, "archiveName", archiveName)).prepareSearch()
                .setFetchContext(SUMMARY).search(0, Integer.MAX_VALUE).getData();
    }

    /**
     * Get a single topology from it's id or throw a NotFoundException.
     * 
     * @param id The id of the topology to look for.
     * @return The topology matching the requested id.
     */
    public Topology getOrFail(String id) {
        Topology topology = get(id);
        if (topology == null) {
            throw new NotFoundException("Unable to find a topology with id <" + id + ">");
        }
        return topology;
    }

    public Topology get(String id) {
        return alienDAO.findById(Topology.class, id);
    }

    /**
     * Return true if the given id exists.
     *
     * @param id The id to check.
     * @return True if a topology with the given id exists, false if not.
     */
    public boolean exists(String id) {
        return alienDAO.buildQuery(Topology.class).setFilters(singleKeyFilter("id", id)).count() > 0;
    }
}