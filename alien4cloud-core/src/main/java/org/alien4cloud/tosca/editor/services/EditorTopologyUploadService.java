package org.alien4cloud.tosca.editor.services;

import java.nio.file.Path;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.exception.EditorToscaYamlUpdateException;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.ToscaArchiveParser;

/**
 * Process the upload of a topology in the context of the editor.
 */
@Component
public class EditorTopologyUploadService {
    @Inject
    private ToscaArchiveParser toscaArchiveParser;
    @Inject
    private EditorArchivePostProcessor postProcessor;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;

    /**
     * Process the import of a topology archive or yaml in the context of the editor.
     *
     * @param archivePath The path of the yaml or archive.
     */
    public void processTopology(Path archivePath, String workspace) {
        // parse the archive.
        try {
            ParsingResult<ArchiveRoot> parsingResult = postProcessor.process(archivePath, toscaArchiveParser.parse(archivePath, true), workspace);

            // check if any blocker error has been found during parsing process.
            if (parsingResult.hasError(ParsingErrorLevel.ERROR)) {
                // do not save anything if any blocker error has been found during import.

                throw new EditorToscaYamlUpdateException("Uploaded yaml files is not a valid tosca template", parsingResult.getContext().getParsingErrors());
            }
            if (parsingResult.getResult().hasToscaTypes()) {
                throw new EditorToscaYamlUpdateException("Tosca types are currently not supported in the topology editor context.");
            }
            if (!parsingResult.getResult().hasToscaTopologyTemplate()) {
                throw new EditorToscaYamlUpdateException("A topology template is required in the topology edition context.");
            }
            // FIXME CHECK IF USER TRIED TO CHANGE CSAR ELEMENTS (name/version etc.) and throw exception (you cannot do that from the editor).

            Topology currentTopology = EditionContextManager.getTopology();
            Topology parsedTopology = parsingResult.getResult().getTopology();

            // Copy static elements from the topology
            parsedTopology.setId(currentTopology.getId());
            // Update editor tosca context
            ToscaContext.get().updateDependencies(parsedTopology.getDependencies());

            // init the workflows for the topology based on the yaml
            WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService
                    .buildCachedTopologyContext(new WorkflowsBuilderService.TopologyContext() {
                        @Override
                        public Topology getTopology() {
                            return parsedTopology;
                        }

                        @Override
                        public <T extends AbstractToscaType> T findElement(Class<T> clazz, String id) {
                            return ToscaContext.get(clazz, id);
                        }
                    });
            workflowBuilderService.initWorkflows(topologyContext);

            // update the topology in the edition context with the new one
            EditionContextManager.get().setTopology(parsingResult.getResult().getTopology());
        } catch (ParsingException e) {
            // Manage parsing error and dispatch them in the right editor exception
            throw new EditorToscaYamlUpdateException("The uploaded file to override the topology yaml is not a valid Tosca Yaml.");
        }
    }
}