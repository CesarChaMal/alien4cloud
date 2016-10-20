package alien4cloud.tosca.parser.impl.advanced;

import java.util.HashSet;
import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.ToscaNormativeImports;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
public class ToscaDefinitionVersionParser implements INodeParser<String> {
    @Override
    public String parse(Node node, ParsingContextExecution context) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getParent();
        String toscaDefinitionVersion = ParserUtils.getScalar(node, context);
        if (toscaDefinitionVersion != null) {
            CSARDependency dependency = ToscaNormativeImports.IMPORTS.get(toscaDefinitionVersion);
            if (dependency != null) {
                Set<CSARDependency> dependencies = archiveRoot.getArchive().getDependencies();
                if (dependencies == null) {
                    dependencies = new HashSet<>();
                    archiveRoot.getArchive().setDependencies(dependencies);
                }

                // Normative imports are automatically injected and supposed to be accessible, no specific validation is performed here.
                ToscaContext.get().addDependency(dependency);
                dependencies.add(dependency);
            }
        }
        return toscaDefinitionVersion;
    }
}
